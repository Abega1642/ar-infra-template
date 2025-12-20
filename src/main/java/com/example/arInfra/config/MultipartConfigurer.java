package com.example.arInfra.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 * Secure multipart file upload configuration.
 *
 * <p>Security measures implemented:
 *
 * <ul>
 *   <li>Configurable maximum file sizes to prevent DoS attacks
 *   <li>Request size limits to prevent memory exhaustion
 *   <li>File size threshold for in-memory vs disk storage
 *   <li>Temporary file location for secure file handling
 * </ul>
 */
@Configuration
public class MultipartConfigurer {

  @Value("${spring.servlet.multipart.max-file-size:10MB}")
  private String maxFileSize;

  @Value("${spring.servlet.multipart.max-request-size:10MB}")
  private String maxRequestSize;

  @Value("${spring.servlet.multipart.file-size-threshold:2MB}")
  private String fileSizeThreshold;

  @Value("${spring.servlet.multipart.location:${java.io.tmpdir}}")
  private String location;

  /**
   * Configures multipart file upload limits with security controls.
   *
   * @return configured MultipartConfigElement
   */
  @Bean
  public MultipartConfigElement multipartConfigElement() {
    MultipartConfigFactory factory = new MultipartConfigFactory();

    factory.setMaxFileSize(DataSize.parse(maxFileSize));
    factory.setMaxRequestSize(DataSize.parse(maxRequestSize));
    factory.setFileSizeThreshold(DataSize.parse(fileSizeThreshold));
    factory.setLocation(location);

    return factory.createMultipartConfig();
  }

  /**
   * Standard servlet multipart resolver.
   *
   * @return configured MultipartResolver
   */
  @Bean
  public MultipartResolver multipartResolver() {
    return new StandardServletMultipartResolver();
  }
}
