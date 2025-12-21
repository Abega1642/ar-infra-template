package com.example.arinfra.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.arinfra.InfraGenerated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@InfraGenerated
class FilenameSanitizerTest {

  private static final String DEFAULT_FILENAME = "upload.tmp";

  private FilenameSanitizer sanitizer;

  @BeforeEach
  void setUp() {
    sanitizer = new FilenameSanitizer();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t", "\n"})
  void apply_should_return_default_for_null_or_blank_input(String input) {
    String result = sanitizer.apply(input);

    assertEquals(DEFAULT_FILENAME, result);
  }

  @Test
  void apply_should_remove_windows_path_traversal() {
    String malicious = "..\\..\\..\\Windows\\System32\\cmd.exe";

    String result = sanitizer.apply(malicious);

    assertEquals("WindowsSystem32cmd.exe", result);
    assertFalse(result.contains(".."));
    assertFalse(result.contains("\\"));
  }

  @Test
  void apply_should_extract_basename_from_windows_path() {
    String path = "C:\\Users\\Admin\\Desktop\\file.txt";

    String result = sanitizer.apply(path);

    assertEquals("file.txt", result);
  }

  @Test
  void apply_should_prevent_hidden_file_exploitation() {
    String hiddenFile = ".htaccess";

    String result = sanitizer.apply(hiddenFile);

    assertEquals("_htaccess", result);
    assertFalse(result.startsWith("."));
  }

  @Test
  void apply_should_preserve_valid_filename() {
    String validFilename = "my-video_file.mp4";

    String result = sanitizer.apply(validFilename);

    assertEquals("my-video_file.mp4", result);
  }

  @Test
  void apply_should_allow_safe_characters() {
    String filename = "Test_File-123.ABC.xyz";

    String result = sanitizer.apply(filename);

    assertEquals("Test_File-123.ABC.xyz", result);
  }

  @Test
  void apply_should_replace_spaces_with_underscore() {
    String filename = "my video file.mp4";

    String result = sanitizer.apply(filename);

    assertEquals("my_video_file.mp4", result);
  }

  @Test
  void apply_should_truncate_long_filenames_preserving_extension() {
    String longFilename = "a".repeat(250) + ".mp4";

    String result = sanitizer.apply(longFilename);

    assertTrue(result.length() <= 200);
    assertTrue(result.endsWith(".mp4"));
    assertEquals(200, result.length());
  }

  @Test
  void apply_should_truncate_long_filenames_without_extension() {
    String longFilename = "a".repeat(250);

    String result = sanitizer.apply(longFilename);

    assertEquals(200, result.length());
  }

  @ParameterizedTest
  @CsvSource({
    "file.jpg, file.jpg",
    "FILE.MP4, FILE.MP4",
    "document.pdf, document.pdf",
    "image.PNG, image.PNG"
  })
  void apply_should_preserve_extensions(String input, String expected) {
    String result = sanitizer.apply(input);

    assertEquals(expected, result);
  }

  @Test
  void apply_should_handle_command_injection_attempt() {
    String malicious = "file;rm -rf /.jpg";

    String result = sanitizer.apply(malicious);

    assertEquals("file_rm_-rf_.jpg", result);
    assertFalse(result.contains(";"));
  }

  @Test
  void apply_should_handle_unicode_characters() {
    String unicode = "文件.jpg";

    String result = sanitizer.apply(unicode);

    assertEquals("__.jpg", result);
  }

  @Test
  void apply_should_return_default_when_only_invalid_chars() {
    String invalid = "@#$%^&*()";

    String result = sanitizer.apply(invalid);

    assertEquals(DEFAULT_FILENAME, result);
  }

  @Test
  void apply_should_return_default_when_only_dots() {
    String dots = "....";

    String result = sanitizer.apply(dots);

    assertEquals(DEFAULT_FILENAME, result);
  }

  @Test
  void apply_should_be_idempotent() {
    String filename = "test-file.mp4";

    String firstPass = sanitizer.apply(filename);
    String secondPass = sanitizer.apply(firstPass);

    assertEquals(firstPass, secondPass);
  }

  @Test
  void apply_should_handle_multiple_dots_in_filename() {
    String filename = "my.file.name.with.dots.mp4";

    String result = sanitizer.apply(filename);

    assertEquals("my.file.name.with.dots.mp4", result);
  }

  @Test
  void apply_should_handle_edge_case_with_only_extension() {
    String filename = ".mp4";

    String result = sanitizer.apply(filename);

    assertEquals("_.mp4", result);
  }

  @Test
  void apply_should_handle_extremely_long_extension() {
    String filename = "file." + "x".repeat(50);

    String result = sanitizer.apply(filename);

    assertTrue(result.length() <= 200);
    assertTrue(result.startsWith("file."));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "../../etc/shadow",
        "..\\..\\Windows\\win.ini",
        "/etc/passwd",
        "C:\\Windows\\System32\\config\\SAM",
        "file|rm -rf /",
        "file && malicious",
        "file`whoami`",
        "file$(command)"
      })
  void apply_should_sanitize_various_attack_vectors(String malicious) {
    String result = sanitizer.apply(malicious);

    assertNotNull(result);
    assertFalse(result.contains(".."));
    assertFalse(result.contains("/"));
    assertFalse(result.contains("\\"));
    assertFalse(result.contains("|"));
    assertFalse(result.contains("&"));
    assertFalse(result.contains("`"));
    assertFalse(result.contains("$"));
  }
}
