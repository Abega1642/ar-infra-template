package com.example.arinfra.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.arinfra.InfraGenerated;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

@InfraGenerated
class MultipartFileConverterTest {

  private static final String TEST_CONTENT = "test file content";
  private static final String VALID_FILENAME = "document.pdf";
  private final TempFileCleaner cleaner = new TempFileCleaner();
  private final SecureTempFileManager TEMP_FILE_MANAGER = new SecureTempFileManager(cleaner);
  private MultipartFileConverter converter;

  @BeforeEach
  void setUp() {
    converter = new MultipartFileConverter(TEMP_FILE_MANAGER);
  }

  @Test
  void converts_valid_file_successfully() throws IOException {
    MultipartFile mockFile = createMockFile(VALID_FILENAME, TEST_CONTENT);

    File result = converter.apply(mockFile);

    assertNotNull(result, "Converted file should not be null");
    assertTrue(result.exists(), "Converted file should exist");
    assertTrue(result.getName().startsWith("upload-"), "File should have correct prefix");
    assertTrue(result.getName().endsWith(".pdf"), "File should have correct extension");

    String content = Files.readString(result.toPath());
    assertEquals(TEST_CONTENT, content, "File content should match");

    boolean deleted = result.delete();
    assertTrue(deleted, "File should be deleted successfully");
  }

  @Test
  void handles_different_valid_extensions() throws IOException {
    String[] validFilenames = {"image.jpg", "document.pdf", "data.txt", "sheet.csv", "file.png"};

    for (String filename : validFilenames) {
      MultipartFile mockFile = createMockFile(filename, TEST_CONTENT);
      File result = converter.apply(mockFile);

      assertNotNull(result, "File should be converted: " + filename);
      assertTrue(result.exists(), "Converted file should exist: " + filename);

      boolean deleted = result.delete();
      assertTrue(deleted, "File should be deleted successfully: " + filename);
    }
  }

  @Test
  void sanitizes_path_traversal_attempt() throws IOException {
    MultipartFile mockFile = createMockFile("../../etc/passwd", TEST_CONTENT);

    File result = converter.apply(mockFile);

    assertNotNull(result, "File should still be created with safe extension");
    assertTrue(result.getName().endsWith(".tmp"), "Should fallback to .tmp for malicious filename");
    assertTrue(result.getAbsolutePath().contains("upload-"), "Should be in temp directory");

    boolean deleted = result.delete();
    assertTrue(deleted, "File should be deleted successfully");
  }

  @Test
  void sanitizes_filename_with_path_separators() throws IOException {
    String[] maliciousFilenames = {
      "/etc/passwd.txt", "\\windows\\system32\\config.txt", "../../../secret.txt"
    };

    for (String filename : maliciousFilenames) {
      MultipartFile mockFile = createMockFile(filename, TEST_CONTENT);
      File result = converter.apply(mockFile);

      assertNotNull(result, "File should be created safely: " + filename);
      assertTrue(result.exists(), "File should exist: " + filename);

      boolean deleted = result.delete();
      assertTrue(deleted, "File should be deleted successfully: " + filename);
    }
  }

  @Test
  void handles_filename_without_extension() throws IOException {
    MultipartFile mockFile = createMockFile("fileWithoutExtension", TEST_CONTENT);

    File result = converter.apply(mockFile);

    assertNotNull(result, "File should be created with default extension");
    assertTrue(result.getName().endsWith(".tmp"), "Should use .tmp for files without extension");

    boolean deleted = result.delete();
    assertTrue(deleted, "File should be deleted successfully");
  }

  @Test
  void handles_null_filename() throws IOException {
    MultipartFile mockFile = createMockFile(null, TEST_CONTENT);

    File result = converter.apply(mockFile);

    assertNotNull(result, "File should be created with default extension");
    assertTrue(result.getName().endsWith(".tmp"), "Should use .tmp for null filename");

    boolean deleted = result.delete();
    assertTrue(deleted, "File should be deleted successfully");
  }

  @Test
  void handles_blank_filename() throws IOException {
    MultipartFile mockFile = createMockFile("   ", TEST_CONTENT);

    File result = converter.apply(mockFile);

    assertNotNull(result, "File should be created with default extension");
    assertTrue(result.getName().endsWith(".tmp"), "Should use .tmp for blank filename");

    boolean deleted = result.delete();
    assertTrue(deleted, "File should be deleted successfully");
  }

  @Test
  void handles_filename_with_only_dots() throws IOException {
    MultipartFile mockFile = createMockFile("...", TEST_CONTENT);

    File result = converter.apply(mockFile);

    assertNotNull(result, "File should be created with default extension");
    assertTrue(result.getName().endsWith(".tmp"), "Should use .tmp for filename with only dots");

    boolean deleted = result.delete();
    assertTrue(deleted, "File should be deleted successfully");
  }

  @Test
  void handles_very_long_extension() throws IOException {
    MultipartFile mockFile = createMockFile("file.verylongextensionnamehere", TEST_CONTENT);

    File result = converter.apply(mockFile);

    assertNotNull(result, "File should be created with default extension");
    assertTrue(result.getName().endsWith(".tmp"), "Should use .tmp for very long extension");

    boolean deleted = result.delete();
    assertTrue(deleted, "File should be deleted successfully");
  }

  @Test
  void handles_extension_with_special_characters() throws IOException {
    String[] specialCharFilenames = {"file.ex!e", "file.pd@f", "file.tx#t", "file.jp$g"};

    for (String filename : specialCharFilenames) {
      MultipartFile mockFile = createMockFile(filename, TEST_CONTENT);
      File result = converter.apply(mockFile);

      assertNotNull(result, "File should be created: " + filename);
      assertTrue(
          result.getName().endsWith(".tmp"), "Should use .tmp for special chars: " + filename);

      boolean deleted = result.delete();
      assertTrue(deleted, "File should be deleted successfully: " + filename);
    }
  }

  @Test
  void throws_exception_when_transfer_fails() throws IOException {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn(VALID_FILENAME);
    doThrow(new IOException("Transfer failed"))
        .when(mockFile)
        .transferTo(org.mockito.ArgumentMatchers.any(File.class));

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> converter.apply(mockFile),
            "Should throw RuntimeException when transfer fails");

    assertEquals("Failed to convert multipart file", exception.getMessage());
  }

  @Test
  void created_file_is_marked_for_deletion_on_exit() throws IOException {
    MultipartFile mockFile = createMockFile(VALID_FILENAME, TEST_CONTENT);

    File result = converter.apply(mockFile);

    // We can't directly test deleteOnExit, but we can verify the file was created
    assertNotNull(result, "File should be created");
    assertTrue(result.exists(), "File should exist");

    // Clean up manually for test
    boolean deleted = result.delete();
    assertTrue(deleted, "File should be deleted successfully");
  }

  @Test
  void handles_mixed_case_extensions() throws IOException {
    String[] mixedCaseFilenames = {"document.PDF", "image.JpG", "file.TxT"};

    for (String filename : mixedCaseFilenames) {
      MultipartFile mockFile = createMockFile(filename, TEST_CONTENT);
      File result = converter.apply(mockFile);

      assertNotNull(result, "File should be converted: " + filename);
      assertTrue(result.exists(), "File should exist: " + filename);

      boolean deleted = result.delete();
      assertTrue(deleted, "File should be deleted successfully: " + filename);
    }
  }

  @Test
  void handles_multiple_dots_in_filename() throws IOException {
    MultipartFile mockFile = createMockFile("my.document.final.pdf", TEST_CONTENT);

    File result = converter.apply(mockFile);

    assertNotNull(result, "File should be converted");
    assertTrue(result.getName().endsWith(".pdf"), "Should use last extension");

    boolean deleted = result.delete();
    assertTrue(deleted, "File should be deleted successfully");
  }

  @Test
  void file_content_is_preserved_correctly() throws IOException {
    String specialContent = "Special chars: éàü\n新しい\nEmoji: 😀";
    MultipartFile mockFile = createMockFile(VALID_FILENAME, specialContent);

    File result = converter.apply(mockFile);

    String content = Files.readString(result.toPath());
    assertEquals(specialContent, content, "Special characters should be preserved");

    boolean deleted = result.delete();
    assertTrue(deleted, "File should be deleted successfully");
  }

  private MultipartFile createMockFile(String filename, String content) throws IOException {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn(filename);
    when(mockFile.getSize()).thenReturn((long) content.length());

    // Mock the transferTo method to actually write the content
    org.mockito.stubbing.Answer<Void> answer =
        invocation -> {
          File targetFile = invocation.getArgument(0);
          Files.writeString(targetFile.toPath(), content);
          return null;
        };
    org.mockito.Mockito.doAnswer(answer)
        .when(mockFile)
        .transferTo(org.mockito.ArgumentMatchers.any(File.class));

    return mockFile;
  }
}
