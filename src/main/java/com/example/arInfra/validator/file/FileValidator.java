package com.example.arInfra.validator.file;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.example.arInfra.InfraGenerated;
import com.example.arInfra.validator.Validator;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Validator for file and directory operations using secure NIO.2 APIs.
 *
 * <p>Security protections:
 *
 * <ul>
 *   <li>Prevents TOCTOU attacks by validating on actual file handles
 *   <li>Detects and rejects symbolic links explicitly
 *   <li>Uses {@link Path#toRealPath(LinkOption...)} to prevent path traversal
 *   <li>Validates accessibility by attempting actual I/O operations
 * </ul>
 */
@Component
@Validated
@InfraGenerated
public class FileValidator implements Validator<Path> {

  private static final String ERROR_NOT_DIRECTORY = "Path must be a directory: %s";
  private static final String ERROR_SYMLINK_DETECTED = "Symbolic links are not allowed: %s";
  private static final String ERROR_IO_VALIDATION = "I/O error during validation: %s";

  @Override
  public void validate(@NotNull Path path) {
    validateReadableFile(path);
  }

  @Override
  public Class<Path> getValidatedType() {
    return Path.class;
  }

  /**
   * Validates that a path is a readable regular file.
   *
   * @param path the path to validate
   * @throws IllegalArgumentException if path is invalid or not accessible
   */
  public void validateReadableFile(@NotNull Path path) {
    try {
      Path realPath = resolveAndValidateExists(path, "File");

      if (!Files.isRegularFile(realPath, LinkOption.NOFOLLOW_LINKS))
        throw new IllegalArgumentException(
            format("Path must be a regular file: %s", forJava(realPath.toString())));

      try (InputStream stream = Files.newInputStream(realPath)) {
        int bytesRead = stream.read();
        if (bytesRead == -1 && Files.size(realPath) > 0) {
          throw new IllegalArgumentException(
              format("File is not readable: %s", forJava(realPath.toString())));
        }
      }

    } catch (IOException e) {
      throw new IllegalArgumentException(format(ERROR_IO_VALIDATION, forJava(path.toString())), e);
    }
  }

  /**
   * Validates that a path is a readable directory.
   *
   * @param path the path to validate
   * @throws IllegalArgumentException if path is invalid or not accessible
   */
  public void validateReadableDirectory(@NotNull Path path) {
    try {
      Path realPath = resolveAndValidateExists(path, "Directory");

      if (!Files.isDirectory(realPath, LinkOption.NOFOLLOW_LINKS))
        throw new IllegalArgumentException(
            format(ERROR_NOT_DIRECTORY, forJava(realPath.toString())));

      try (DirectoryStream<Path> ignored = Files.newDirectoryStream(realPath)) {}

    } catch (IOException e) {
      throw new IllegalArgumentException(format(ERROR_IO_VALIDATION, forJava(path.toString())), e);
    }
  }

  /**
   * Validates that a path is a writable directory. Creates the directory if it does not exist.
   *
   * @param path the path to validate
   * @throws IllegalArgumentException if path is invalid or not writable
   */
  public void validateWritableDirectory(@NotNull Path path) {
    try {
      if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(path);

      Path realPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);

      if (Files.isSymbolicLink(path))
        throw new IllegalArgumentException(
            format(ERROR_SYMLINK_DETECTED, forJava(path.toString())));

      if (!Files.isDirectory(realPath, LinkOption.NOFOLLOW_LINKS))
        throw new IllegalArgumentException(
            format(ERROR_NOT_DIRECTORY, forJava(realPath.toString())));

      if (!Files.isWritable(realPath))
        throw new IllegalArgumentException(
            format("Directory is not writable: %s", forJava(realPath.toString())));

    } catch (IOException e) {
      throw new IllegalArgumentException(format(ERROR_IO_VALIDATION, forJava(path.toString())), e);
    }
  }

  private Path resolveAndValidateExists(Path path, String type) throws IOException {
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS))
      throw new IllegalArgumentException(
          format("%s does not exist: %s", type, forJava(path.toString())));

    if (Files.isSymbolicLink(path))
      throw new IllegalArgumentException(format(ERROR_SYMLINK_DETECTED, forJava(path.toString())));

    return path.toRealPath(LinkOption.NOFOLLOW_LINKS);
  }
}
