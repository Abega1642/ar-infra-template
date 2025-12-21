package com.example.arinfra.validator.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.arinfra.InfraGenerated;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@InfraGenerated
class FileValidatorTest {

  private static final String TEST_CONTENT = "test content";
  private static final String FILE_TXT = "file.txt";
  private static final String SUBDIR = "subdir";
  @TempDir Path tempDir;
  private FileValidator subject;

  @BeforeEach
  void setUp() {
    subject = new FileValidator();
  }

  @AfterEach
  void tearDown() {
    subject = null;
  }

  @Test
  void validate_readable_file_with_valid_file_should_succeed() throws IOException {
    Path file = createFile();

    assertDoesNotThrow(() -> subject.validateReadableFile(file));
  }

  @Test
  void validate_readable_file_with_nonexistent_file_should_throw_exception() {
    Path nonexistent = tempDir.resolve("nonexistent.txt");

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> subject.validateReadableFile(nonexistent));

    assertTrue(exception.getMessage().contains("does not exist"));
  }

  @Test
  void validate_readable_file_with_directory_should_throw_exception() throws IOException {
    Path directory = Files.createDirectory(tempDir.resolve(SUBDIR));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> subject.validateReadableFile(directory));

    assertTrue(exception.getMessage().contains("must be a regular file"));
  }

  @Test
  void validate_readable_file_with_symbolic_link_should_throw_exception() throws IOException {
    Path target = createFile();
    Path symlink = tempDir.resolve("symlink.txt");

    try {
      Files.createSymbolicLink(symlink, target);
    } catch (UnsupportedOperationException | SecurityException e) {
      return;
    }

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> subject.validateReadableFile(symlink));

    assertTrue(exception.getMessage().contains("Symbolic links are not allowed"));
  }

  @Test
  void validate_readable_file_with_unreadable_file_should_throw_exception() throws IOException {
    Path file = createFile();

    if (makeUnreadable(file)) {
      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> subject.validateReadableFile(file));

      assertTrue(exception.getMessage().contains("I/O error during validation"));
    }
  }

  @Test
  void validate_with_valid_file_should_succeed() throws IOException {
    Path file = createFile();

    assertDoesNotThrow(() -> subject.validate(file));
  }

  @Test
  void validate_readable_directory_with_valid_directory_should_succeed() throws IOException {
    Path directory = Files.createDirectory(tempDir.resolve(SUBDIR));

    assertDoesNotThrow(() -> subject.validateReadableDirectory(directory));
  }

  @Test
  void validate_readable_directory_with_nonexistent_directory_should_throw_exception() {
    Path nonexistent = tempDir.resolve("nonexistent");

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> subject.validateReadableDirectory(nonexistent));

    assertTrue(exception.getMessage().contains("does not exist"));
  }

  @Test
  void validate_readable_directory_with_file_should_throw_exception() throws IOException {
    Path file = createFile();

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> subject.validateReadableDirectory(file));

    assertTrue(exception.getMessage().contains("must be a directory"));
  }

  @Test
  void validate_readable_directory_with_symbolic_link_should_throw_exception() throws IOException {
    Path target = Files.createDirectory(tempDir.resolve(SUBDIR));
    Path symlink = tempDir.resolve("symlink-dir");

    try {
      Files.createSymbolicLink(symlink, target);
    } catch (UnsupportedOperationException | SecurityException e) {
      return;
    }

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> subject.validateReadableDirectory(symlink));

    assertTrue(exception.getMessage().contains("Symbolic links are not allowed"));
  }

  @Test
  void validate_readable_directory_with_unreadable_directory_should_throw_exception()
      throws IOException {
    Path directory = Files.createDirectory(tempDir.resolve(SUBDIR));

    if (makeUnreadable(directory)) {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> subject.validateReadableDirectory(directory));

      assertTrue(exception.getMessage().contains("I/O error during validation"));
    }
  }

  @Test
  void validate_writable_directory_with_valid_directory_should_succeed() throws IOException {
    Path directory = Files.createDirectory(tempDir.resolve(SUBDIR));

    assertDoesNotThrow(() -> subject.validateWritableDirectory(directory));
  }

  @Test
  void validate_writable_directory_with_nonexistent_directory_should_create_and_succeed() {
    Path nonexistent = tempDir.resolve("new-dir");

    assertDoesNotThrow(() -> subject.validateWritableDirectory(nonexistent));
    assertTrue(Files.exists(nonexistent));
    assertTrue(Files.isDirectory(nonexistent));
  }

  @Test
  void validate_writable_directory_with_nested_nonexistent_directories_should_create_all() {
    Path nested = tempDir.resolve("level1/level2/level3");

    assertDoesNotThrow(() -> subject.validateWritableDirectory(nested));
    assertTrue(Files.exists(nested));
    assertTrue(Files.isDirectory(nested));
  }

  @Test
  void validate_writable_directory_with_file_should_throw_exception() throws IOException {
    Path file = createFile();

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> subject.validateWritableDirectory(file));

    assertTrue(exception.getMessage().contains("must be a directory"));
  }

  @Test
  void validate_writable_directory_with_unwritable_directory_should_throw_exception()
      throws IOException {
    Path directory = Files.createDirectory(tempDir.resolve(SUBDIR));

    if (makeUnwritable(directory)) {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> subject.validateWritableDirectory(directory));

      assertTrue(exception.getMessage().contains("not writable"));
    }
  }

  @Test
  void get_validated_type_should_return_path_class() {
    assertEquals(Path.class, subject.getValidatedType());
  }

  private Path createFile() throws IOException {
    Path file = tempDir.resolve(FileValidatorTest.FILE_TXT);
    Files.writeString(file, TEST_CONTENT);
    return file;
  }

  private boolean makeUnreadable(Path path) {
    try {
      Set<PosixFilePermission> perms = new HashSet<>();
      Files.setPosixFilePermissions(path, perms);
      return true;
    } catch (UnsupportedOperationException | IOException e) {
      // Skip test if POSIX permissions are not supported
      return false;
    }
  }

  private boolean makeUnwritable(Path path) {
    try {
      Set<PosixFilePermission> perms = new HashSet<>();
      perms.add(PosixFilePermission.OWNER_READ);
      perms.add(PosixFilePermission.OWNER_EXECUTE);
      Files.setPosixFilePermissions(path, perms);
      return true;
    } catch (UnsupportedOperationException | IOException e) {
      return false;
    }
  }
}
