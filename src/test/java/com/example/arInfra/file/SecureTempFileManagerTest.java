package com.example.arInfra.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.owasp.encoder.Encode.forJava;

import com.example.arInfra.InfraGenerated;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@Slf4j
@InfraGenerated
class SecureTempFileManagerTest {

  private static final String TEST_PREFIX = "test-";
  private static final String TEST_SUFFIX = ".txt";
  private static final String TEST_CONTENT = "Test content for secure file";
  private static final byte[] TEST_BYTES = "Binary test content".getBytes();
  private static final String MIN_PREFIX = "abc";
  private static final String SHORT_PREFIX = "ab";
  private static final String EMPTY_SUFFIX = "";
  private static final String SPECIAL_CONTENT =
      "Special chars: €, ñ, 中文, emoji: 🔒, newlines:\n\ttabs\r\n";
  private static final int LARGE_CONTENT_SIZE = 10_000;

  private final TempFileCleaner tempFileCleaner = new TempFileCleaner();
  private SecureTempFileManager subject;
  private File createdFile;

  @BeforeEach
  void setUp() {

    subject = new SecureTempFileManager(tempFileCleaner);
    createdFile = null;
  }

  @AfterEach
  void tearDown() {
    if (createdFile != null && createdFile.exists()) {
      subject.deleteTempFile(createdFile);
    }
  }

  @Test
  void should_create_temp_file_with_default_parameters() throws IOException {
    createdFile = subject.createSecureTempFile();

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
    assertTrue(createdFile.isFile());
    assertTrue(createdFile.getAbsolutePath().contains("temp-"));
    assertTrue(createdFile.getName().endsWith(".tmp"));
  }

  @Test
  void should_create_temp_file_with_custom_prefix_and_suffix() throws IOException {
    createdFile = subject.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
    assertTrue(createdFile.getName().startsWith(TEST_PREFIX));
    assertTrue(createdFile.getName().endsWith(TEST_SUFFIX));
  }

  @Test
  void should_create_multiple_unique_temp_files() throws IOException {
    File firstFile = subject.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);
    File secondFile = subject.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    try {
      assertNotNull(firstFile);
      assertNotNull(secondFile);
      assertNotEquals(firstFile.getAbsolutePath(), secondFile.getAbsolutePath());
      assertTrue(firstFile.exists());
      assertTrue(secondFile.exists());
    } finally {
      subject.deleteTempFile(firstFile);
      subject.deleteTempFile(secondFile);
    }
  }

  @Test
  void should_create_temp_file_with_string_content() throws IOException {
    createdFile = subject.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, TEST_CONTENT);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());

    String actualContent = Files.readString(createdFile.toPath());
    assertEquals(TEST_CONTENT, actualContent);
  }

  @Test
  void should_create_temp_file_with_byte_content() throws IOException {
    createdFile = subject.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, TEST_BYTES);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());

    byte[] actualBytes = Files.readAllBytes(createdFile.toPath());
    assertArrayEquals(TEST_BYTES, actualBytes);
  }

  @Test
  void should_create_temp_file_with_empty_string_content() throws IOException {
    String emptyContent = "";
    createdFile = subject.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, emptyContent);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());

    String actualContent = Files.readString(createdFile.toPath());
    assertEquals(emptyContent, actualContent);
  }

  @Test
  void should_create_temp_file_with_empty_byte_array() throws IOException {
    byte[] emptyBytes = new byte[0];
    createdFile = subject.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, emptyBytes);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
    assertEquals(0, createdFile.length());
  }

  @Test
  void should_delete_temp_file_successfully() throws IOException {
    createdFile = subject.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);
    assertTrue(createdFile.exists());

    subject.deleteTempFile(createdFile);

    assertFalse(createdFile.exists());
  }

  @Test
  void should_handle_deleting_null_file() {
    assertDoesNotThrow(() -> subject.deleteTempFile(null));
  }

  @Test
  void should_handle_deleting_non_existent_file() {
    File nonExistentFile = new File("/tmp/non-existent-file-" + System.currentTimeMillis());

    assertFalse(nonExistentFile.exists(), "File should not exist before test");
    assertDoesNotThrow(() -> subject.deleteTempFile(nonExistentFile));
    assertFalse(nonExistentFile.exists(), "File should still not exist after delete attempt");
  }

  @Test
  void should_throw_exception_when_prefix_is_null() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> subject.createSecureTempFile(null, TEST_SUFFIX));

    assertEquals("Prefix must be at least 3 characters long", exception.getMessage());
  }

  @Test
  void should_throw_exception_when_prefix_is_too_short() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> subject.createSecureTempFile(SHORT_PREFIX, TEST_SUFFIX));

    assertEquals("Prefix must be at least 3 characters long", exception.getMessage());
  }

  @Test
  void should_throw_exception_when_suffix_is_null() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> subject.createSecureTempFile(TEST_PREFIX, null));

    assertEquals("Suffix cannot be null", exception.getMessage());
  }

  @Test
  void should_accept_empty_suffix() throws IOException {
    createdFile = subject.createSecureTempFile(TEST_PREFIX, EMPTY_SUFFIX);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
    assertTrue(createdFile.getName().startsWith(TEST_PREFIX));
  }

  @Test
  void should_accept_minimum_valid_prefix_length() throws IOException {
    createdFile = subject.createSecureTempFile(MIN_PREFIX, TEST_SUFFIX);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void should_create_file_with_owner_only_permissions_on_posix() throws IOException {
    createdFile = subject.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(createdFile.toPath());
    Set<PosixFilePermission> expectedPermissions = PosixFilePermissions.fromString("rw-------");

    assertEquals(expectedPermissions, permissions);
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void should_not_allow_group_read_on_posix() throws IOException {
    createdFile = subject.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(createdFile.toPath());

    assertFalse(permissions.contains(PosixFilePermission.GROUP_READ));
    assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
    assertFalse(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void should_not_allow_others_read_on_posix() throws IOException {
    createdFile = subject.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(createdFile.toPath());

    assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
    assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
    assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void should_create_file_in_user_temp_directory_on_windows() throws IOException {
    createdFile = subject.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    String userTempDir = System.getProperty("java.io.tmpdir");
    assertTrue(createdFile.getAbsolutePath().startsWith(userTempDir));
  }

  @Test
  void should_cleanup_file_on_content_write_failure() {
    String invalidContent = null;

    assertThrows(
        NullPointerException.class,
        () -> subject.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, invalidContent));
  }

  @Test
  void should_handle_large_content() throws IOException {
    StringBuilder largeContent = new StringBuilder();

    for (int i = 0; i < LARGE_CONTENT_SIZE; i++) {
      largeContent.append("Line ").append(i).append("\n");
    }

    createdFile =
        subject.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, largeContent.toString());

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
    assertTrue(createdFile.length() > 0);

    String readContent = Files.readString(createdFile.toPath());
    assertEquals(largeContent.toString(), readContent);
  }

  @Test
  void should_handle_special_characters_in_content() throws IOException {
    createdFile =
        subject.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, SPECIAL_CONTENT);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());

    String actualContent = Files.readString(createdFile.toPath());
    assertEquals(SPECIAL_CONTENT, actualContent);
  }

  @Test
  void should_create_readable_file() throws IOException {
    createdFile = subject.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    assertTrue(createdFile.canRead());
  }

  @Test
  void should_create_writable_file() throws IOException {
    createdFile = subject.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    assertTrue(createdFile.canWrite());
  }

  @Test
  void should_not_create_executable_file() throws IOException {
    createdFile = subject.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    assertFalse(createdFile.canExecute());
  }

  @Test
  void should_create_temp_directory_with_prefix() throws IOException {
    File createdDir = subject.createSecureTempDirectory(TEST_PREFIX);

    try {
      assertNotNull(createdDir);
      assertTrue(createdDir.exists());
      assertTrue(createdDir.isDirectory());
      assertTrue(createdDir.getName().startsWith(TEST_PREFIX));
    } finally {
      deleteDirectoryIfExists(createdDir);
    }
  }

  @Test
  void should_create_multiple_unique_temp_directories() throws IOException {
    File firstDir = subject.createSecureTempDirectory(TEST_PREFIX);
    File secondDir = subject.createSecureTempDirectory(TEST_PREFIX);

    try {
      assertNotNull(firstDir);
      assertNotNull(secondDir);
      assertNotEquals(firstDir.getAbsolutePath(), secondDir.getAbsolutePath());
      assertTrue(firstDir.exists());
      assertTrue(secondDir.exists());
      assertTrue(firstDir.isDirectory());
      assertTrue(secondDir.isDirectory());
    } finally {
      deleteDirectoryIfExists(firstDir);
      deleteDirectoryIfExists(secondDir);
    }
  }

  @Test
  void should_throw_exception_when_directory_prefix_is_null() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> subject.createSecureTempDirectory(null));

    assertEquals("Prefix must be at least 3 characters long", exception.getMessage());
  }

  @Test
  void should_throw_exception_when_directory_prefix_is_too_short() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> subject.createSecureTempDirectory(SHORT_PREFIX));

    assertEquals("Prefix must be at least 3 characters long", exception.getMessage());
  }

  @Test
  void should_accept_minimum_valid_directory_prefix_length() throws IOException {
    File createdDir = subject.createSecureTempDirectory(MIN_PREFIX);

    try {
      assertNotNull(createdDir);
      assertTrue(createdDir.exists());
      assertTrue(createdDir.isDirectory());
    } finally {
      deleteDirectoryIfExists(createdDir);
    }
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void should_create_directory_with_owner_only_permissions_on_posix() throws IOException {
    File createdDir = subject.createSecureTempDirectory(TEST_PREFIX);

    try {
      Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(createdDir.toPath());
      Set<PosixFilePermission> expectedPermissions = PosixFilePermissions.fromString("rwx------");

      assertEquals(expectedPermissions, permissions);
    } finally {
      deleteDirectoryIfExists(createdDir);
    }
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void should_not_allow_group_access_on_directory_on_posix() throws IOException {
    File createdDir = subject.createSecureTempDirectory(TEST_PREFIX);

    try {
      Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(createdDir.toPath());

      assertFalse(permissions.contains(PosixFilePermission.GROUP_READ));
      assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
      assertFalse(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
    } finally {
      deleteDirectoryIfExists(createdDir);
    }
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void should_not_allow_others_access_on_directory_on_posix() throws IOException {
    File createdDir = subject.createSecureTempDirectory(TEST_PREFIX);

    try {
      Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(createdDir.toPath());

      assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
      assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
      assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));
    } finally {
      deleteDirectoryIfExists(createdDir);
    }
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void should_create_directory_in_user_temp_directory_on_windows() throws IOException {
    File createdDir = subject.createSecureTempDirectory(TEST_PREFIX);

    try {
      String userTempDir = System.getProperty("java.io.tmpdir");
      assertTrue(createdDir.getAbsolutePath().startsWith(userTempDir));
    } finally {
      deleteDirectoryIfExists(createdDir);
    }
  }

  @Test
  void should_create_readable_directory() throws IOException {
    File createdDir = subject.createSecureTempDirectory(TEST_PREFIX);

    try {
      assertTrue(createdDir.canRead());
    } finally {
      deleteDirectoryIfExists(createdDir);
    }
  }

  @Test
  void should_create_writable_directory() throws IOException {
    File createdDir = subject.createSecureTempDirectory(TEST_PREFIX);

    try {
      assertTrue(createdDir.canWrite());
    } finally {
      deleteDirectoryIfExists(createdDir);
    }
  }

  @Test
  void should_create_executable_directory() throws IOException {
    File createdDir = subject.createSecureTempDirectory(TEST_PREFIX);

    try {
      assertTrue(createdDir.canExecute());
    } finally {
      deleteDirectoryIfExists(createdDir);
    }
  }

  @Test
  void should_allow_creating_files_inside_secure_directory() throws IOException {
    File createdDir = subject.createSecureTempDirectory(TEST_PREFIX);
    File fileInDir = null;

    try {
      fileInDir = new File(createdDir, "test-file.txt");
      assertTrue(fileInDir.createNewFile());
      assertTrue(fileInDir.exists());
      assertTrue(fileInDir.isFile());

      Files.writeString(fileInDir.toPath(), TEST_CONTENT);
      String readContent = Files.readString(fileInDir.toPath());
      assertEquals(TEST_CONTENT, readContent);
    } finally {
      deleteFileIfExists(fileInDir);
      deleteDirectoryIfExists(createdDir);
    }
  }

  @Test
  void should_allow_creating_subdirectories_inside_secure_directory() throws IOException {
    File createdDir = subject.createSecureTempDirectory(TEST_PREFIX);
    File subDir = null;

    try {
      subDir = new File(createdDir, "subdir");
      assertTrue(subDir.mkdir());
      assertTrue(subDir.exists());
      assertTrue(subDir.isDirectory());
    } finally {
      deleteDirectoryIfExists(subDir);
      deleteDirectoryIfExists(createdDir);
    }
  }

  @Test
  void should_support_directory_with_multiple_files() throws IOException {
    File createdDir = subject.createSecureTempDirectory(TEST_PREFIX);
    File file1 = null;
    File file2 = null;
    File file3 = null;

    try {
      file1 = new File(createdDir, "file1.txt");
      file2 = new File(createdDir, "file2.txt");
      file3 = new File(createdDir, "file3.txt");

      assertTrue(file1.createNewFile());
      assertTrue(file2.createNewFile());
      assertTrue(file3.createNewFile());

      Files.writeString(file1.toPath(), "Content 1");
      Files.writeString(file2.toPath(), "Content 2");
      Files.writeString(file3.toPath(), "Content 3");

      assertEquals(3, Objects.requireNonNull(createdDir.listFiles()).length);
    } finally {
      deleteFileIfExists(file1);
      deleteFileIfExists(file2);
      deleteFileIfExists(file3);
      deleteDirectoryIfExists(createdDir);
    }
  }

  private void deleteFileIfExists(File file) {
    if (file != null && file.exists() && !file.delete())
      log.error("Failed to delete file: {}", forJava(file.getAbsolutePath()));
  }

  private void deleteDirectoryIfExists(File directory) {
    if (directory != null && directory.exists() && directory.isDirectory() && !directory.delete())
      log.error("Failed to delete directory: {}", forJava(directory.getAbsolutePath()));
  }
}
