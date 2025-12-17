package com.example.arInfra.file;

import static org.owasp.encoder.Encode.forJava;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Sanitizes filenames to prevent path traversal and injection attacks. Implements UnaryOperator for
 * functional composition and reusability.
 *
 * <p>Security features:
 *
 * <ul>
 *   <li>Removes directory path components
 *   <li>Eliminates directory traversal sequences
 *   <li>Applies strict character whitelist
 *   <li>Prevents hidden file exploitation
 *   <li>Enforces maximum length limits
 * </ul>
 *
 * @see <a href="https://owasp.org/www-community/attacks/Path_Traversal">OWASP Path Traversal</a>
 */
@Slf4j
@Component
public class FilenameSanitizer implements UnaryOperator<String> {

  private static final String DEFAULT_FILENAME = "upload.tmp";
  private static final String UNDERSCORE = "_";
  private static final String DOT = ".";
  private static final String EMPTY = "";

  private static final int MAX_FILENAME_LENGTH = 200;
  private static final int MAX_EXTENSION_LENGTH = 10;

  private static final String SAFE_CHAR_PATTERN = "[^a-zA-Z0-9._-]";

  private static final Set<Character> UNSAFE_CHARS = Set.of(';', '<', '>', '|', '&', '$', '`');
  private static final int MAX_PATH_LENGTH = 4_096;

  @Override
  public String apply(String originalFilename) {
    if (StringUtils.isBlank(originalFilename)) return handleNullOrBlank();

    String sanitized = originalFilename;

    boolean isLegitPath = isLegitimateFilePath(sanitized);

    if (isLegitPath) {
      sanitized = FilenameUtils.getName(sanitized);
      if (StringUtils.isBlank(sanitized)) return handleInvalidResult();

    } else {
      sanitized = sanitized.replace("..", EMPTY);
      sanitized = removeMaliciousSeparators(sanitized);
    }

    String normalized = FilenameUtils.normalize(sanitized);
    if (normalized != null) sanitized = normalized;

    sanitized = preventHiddenFile(sanitized);
    sanitized = applyCharacterWhitelist(sanitized);
    sanitized = cleanupConsecutiveDots(sanitized);
    sanitized = enforceMaxLength(sanitized);

    if (isInvalidAfterSanitization(sanitized)) return handleInvalidResult();

    logSanitization(originalFilename, sanitized);
    return sanitized;
  }

  /**
   * Determines if the input looks like a legitimate file path vs a malicious pattern. Legitimate:
   * C:\Users\Admin\file.txt, /home/user/document.pdf Malicious: ..\..\..\Windows\System32\cmd.exe,
   * file;rm -rf /
   */
  private boolean isLegitimateFilePath(String filename) {
    if (filename == null || filename.isEmpty() || filename.length() > MAX_PATH_LENGTH) return false;

    try {
      Path path = Paths.get(filename).normalize();
      String normalized = path.toString();

      if (normalized.contains("..")) return false;

      for (int i = 0; i < filename.length(); i++) {
        if (UNSAFE_CHARS.contains(filename.charAt(i))) return false;
      }

      return path.isAbsolute() || path.getNameCount() > 0;

    } catch (InvalidPathException e) {
      return false;
    }
  }

  /** Removes path separators from malicious patterns to flatten them. */
  private String removeMaliciousSeparators(String filename) {
    return filename.replace("\\", EMPTY).replace("/", EMPTY);
  }

  private String handleNullOrBlank() {
    log.warn("Filename is null or blank, using default: {}", DEFAULT_FILENAME);
    return DEFAULT_FILENAME;
  }

  /** Prevents hidden file exploitation by prefixing with underscore. */
  private String preventHiddenFile(String filename) {
    if (!filename.startsWith(DOT)) {
      return filename;
    }

    String withoutLeadingDots = filename.replaceAll("^\\.+", "");

    if (withoutLeadingDots.isEmpty()) return UNDERSCORE;

    boolean isExtensionOnly =
        withoutLeadingDots.length() >= 2
            && withoutLeadingDots.length() <= 5
            && withoutLeadingDots.matches("^[a-z0-9]+$|^[A-Z0-9]+$");

    if (isExtensionOnly) return UNDERSCORE + DOT + withoutLeadingDots;

    return UNDERSCORE + withoutLeadingDots;
  }

  /**
   * Applies strict whitelist allowing only safe characters: alphanumeric, underscore, hyphen, and
   * dot.
   */
  private String applyCharacterWhitelist(String filename) {
    return filename.replaceAll(SAFE_CHAR_PATTERN, UNDERSCORE);
  }

  /** Removes consecutive dots that may appear after sanitization. */
  private String cleanupConsecutiveDots(String filename) {
    while (filename.contains("..")) {
      filename = filename.replace("..", DOT);
    }
    return filename;
  }

  /** Enforces maximum filename length while preserving file extension. */
  private String enforceMaxLength(String filename) {
    if (filename.length() <= MAX_FILENAME_LENGTH) {
      return filename;
    }

    String extension = FilenameUtils.getExtension(filename);

    if (StringUtils.isNotEmpty(extension) && extension.length() <= MAX_EXTENSION_LENGTH) {
      String baseName = FilenameUtils.getBaseName(filename);
      int maxBaseLength = MAX_FILENAME_LENGTH - extension.length() - 1; // -1 for the dot
      return baseName.substring(0, Math.min(baseName.length(), maxBaseLength)) + DOT + extension;
    }

    return filename.substring(0, MAX_FILENAME_LENGTH);
  }

  private boolean isInvalidAfterSanitization(String filename) {
    return StringUtils.isBlank(filename)
        || filename.equals(UNDERSCORE)
        || filename.equals(DOT)
        || filename.chars().allMatch(c -> c == '_');
  }

  private String handleInvalidResult() {
    log.warn("Filename became invalid after sanitization, using default: {}", DEFAULT_FILENAME);
    return DEFAULT_FILENAME;
  }

  private void logSanitization(String original, String sanitized) {
    if (!original.equals(sanitized))
      log.debug(
          "Filename sanitized: original={}, sanitized={}", forJava(original), forJava(sanitized));
  }
}
