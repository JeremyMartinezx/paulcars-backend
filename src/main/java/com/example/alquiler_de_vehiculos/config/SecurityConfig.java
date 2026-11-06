package com.example.alquiler_de_vehiculos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.example.alquiler_de_vehiculos.security.JwtAuthenticationFilter;
import com.example.alquiler_de_vehiculos.security.RateLimitingFilter;

/**
 * Arquitectura de Seguridad Spring Security 6 (Spring Boot 3 / Java 21).
 * Implementa autenticación Stateless, políticas estrictas de cabeceras HTTP de seguridad,
 * mitigación de Clickjacking, XSS, MIME-sniffing, y aislamiento perimetral basado en roles.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${app.swagger.permit-all:true}")
    private boolean swaggerPermitAll;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   RateLimitingFilter rateLimitingFilter) throws Exception {
        http
            // 1. Integración explícita de CORS estricto
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // 2. Desactivación de CSRF porque la arquitectura es 100% Stateless con Bearer JWT
            .csrf(csrf -> csrf.disable())

            // 3. Sesiones sin estado (Stateless)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. EntryPoint y AccessDeniedHandler para respuestas limpias de seguridad
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"timestamp\":\"" + java.time.LocalDateTime.now() + "\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Acceso denegado\",\"path\":\"" + request.getRequestURI() + "\"}");
                })
            )

            // 5. Blindaje de Cabeceras HTTP de Seguridad (OWASP Secure Headers)
            .headers(headers -> {
                // Prevención de Clickjacking
                headers.frameOptions(frame -> frame.deny());
                // Prevención de MIME-type Sniffing
                headers.contentTypeOptions(contentType -> {});
                // HTTP Strict Transport Security (HSTS)
                headers.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000));
                // Referrer Policy restrictivo
                headers.referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                // Permissions Policy para inhabilitar hardware y sensores no requeridos (API no deprecada)
                headers.permissionsPolicyHeader(permissions -> permissions
                    .policy("camera=(), microphone=(), geolocation=(), payment=(self)"));
                // Content Security Policy (CSP)
                headers.contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'; img-src 'self' data: https:; font-src 'self' https: data:; style-src 'self' 'unsafe-inline' https:; script-src 'self' 'unsafe-inline';"));
            })

            // 6. Matriz de Autorización perimetral
            .authorizeHttpRequests(auth -> {
                // Documentación técnica Swagger / OpenAPI (Permitido según configuración, o restringido a ADMIN)
                if (swaggerPermitAll) {
                    auth.requestMatchers("/v3/api-docs/**", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                } else {
                    auth.requestMatchers("/v3/api-docs/**", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN");
                }

                // Monitoreo operativo básico público, métricas sensibles restringidas
                auth.requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").hasRole("ADMIN");

                // Endpoints de autenticación y recuperación de credenciales
                auth.requestMatchers(HttpMethod.POST,
                        "/api/auth/registro",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/recuperar-password/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated();

                // Catálogo y recursos estáticos públicos para visualización
                auth.requestMatchers(HttpMethod.GET,
                        "/api/vehiculos",
                        "/api/vehiculos/**",
                        "/api/catalogos",
                        "/api/catalogos/**",
                        "/images/**",
                        "/uploads/**",
                        "/api/alquileres/horarios").permitAll();

                // Creación de cliente y reserva pública desde el portal
                auth.requestMatchers(HttpMethod.POST, "/api/clientes", "/api/alquileres").permitAll();
                auth.requestMatchers(HttpMethod.POST, "/api/pagos/yape", "/api/pagos/plin", "/api/pagos/tarjeta").permitAll();

                // Área privada del cliente logueado
                auth.requestMatchers(HttpMethod.GET, "/api/auth/me", "/api/alquileres/mis-alquileres").authenticated();
                auth.requestMatchers(HttpMethod.PUT, "/api/alquileres/*/cancelar").authenticated();
                auth.requestMatchers("/api/clientes/mi-perfil", "/api/clientes/mi-perfil/**").authenticated();
                auth.requestMatchers(HttpMethod.POST, "/api/clientes/solicitar-codigo-password", "/api/clientes/cambiar-password").authenticated();
                auth.requestMatchers("/api/notificaciones", "/api/notificaciones/**").authenticated();

                // Seguridad en pagos: las consultas de pagos por alquiler o cliente requieren autenticación obligatoria
                auth.requestMatchers("/api/pagos/alquiler/**", "/api/pagos/cliente/**").authenticated();

                // Módulo operacional de reportes del día (Compartido para Counter / Asesor y Admin)
                auth.requestMatchers("/api/reportes/operativo").hasAnyRole("ADMIN", "EMPLEADO");

                // Módulo exclusivo de Administrador
                auth.requestMatchers("/api/usuarios/**", "/api/reportes/**").hasRole("ADMIN");
                auth.requestMatchers(HttpMethod.POST, "/api/catalogos/**").hasRole("ADMIN");
                auth.requestMatchers(HttpMethod.PUT, "/api/catalogos/**").hasRole("ADMIN");
                auth.requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN");

                // Módulos operacionales compartidos (ADMIN y EMPLEADO)
                auth.requestMatchers("/api/**").hasAnyRole("ADMIN", "EMPLEADO");

                // Bloqueo total por defecto
                auth.anyRequest().authenticated();
            })

            // 7. Filtros de seguridad en cadena
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
