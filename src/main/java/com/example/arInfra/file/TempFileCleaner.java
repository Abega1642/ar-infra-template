package com.example.arInfra.file;

import static org.owasp.encoder.Encode.forJava;

import com.example.arInfra.InfraGenerated;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility component for safely cleaning up temporary files.
 *
 * <p>This component provides secure deletion of temporary files with proper error handling and
 * logging. It prevents path traversal attacks by encoding file paths in logs using OWASP encoding.
 *
 * <p>Security considerations:
 *
 * <ul>
 *   <li>All file paths are encoded using OWASP Encoder before logging
 *   <li>Handles null and non-existent files gracefully
 *   <li>Does not throw exceptions on cleanup failures
 *   <li>Logs all operations for audit trail
 * </ul>
 */
@Component
@Slf4j
@InfraGenerated
public class TempFileCleaner {

  /**
   * Cleans up (deletes) the specified temporary files.
   *
   * <p>This method safely deletes multiple files, continuing even if individual deletions fail. It
   * handles null files and non-existent files gracefully without throwing exceptions.
   *
   * @param files the files to delete, can be null or empty
   */
  public void cleanUp(File... files) {
    if (isNothingToClean(files)) {
      log.debug("No temporary files to clean up");
      return;
    }

    CleanupResult result = deleteFiles(files);
    logCleanupSummary(result);
  }

  /**
   * Checks if there are no files to clean up.
   *
   * @param files the array of files to check
   * @return true if files is null or empty, false otherwise
   */
  private boolean isNothingToClean(File[] files) {
    return files == null || files.length == 0;
  }

  /**
   * Attempts to delete all provided files and tracks the results.
   *
   * @param files the array of files to delete
   * @return a CleanupResult containing success and failure counts
   */
  private CleanupResult deleteFiles(File[] files) {
    int successCount = 0;
    int failureCount = 0;

    for (File file : files) {
      if (shouldSkipFile(file)) continue;

      if (deleteFile(file)) successCount++;
      else failureCount++;
    }

    return new CleanupResult(successCount, failureCount);
  }

  /**
   * Determines if a file should be skipped during cleanup.
   *
   * <p>Files are skipped if they are:
   *
   * <ul>
   *   <li>null
   *   <li>already deleted or non-existent
   * </ul>
   *
   * @param file the file to check
   * @return true if the file should be skipped, false otherwise
   */
  private boolean shouldSkipFile(File file) {
    if (file == null) return true;

    if (!file.exists()) {
      log.debug("File already deleted or doesn't exist: {}", forJava(file.getAbsolutePath()));
      return true;
    }

    return false;
  }

  /**
   * Attempts to delete a single file securely.
   *
   * <p>Uses {@link Files#delete(java.nio.file.Path)} for secure file deletion. All file paths are
   * encoded using OWASP Encoder before logging to prevent log injection attacks.
   *
   * @param file the file to delete, must not be null and must exist
   * @return true if deletion was successful, false otherwise
   */
  private boolean deleteFile(File file) {
    try {
      Files.delete(file.toPath());
      log.debug("Deleted temporary file: {}", forJava(file.getAbsolutePath()));
      return true;
    } catch (IOException e) {
      log.warn(
          "Failed to delete temporary file: {}. Reason: {}",
          forJava(file.getAbsolutePath()),
          forJava(e.getMessage()));
      return false;
    }
  }

  /**
   * Logs a summary of the cleanup operation.
   *
   * @param result the cleanup result containing success and failure counts
   */
  private void logCleanupSummary(CleanupResult result) {
    log.debug(
        "Cleanup completed: {} succeeded, {} failed", result.successCount(), result.failureCount());
  }

  /**
   * Internal record to track cleanup operation results.
   *
   * @param successCount the number of files successfully deleted
   * @param failureCount the number of files that failed to delete
   */
  private record CleanupResult(int successCount, int failureCount) {}
}
