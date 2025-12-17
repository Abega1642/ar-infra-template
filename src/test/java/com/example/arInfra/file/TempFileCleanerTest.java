package com.example.arInfra.file;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.owasp.encoder.Encode.forJava;

import com.example.arInfra.InfraGenerated;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Slf4j
@InfraGenerated
class TempFileCleanerTest {

  private static final String TEST_FILE_0 = "test0.txt";
  private static final String TEST_FILE_1 = "test_1.txt";
  private static final String TEST_FILE_2 = "test_2.txt";
  private static final String TEST_FILE_3 = "test_3.txt";
  private static final String TEST_FILE_01 = "test01.txt";
  private static final String TEST_FILE_02 = "test02.txt";
  private static final String DOCUMENT_TXT = "document.txt";
  private static final String VIDEO_MP4 = "video.mp4";
  private static final String AUDIO_MP3 = "audio.mp3";
  private static final String SPECIAL_CHARS_FILE = "file-with_special.chars@2024.tmp";
  private static final String READONLY_FILE = "readonly.txt";
  private static final String NESTED_FILE = "subdir/nested/file.txt";
  private static final String NONEXISTENT_FILE = "nonexistent.txt";
  private static final String MISSING_FILE_1 = "missing1.txt";
  private static final String MISSING_FILE_2 = "missing2.txt";
  private static final String MISSING_FILE_3 = "missing3.txt";
  private static final String VALID_FILE_1 = "valid1.txt";
  private static final String VALID_FILE_2 = "valid2.txt";
  private static final int LARGE_FILE_COUNT = 100;

  private final TempFileCleaner tempFileCleaner = new TempFileCleaner();

  @TempDir Path tempDir;

  @Test
  void should_delete_single_existing_file() throws IOException {
    File file = givenExistingFile(TEST_FILE_0);

    whenCleanUp(file);

    thenFileIsDeleted(file);
  }

  @Test
  void should_delete_multiple_files() throws IOException {
    File[] files = givenExistingFiles(TEST_FILE_1, TEST_FILE_2, TEST_FILE_3);

    whenCleanUp(files);

    thenAllFilesAreDeleted(files);
  }

  @Test
  void should_delete_files_with_different_extensions() throws IOException {
    File[] files = givenExistingFiles(DOCUMENT_TXT, VIDEO_MP4, AUDIO_MP3);

    whenCleanUp(files);

    thenAllFilesAreDeleted(files);
  }

  @Test
  void should_delete_file_in_subdirectory() throws IOException {
    File file = givenFileInSubdirectory();

    whenCleanUp(file);

    thenFileIsDeleted(file);
  }

  @Test
  void should_delete_file_with_special_characters() throws IOException {
    File file = givenExistingFile(SPECIAL_CHARS_FILE);

    whenCleanUp(file);

    thenFileIsDeleted(file);
  }

  @Test
  void should_handle_null_file() {
    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp((File) null));
  }

  @Test
  void should_handle_null_array() {
    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp((File[]) null));
  }

  @Test
  void should_handle_empty_array() {
    thenNoExceptionThrown(tempFileCleaner::cleanUp);
  }

  @Test
  void should_handle_mixed_null_and_valid_files() throws IOException {
    File file1 = givenExistingFile(TEST_FILE_1);
    File file3 = givenExistingFile(TEST_FILE_3);

    whenCleanUp(file1, null, file3);

    thenFileIsDeleted(file1);
    thenFileIsDeleted(file3);
  }

  @Test
  void should_handle_all_null_files() {
    File[] allNulls = new File[] {null, null, null};

    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp(allNulls));
  }

  @Test
  void should_handle_non_existent_file() {
    File nonExistent = givenNonExistentFile(NONEXISTENT_FILE);

    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp(nonExistent));
  }

  @Test
  void should_handle_already_deleted_file() throws IOException {
    File file = givenExistingFile(TEST_FILE_0);
    Files.delete(file.toPath());

    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp(file));
  }

  @Test
  void should_handle_all_non_existent_files() {
    File[] nonExistent = givenNonExistentFiles();

    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp(nonExistent));
  }

  @Test
  void should_continue_deleting_when_one_file_fails() throws IOException {
    File validFile1 = givenExistingFile(VALID_FILE_1);
    File readOnlyFile = givenReadOnlyFile();
    File validFile2 = givenExistingFile(VALID_FILE_2);

    whenCleanUp(validFile1, readOnlyFile, validFile2);

    thenFileIsDeleted(validFile1);
    thenFileIsDeleted(validFile2);

    cleanupReadOnlyFile(readOnlyFile);
  }

  @Test
  void should_handle_mixed_success_and_failure_scenarios() throws IOException {
    File existingFile1 = givenExistingFile(TEST_FILE_01);
    File existingFile2 = givenExistingFile(TEST_FILE_02);
    File nonExistent = givenNonExistentFile(NONEXISTENT_FILE);

    whenCleanUp(existingFile1, nonExistent, null, existingFile2);

    thenFileIsDeleted(existingFile1);
    thenFileIsDeleted(existingFile2);
  }

  @Test
  void should_handle_large_number_of_files() throws IOException {
    File[] files = givenLargeNumberOfFiles();

    whenCleanUp(files);

    thenAllFilesAreDeleted(files);
  }

  private File givenExistingFile(String filename) throws IOException {
    File file = tempDir.resolve(filename).toFile();
    boolean created = file.createNewFile();
    assertTrue(created, "Should create file: " + filename);
    return file;
  }

  private File[] givenExistingFiles(String... filenames) throws IOException {
    File[] files = new File[filenames.length];
    for (int i = 0; i < filenames.length; i++) {
      files[i] = givenExistingFile(filenames[i]);
    }
    return files;
  }

  private File[] givenLargeNumberOfFiles() throws IOException {
    File[] files = new File[LARGE_FILE_COUNT];
    for (int i = 0; i < LARGE_FILE_COUNT; i++) {
      files[i] = givenExistingFile(format("file_%d.txt", i));
    }
    return files;
  }

  private File givenReadOnlyFile() throws IOException {
    File file = givenExistingFile(READONLY_FILE);
    boolean madeReadOnly = file.setWritable(false);
    assertTrue(madeReadOnly, "Should make file read-only");
    return file;
  }

  private File givenFileInSubdirectory() throws IOException {
    Path filePath = tempDir.resolve(NESTED_FILE);
    Files.createDirectories(filePath.getParent());
    File file = filePath.toFile();
    boolean created = file.createNewFile();
    assertTrue(created, "Should create file in subdirectory");
    return file;
  }

  private File givenNonExistentFile(String filename) {
    return new File(tempDir.toFile(), filename);
  }

  private File[] givenNonExistentFiles() {
    File[] files = new File[new String[] {MISSING_FILE_1, MISSING_FILE_2, MISSING_FILE_3}.length];
    for (int i = 0; i < new String[] {MISSING_FILE_1, MISSING_FILE_2, MISSING_FILE_3}.length; i++) {
      files[i] =
          givenNonExistentFile(new String[] {MISSING_FILE_1, MISSING_FILE_2, MISSING_FILE_3}[i]);
    }
    return files;
  }

  private void whenCleanUp(File... files) {
    tempFileCleaner.cleanUp(files);
  }

  private void thenFileIsDeleted(File file) {
    assertFalse(file.exists(), "File should be deleted: " + file.getName());
  }

  private void thenAllFilesAreDeleted(File... files) {
    for (File file : files) {
      thenFileIsDeleted(file);
    }
  }

  private void thenNoExceptionThrown(Runnable action) {
    assertDoesNotThrow(action::run, "Should not throw any exception");
  }

  private void cleanupReadOnlyFile(File file) {
    boolean madeWritable = file.setWritable(true);
    if (madeWritable) {
      boolean deleted = file.delete();
      logErrorOnNonDeletion(file, deleted);
    } else {
      log.error("Warning: Could not make file writable: {}", forJava(file.getAbsolutePath()));
      boolean deleted = file.delete();
      logErrorOnNonDeletion(file, deleted);
    }
  }

  private void logErrorOnNonDeletion(File file, boolean deleted) {
    if (!deleted)
      log.error("Warning: Could not delete read-only file: {}", forJava(file.getAbsolutePath()));
  }
}
