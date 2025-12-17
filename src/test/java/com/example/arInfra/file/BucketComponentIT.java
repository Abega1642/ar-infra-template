package com.example.arInfra.file;

import static org.junit.jupiter.api.Assertions.*;

import com.example.arInfra.InfraGenerated;
import com.example.arInfra.conf.FacadeIT;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

@InfraGenerated
class BucketComponentIT extends FacadeIT {

  private static final String TEST_CONTENT = "This is a test file content";
  private static final String FILE_1_CONTENT = "Content of file 1";
  private static final String FILE_2_CONTENT = "Content of file 2";
  private static final String FILE_3_CONTENT = "Content of file 3 in subdirectory";

  @TempDir Path tempDir;

  @Autowired private BucketComponent bucketComponent;

  private File testFile;
  private File testDirectory;

  @BeforeEach
  void set_up() throws IOException {
    testFile = createFile(tempDir.resolve("test-file.txt"), TEST_CONTENT);
    testDirectory = createTestDirectory();
  }

  @Test
  void should_upload_directory() {
    var bucketKey = "test/directory";

    FileHash result = bucketComponent.upload(testDirectory, bucketKey);

    assertNotHashed(result);
  }

  @Test
  void should_download_file_after_upload() throws IOException {
    var bucketKey = "test/download-test.txt";

    bucketComponent.upload(testFile, bucketKey);
    File downloaded = bucketComponent.download(bucketKey);

    assertFileContentEquals(testFile, downloaded);
  }

  @Test
  void should_upload_and_download_preserve_content() throws IOException {
    var bucketKey = "test/round-trip.txt";
    var content = "Original content for round trip test";

    Files.writeString(testFile.toPath(), content);
    bucketComponent.upload(testFile, bucketKey);

    File downloaded = bucketComponent.download(bucketKey);
    assertEquals(content, Files.readString(downloaded.toPath()));
  }

  @Test
  void should_generate_presigned_url() {
    var bucketKey = "test/presign-test.txt";
    bucketComponent.upload(testFile, bucketKey);

    URL presignedUrl = bucketComponent.presign(bucketKey, Duration.ofHours(1));

    assertNotNull(presignedUrl);
    assertTrue(
        presignedUrl.toString().contains(bucketKey.replace("/", "%2F"))
            || presignedUrl.toString().contains(bucketKey));
  }

  @Test
  void should_upload_directory_with_nested_structure() throws IOException {
    var bucketKey = "test/nested-directory";

    bucketComponent.upload(testDirectory, bucketKey);

    assertDownloadedFile(bucketKey + "/file1.txt", FILE_1_CONTENT);
    assertDownloadedFile(bucketKey + "/file2.txt", FILE_2_CONTENT);
    assertDownloadedFile(bucketKey + "/subdir/file3.txt", FILE_3_CONTENT);
  }

  @Test
  void should_throw_exception_when_downloading_non_existent_file() {
    var bucketKey = "test/non-existent-file.txt";

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> bucketComponent.download(bucketKey));

    assertTrue(exception.getMessage().contains("Download failed"));
  }

  @Test
  void should_handle_file_with_special_characters_in_name() throws IOException {
    File specialFile =
        createFile(tempDir.resolve("file with spaces & symbols.txt"), "Special content");

    var bucketKey = "test/special-chars.txt";
    bucketComponent.upload(specialFile, bucketKey);

    File downloaded = bucketComponent.download(bucketKey);
    assertEquals("Special content", Files.readString(downloaded.toPath()));
  }

  @Test
  void should_overwrite_existing_file_on_upload() throws IOException {
    var bucketKey = "test/overwrite-test.txt";

    Files.writeString(testFile.toPath(), "Original content");
    bucketComponent.upload(testFile, bucketKey);

    Files.writeString(testFile.toPath(), "Updated content");
    bucketComponent.upload(testFile, bucketKey);

    File downloaded = bucketComponent.download(bucketKey);
    assertEquals("Updated content", Files.readString(downloaded.toPath()));
  }

  @Test
  void should_handle_empty_file() throws IOException {
    File emptyFile = createFile(tempDir.resolve("empty.txt"), "");

    var bucketKey = "test/empty-file.txt";
    bucketComponent.upload(emptyFile, bucketKey);

    File downloaded = bucketComponent.download(bucketKey);
    assertEquals("", Files.readString(downloaded.toPath()));
  }

  @Test
  void should_handle_binary_file() throws IOException {
    var bucketKey = "test/binary-file.bin";
    byte[] content = {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};

    File binaryFile = tempDir.resolve("binary.bin").toFile();
    Files.write(binaryFile.toPath(), content);

    bucketComponent.upload(binaryFile, bucketKey);
    File downloaded = bucketComponent.download(bucketKey);

    assertArrayEquals(content, Files.readAllBytes(downloaded.toPath()));
  }

  private File createFile(Path path, String content) throws IOException {
    Files.writeString(path, content);
    return path.toFile();
  }

  private File createTestDirectory() throws IOException {
    File directory = tempDir.resolve("test-directory").toFile();
    assertTrue(directory.mkdir());

    createFile(directory.toPath().resolve("file1.txt"), FILE_1_CONTENT);
    createFile(directory.toPath().resolve("file2.txt"), FILE_2_CONTENT);

    File subDir = directory.toPath().resolve("subdir").toFile();
    assertTrue(subDir.mkdir());
    createFile(subDir.toPath().resolve("file3.txt"), FILE_3_CONTENT);

    return directory;
  }

  private void assertFileContentEquals(File expected, File actual) throws IOException {
    assertNotNull(actual);
    assertTrue(actual.exists());
    assertEquals(Files.readString(expected.toPath()), Files.readString(actual.toPath()));
  }

  private void assertDownloadedFile(String key, String expectedContent) throws IOException {
    File downloaded = bucketComponent.download(key);
    assertEquals(expectedContent, Files.readString(downloaded.toPath()));
  }

  private void assertNotHashed(FileHash result) {
    assertNotNull(result);
    assertEquals("NONE", result.algorithm());
    assertNull(result.value());
  }
}
