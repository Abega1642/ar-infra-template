package com.example.arinfra.validator.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.util.unit.DataSize.ofBytes;
import static org.springframework.util.unit.DataSize.ofMegabytes;

import com.example.arinfra.InfraGenerated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;

@InfraGenerated
class MultipartPropertiesValidatorTest {

  private MultipartPropertiesValidator subject;

  @BeforeEach
  void setUp() {
    subject = new MultipartPropertiesValidator();
  }

  @Test
  void validate_with_valid_configuration_should_succeed() {
    MultipartProperties properties = new MultipartProperties();
    properties.setMaxFileSize(ofMegabytes(8));
    properties.setMaxRequestSize(ofMegabytes(8));

    assertDoesNotThrow(() -> subject.validate(properties));
  }

  @Test
  void validate_with_5mb_limit_should_succeed() {
    MultipartProperties properties = new MultipartProperties();
    properties.setMaxFileSize(ofMegabytes(5));
    properties.setMaxRequestSize(ofMegabytes(5));

    assertDoesNotThrow(() -> subject.validate(properties));
  }

  @Test
  void validate_with_10mb_limit_should_succeed_with_warning() {
    MultipartProperties properties = new MultipartProperties();
    properties.setMaxFileSize(ofMegabytes(10));
    properties.setMaxRequestSize(ofMegabytes(10));

    assertDoesNotThrow(() -> subject.validate(properties));
  }

  @Test
  void validate_with_null_max_file_size_should_throw_exception() {
    MultipartProperties properties = new MultipartProperties();
    properties.setMaxFileSize(null);
    properties.setMaxRequestSize(ofMegabytes(8));

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validate(properties));

    assertTrue(exception.getMessage().contains("max-file-size"));
    assertTrue(exception.getMessage().contains("explicitly configured"));
  }

  @Test
  void validate_with_zero_max_file_size_should_throw_exception() {
    MultipartProperties properties = new MultipartProperties();
    properties.setMaxFileSize(ofBytes(0));
    properties.setMaxRequestSize(ofMegabytes(8));

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validate(properties));

    assertTrue(exception.getMessage().contains("max-file-size"));
  }

  @Test
  void validate_with_negative_max_file_size_should_throw_exception() {
    MultipartProperties properties = new MultipartProperties();
    properties.setMaxFileSize(ofBytes(-1));
    properties.setMaxRequestSize(ofMegabytes(8));

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validate(properties));

    assertTrue(exception.getMessage().contains("max-file-size"));
  }

  @Test
  void validate_with_null_max_request_size_should_throw_exception() {
    MultipartProperties properties = new MultipartProperties();
    properties.setMaxFileSize(ofMegabytes(8));
    properties.setMaxRequestSize(null);

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validate(properties));

    assertTrue(exception.getMessage().contains("max-request-size"));
    assertTrue(exception.getMessage().contains("explicitly configured"));
  }

  @Test
  void validate_with_excessive_max_file_size_should_throw_exception() {
    MultipartProperties properties = new MultipartProperties();
    properties.setMaxFileSize(ofMegabytes(150));
    properties.setMaxRequestSize(ofMegabytes(8));

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validate(properties));

    assertTrue(exception.getMessage().contains("exceeds absolute maximum"));
    assertTrue(exception.getMessage().contains("DoS risk"));
  }

  @Test
  void validate_with_excessive_max_request_size_should_throw_exception() {
    MultipartProperties properties = new MultipartProperties();
    properties.setMaxFileSize(ofMegabytes(8));
    properties.setMaxRequestSize(ofMegabytes(150));

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validate(properties));

    assertTrue(exception.getMessage().contains("exceeds absolute maximum"));
    assertTrue(exception.getMessage().contains("DoS risk"));
  }

  @Test
  void validate_with_exactly_100mb_limit_should_succeed() {
    MultipartProperties properties = new MultipartProperties();
    properties.setMaxFileSize(ofMegabytes(100));
    properties.setMaxRequestSize(ofMegabytes(100));

    assertDoesNotThrow(() -> subject.validate(properties));
  }

  @Test
  void validate_with_101mb_limit_should_throw_exception() {
    MultipartProperties properties = new MultipartProperties();
    properties.setMaxFileSize(ofMegabytes(101));
    properties.setMaxRequestSize(ofMegabytes(8));

    SecurityException exception =
        assertThrows(SecurityException.class, () -> subject.validate(properties));

    assertTrue(exception.getMessage().contains("exceeds absolute maximum"));
  }

  @Test
  void get_validated_type_should_return_multipart_properties_class() {
    assertEquals(MultipartProperties.class, subject.getValidatedType());
  }

  @Test
  void validate_with_null_properties_should_throw_exception() {
    assertThrows(NullPointerException.class, () -> subject.validate(null));
  }
}
