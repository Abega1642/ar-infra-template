package com.example.arinfra.validator.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.arinfra.InfraGenerated;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@InfraGenerated
class ZipEntryValidatorTest {

  private static final long BYTES_PER_KB = 1_024L;
  private static final long BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB;
  private static final long BYTES_PER_GB = BYTES_PER_KB * BYTES_PER_MB;
  private static final long MAX_DECOMPRESSED_SIZE = BYTES_PER_GB;
  private static final long MAX_ENTRY_SIZE = 512L * BYTES_PER_MB;
  private static final int MAX_ENTRY_COUNT = 10_000;
  private static final int MAX_COMPRESSION_RATIO = 100;

  private static final String VALID_ENTRY_NAME = "dir/file.txt";
  private static final String EMPTY_ENTRY_NAME = "";
  private static final long VALID_SIZE = 1000L;
  private static final long VALID_COMPRESSED_SIZE = 500L;

  private ZipEntryValidator subject;

  @BeforeEach
  void setUp() {
    subject = new ZipEntryValidator();
  }

  @Test
  void validate_with_valid_entry_should_succeed() {
    ZipEntry entry = createValidEntry(VALID_ENTRY_NAME);
    assertDoesNotThrow(() -> subject.validate(entry));
  }

  @Test
  void validate_with_invalid_entry_name_should_throw_exception() {
    ZipEntry entry = createValidEntry("../../../etc/passwd");
    assertThrows(SecurityException.class, () -> subject.validate(entry));
  }

  @Test
  void validate_with_oversized_entry_should_throw_exception() {
    ZipEntry entry = createValidEntry(VALID_ENTRY_NAME);
    entry.setSize(MAX_ENTRY_SIZE + 1);

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validate(entry));

    assertTrue(exception.getMessage().contains("size too large"));
  }

  @Test
  void validate_entry_count_with_valid_count_should_succeed() {
    assertDoesNotThrow(() -> subject.validateEntryCount(100));
    assertDoesNotThrow(() -> subject.validateEntryCount(MAX_ENTRY_COUNT));
  }

  @Test
  void validate_entry_count_with_exceeding_count_should_throw_exception() {
    SecurityException exception =
        assertThrows(
            SecurityException.class, () -> subject.validateEntryCount(MAX_ENTRY_COUNT + 1));

    assertTrue(exception.getMessage().contains("too many entries"));
    assertTrue(exception.getMessage().contains("zip bomb"));
  }

  @Test
  void validate_entry_count_at_boundary_should_succeed() {
    assertDoesNotThrow(() -> subject.validateEntryCount(MAX_ENTRY_COUNT));
  }

  @Test
  void validate_entry_name_with_valid_name_should_succeed() {
    assertDoesNotThrow(() -> subject.validateEntryName("file.txt"));
    assertDoesNotThrow(() -> subject.validateEntryName("dir/file.txt"));
    assertDoesNotThrow(() -> subject.validateEntryName("dir/subdir/file.txt"));
  }

  @Test
  void validate_entry_name_with_null_should_throw_exception() {
    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validateEntryName(null));

    assertTrue(exception.getMessage().contains("cannot be null or empty"));
  }

  @Test
  void validate_entry_name_with_empty_string_should_throw_exception() {
    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validateEntryName(EMPTY_ENTRY_NAME));

    assertTrue(exception.getMessage().contains("cannot be null or empty"));
  }

  @Test
  void validate_entry_name_with_whitespace_only_should_throw_exception() {
    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validateEntryName("   "));

    assertTrue(exception.getMessage().contains("cannot be null or empty"));
  }

  @Test
  void validate_entry_name_with_path_traversal_should_throw_exception() {
    assertThrows(SecurityException.class, () -> subject.validateEntryName("../file.txt"));
    assertThrows(SecurityException.class, () -> subject.validateEntryName("dir/../../file.txt"));
    assertThrows(SecurityException.class, () -> subject.validateEntryName(".."));
  }

  @Test
  void validate_entry_name_with_absolute_path_should_throw_exception() {
    SecurityException exception1 =
        assertThrows(SecurityException.class, () -> subject.validateEntryName("/etc/passwd"));
    assertTrue(exception1.getMessage().contains("absolute path"));

    SecurityException exception2 =
        assertThrows(SecurityException.class, () -> subject.validateEntryName("/file.txt"));
    assertTrue(exception2.getMessage().contains("absolute path"));
  }

  @Test
  void validate_entry_name_with_windows_absolute_path_should_throw_exception() {
    SecurityException exception =
        assertThrows(
            SecurityException.class,
            () -> subject.validateEntryName("C:/Windows/System32/file.txt"));

    assertTrue(exception.getMessage().contains("absolute path"));
  }

  @Test
  void validate_entry_name_with_windows_absolute_path_lowercase_should_throw_exception() {
    SecurityException exception =
        assertThrows(
            SecurityException.class, () -> subject.validateEntryName("c:/windows/file.txt"));

    assertTrue(exception.getMessage().contains("absolute path"));
  }

  @Test
  void validate_entry_name_with_symbolic_link_should_throw_exception() {
    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validateEntryName("link->target"));

    assertTrue(exception.getMessage().contains("symbolic link"));
  }

  @Test
  void validate_entry_name_with_backslashes_should_normalize() {
    assertDoesNotThrow(() -> subject.validateEntryName("dir\\file.txt"));
  }

  @Test
  void validate_entry_name_with_too_long_name_should_throw_exception() {
    String longName = "a".repeat(5000);

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validateEntryName(longName));

    assertTrue(exception.getMessage().contains("too long"));
  }

  @Test
  void validate_entry_name_with_null_byte_should_throw_exception() {
    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validateEntryName("file\0.txt"));

    assertTrue(exception.getMessage().contains("null byte"));
  }

  @Test
  void validate_entry_name_with_control_characters_should_throw_exception() {
    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validateEntryName("file\u0001.txt"));

    assertTrue(exception.getMessage().contains("control character"));
  }

  @Test
  void validate_entry_name_with_encoded_traversal_should_throw_exception() {
    assertThrows(SecurityException.class, () -> subject.validateEntryName("dir/%2e%2e/file.txt"));
  }

  @Test
  void validate_entry_name_with_multiple_leading_slashes_should_throw_exception() {
    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validateEntryName("///etc/passwd"));

    assertTrue(exception.getMessage().contains("absolute path"));
  }

  @Test
  void validate_entry_size_with_valid_size_should_succeed() {
    ZipEntry entry = createValidEntry(VALID_ENTRY_NAME);
    entry.setSize(VALID_SIZE);
    entry.setCompressedSize(VALID_COMPRESSED_SIZE);

    assertDoesNotThrow(() -> subject.validateEntrySize(entry));
  }

  @Test
  void validate_entry_size_with_exceeding_size_should_throw_exception() {
    ZipEntry entry = createValidEntry(VALID_ENTRY_NAME);
    entry.setSize(MAX_ENTRY_SIZE + 1);

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validateEntrySize(entry));

    assertTrue(exception.getMessage().contains("size too large"));
  }

  @Test
  void validate_entry_size_with_suspicious_compression_ratio_should_throw_exception() {
    ZipEntry entry = createValidEntry(VALID_ENTRY_NAME);
    entry.setSize(10_000_000);
    entry.setCompressedSize(10_000);

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validateEntrySize(entry));

    assertTrue(exception.getMessage().contains("compression ratio"));
  }

  @Test
  void validate_entry_size_with_compression_ratio_at_limit_should_succeed() {
    ZipEntry entry = createValidEntry(VALID_ENTRY_NAME);
    entry.setSize(MAX_COMPRESSION_RATIO * 1000);
    entry.setCompressedSize(1_000);

    assertDoesNotThrow(() -> subject.validateEntrySize(entry));
  }

  @Test
  void validate_entry_size_with_unknown_size_should_throw_exception() {
    ZipEntry entry = new ZipEntry(VALID_ENTRY_NAME);

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validateEntrySize(entry));

    assertTrue(exception.getMessage().contains("unknown size"));
    assertTrue(exception.getMessage().contains("getSize() returned -1"));
  }

  @Test
  void validate_entry_size_with_zero_compressed_size_should_succeed() {
    ZipEntry entry = createValidEntry(VALID_ENTRY_NAME);
    entry.setSize(VALID_SIZE);
    entry.setCompressedSize(0);

    assertDoesNotThrow(() -> subject.validateEntrySize(entry));
  }

  @Test
  void validate_entry_size_at_boundary_should_succeed() {
    ZipEntry entry = createValidEntry(VALID_ENTRY_NAME);
    entry.setSize(MAX_ENTRY_SIZE);
    entry.setCompressedSize(MAX_ENTRY_SIZE / 10); // Reasonable compression ratio

    assertDoesNotThrow(() -> subject.validateEntrySize(entry));
  }

  @Test
  void validate_path_traversal_with_valid_path_should_succeed() {
    Path targetDir = Paths.get("/tmp/extract");
    Path entryPath = Paths.get("/tmp/extract/file.txt");

    assertDoesNotThrow(() -> subject.validatePathTraversal(entryPath, targetDir));
  }

  @Test
  void validate_path_traversal_with_nested_valid_path_should_succeed() {
    Path targetDir = Paths.get("/tmp/extract");
    Path entryPath = Paths.get("/tmp/extract/dir/subdir/file.txt");

    assertDoesNotThrow(() -> subject.validatePathTraversal(entryPath, targetDir));
  }

  @Test
  void validate_path_traversal_with_path_outside_target_should_throw_exception() {
    Path targetDir = Paths.get("/tmp/extract");
    Path entryPath = Paths.get("/tmp/other/file.txt");

    SecurityException exception =
        assertThrows(
            SecurityException.class, () -> subject.validatePathTraversal(entryPath, targetDir));

    assertTrue(exception.getMessage().contains("Path traversal attempt"));
  }

  @Test
  void validate_path_traversal_with_parent_reference_outside_should_throw_exception() {
    Path targetDir = Paths.get("/tmp/extract");
    Path entryPath = Paths.get("/tmp/extract/../file.txt");

    SecurityException exception =
        assertThrows(
            SecurityException.class, () -> subject.validatePathTraversal(entryPath, targetDir));

    assertTrue(exception.getMessage().contains("outside target directory"));
  }

  @Test
  void validate_duplicate_entry_with_unique_entry_should_succeed() {
    Set<String> extractedPaths = new HashSet<>();
    extractedPaths.add("file1.txt");

    assertDoesNotThrow(() -> subject.validateDuplicateEntry("file2.txt", extractedPaths));
  }

  @Test
  void validate_duplicate_entry_with_duplicate_should_throw_exception() {
    Set<String> extractedPaths = new HashSet<>();
    extractedPaths.add("file.txt");

    SecurityException exception =
        assertThrows(
            SecurityException.class,
            () -> subject.validateDuplicateEntry("file.txt", extractedPaths));

    assertTrue(exception.getMessage().contains("Duplicate ZIP entry"));
  }

  @Test
  void validate_duplicate_entry_with_empty_set_should_succeed() {
    Set<String> extractedPaths = new HashSet<>();

    assertDoesNotThrow(() -> subject.validateDuplicateEntry("file.txt", extractedPaths));
  }

  @Test
  void validate_total_decompressed_size_with_valid_size_should_succeed() {
    assertDoesNotThrow(() -> subject.validateTotalDecompressedSize(100 * BYTES_PER_MB));
    assertDoesNotThrow(() -> subject.validateTotalDecompressedSize(MAX_DECOMPRESSED_SIZE));
  }

  @Test
  void validate_total_decompressed_size_with_exceeding_size_should_throw_exception() {
    SecurityException exception =
        assertThrows(
            SecurityException.class,
            () -> subject.validateTotalDecompressedSize(MAX_DECOMPRESSED_SIZE + 1));

    assertTrue(exception.getMessage().contains("Total decompressed size exceeds limit"));
    assertTrue(exception.getMessage().contains("zip bomb"));
  }

  @Test
  void validate_total_decompressed_size_at_boundary_should_succeed() {
    assertDoesNotThrow(() -> subject.validateTotalDecompressedSize(MAX_DECOMPRESSED_SIZE));
  }

  @Test
  void validate_actual_extracted_size_with_valid_size_should_succeed() {
    assertDoesNotThrow(() -> subject.validateActualExtractedSize(VALID_SIZE, "file.txt"));
    assertDoesNotThrow(() -> subject.validateActualExtractedSize(MAX_ENTRY_SIZE, "file.txt"));
  }

  @Test
  void validate_actual_extracted_size_with_exceeding_size_should_throw_exception() {
    SecurityException exception =
        assertThrows(
            SecurityException.class,
            () -> subject.validateActualExtractedSize(MAX_ENTRY_SIZE + 1, "file.txt"));

    assertTrue(exception.getMessage().contains("Entry size exceeded during extraction"));
  }

  @Test
  void validate_actual_extracted_size_at_boundary_should_succeed() {
    assertDoesNotThrow(() -> subject.validateActualExtractedSize(MAX_ENTRY_SIZE, "file.txt"));
  }

  @Test
  void get_validated_type_should_return_zip_entry_class() {
    assertEquals(ZipEntry.class, subject.getValidatedType());
  }

  private ZipEntry createValidEntry(String name) {
    ZipEntry entry = new ZipEntry(name);
    entry.setSize(VALID_SIZE);
    entry.setCompressedSize(VALID_COMPRESSED_SIZE);
    return entry;
  }
}
