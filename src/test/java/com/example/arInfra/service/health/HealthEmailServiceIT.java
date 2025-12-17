package com.example.arInfra.service.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.arInfra.InfraGenerated;
import com.example.arInfra.file.SecureTempFileManager;
import com.example.arInfra.mail.Email;
import com.example.arInfra.mail.Mailer;
import jakarta.mail.internet.AddressException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@InfraGenerated
class HealthEmailServiceIT {

  private static final String VALID_EMAIL = "test@example.com";
  private static final String INVALID_EMAIL = "invalid-email";
  private static final String HEALTH_CHECK_PREFIX = "[unfaked health check";
  private static final String TEST_ATTACHMENT = "test-attachment-12345.txt";

  @TempDir Path tempDir;

  @Mock private Mailer mailer;
  @Mock private SecureTempFileManager secureTempFileManager;

  @InjectMocks private HealthEmailService healthEmailService;

  @Captor private ArgumentCaptor<Email> emailCaptor;

  @BeforeEach
  void setUp() throws Exception {
    File mockAttachment = tempDir.resolve(TEST_ATTACHMENT).toFile();
    boolean created = mockAttachment.createNewFile();
    if (!created) {
      throw new IOException("Failed to create test attachment file");
    }

    lenient()
        .when(
            secureTempFileManager.createSecureTempFileWithContent(
                anyString(), anyString(), anyString()))
        .thenReturn(mockAttachment);
  }

  @Test
  void should_send_all_five_health_check_emails_successfully() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);
    verify(mailer, times(5)).accept(any(Email.class));
  }

  @Test
  void should_send_subject_only_email_as_first_test() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    Email email = emailCaptor.getAllValues().getFirst();

    assertEquals(VALID_EMAIL, email.to().getAddress());
    assertTrue(email.subject().contains("1/5] Subject only"));
    assertTrue(email.cc().isEmpty());
    assertTrue(email.bcc().isEmpty());
    assertNull(email.htmlBody());
    assertTrue(email.attachments().isEmpty());
  }

  @Test
  void should_send_email_with_cc_as_second_test() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    Email email = emailCaptor.getAllValues().get(1);

    assertEquals("test+cc@example.com", email.cc().getFirst().getAddress());
  }

  @Test
  void should_send_email_with_bcc_as_third_test() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    Email email = emailCaptor.getAllValues().get(2);

    assertEquals("test+bcc@example.com", email.bcc().getFirst().getAddress());
  }

  @Test
  void should_send_email_with_html_body_as_fourth_test() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    Email email = emailCaptor.getAllValues().get(3);

    assertNotNull(email.htmlBody());
    assertTrue(email.htmlBody().contains("Hello from Unfaked!"));
  }

  @Test
  void should_send_email_with_attachment_as_fifth_test() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    Email email = emailCaptor.getAllValues().get(4);

    assertEquals(1, email.attachments().size());
    assertTrue(email.attachments().getFirst().getName().startsWith("test-attachment"));

    verify(secureTempFileManager).deleteTempFile(any(File.class));
  }

  @Test
  void should_throw_address_exception_when_email_is_invalid() {
    assertThrows(
        AddressException.class, () -> healthEmailService.sendHealthCheckEmails(INVALID_EMAIL));

    verify(mailer, never()).accept(any());
  }

  @Test
  void should_handle_email_with_subdomain_correctly() throws Exception {
    healthEmailService.sendHealthCheckEmails("user@mail.example.com");

    verify(mailer, times(5)).accept(emailCaptor.capture());
    Email email = emailCaptor.getAllValues().get(1);

    assertEquals("user+cc@mail.example.com", email.cc().getFirst().getAddress());
  }

  @Test
  void should_use_correct_health_check_prefix_in_all_subjects() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    emailCaptor
        .getAllValues()
        .forEach(e -> assertTrue(e.subject().startsWith(HEALTH_CHECK_PREFIX)));
  }

  @Test
  void should_delete_attachment_file_after_sending() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(secureTempFileManager)
        .createSecureTempFileWithContent(anyString(), anyString(), anyString());
    verify(secureTempFileManager).deleteTempFile(any(File.class));
  }
}
