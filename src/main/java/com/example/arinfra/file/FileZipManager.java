package com.example.arinfra.file;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.validator.file.FileValidator;
import com.example.arinfra.validator.file.ZipEntryValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Secure file and directory compression/decompression manager. This component provides hardened ZIP
 * operations with comprehensive security controls to prevent common vulnerabilities.
 *
 * <p>Security Features:
 *
 * <ul>
 *   <li>Path Traversal Prevention: Validates all entry paths against directory traversal attacks
 *   <li>Zip Bomb Protection: Enforces size and compression ratio limits
 *   <li>DoS Prevention: Limits entry count and decompressed size
 *   <li>Information Disclosure Prevention: OWASP encoding for all logged paths
 *   <li>Cross-platform Support: Windows, Linux, macOS path handling
 *   <li>Symbolic Link Detection: Detects and rejects symbolic links using Unix attributes
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Compress a directory
 * File zipFile = fileZipManager.zipDirectory(sourceDir, "archive.zip");
 *
 * // Decompress with security validation
 * File extractedDir = fileZipManager.unzip(zipFile, targetDir);
 * }</pre>
 *
 * @see <a href="https://owasp.org/www-community/attacks/Path_Traversal">OWASP Path Traversal</a>
 * @see <a href="https://en.wikipedia.org/wiki/Zip_bomb">Zip Bomb</a>
 */
@Slf4j
@Component
@Validated
@InfraGenerated
@RequiredArgsConstructor
public class FileZipManager {

  private static final long BYTES_PER_KB = 1_024L;
  private static final long BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB;
  private static final long BYTES_PER_GB = BYTES_PER_KB * BYTES_PER_MB;

  private static final long MAX_DECOMPRESSED_SIZE = BYTES_PER_GB;
  private static final int BUFFER_SIZE = 8_192;
  private static final String ZIP_EXTENSION = ".zip";
  private static final String DEFAULT_ARCHIVE_NAME = "archive";
  private static final String ZIP_PREFIX = "zip-";
  private static final String UNZIP_PREFIX = "unzip-";
  private static final String UNIX_PATH_SEPARATOR = "/";

  private static final int SYMLINK_UNIX_MODE = 0120000;
  private static final int UNIX_FILE_TYPE_MASK = 0170000;

  private final SecureTempFileManager secureTempFileManager;
  private final FilenameSanitizer filenameSanitizer;
  private final FileValidator fileValidator;
  private final ZipEntryValidator zipEntryValidator;

  /**
   * Compresses a single file into a ZIP archive.
   *
   * @param sourceFile the file to compress
   * @param zipFileName the name of the output ZIP file
   * @return the created ZIP file
   * @throws IOException if compression fails
   */
  public File zipFile(
      @NotNull(message = "Source file cannot be null") File sourceFile,
      @NotBlank(message = "ZIP file name cannot be blank") String zipFileName)
      throws IOException {

    Path sourcePath = sourceFile.toPath();
    fileValidator.validateReadableFile(sourcePath);

    String sanitizedZipFileName = ensureZipExtension(filenameSanitizer.apply(zipFileName));
    File zipFile = secureTempFileManager.createSecureTempFile(ZIP_PREFIX, sanitizedZipFileName);

    try (ZipArchiveOutputStream zos = createZipOutputStream(zipFile.toPath())) {
      addFileToZip(zos, sourcePath, sourcePath.getFileName().toString());

      log.info(
          "Successfully zipped file: {} to {}",
          forJava(sourcePath.toString()),
          forJava(zipFile.getAbsolutePath()));
      return zipFile;
    } catch (IOException e) {
      secureTempFileManager.deleteTempFile(zipFile);
      log.error("Failed to zip file: {}", forJava(sourcePath.toString()), e);
      throw e;
    }
  }

  /**
   * Compresses a directory and all its contents into a ZIP archive.
   *
   * @param sourceDir the directory to compress
   * @param zipFileName the name of the output ZIP file
   * @return the created ZIP file
   * @throws IOException if compression fails
   */
  public File zipDirectory(
      @NotNull(message = "Source directory cannot be null") File sourceDir,
      @NotBlank(message = "ZIP file name cannot be blank") String zipFileName)
      throws IOException {

    Path sourcePath = sourceDir.toPath();
    fileValidator.validateReadableDirectory(sourcePath);

    String sanitizedZipFileName = ensureZipExtension(filenameSanitizer.apply(zipFileName));
    File zipFile = secureTempFileManager.createSecureTempFile(ZIP_PREFIX, sanitizedZipFileName);

    try (ZipArchiveOutputStream zos = createZipOutputStream(zipFile.toPath())) {
      AtomicLong entryCount = new AtomicLong(0);
      addDirectoryToZip(zos, sourcePath, entryCount);

      log.info(
          "Successfully zipped directory: {} ({} entries) to {}",
          forJava(sourcePath.toString()),
          entryCount.get(),
          forJava(zipFile.getAbsolutePath()));
      return zipFile;
    } catch (IOException | SecurityException e) {
      secureTempFileManager.deleteTempFile(zipFile);
      log.error("Failed to zip directory: {}", forJava(sourcePath.toString()), e);
      throw e;
    }
  }

  /**
   * Extracts a ZIP archive to a target directory with comprehensive security validation.
   *
   * @param zipFile the ZIP file to extract
   * @param targetDir the directory to extract files to (will be created if it doesn't exist)
   * @return the target directory containing extracted files
   * @throws IOException if extraction fails
   */
  public File unzip(
      @NotNull(message = "ZIP file cannot be null") File zipFile,
      @NotNull(message = "Target directory cannot be null") File targetDir)
      throws IOException {

    Path zipPath = zipFile.toPath();
    Path targetPath = targetDir.toPath();

    fileValidator.validateReadableFile(zipPath);

    if (!Files.exists(targetPath)) Files.createDirectories(targetPath);

    fileValidator.validateWritableDirectory(targetPath);

    ExtractionContext extractionContext =
        new ExtractionContext(targetPath.toRealPath(LinkOption.NOFOLLOW_LINKS));

    try (ZipFile zip = new ZipFile(zipFile)) {
      processZipEntries(zip, extractionContext);

      zipEntryValidator.validateTotalDecompressedSize(
          extractionContext.getTotalDecompressedSize().get());

      log.info(
          "Successfully unzipped {} ({} entries, {} bytes) to {}",
          forJava(zipPath.toString()),
          extractionContext.getEntryCount(),
          extractionContext.getTotalDecompressedSize().get(),
          forJava(targetPath.toString()));

      return targetDir;
    }
  }

  /**
   * Extracts a ZIP archive to a new secure temporary directory.
   *
   * @param zipFile the ZIP file to extract
   * @return the temporary directory containing extracted files
   * @throws IOException if extraction fails
   */
  public File unzipToTempDirectory(@NotNull(message = "ZIP file cannot be null") File zipFile)
      throws IOException {

    fileValidator.validateReadableFile(zipFile.toPath());
    File tempDir = secureTempFileManager.createSecureTempDirectory(UNZIP_PREFIX);
    return unzip(zipFile, tempDir);
  }

  private ZipArchiveOutputStream createZipOutputStream(Path zipPath) throws IOException {
    BufferedOutputStream bos =
        new BufferedOutputStream(Files.newOutputStream(zipPath), BUFFER_SIZE);
    ZipArchiveOutputStream zos = new ZipArchiveOutputStream(bos);
    zos.setLevel(ZipArchiveOutputStream.DEFLATED);
    return zos;
  }

  private void processZipEntries(ZipFile zip, ExtractionContext context) throws IOException {
    Enumeration<ZipArchiveEntry> entries = zip.getEntries();

    while (entries.hasMoreElements()) {
      ZipArchiveEntry entry = entries.nextElement();
      processZipEntry(zip, entry, context);
    }
  }

  private void processZipEntry(ZipFile zip, ZipArchiveEntry entry, ExtractionContext context)
      throws IOException {

    context.incrementEntryCount();
    zipEntryValidator.validateEntryCount(context.getEntryCount());
    zipEntryValidator.validateEntryName(entry.getName());
    zipEntryValidator.validateEntrySize(entry);

    if (isSymbolicLinkEntry(entry))
      throw new SecurityException(
          format("Symbolic link entries are not allowed: %s", forJava(entry.getName())));

    String sanitizedName = sanitizeEntryName(entry.getName());
    Path entryPath = context.resolveEntryPath(sanitizedName);

    zipEntryValidator.validatePathTraversal(entryPath, context.getTargetPath());
    zipEntryValidator.validateDuplicateEntry(sanitizedName, context.getExtractedPaths());

    if (entry.isDirectory()) createDirectory(entryPath);
    else extractFile(zip, entry, entryPath, context);

    context.addExtractedPath(sanitizedName);
  }

  /**
   * Detects if a ZIP entry is a symbolic link based on Unix file attributes.
   *
   * <p>This method checks the Unix mode stored in the ZIP entry's external attributes. Symbolic
   * links have the file type bits set to 0120000 (octal).
   *
   * @param entry the ZIP archive entry to check
   * @return true if the entry is a symbolic link, false otherwise
   */
  private boolean isSymbolicLinkEntry(ZipArchiveEntry entry) {
    int unixMode = entry.getUnixMode();

    if (unixMode == 0) return false;

    int fileType = unixMode & UNIX_FILE_TYPE_MASK;
    return fileType == SYMLINK_UNIX_MODE;
  }

  private void createDirectory(Path directoryPath) throws IOException {
    Files.createDirectories(directoryPath);
    log.debug("Created directory: {}", forJava(directoryPath.toString()));
  }

  private void extractFile(
      ZipFile zip, ZipArchiveEntry entry, Path entryPath, ExtractionContext context)
      throws IOException {

    Files.createDirectories(entryPath.getParent());

    try (InputStream inputStream = zip.getInputStream(entry);
        BufferedInputStream bis = new BufferedInputStream(inputStream, BUFFER_SIZE);
        BufferedOutputStream bos =
            new BufferedOutputStream(Files.newOutputStream(entryPath), BUFFER_SIZE)) {

      long entrySize = copyWithSizeValidation(bis, bos, entry.getName(), context);
      log.debug("Extracted file: {} ({} bytes)", forJava(entryPath.toString()), entrySize);
    }
  }

  private long copyWithSizeValidation(
      InputStream input, BufferedOutputStream output, String entryName, ExtractionContext context)
      throws IOException {

    byte[] buffer = new byte[BUFFER_SIZE];
    long entrySize = 0;
    int bytesRead;

    while ((bytesRead = input.read(buffer)) != -1) {
      entrySize += bytesRead;
      context.addToTotalSize(bytesRead);

      zipEntryValidator.validateActualExtractedSize(entrySize, entryName);

      if (context.getTotalDecompressedSize().get() > MAX_DECOMPRESSED_SIZE)
        throw new SecurityException(
            "Total decompressed size exceeded during extraction. Possible zip bomb attack.");

      output.write(buffer, 0, bytesRead);
    }

    return entrySize;
  }

  private void addFileToZip(ZipArchiveOutputStream zos, Path filePath, String entryName)
      throws IOException {

    ZipArchiveEntry entry = new ZipArchiveEntry(filePath.toFile(), entryName);
    zos.putArchiveEntry(entry);

    try (InputStream inputStream = Files.newInputStream(filePath);
        BufferedInputStream bis = new BufferedInputStream(inputStream, BUFFER_SIZE)) {
      copyStream(bis, zos);
    }

    zos.closeArchiveEntry();
    log.debug("Added file to ZIP: {}", forJava(entryName));
  }

  private void copyStream(InputStream input, ZipArchiveOutputStream output) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = input.read(buffer)) != -1) {
      output.write(buffer, 0, bytesRead);
    }
  }

  private void addDirectoryToZip(ZipArchiveOutputStream zos, Path dirPath, AtomicLong entryCount)
      throws IOException {

    Collection<File> files = FileUtils.listFiles(dirPath.toFile(), null, true);

    if (files.isEmpty()) {
      log.warn("Directory is empty or not accessible: {}", forJava(dirPath.toString()));
      return;
    }

    Set<String> addedDirectories = new HashSet<>();

    for (File file : files) {
      entryCount.incrementAndGet();
      zipEntryValidator.validateEntryCount(entryCount.intValue());

      Path filePath = file.toPath();
      String entryName = calculateEntryName(filePath, dirPath);

      addParentDirectoriesIfNeeded(zos, entryName, addedDirectories);
      addFileToZip(zos, filePath, entryName);
    }
  }

  private void addParentDirectoriesIfNeeded(
      ZipArchiveOutputStream zos, String entryName, Set<String> addedDirectories)
      throws IOException {

    String[] pathParts = entryName.split(UNIX_PATH_SEPARATOR);

    for (int i = 0; i < pathParts.length - 1; i++) {
      StringBuilder currentPath = new StringBuilder();
      for (int j = 0; j <= i; j++) {
        if (j > 0) currentPath.append(UNIX_PATH_SEPARATOR);

        currentPath.append(pathParts[j]);
      }
      String dirPath = currentPath + UNIX_PATH_SEPARATOR;

      if (addedDirectories.add(dirPath)) addDirectoryEntry(zos, dirPath);
    }
  }

  private String calculateEntryName(Path filePath, Path basePath) {
    Path relativePath = basePath.relativize(filePath);
    return relativePath.toString().replace(File.separatorChar, UNIX_PATH_SEPARATOR.charAt(0));
  }

  private void addDirectoryEntry(ZipArchiveOutputStream zos, String entryName) throws IOException {
    ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
    zos.putArchiveEntry(entry);
    zos.closeArchiveEntry();
    log.debug("Added directory to ZIP: {}", forJava(entryName));
  }

  private String sanitizeEntryName(String entryName) {
    String normalized = entryName.replace('\\', '/');

    return Stream.of(normalized.split("/"))
        .map(filenameSanitizer)
        .reduce((a, b) -> a + "/" + b)
        .orElse(StringUtils.EMPTY);
  }

  private String ensureZipExtension(String fileName) {
    if (StringUtils.isBlank(fileName)) return DEFAULT_ARCHIVE_NAME + ZIP_EXTENSION;

    return fileName.toLowerCase().endsWith(ZIP_EXTENSION) ? fileName : fileName + ZIP_EXTENSION;
  }

  /** Context holder for extraction state to reduce parameter passing. */
  private static class ExtractionContext {
    @Getter private final Path targetPath;
    @Getter private final Set<String> extractedPaths = new HashSet<>();
    @Getter private final AtomicLong totalDecompressedSize = new AtomicLong(0);
    @Getter private int entryCount = 0;

    ExtractionContext(Path targetPath) {
      this.targetPath = targetPath;
    }

    void incrementEntryCount() {
      entryCount++;
    }

    void addToTotalSize(long size) {
      totalDecompressedSize.addAndGet(size);
    }

    void addExtractedPath(String path) {
      extractedPaths.add(path);
    }

    Path resolveEntryPath(String entryName) {
      return targetPath.resolve(entryName).normalize();
    }
  }
}
