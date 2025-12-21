package com.example.arinfra.file;

import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.exception.MultipartFileConversionException;
import java.io.File;
import java.io.IOException;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Converter for transforming MultipartFile objects into secure temporary File objects. Uses
 * SecureTempFileManager to ensure files are created with restricted permissions.
 */
@Slf4j
@Component
@InfraGenerated
@AllArgsConstructor
public class MultipartFileConverter implements Function<MultipartFile, File> {

  private static final String UPLOAD_PREFIX = "upload-";
  private static final String DEFAULT_EXTENSION = "tmp";
  private static final int MAX_EXTENSION_LENGTH = 10;

  private final SecureTempFileManager secureTempFileManager;

  /**
   * Converts a MultipartFile to a secure temporary File.
   *
   * @param multipartFile the multipart file to convert
   * @return a secure temporary file containing the multipart file data
   * @throws MultipartFileConversionException if conversion fails
   */
  @Override
  public File apply(MultipartFile multipartFile) {
    String originalFilename = multipartFile.getOriginalFilename();
    log.debug("Converting multipart file to File: filename={}", forJava(originalFilename));

    try {
      String safeSuffix = "." + extractSafeFileExtension(originalFilename);
      File tempFile = secureTempFileManager.createSecureTempFile(UPLOAD_PREFIX, safeSuffix);

      multipartFile.transferTo(tempFile);

      log.debug("File conversion successful: tempFile={}", forJava(tempFile.getAbsolutePath()));
      return tempFile;

    } catch (IOException e) {
      log.error("Failed to convert multipart file: filename={}", forJava(originalFilename), e);
      throw new MultipartFileConversionException("Failed to convert multipart file", e);
    }
  }

  /**
   * Extracts and sanitizes file extension using Apache Commons IO. FilenameUtils.getExtension()
   * automatically: - Removes path traversal attempts (../, /, \) - Extracts only the extension part
   * - Returns empty string for invalid filenames
   *
   * @param filename the original filename
   * @return a safe file extension without dot, or default extension
   */
  private String extractSafeFileExtension(String filename) {
    if (filename == null || filename.isBlank()) {
      log.warn("Invalid filename, using default extension");
      return DEFAULT_EXTENSION;
    }

    // FilenameUtils.getExtension() is secure - it strips path components
    // and only returns the actual extension (without the dot)
    String extension = FilenameUtils.getExtension(filename);

    if (extension.isBlank()) {
      log.warn("No extension found, using default: filename={}", forJava(filename));
      return DEFAULT_EXTENSION;
    }

    // Additional safety: validate extension length and characters
    if (extension.length() > MAX_EXTENSION_LENGTH) {
      log.warn("Extension too long, using default: extension={}", forJava(extension));
      return DEFAULT_EXTENSION;
    }

    // Only allow alphanumeric extensions (no special characters)
    if (!extension.matches("^[a-zA-Z0-9]+$")) {
      log.warn(
          "Extension contains invalid characters, using default: extension={}", forJava(extension));
      return DEFAULT_EXTENSION;
    }

    log.debug("Safe extension extracted: extension={}", forJava(extension));
    return extension.toLowerCase();
  }
}
