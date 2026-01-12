package com.example.arinfra.file;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.validator.file.FileValidator;
import com.example.arinfra.validator.file.ZipEntryValidator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@InfraGenerated
class FileZipManagerTest {

  private static final String TEST_CONTENT = "test content for zip operations";
  private static final String TEST_FILE_NAME = "test.txt";
  private static final String TEST_ZIP_NAME = "test.zip";
  private static final String SUBDIR_NAME = "subdir";
  private static final String NESTED_FILE_NAME = "nested.txt";

  @TempDir Path tempDir;

  @Mock private TempFileManager tempFileManager;

  @Mock private FilenameSanitizer filenameSanitizer;

  @Mock private FileValidator fileValidator;

  @Mock private ZipEntryValidator zipEntryValidator;

  private FileZipManager fileZipManager;

  @BeforeEach
  void setUp() {
    fileZipManager =
        new FileZipManager(tempFileManager, filenameSanitizer, fileValidator, zipEntryValidator);
  }

  @Test
  void zip_file_with_valid_file_should_create_zip() throws IOException {
    File sourceFile = createFile();
    File zipFile = tempDir.resolve(TEST_ZIP_NAME).toFile();

    when(filenameSanitizer.apply(TEST_ZIP_NAME)).thenReturn(TEST_ZIP_NAME);
    when(tempFileManager.createSecureTempFile(anyString(), eq(TEST_ZIP_NAME))).thenReturn(zipFile);
    doNothing().when(fileValidator).validateReadableFile(any(Path.class));

    File result = fileZipManager.zipFile(sourceFile, TEST_ZIP_NAME);

    assertNotNull(result);
    assertTrue(result.exists());
    verify(fileValidator).validateReadableFile(sourceFile.toPath());
    verify(filenameSanitizer).apply(TEST_ZIP_NAME);
    verify(tempFileManager).createSecureTempFile(anyString(), eq(TEST_ZIP_NAME));

    verifyZipContainsEntry(result, TEST_FILE_NAME);
  }

  @Test
  void zip_file_with_null_file_should_throw_exception() {
    assertThrows(NullPointerException.class, () -> fileZipManager.zipFile(null, TEST_ZIP_NAME));
  }

  @Test
  void zip_file_with_blank_zip_name_should_throw_exception() throws IOException {
    File sourceFile = createFile();

    assertThrows(Exception.class, () -> fileZipManager.zipFile(sourceFile, ""));
  }

  @Test
  void zip_file_with_invalid_source_should_throw_exception() throws IOException {
    File sourceFile = createFile();

    doThrow(new IllegalArgumentException("File not readable"))
        .when(fileValidator)
        .validateReadableFile(any(Path.class));

    assertThrows(
        IllegalArgumentException.class, () -> fileZipManager.zipFile(sourceFile, TEST_ZIP_NAME));
  }

  @Test
  void zip_file_should_add_zip_extension_if_missing() throws IOException {
    File sourceFile = createFile();
    File zipFile = tempDir.resolve(TEST_ZIP_NAME).toFile();
    String nameWithoutExtension = "test";

    when(filenameSanitizer.apply(nameWithoutExtension)).thenReturn(nameWithoutExtension);
    when(tempFileManager.createSecureTempFile(anyString(), eq(TEST_ZIP_NAME))).thenReturn(zipFile);
    doNothing().when(fileValidator).validateReadableFile(any(Path.class));

    File result = fileZipManager.zipFile(sourceFile, nameWithoutExtension);

    assertNotNull(result);
    assertTrue(result.exists());
  }

  @Test
  void zip_directory_with_valid_directory_should_create_zip() throws IOException {
    Path sourceDir = createDirectoryWithFiles();
    File zipFile = tempDir.resolve(TEST_ZIP_NAME).toFile();

    when(filenameSanitizer.apply(anyString())).thenAnswer(i -> i.getArgument(0));
    when(tempFileManager.createSecureTempFile(anyString(), eq(TEST_ZIP_NAME))).thenReturn(zipFile);
    doNothing().when(fileValidator).validateReadableDirectory(any(Path.class));
    doNothing().when(zipEntryValidator).validateEntryCount(anyInt());

    File result = fileZipManager.zipDirectory(sourceDir.toFile(), TEST_ZIP_NAME);

    assertNotNull(result);
    assertTrue(result.exists());
    verify(fileValidator).validateReadableDirectory(sourceDir);
    verify(filenameSanitizer, atLeastOnce()).apply(anyString());

    verifyZipContainsEntry(result, TEST_FILE_NAME);
  }

  @Test
  void zip_directory_with_null_directory_should_throw_exception() {
    assertThrows(
        NullPointerException.class, () -> fileZipManager.zipDirectory(null, TEST_ZIP_NAME));
  }

  @Test
  void zip_directory_with_nested_structure_should_preserve_hierarchy() throws IOException {
    Path sourceDir = createNestedDirectoryStructure();
    File zipFile = tempDir.resolve(TEST_ZIP_NAME).toFile();

    when(filenameSanitizer.apply(anyString())).thenAnswer(i -> i.getArgument(0));
    when(tempFileManager.createSecureTempFile(anyString(), anyString())).thenReturn(zipFile);
    doNothing().when(fileValidator).validateReadableDirectory(any(Path.class));
    doNothing().when(zipEntryValidator).validateEntryCount(anyInt());

    File result = fileZipManager.zipDirectory(sourceDir.toFile(), TEST_ZIP_NAME);

    assertNotNull(result);
    assertTrue(result.exists());

    verifyZipContainsEntry(result, TEST_FILE_NAME);
    verifyZipContainsEntry(result, SUBDIR_NAME + "/" + NESTED_FILE_NAME);
  }

  @Test
  void zip_directory_with_empty_directory_should_succeed() throws IOException {
    Path emptyDir = Files.createDirectory(tempDir.resolve("empty"));
    File zipFile = tempDir.resolve(TEST_ZIP_NAME).toFile();

    when(filenameSanitizer.apply(TEST_ZIP_NAME)).thenReturn(TEST_ZIP_NAME);
    when(tempFileManager.createSecureTempFile(anyString(), eq(TEST_ZIP_NAME))).thenReturn(zipFile);
    doNothing().when(fileValidator).validateReadableDirectory(any(Path.class));

    File result = fileZipManager.zipDirectory(emptyDir.toFile(), TEST_ZIP_NAME);

    assertNotNull(result);
    assertTrue(result.exists());
  }

  @Test
  void zip_directory_exceeding_entry_limit_should_throw_exception() throws IOException {
    Path sourceDir = createDirectoryWithFiles();
    File zipFile = tempDir.resolve(TEST_ZIP_NAME).toFile();

    when(filenameSanitizer.apply(anyString())).thenAnswer(i -> i.getArgument(0));
    when(tempFileManager.createSecureTempFile(anyString(), anyString())).thenReturn(zipFile);
    doNothing().when(fileValidator).validateReadableDirectory(any(Path.class));
    doThrow(new SecurityException("Too many entries"))
        .when(zipEntryValidator)
        .validateEntryCount(anyInt());
    doNothing().when(tempFileManager).deleteTempFile(zipFile);

    assertThrows(
        SecurityException.class,
        () -> fileZipManager.zipDirectory(sourceDir.toFile(), TEST_ZIP_NAME));

    verify(tempFileManager).deleteTempFile(zipFile);
  }

  @Test
  void unzip_with_valid_zip_should_extract_files() throws IOException {
    File zipFile = createZipWithFiles();
    Path extractDir = tempDir.resolve("extract");

    doNothing().when(fileValidator).validateReadableFile(any(Path.class));
    doNothing().when(fileValidator).validateWritableDirectory(any(Path.class));
    doNothing().when(zipEntryValidator).validateEntryCount(anyInt());
    doNothing().when(zipEntryValidator).validateEntryName(anyString());
    doNothing().when(zipEntryValidator).validateEntrySize(any(ZipArchiveEntry.class));
    doNothing().when(zipEntryValidator).validatePathTraversal(any(Path.class), any(Path.class));
    doNothing().when(zipEntryValidator).validateDuplicateEntry(anyString(), any(Set.class));
    doNothing().when(zipEntryValidator).validateActualExtractedSize(anyLong(), anyString());
    doNothing().when(zipEntryValidator).validateTotalDecompressedSize(anyLong());
    when(filenameSanitizer.apply(anyString())).thenAnswer(i -> i.getArgument(0));

    File result = fileZipManager.unzip(zipFile, extractDir.toFile());

    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(Files.exists(extractDir.resolve(TEST_FILE_NAME)));

    verify(fileValidator).validateReadableFile(zipFile.toPath());
    verify(fileValidator).validateWritableDirectory(extractDir);
    verify(zipEntryValidator, atLeastOnce()).validateEntryName(anyString());
  }

  @Test
  void unzip_with_null_zip_file_should_throw_exception() throws IOException {
    Path extractDir = tempDir.resolve("extract");

    assertThrows(NullPointerException.class, () -> fileZipManager.unzip(null, extractDir.toFile()));
  }

  @Test
  void unzip_with_null_target_directory_should_throw_exception() throws IOException {
    File zipFile = createZipWithFiles();

    assertThrows(NullPointerException.class, () -> fileZipManager.unzip(zipFile, null));
  }

  @Test
  void unzip_with_nonexistent_target_directory_should_create_it() throws IOException {
    File zipFile = createZipWithFiles();
    Path extractDir = tempDir.resolve("new-extract");

    doNothing().when(fileValidator).validateReadableFile(any(Path.class));
    doNothing().when(fileValidator).validateWritableDirectory(any(Path.class));
    doNothing().when(zipEntryValidator).validateEntryCount(anyInt());
    doNothing().when(zipEntryValidator).validateEntryName(anyString());
    doNothing().when(zipEntryValidator).validateEntrySize(any(ZipArchiveEntry.class));
    doNothing().when(zipEntryValidator).validatePathTraversal(any(Path.class), any(Path.class));
    doNothing().when(zipEntryValidator).validateDuplicateEntry(anyString(), any(Set.class));
    doNothing().when(zipEntryValidator).validateActualExtractedSize(anyLong(), anyString());
    doNothing().when(zipEntryValidator).validateTotalDecompressedSize(anyLong());
    when(filenameSanitizer.apply(anyString())).thenAnswer(i -> i.getArgument(0));

    File result = fileZipManager.unzip(zipFile, extractDir.toFile());

    assertNotNull(result);
    assertTrue(Files.exists(extractDir));
  }

  @Test
  void unzip_with_path_traversal_attempt_should_throw_exception() throws IOException {
    File zipFile = createZipWithFiles();
    Path extractDir = tempDir.resolve("extract");

    doNothing().when(fileValidator).validateReadableFile(any(Path.class));
    doNothing().when(fileValidator).validateWritableDirectory(any(Path.class));
    doNothing().when(zipEntryValidator).validateEntryCount(anyInt());
    doThrow(new SecurityException("Path traversal detected"))
        .when(zipEntryValidator)
        .validateEntryName(anyString());

    assertThrows(SecurityException.class, () -> fileZipManager.unzip(zipFile, extractDir.toFile()));
  }

  @Test
  void unzip_with_zip_bomb_should_throw_exception() throws IOException {
    File zipFile = createZipWithFiles();
    Path extractDir = tempDir.resolve("extract");

    doNothing().when(fileValidator).validateReadableFile(any(Path.class));
    doNothing().when(fileValidator).validateWritableDirectory(any(Path.class));
    doNothing().when(zipEntryValidator).validateEntryCount(anyInt());
    doNothing().when(zipEntryValidator).validateEntryName(anyString());
    doNothing().when(zipEntryValidator).validateEntrySize(any(ZipArchiveEntry.class));
    doNothing().when(zipEntryValidator).validatePathTraversal(any(Path.class), any(Path.class));
    doNothing().when(zipEntryValidator).validateDuplicateEntry(anyString(), any(Set.class));
    doThrow(new SecurityException("Entry size exceeded"))
        .when(zipEntryValidator)
        .validateActualExtractedSize(anyLong(), anyString());
    when(filenameSanitizer.apply(anyString())).thenAnswer(i -> i.getArgument(0));

    assertThrows(SecurityException.class, () -> fileZipManager.unzip(zipFile, extractDir.toFile()));
  }

  @Test
  void unzip_to_temp_directory_should_extract_to_temp_location() throws IOException {
    File zipFile = createZipWithFiles();
    Path tempExtractDir = tempDir.resolve("temp-extract");

    when(tempFileManager.createSecureTempDirectory(anyString()))
        .thenReturn(tempExtractDir.toFile());
    doNothing().when(fileValidator).validateReadableFile(any(Path.class));
    doNothing().when(fileValidator).validateWritableDirectory(any(Path.class));
    doNothing().when(zipEntryValidator).validateEntryCount(anyInt());
    doNothing().when(zipEntryValidator).validateEntryName(anyString());
    doNothing().when(zipEntryValidator).validateEntrySize(any(ZipArchiveEntry.class));
    doNothing().when(zipEntryValidator).validatePathTraversal(any(Path.class), any(Path.class));
    doNothing().when(zipEntryValidator).validateDuplicateEntry(anyString(), any(Set.class));
    doNothing().when(zipEntryValidator).validateActualExtractedSize(anyLong(), anyString());
    doNothing().when(zipEntryValidator).validateTotalDecompressedSize(anyLong());
    when(filenameSanitizer.apply(anyString())).thenAnswer(i -> i.getArgument(0));

    File result = fileZipManager.unzipToTempDirectory(zipFile);

    assertNotNull(result);
    verify(tempFileManager).createSecureTempDirectory(anyString());
    verify(fileValidator, times(2)).validateReadableFile(any(Path.class));
  }

  @Test
  void unzip_to_temp_directory_with_null_zip_should_throw_exception() {
    assertThrows(NullPointerException.class, () -> fileZipManager.unzipToTempDirectory(null));
  }

  @Test
  void unzip_with_symbolic_link_entry_should_throw_exception() throws IOException {
    File zipFile = createZipWithSymbolicLink();
    Path extractDir = tempDir.resolve("extract");

    SecurityException exception =
        assertThrows(
            SecurityException.class, () -> fileZipManager.unzip(zipFile, extractDir.toFile()));

    assertTrue(exception.getMessage().contains("Symbolic link entries are not allowed"));
  }

  private File createFile() throws IOException {
    Path filePath = tempDir.resolve(FileZipManagerTest.TEST_FILE_NAME);
    Files.writeString(filePath, FileZipManagerTest.TEST_CONTENT);
    return filePath.toFile();
  }

  private Path createDirectoryWithFiles() throws IOException {
    Path dir = Files.createDirectory(tempDir.resolve("source"));
    Files.writeString(dir.resolve(TEST_FILE_NAME), TEST_CONTENT);
    return dir;
  }

  private Path createNestedDirectoryStructure() throws IOException {
    Path dir = Files.createDirectory(tempDir.resolve("nested-source"));
    Files.writeString(dir.resolve(TEST_FILE_NAME), TEST_CONTENT);

    Path subDir = Files.createDirectory(dir.resolve(SUBDIR_NAME));
    Files.writeString(subDir.resolve(NESTED_FILE_NAME), "nested content");

    return dir;
  }

  private File createZipWithFiles() throws IOException {
    Path zipPath = tempDir.resolve(TEST_ZIP_NAME);

    try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(Files.newOutputStream(zipPath))) {
      ZipArchiveEntry entry = new ZipArchiveEntry(TEST_FILE_NAME);
      zos.putArchiveEntry(entry);
      zos.write(TEST_CONTENT.getBytes());
      zos.closeArchiveEntry();
    }

    return zipPath.toFile();
  }

  private File createZipWithSymbolicLink() throws IOException {
    Path zipPath = tempDir.resolve("symlink.zip");

    try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(Files.newOutputStream(zipPath))) {
      ZipArchiveEntry entry = new ZipArchiveEntry("symlink.txt");
      entry.setUnixMode(0120000);
      zos.putArchiveEntry(entry);
      zos.write("target".getBytes());
      zos.closeArchiveEntry();
    }

    return zipPath.toFile();
  }

  private void verifyZipContainsEntry(File zipFile, String entryName) throws IOException {
    try (ZipFile zip = new ZipFile(zipFile)) {
      ZipArchiveEntry entry = zip.getEntry(entryName);
      assertNotNull(entry, "ZIP should contain entry: " + entryName);
    }
  }
}
