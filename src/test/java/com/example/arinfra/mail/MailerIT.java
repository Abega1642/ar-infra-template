package com.example.arinfra.mail;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.conf.FacadeIT;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;

@InfraGenerated
class MailerIT extends FacadeIT {

  @TempDir Path tempDir;
  @Autowired private Mailer mailer;
  @Autowired private JavaMailSender mailSender;
  private InternetAddress testRecipient;
  private InternetAddress ccRecipient;
  private InternetAddress bccRecipient;

  @BeforeEach
  void setUp() throws Exception {
    testRecipient = new InternetAddress("test@example.com");
    ccRecipient = new InternetAddress("cc@example.com");
    bccRecipient = new InternetAddress("bcc@example.com");
  }

  @Test
  void should_send_simple_email() {
    var email =
        new Email(
            testRecipient, List.of(), List.of(), "Test Subject", "<p>Test Body</p>", List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_send_email_with_cc() {
    var email =
        new Email(
            testRecipient,
            List.of(ccRecipient),
            List.of(),
            "Test with CC",
            "<p>Test Body</p>",
            List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_send_email_with_bcc() {
    var email =
        new Email(
            testRecipient,
            List.of(),
            List.of(bccRecipient),
            "Test with BCC",
            "<p>Test Body</p>",
            List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_send_email_with_cc_and_bcc() {
    var email =
        new Email(
            testRecipient,
            List.of(ccRecipient),
            List.of(bccRecipient),
            "Test with CC and BCC",
            "<p>Test Body</p>",
            List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_send_email_without_html_body() {
    var email =
        new Email(testRecipient, List.of(), List.of(), "Test without HTML", null, List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_send_email_with_empty_html_body() {
    var email =
        new Email(testRecipient, List.of(), List.of(), "Test with empty HTML", "", List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_send_email_with_attachment() throws IOException {
    var attachment = createTestFile("test-attachment.txt", "This is test content");
    var email =
        new Email(
            testRecipient,
            List.of(),
            List.of(),
            "Test with Attachment",
            "<p>See attachment</p>",
            List.of(attachment));

    assertDoesNotThrow(() -> mailer.accept(email));
    assertTrue(attachment.exists(), "Attachment file should still exist after sending");
  }

  @Test
  void should_send_email_with_multiple_attachments() throws IOException {
    var attachment1 = createTestFile("attachment1.txt", "Content 1");
    var attachment2 = createTestFile("attachment2.txt", "Content 2");
    var attachment3 = createTestFile("attachment3.pdf", "PDF Content");

    var email =
        new Email(
            testRecipient,
            List.of(),
            List.of(),
            "Test with Multiple Attachments",
            "<p>See attachments</p>",
            List.of(attachment1, attachment2, attachment3));

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_handle_null_recipient_gracefully() {
    var email =
        new Email(null, List.of(), List.of(), "Test Subject", "<p>Test Body</p>", List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_handle_null_cc_list() {
    var email =
        new Email(testRecipient, null, List.of(), "Test Subject", "<p>Test Body</p>", List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_handle_null_bcc_list() {
    var email =
        new Email(testRecipient, List.of(), null, "Test Subject", "<p>Test Body</p>", List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_handle_null_attachments_list() {
    var email =
        new Email(testRecipient, List.of(), List.of(), "Test Subject", "<p>Test Body</p>", null);

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_handle_empty_lists() {
    var email =
        new Email(
            testRecipient, List.of(), List.of(), "Test Subject", "<p>Test Body</p>", List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_send_email_with_long_subject() {
    String longSubject =
        "This is a very long subject line that might cause issues if not handled properly by the"
            + " email system and we want to make sure it works correctly";
    var email =
        new Email(testRecipient, List.of(), List.of(), longSubject, "<p>Test Body</p>", List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_send_email_with_unicode_characters() {
    var email =
        new Email(
            testRecipient,
            List.of(),
            List.of(),
            "Test with émojis 🎉 and ñoñó",
            "<p>Unicode test: こんにちは 你好 مرحبا</p>",
            List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_continue_sending_even_if_one_attachment_fails() throws IOException {
    var validAttachment = createTestFile("valid.txt", "Valid content");
    var invalidAttachment = new File("/non/existent/path/invalid.txt");

    var email =
        new Email(
            testRecipient,
            List.of(),
            List.of(),
            "Test with Invalid Attachment",
            "<p>Mixed attachments</p>",
            List.of(validAttachment, invalidAttachment));

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_send_email_with_complex_html() {
    String complexHtml =
        """
        <html>
          <head>
            <style>
              body { font-family: Arial; }
              .header { color: blue; }
            </style>
          </head>
          <body>
            <div class="header">
              <h1>Welcome!</h1>
            </div>
            <p>This is a <strong>complex</strong> HTML email.</p>
            <ul>
              <li>Item 1</li>
              <li>Item 2</li>
            </ul>
          </body>
        </html>
        """;

    var email =
        new Email(testRecipient, List.of(), List.of(), "Complex HTML Test", complexHtml, List.of());

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_handle_empty_file() throws IOException {
    var emptyFile = createTestFile("empty.txt", "");
    var email =
        new Email(
            testRecipient,
            List.of(),
            List.of(),
            "Test with Empty Attachment",
            "<p>Empty file attached</p>",
            List.of(emptyFile));

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  @Test
  void should_handle_binary_file_attachment() throws IOException {
    var binaryFile = tempDir.resolve("binary.bin").toFile();
    byte[] binaryContent = new byte[] {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};
    Files.write(binaryFile.toPath(), binaryContent);
    assertTrue(binaryFile.exists(), "Binary file should be created");

    var email =
        new Email(
            testRecipient,
            List.of(),
            List.of(),
            "Test with Binary Attachment",
            "<p>Binary file attached</p>",
            List.of(binaryFile));

    assertDoesNotThrow(() -> mailer.accept(email));
  }

  private File createTestFile(String filename, String content) throws IOException {
    Path filePath = tempDir.resolve(filename);
    Files.writeString(filePath, content);
    File file = filePath.toFile();
    assertTrue(file.exists(), "Test file should be created: " + filename);
    return file;
  }
}
