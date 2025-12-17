package com.example.arInfra.service.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.arInfra.exception.bucket.BucketDirectoryUploadException;
import com.example.arInfra.exception.bucket.BucketHealthCheckException;
import com.example.arInfra.file.BucketComponent;
import com.example.arInfra.file.SecureTempFileManager;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HealthBucketServiceIT {

  private static final String TEST_CONTENT = "test-content-12345";
  private static final String BUCKET_KEY_PREFIX = "health/";
  private static final String TEST_URL = "https://example.com/presigned-url";
  private static final Duration PRESIGN_DURATION = Duration.ofMinutes(2);
  @TempDir File tempDir;
  @Mock private BucketComponent bucketComponent;
  @Mock private SecureTempFileManager secureTempFileManager;
  private HealthBucketService healthBucketService;

  @BeforeEach
  void setUp() {
    healthBucketService = new HealthBucketService(bucketComponent, secureTempFileManager);
  }

  @Test
  void should_complete_health_check_successfully() throws IOException {
    File uploadFile = createTempFile("upload.txt");
    File downloadFile = createTempFile("download.txt");
    File dirUploadFile = createTempFile("dir-upload.txt");
    File finalUploadFile = createTempFile("final-upload.txt");
    File tempDirectory = createTempDirectory();
    URL expectedUrl = createTestUrl();

    when(secureTempFileManager.createSecureTempDirectory(anyString())).thenReturn(tempDirectory);
    when(secureTempFileManager.createSecureTempFileWithContent(
            anyString(), anyString(), anyString()))
        .thenReturn(uploadFile, dirUploadFile, finalUploadFile);

    when(bucketComponent.download(anyString())).thenReturn(downloadFile);
    when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(expectedUrl);

    URL result = healthBucketService.performHealthCheck();

    assertNotNull(result);
    assertEquals(expectedUrl, result);

    verify(bucketComponent, atLeast(2)).upload(any(File.class), anyString());
    verify(bucketComponent).download(anyString());
    verify(bucketComponent).presign(anyString(), eq(PRESIGN_DURATION));
    verify(secureTempFileManager, atLeast(3))
        .createSecureTempFileWithContent(anyString(), anyString(), anyString());
    verify(secureTempFileManager, atLeast(3)).deleteTempFile(any(File.class));
  }

  @Test
  void should_throw_exception_when_temp_file_creation_fails() throws IOException {
    when(secureTempFileManager.createSecureTempFileWithContent(
            anyString(), anyString(), anyString()))
        .thenThrow(new IOException("Failed to create temp file"));

    RuntimeException exception =
        assertThrows(
            BucketHealthCheckException.class, () -> healthBucketService.performHealthCheck());

    assertInstanceOf(IOException.class, exception.getCause());
    assertEquals("Failed to create temp file", exception.getCause().getMessage());
  }

  @Test
  void should_throw_exception_when_presign_fails() throws IOException {
    File uploadFile = createTempFile("upload.txt");
    File downloadFile = createTempFile("download.txt");
    File dirUploadFile = createTempFile("dir-upload.txt");
    File finalUploadFile = createTempFile("final-upload.txt");
    File tempDirectory = createTempDirectory();

    when(secureTempFileManager.createSecureTempDirectory(anyString())).thenReturn(tempDirectory);
    when(secureTempFileManager.createSecureTempFileWithContent(
            anyString(), anyString(), anyString()))
        .thenReturn(uploadFile, dirUploadFile, finalUploadFile);

    when(bucketComponent.download(anyString())).thenReturn(downloadFile);
    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenThrow(new RuntimeException("Presign failed"));

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> healthBucketService.performHealthCheck());

    assertEquals("Presign failed", exception.getMessage());
  }

  @Test
  void should_cleanup_files_after_successful_operations() throws IOException {
    File uploadFile = createTempFile("upload.txt");
    File downloadFile = createTempFile("download.txt");
    File dirUploadFile = createTempFile("dir-upload.txt");
    File finalUploadFile = createTempFile("final-upload.txt");
    File tempDirectory = createTempDirectory();
    URL expectedUrl = createTestUrl();

    when(secureTempFileManager.createSecureTempDirectory(anyString())).thenReturn(tempDirectory);
    when(secureTempFileManager.createSecureTempFileWithContent(
            anyString(), anyString(), anyString()))
        .thenReturn(uploadFile, dirUploadFile, finalUploadFile);

    when(bucketComponent.download(anyString())).thenReturn(downloadFile);
    when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(expectedUrl);

    healthBucketService.performHealthCheck();

    verify(secureTempFileManager, atLeast(3)).deleteTempFile(any(File.class));
  }

  @Test
  void should_cleanup_files_even_when_upload_fails() throws IOException {
    File uploadFile = createTempFile("upload.txt");

    when(secureTempFileManager.createSecureTempFileWithContent(
            anyString(), anyString(), anyString()))
        .thenReturn(uploadFile);

    doThrow(BucketDirectoryUploadException.class)
        .when(bucketComponent)
        .upload(any(File.class), anyString());

    assertThrows(RuntimeException.class, () -> healthBucketService.performHealthCheck());

    verify(secureTempFileManager, atLeastOnce()).deleteTempFile(uploadFile);
  }

  @Test
  void should_cleanup_downloaded_file_after_validation() throws IOException {
    File uploadFile = createTempFile("upload.txt");
    File downloadFile = createTempFile("download.txt");
    File dirUploadFile = createTempFile("dir-upload.txt");
    File finalUploadFile = createTempFile("final-upload.txt");
    File tempDirectory = createTempDirectory();
    URL expectedUrl = createTestUrl();

    when(secureTempFileManager.createSecureTempDirectory(anyString())).thenReturn(tempDirectory);
    when(secureTempFileManager.createSecureTempFileWithContent(
            anyString(), anyString(), anyString()))
        .thenReturn(uploadFile, dirUploadFile, finalUploadFile);

    when(bucketComponent.download(anyString())).thenReturn(downloadFile);
    when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(expectedUrl);

    healthBucketService.performHealthCheck();

    verify(secureTempFileManager).deleteTempFile(downloadFile);
  }

  @Test
  void should_use_correct_bucket_key_format() throws IOException {
    File uploadFile = createTempFile("upload.txt");
    File downloadFile = createTempFile("download.txt");
    File dirUploadFile = createTempFile("dir-upload.txt");
    File finalUploadFile = createTempFile("final-upload.txt");
    File tempDirectory = createTempDirectory();
    URL expectedUrl = createTestUrl();

    when(secureTempFileManager.createSecureTempDirectory(anyString())).thenReturn(tempDirectory);
    when(secureTempFileManager.createSecureTempFileWithContent(
            anyString(), anyString(), anyString()))
        .thenReturn(uploadFile, dirUploadFile, finalUploadFile);

    when(bucketComponent.download(anyString())).thenReturn(downloadFile);
    when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(expectedUrl);

    healthBucketService.performHealthCheck();

    verify(bucketComponent, atLeastOnce())
        .upload(any(File.class), argThat(key -> key.startsWith(BUCKET_KEY_PREFIX)));
    verify(bucketComponent).download(argThat(key -> key.startsWith(BUCKET_KEY_PREFIX)));
    verify(bucketComponent).presign(argThat(key -> key.startsWith(BUCKET_KEY_PREFIX)), any());
  }

  @Test
  void should_use_correct_presign_duration() throws IOException {
    File uploadFile = createTempFile("upload.txt");
    File downloadFile = createTempFile("download.txt");
    File dirUploadFile = createTempFile("dir-upload.txt");
    File finalUploadFile = createTempFile("final-upload.txt");
    File tempDirectory = createTempDirectory();
    URL expectedUrl = createTestUrl();

    when(secureTempFileManager.createSecureTempDirectory(anyString())).thenReturn(tempDirectory);
    when(secureTempFileManager.createSecureTempFileWithContent(
            anyString(), anyString(), anyString()))
        .thenReturn(uploadFile, dirUploadFile, finalUploadFile);

    when(bucketComponent.download(anyString())).thenReturn(downloadFile);
    when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(expectedUrl);

    healthBucketService.performHealthCheck();

    verify(bucketComponent).presign(anyString(), eq(PRESIGN_DURATION));
  }

  @Test
  void should_perform_directory_upload() throws IOException {
    File uploadFile = createTempFile("upload.txt");
    File downloadFile = createTempFile("download.txt");
    File dirUploadFile = createTempFile("dir-upload.txt");
    File finalUploadFile = createTempFile("final-upload.txt");
    File tempDirectory = createTempDirectory();
    URL expectedUrl = createTestUrl();

    when(secureTempFileManager.createSecureTempDirectory(anyString())).thenReturn(tempDirectory);
    when(secureTempFileManager.createSecureTempFileWithContent(
            anyString(), anyString(), anyString()))
        .thenReturn(uploadFile, dirUploadFile, finalUploadFile);

    when(bucketComponent.download(anyString())).thenReturn(downloadFile);
    when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(expectedUrl);

    healthBucketService.performHealthCheck();

    verify(bucketComponent, atLeast(2)).upload(any(File.class), anyString());
  }

  @Test
  void should_create_files_with_correct_parameters() throws IOException {
    File uploadFile = createTempFile("upload.txt");
    File downloadFile = createTempFile("download.txt");
    File dirUploadFile = createTempFile("dir-upload.txt");
    File finalUploadFile = createTempFile("final-upload.txt");
    File tempDirectory = createTempDirectory();
    URL expectedUrl = createTestUrl();

    when(secureTempFileManager.createSecureTempDirectory(anyString())).thenReturn(tempDirectory);
    when(secureTempFileManager.createSecureTempFileWithContent(
            anyString(), anyString(), anyString()))
        .thenReturn(uploadFile, dirUploadFile, finalUploadFile);

    when(bucketComponent.download(anyString())).thenReturn(downloadFile);
    when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(expectedUrl);

    healthBucketService.performHealthCheck();

    verify(secureTempFileManager, atLeast(3))
        .createSecureTempFileWithContent(
            argThat(prefix -> prefix.startsWith("bucket-health-")), eq(".txt"), anyString());
  }

  private File createTempFile(String filename) throws IOException {
    File file = new File(tempDir, filename);
    Files.writeString(file.toPath(), HealthBucketServiceIT.TEST_CONTENT);
    return file;
  }

  private File createTempDirectory() throws IOException {
    File dir = new File(tempDir, "test-dir");
    if (!dir.mkdir())
      throw new IOException("Failed to create test directory: " + dir.getAbsolutePath());

    return dir;
  }

  private URL createTestUrl() {
    try {
      return URI.create(TEST_URL).toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid test URL", e);
    }
  }
}
