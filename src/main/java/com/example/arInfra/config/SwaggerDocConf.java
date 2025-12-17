package com.example.arInfra.config;

import com.example.arInfra.InfraGenerated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for API documentation access via Swagger UI.
 *
 * <p>This configuration sets up convenient URL redirects and static resource handlers for accessing
 * the Swagger UI interface. It provides multiple entry points to the API documentation, making it
 * easily accessible for developers.
 *
 * <p><b>URL mappings:</b>
 *
 * <ul>
 *   <li>{@code /} (root) → redirects to Swagger UI
 *   <li>{@code /doc} → redirects to Swagger UI
 *   <li>{@code /doc/**} → serves static documentation files from project directory
 * </ul>
 *
 * <p>The configuration also enables serving static documentation files (if present) from the {@code
 * /doc} directory within the project root, allowing for custom documentation resources alongside
 * the generated Swagger UI.
 *
 * <p><b>System property required:</b>
 *
 * <ul>
 *   <li>{@code user.dir} - Current working directory (automatically provided by JVM)
 * </ul>
 */
@InfraGenerated
@Configuration
public class SwaggerDocConf implements WebMvcConfigurer {

  private static final String REDIRECT_URL = "/swagger-ui/index.html";
  private static final String DOC_URL_PATH = "/doc";
  private static final String ROOT_PATH = "/";

  @Value("${user.dir}")
  private String projectRoot;

  /**
   * Configures view controller redirects for convenient API documentation access.
   *
   * <p>Sets up two redirect mappings:
   *
   * <ul>
   *   <li>Root path ({@code /}) redirects to Swagger UI for immediate access
   *   <li>Documentation path ({@code /doc}) redirects to Swagger UI for explicit access
   * </ul>
   *
   * <p>These redirects allow developers to access the API documentation by simply navigating to the
   * application root URL or the {@code /doc} endpoint.
   *
   * @param registry the ViewControllerRegistry for registering redirect controllers
   */
  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController(ROOT_PATH, REDIRECT_URL);
    registry.addRedirectViewController(DOC_URL_PATH, REDIRECT_URL);
  }

  /**
   * Configures static resource handlers for serving documentation files.
   *
   * <p>Maps the {@code /doc/**} URL pattern to serve static files from the {@code /doc} directory
   * in the project root. This allows custom documentation resources (HTML, images, PDFs, etc.) to
   * be served alongside the Swagger UI.
   *
   * <p>The resource location is constructed dynamically based on the current working directory,
   * making the configuration portable across different deployment environments.
   *
   * @param registry the ResourceHandlerRegistry for registering static resource handlers
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler(DOC_URL_PATH + "/**")
        .addResourceLocations("file:" + projectRoot + DOC_URL_PATH + ROOT_PATH);
  }
}
