package com.example.arinfra.mail;

import com.example.arinfra.InfraGenerated;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.util.List;

/**
 * Immutable data carrier for email message composition.
 *
 * <p>This record encapsulates all components needed to construct and send an email, including
 * recipients (to, cc, bcc), content (subject, HTML body), and optional file attachments.
 *
 * <p>Used primarily by email services for health checks and notification functionality. All
 * recipient fields use {@link InternetAddress} for proper email address validation.
 *
 * @param to the primary recipient of the email
 * @param cc the list of carbon copy recipients (can be null or empty)
 * @param bcc the list of blind carbon copy recipients (can be null or empty)
 * @param subject the email subject line
 * @param htmlBody the HTML-formatted email body content
 * @param attachments the list of files to attach to the email (can be null or empty)
 */
@InfraGenerated
public record Email(
    InternetAddress to,
    List<InternetAddress> cc,
    List<InternetAddress> bcc,
    String subject,
    String htmlBody,
    List<File> attachments) {}
