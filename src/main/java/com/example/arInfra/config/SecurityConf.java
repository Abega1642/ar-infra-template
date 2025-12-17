package com.example.arInfra.config;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import com.example.arInfra.InfraGenerated;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration for application authentication and authorization.
 *
 * <p>This configuration establishes the security baseline for the application, defining which
 * endpoints are publicly accessible and which require authentication. It also configures CORS
 * (Cross-Origin Resource Sharing) and CSRF (Cross-Site Request Forgery) protection settings.
 *
 * <p><b>Public endpoints (no authentication required):</b>
 *
 * <ul>
 *   <li>{@code /ping} - Health check endpoint
 *   <li>{@code /health/**} - Application health endpoints
 *   <li>{@code /actuator/**} - Spring Boot Actuator monitoring endpoints
 *   <li>{@code /} - Root/documentation redirect
 *   <li>{@code /doc/**} - API documentation resources
 *   <li>{@code /swagger-ui/**} - Swagger UI interface
 *   <li>{@code /v3/api-docs/**} - OpenAPI specification endpoints
 * </ul>
 *
 * <p><b>Protected endpoints:</b> All other endpoints require authentication by default.
 *
 * <p><b>CORS configuration:</b> Permissive CORS policy allowing all origins, methods, and headers
 * with credentials support. This is suitable for development but should be restricted in production
 * environments.
 *
 * <p><b>CSRF protection:</b> Disabled for public endpoints to allow external access without CSRF
 * tokens. Enabled for authenticated endpoints by default.
 *
 * <p><b>⚠️ IMPORTANT FOR DEVELOPERS:</b> <br>
 * This configuration provides a secure starting point but <b>must be customized</b> for your
 * specific application requirements:
 *
 * <ul>
 *   <li>Add your application-specific endpoints to {@code filterChain()}
 *   <li>Define appropriate authorization rules (roles, permissions)
 *   <li>Restrict CORS to specific origins in production
 *   <li>Consider enabling CSRF for stateful applications
 *   <li>Integrate with your authentication provider (OAuth2, JWT, etc.)
 * </ul>
 *
 * @see org.springframework.security.config.annotation.web.builders.HttpSecurity
 */
@InfraGenerated
@EnableWebSecurity
@Configuration
@AllArgsConstructor
public class SecurityConf {
  private static final String PING_ENDPOINT = "/ping";
  private static final String HEALTH_ENDPOINT = "/health/**";
  private static final String ACTUATOR_ENDPOINT = "/actuator/**";
  private static final String ROOT_ENDPOINT = "/";
  private static final String DOC_ENDPOINT = "/doc";
  private static final String SWAGGER_UI_ENDPOINT = "/swagger-ui/**";
  private static final String ANY_SUBPATH = "/**";
  private static final String V_3_API_DOCS = "/v3/api-docs/**";
  private static final String V_3_API_DOCS_YAML = "/v3/api-docs.yaml";

  /**
   * Configures the Spring Security filter chain with authentication and authorization rules.
   *
   * <p><b>Configuration includes:</b>
   *
   * <ul>
   *   <li><b>CSRF protection:</b> Disabled for public/documentation endpoints
   *   <li><b>CORS:</b> Configured via {@link #corsConfigurationSource()}
   *   <li><b>Authorization:</b> Public access for health/docs, authentication required for all
   *       others
   * </ul>
   *
   * <p><b>⚠️ CUSTOMIZE THIS METHOD:</b> <br>
   * Add your application-specific security rules here. Examples:
   *
   * <pre>{@code
   * // Role-based access control
   * .requestMatchers("/admin/**").hasRole("ADMIN")
   * .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
   *
   * // Method-specific access control
   * .requestMatchers(POST, "/api/articles").authenticated()
   * .requestMatchers(GET, "/api/articles").permitAll()
   *
   * // Path-specific rules
   * .requestMatchers("/public/**").permitAll()
   * .requestMatchers("/internal/**").hasIpAddress("192.168.1.0/24")
   * }</pre>
   *
   * @param http the HttpSecurity to configure
   * @return the configured SecurityFilterChain
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.csrf(
            csrf ->
                csrf.ignoringRequestMatchers(
                    PING_ENDPOINT,
                    HEALTH_ENDPOINT,
                    ACTUATOR_ENDPOINT,
                    ROOT_ENDPOINT,
                    DOC_ENDPOINT,
                    DOC_ENDPOINT + ANY_SUBPATH,
                    SWAGGER_UI_ENDPOINT,
                    V_3_API_DOCS,
                    V_3_API_DOCS_YAML))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(GET, PING_ENDPOINT)
                    .permitAll()
                    .requestMatchers(GET, HEALTH_ENDPOINT)
                    .permitAll()
                    .requestMatchers(ACTUATOR_ENDPOINT)
                    .permitAll()
                    .requestMatchers(GET, ROOT_ENDPOINT)
                    .permitAll()
                    .requestMatchers(
                        ROOT_ENDPOINT,
                        DOC_ENDPOINT,
                        DOC_ENDPOINT + ANY_SUBPATH,
                        SWAGGER_UI_ENDPOINT,
                        V_3_API_DOCS,
                        V_3_API_DOCS_YAML)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .build();
  }

  /**
   * Configures CORS (Cross-Origin Resource Sharing) settings.
   *
   * <p><b>Current configuration (PERMISSIVE - for development):</b>
   *
   * <ul>
   *   <li><b>Allowed origins:</b> All origins (*) - allows requests from any domain
   *   <li><b>Allowed methods:</b> GET, POST, PUT, DELETE, OPTIONS, PATCH
   *   <li><b>Allowed headers:</b> All headers (*)
   *   <li><b>Credentials:</b> Enabled (allows cookies and authentication headers)
   * </ul>
   *
   * <p><b>⚠️ SECURITY WARNING - CUSTOMIZE FOR PRODUCTION:</b> <br>
   * The current configuration allows requests from ANY origin, which is convenient for development
   * but <b>insecure for production</b>. Update this method to:
   *
   * <pre>{@code
   * // Restrict to specific domains
   * configuration.setAllowedOrigins(List.of(
   *     "https://yourdomain.com",
   *     "https://app.yourdomain.com"
   * ));
   *
   * // Or use patterns for subdomains
   * configuration.setAllowedOriginPatterns(List.of(
   *     "https://*.yourdomain.com"
   * ));
   *
   * // Restrict methods if needed
   * configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
   *
   * // Restrict headers for tighter security
   * configuration.setAllowedHeaders(List.of(
   *     "Content-Type",
   *     "Authorization",
   *     "X-Custom-Header"
   * ));
   * }</pre>
   *
   * @return configured CorsConfigurationSource for the application
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    var configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(List.of("*"));
    configuration.setAllowedMethods(
        List.of(GET.name(), POST.name(), PUT.name(), DELETE.name(), OPTIONS.name(), PATCH.name()));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration(ANY_SUBPATH, configuration);
    return source;
  }
}
