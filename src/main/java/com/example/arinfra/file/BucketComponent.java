package com.example.arinfra.file;

import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.config.BucketConf;
import com.example.arinfra.exception.bucket.BucketDirectoryUploadException;
import com.example.arinfra.exception.bucket.BucketDownloadException;
import com.example.arinfra.exception.bucket.BucketUploadException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

/**
 * Infrastructure component responsible for interacting with the S3-compatible bucket.
 *
 * <p>This component provides:
 *
 * <ul>
 *   <li>File uploads (single file or directory)
 *   <li>File downloads using secure temporary files
 *   <li>Presigned URL generation
 * </ul>
 *
 * <p><b>Security considerations:</b>
 *
 * <ul>
 *   <li>All bucket keys are sanitized before logging
 *   <li>Temporary files are created using {@link TempFileManager}
 *   <li>Exceptions are domain-specific and controller-advice friendly
 * </ul>
 *
 * <p>This class is {@link InfraGenerated} and intended to be used by higher-level services.
 */
@Slf4j
@InfraGenerated
@Component
@AllArgsConstructor
public class BucketComponent {

  private final BucketConf bucketConf;
  private final TempFileManager tempFileManager;

  /**
   * Uploads a file or directory to the configured bucket.
   *
   * @param file the file or directory to upload
   * @param bucketKey the target bucket key
   * @return the computed {@link FileHash}
   */
  public FileHash upload(File file, String bucketKey) {
    return file.isDirectory() ? uploadDirectory(file, bucketKey) : uploadFile(file, bucketKey);
  }

  private FileHash uploadDirectory(File directory, String bucketKey) {
    log.info("Uploading directory to bucket: key={}", forJava(bucketKey));

    try (Stream<Path> files = Files.walk(directory.toPath())) {
      files
          .filter(Files::isRegularFile)
          .forEach(path -> uploadDirectoryFile(directory, path, bucketKey));
      return new FileHash("NONE", null);
    } catch (IOException e) {
      throw new BucketDirectoryUploadException(
          "Failed to upload directory: " + forJava(directory.getAbsolutePath()), e);
    }
  }

  private void uploadDirectoryFile(File rootDir, Path path, String bucketKey) {
    String relativeKey =
        bucketKey + "/" + rootDir.toPath().relativize(path).toString().replace("\\", "/");

    uploadFile(path.toFile(), relativeKey);
  }

  private FileHash uploadFile(File file, String bucketKey) {
    log.info("Uploading file to bucket: key={}", forJava(bucketKey));

    try {
      var request =
          UploadFileRequest.builder()
              .source(file)
              .putObjectRequest(req -> req.bucket(bucketConf.getBucketName()).key(bucketKey))
              .addTransferListener(LoggingTransferListener.create())
              .build();

      var upload = bucketConf.getS3TransferManager().uploadFile(request);
      var completed = upload.completionFuture().join();

      return new FileHash("SHA-256", completed.response().checksumSHA256());
    } catch (Exception e) {
      throw new BucketUploadException("Upload failed for key: " + forJava(bucketKey), e);
    }
  }

  /**
   * Downloads a file from the bucket into a secure temporary file.
   *
   * @param bucketKey the bucket key
   * @return the downloaded file
   */
  public File download(String bucketKey) {
    log.info("Downloading file from bucket: key={}", forJava(bucketKey));

    try {
      File destination =
          tempFileManager.createSecureTempFile("b2-", "-" + bucketKey.replace("/", "-"));

      var request =
          DownloadFileRequest.builder()
              .getObjectRequest(buildGetObjectRequest(bucketKey))
              .destination(destination)
              .build();

      bucketConf.getS3TransferManager().downloadFile(request).completionFuture().join();
      return destination;
    } catch (Exception e) {
      throw new BucketDownloadException("Download failed for key: " + forJava(bucketKey), e);
    }
  }

  /**
   * Generates a presigned URL for a bucket object.
   *
   * @param bucketKey the bucket key
   * @param expiration URL expiration duration
   * @return a presigned {@link URL}
   */
  public URL presign(String bucketKey, Duration expiration) {
    log.debug("Generating presigned URL: key={}, expiration={}", forJava(bucketKey), expiration);

    return bucketConf
        .getS3Presigner()
        .presignGetObject(
            GetObjectPresignRequest.builder()
                .getObjectRequest(buildGetObjectRequest(bucketKey))
                .signatureDuration(expiration)
                .build())
        .url();
  }

  private GetObjectRequest buildGetObjectRequest(String bucketKey) {
    return GetObjectRequest.builder().bucket(bucketConf.getBucketName()).key(bucketKey).build();
  }
}
