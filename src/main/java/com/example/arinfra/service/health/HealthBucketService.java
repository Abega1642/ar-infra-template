package com.example.arinfra.service.health;

import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.exception.bucket.BucketHealthCheckException;
import com.example.arinfra.file.BucketComponent;
import com.example.arinfra.file.SecureTempFileManager;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@InfraGenerated
public class HealthBucketService {

  private static final String HEALTH_KEY = "health";
  private static final String FILE_PREFIX = "bucket-health-";
  private static final String DIR_PREFIX = "dir-";
  private static final String FILE_SUFFIX = ".txt";
  private static final Duration PRESIGN_DURATION = Duration.ofMinutes(2);

  private final BucketComponent bucketComponent;
  private final SecureTempFileManager secureTempFileManager;

  /**
   * Performs a comprehensive health check on bucket operations including file upload, download,
   * directory upload, and presigned URL generation.
   *
   * @return a presigned URL for the test file
   * @throws BucketHealthCheckException if health check fails
   */
  public URL performHealthCheck() {
    try {
      log.info("Starting bucket health check");

      testFileUploadAndDownload();
      testDirectoryUpload();

      String fileBucketKey = uploadTestFile();
      URL presignedUrl = bucketComponent.presign(fileBucketKey, PRESIGN_DURATION);

      log.info("Bucket health check completed successfully");
      return presignedUrl;

    } catch (IOException e) {
      log.error("Bucket health check failed: {}", forJava(e.getMessage()), e);
      throw new BucketHealthCheckException("Bucket health check failed", e);
    }
  }

  /**
   * Tests file upload and download operations by creating a secure temp file, uploading it,
   * downloading it back, and validating the content matches.
   */
  private void testFileUploadAndDownload() throws IOException {
    String fileId = randomUUID().toString();
    String content = generateRandomContent();

    File fileToUpload =
        secureTempFileManager.createSecureTempFileWithContent(FILE_PREFIX, FILE_SUFFIX, content);

    try {
      String fileBucketKey = buildBucketKey(fileId);
      log.debug("Uploading test file with key: {}", forJava(fileBucketKey));

      bucketComponent.upload(fileToUpload, fileBucketKey);
      File downloaded = bucketComponent.download(fileBucketKey);

      try {
        validateFileContent(fileToUpload, downloaded);
        log.debug("File upload and download validation successful");
      } finally {
        secureTempFileManager.deleteTempFile(downloaded);
      }
    } finally {
      secureTempFileManager.deleteTempFile(fileToUpload);
    }
  }

  /**
   * Tests directory upload by creating a secure temporary directory with a file inside and
   * uploading it to the bucket.
   */
  private void testDirectoryUpload() throws IOException {
    String dirId = randomUUID().toString();
    String dirPrefix = DIR_PREFIX + dirId;

    File dir = secureTempFileManager.createSecureTempDirectory(dirPrefix);
    File fileInDir = null;

    try {
      String content = generateRandomContent();
      fileInDir =
          secureTempFileManager.createSecureTempFileWithContent(FILE_PREFIX, FILE_SUFFIX, content);

      File targetFile = new File(dir, fileInDir.getName());
      Files.move(fileInDir.toPath(), targetFile.toPath());
      fileInDir = targetFile;

      String dirBucketKey = HEALTH_KEY + "/" + dirPrefix;
      log.debug("Uploading test directory with key: {}", forJava(dirBucketKey));

      bucketComponent.upload(dir, dirBucketKey);
      log.debug("Directory upload successful");

    } finally {
      cleanupDirectory(dir, fileInDir);
    }
  }

  /**
   * Uploads a test file and returns its bucket key for presigned URL generation.
   *
   * @return the bucket key of the uploaded file
   */
  private String uploadTestFile() throws IOException {
    String fileId = randomUUID().toString();
    String content = generateRandomContent();

    File file =
        secureTempFileManager.createSecureTempFileWithContent(FILE_PREFIX, FILE_SUFFIX, content);

    try {
      String bucketKey = buildBucketKey(fileId);
      bucketComponent.upload(file, bucketKey);
      return bucketKey;
    } finally {
      secureTempFileManager.deleteTempFile(file);
    }
  }

  /**
   * Generates random content for test files.
   *
   * @return a random UUID string
   */
  private String generateRandomContent() {
    return randomUUID().toString();
  }

  /**
   * Builds a bucket key using the health prefix and file identifier.
   *
   * @param fileId the unique file identifier
   * @return the complete bucket key
   */
  private String buildBucketKey(String fileId) {
    return HEALTH_KEY + "/" + fileId + FILE_SUFFIX;
  }

  /**
   * Validates that the content of two files matches exactly.
   *
   * @param original the original file
   * @param downloaded the downloaded file
   * @throws IOException if reading files fails
   * @throws BucketHealthCheckException if content doesn't match
   */
  private void validateFileContent(File original, File downloaded) throws IOException {
    String originalContent = Files.readString(original.toPath());
    String downloadedContent = Files.readString(downloaded.toPath());

    if (!originalContent.equals(downloadedContent)) {
      String errorMsg = "Content mismatch between uploaded and downloaded files";
      log.error(errorMsg);
      throw new BucketHealthCheckException(errorMsg);
    }
  }

  /**
   * Safely cleans up a directory and its contents.
   *
   * @param dir the directory to clean up
   * @param fileInDir the file inside the directory (can be null)
   */
  private void cleanupDirectory(File dir, File fileInDir) {
    if (fileInDir != null && fileInDir.exists()) secureTempFileManager.deleteTempFile(fileInDir);

    if (dir != null && dir.exists()) {
      try {
        Files.delete(dir.toPath());
        log.debug("Cleaned up test directory: {}", dir.getAbsolutePath());
      } catch (IOException e) {
        log.warn("Failed to delete test directory: {}", dir.getAbsolutePath(), e);
      }
    }
  }
}
