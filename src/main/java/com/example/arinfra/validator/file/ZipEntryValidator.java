package com.example.arinfra.validator.file;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.validator.Validator;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Validator for ZIP entry security checks using Path-based validation.
 *
 * <p>Security protections:
 *
 * <ul>
 *   <li>Normalizes paths before validation
 *   <li>Uses safe path validation without direct user input to Paths.get()
 *   <li>Detects and rejects symbolic link entries
 *   <li>Validates actual extracted size, not just metadata
 *   <li>Enforces limits during stream extraction
 *   <li>Handles unknown entry sizes (-1) securely
 *   <li>Protects against zip bomb attacks (CWE-409)
 * </ul>
 *
 * @see <a href="https://owasp.org/www-community/attacks/Zip_Bomb">OWASP Zip Bomb</a>
 * @see <a href="https://cwe.mitre.org/data/definitions/409.html">CWE-409</a>
 */
@Component
public class ZipEntryValidator implements Validator<ZipEntry> {

  private static final long BYTES_PER_KB = 1_024L;
  private static final long BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB;
  private static final long BYTES_PER_GB = BYTES_PER_KB * BYTES_PER_MB;

  private static final long MAX_DECOMPRESSED_SIZE = BYTES_PER_GB;
  private static final long MAX_ENTRY_SIZE = 512L * BYTES_PER_MB;
  private static final int MAX_ENTRY_COUNT = 10_000;
  private static final int MAX_COMPRESSION_RATIO = 100;
  private static final int MAX_PATH_LENGTH = 4_096;
  private static final int SUBSTRING_LIMIT_FOR_ERROR = 100;

  private static final long MIN_COMPRESSED_SIZE_FOR_RATIO_CHECK = 1024L;

  @Override
  public void validate(@NotNull ZipEntry entry) {
    validateEntryName(entry.getName());
    validateEntrySize(entry);
  }

  @Override
  public Class<ZipEntry> getValidatedType() {
    return ZipEntry.class;
  }

  public void validateEntryCount(int count) {
    if (count > MAX_ENTRY_COUNT)
      throw new SecurityException(
          format(
              "ZIP contains too many entries (limit: %d, found: %d). Possible zip bomb attack.",
              MAX_ENTRY_COUNT, count));
  }

  public void validateEntryName(String entryName) {
    validateEntryNameNotBlank(entryName);
    validateEntryNameLength(entryName);
    validateSafeCharacters(entryName);

    String normalizedPath = normalizePath(entryName);

    validateNotAbsolutePath(entryName, normalizedPath);
    validateNoPathTraversal(normalizedPath);
    validateNoSymbolicLink(entryName);
  }

  /**
   * Validates entry size for standard java.util.zip.ZipEntry.
   *
   * <p>This method handles the following security concerns:
   *
   * <ul>
   *   <li>Unknown sizes (getSize() returns -1): Rejects entries with unknown sizes as they cannot
   *       be validated upfront and pose a security risk
   *   <li>Excessive declared sizes: Prevents memory exhaustion attacks
   *   <li>Suspicious compression ratios: Detects potential zip bombs
   * </ul>
   *
   * <p><strong>Note:</strong> This is only the first line of defense. The actual extracted size
   * MUST be validated during extraction using {@link #validateActualExtractedSize}.
   *
   * @param entry the ZIP entry to validate
   * @throws SecurityException if the entry fails validation
   */
  public void validateEntrySize(ZipEntry entry) {
    validateEntrySizeInternal(entry.getSize(), entry.getCompressedSize(), entry.getName());
  }

  /**
   * Validates entry size for Apache Commons Compress ZipArchiveEntry.
   *
   * <p>This method handles the following security concerns:
   *
   * <ul>
   *   <li>Unknown sizes (getSize() returns -1): Rejects entries with unknown sizes as they cannot
   *       be validated upfront and pose a security risk
   *   <li>Excessive declared sizes: Prevents memory exhaustion attacks
   *   <li>Suspicious compression ratios: Detects potential zip bombs
   * </ul>
   *
   * <p><strong>Note:</strong> This is only the first line of defense. The actual extracted size
   * MUST be validated during extraction using {@link #validateActualExtractedSize}.
   *
   * @param entry the ZIP archive entry to validate
   * @throws SecurityException if the entry fails validation
   */
  public void validateEntrySize(ZipArchiveEntry entry) {
    validateEntrySizeInternal(entry.getSize(), entry.getCompressedSize(), entry.getName());
  }

  /**
   * Internal helper method to validate ZIP entry size parameters.
   *
   * <p>This method performs the actual validation logic for both {@link ZipEntry} and {@link
   * ZipArchiveEntry} types, eliminating code duplication.
   *
   * @param declaredSize the declared uncompressed size
   * @param compressedSize the compressed size
   * @param entryName the entry name for error reporting
   * @throws SecurityException if validation fails
   */
  private void validateEntrySizeInternal(long declaredSize, long compressedSize, String entryName) {
    if (declaredSize < 0)
      throw new SecurityException(
          format(
              "ZIP entry has unknown size (getSize() returned -1). Cannot safely validate: %s",
              forJava(entryName)));

    if (declaredSize > MAX_ENTRY_SIZE)
      throw new SecurityException(
          format(
              "ZIP entry declares size too large (limit: %d bytes, declared: %d bytes): %s",
              MAX_ENTRY_SIZE, declaredSize, forJava(entryName)));

    validateCompressionRatio(declaredSize, compressedSize, entryName);
  }

  public void validatePathTraversal(Path entryPath, Path targetPath) {
    Path normalizedEntryPath = entryPath.normalize();
    Path normalizedTargetPath = targetPath.normalize();

    if (!normalizedEntryPath.startsWith(normalizedTargetPath))
      throw new SecurityException(
          format(
              "Path traversal attempt detected. Entry would extract outside target directory: %s",
              forJava(normalizedEntryPath.toString())));
  }

  public void validateDuplicateEntry(String entryName, Set<String> extractedPaths) {
    if (extractedPaths.contains(entryName))
      throw new SecurityException(format("Duplicate ZIP entry detected: %s", forJava(entryName)));
  }

  public void validateTotalDecompressedSize(long totalSize) {
    if (totalSize > MAX_DECOMPRESSED_SIZE)
      throw new SecurityException(
          format(
              "Total decompressed size exceeds limit (limit: %d bytes, actual: %d bytes). Possible"
                  + " zip bomb attack.",
              MAX_DECOMPRESSED_SIZE, totalSize));
  }

  /**
   * Validates the actual extracted size during extraction.
   *
   * <p><strong>Critical:</strong> This method MUST be called during extraction to validate the
   * actual bytes being extracted, as the declared size in ZIP metadata can be manipulated or
   * incorrect.
   *
   * <p>This is the primary defense against zip bombs where the declared size differs from the
   * actual extracted size.
   *
   * @param extractedSize the actual number of bytes extracted so far
   * @param entryName the name of the entry being extracted
   * @throws SecurityException if the actual extracted size exceeds the limit
   */
  public void validateActualExtractedSize(long extractedSize, String entryName) {
    if (extractedSize > MAX_ENTRY_SIZE)
      throw new SecurityException(
          format(
              "Entry size exceeded during extraction: %s (limit: %d bytes, actual: %d bytes)",
              forJava(entryName), MAX_ENTRY_SIZE, extractedSize));
  }

  private void validateEntryNameNotBlank(String entryName) {
    if (StringUtils.isBlank(entryName))
      throw new SecurityException("ZIP entry name cannot be null or empty");
  }

  private void validateEntryNameLength(String entryName) {
    if (entryName.length() > MAX_PATH_LENGTH) {
      String truncated = StringUtils.substring(entryName, 0, SUBSTRING_LIMIT_FOR_ERROR);
      throw new SecurityException(
          format("ZIP entry name too long (limit: %d): %s", MAX_PATH_LENGTH, forJava(truncated)));
    }
  }

  private void validateSafeCharacters(String entryName) {
    if (entryName.contains("\0"))
      throw new SecurityException(
          format("ZIP entry name contains null byte: %s", forJava(entryName)));

    for (char c : entryName.toCharArray()) {
      if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')
        throw new SecurityException(
            format("ZIP entry name contains control character: %s", forJava(entryName)));
    }
  }

  private String normalizePath(String entryName) {
    String normalized = entryName.replace('\\', '/');

    String[] components = normalized.split("/");
    StringBuilder validatedPath = new StringBuilder();

    for (String component : components) {
      switch (component) {
        case "", "." -> {
          continue;
        }
        case ".." ->
            throw new SecurityException(
                format("ZIP entry contains path traversal sequence: %s", forJava(entryName)));
      }

      if (!validatedPath.isEmpty()) validatedPath.append("/");

      validatedPath.append(component);
    }

    return validatedPath.toString();
  }

  private void validateNotAbsolutePath(String originalPath, String normalizedPath) {
    if (originalPath.startsWith("/"))
      throw new SecurityException(
          format("ZIP entry uses absolute path: %s", forJava(originalPath)));

    if (originalPath.length() >= 2 && originalPath.charAt(1) == ':') {
      char drive = originalPath.charAt(0);
      if ((drive >= 'A' && drive <= 'Z') || (drive >= 'a' && drive <= 'z'))
        throw new SecurityException(
            format("ZIP entry uses absolute path: %s", forJava(originalPath)));
    }
  }

  private void validateNoPathTraversal(String normalizedPath) {
    if (normalizedPath.contains(".."))
      throw new SecurityException(
          format(
              "ZIP entry contains path traversal sequence after normalization: %s",
              forJava(normalizedPath)));

    String lowerPath = normalizedPath.toLowerCase();
    if (lowerPath.contains("%2e%2e") || lowerPath.contains("%252e"))
      throw new SecurityException(
          format("ZIP entry contains encoded path traversal: %s", forJava(normalizedPath)));
  }

  private void validateNoSymbolicLink(String entryName) {
    if (entryName.contains("->"))
      throw new SecurityException(
          format("ZIP entry appears to be a symbolic link: %s", forJava(entryName)));
  }

  /**
   * Validates compression ratio to detect potential zip bombs.
   *
   * <p>A zip bomb is a malicious archive that has a very high compression ratio, causing massive
   * data amplification when extracted. For example, a 42KB zip file can expand to 4.5 petabytes.
   *
   * <p>This method calculates the compression ratio and rejects entries that exceed the maximum
   * allowed ratio. Only entries with meaningful compressed sizes are checked to avoid false
   * positives on small files.
   *
   * @param declaredSize the declared uncompressed size
   * @param compressedSize the compressed size
   * @param entryName the entry name for error reporting
   * @throws SecurityException if the compression ratio is suspicious
   */
  private void validateCompressionRatio(long declaredSize, long compressedSize, String entryName) {
    if (compressedSize <= 0 || declaredSize <= 0) return;

    if (compressedSize < MIN_COMPRESSED_SIZE_FOR_RATIO_CHECK) return;

    long ratio = declaredSize / compressedSize;

    if (ratio > MAX_COMPRESSION_RATIO) {
      throw new SecurityException(
          format(
              "ZIP entry has suspicious compression ratio (%d:1, limit: %d:1, declared: %d bytes,"
                  + " compressed: %d bytes): %s. Possible zip bomb attack.",
              ratio, MAX_COMPRESSION_RATIO, declaredSize, compressedSize, forJava(entryName)));
    }
  }
}
