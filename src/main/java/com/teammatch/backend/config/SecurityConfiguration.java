
package com.teammatch.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository) throws Exception {

        http
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/actuator/health").permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/students",
                    "/api/students/**",
                    "/api/auth/login"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/csrf").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/students").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/skills/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/skills/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.authenticationEntryPoint(
                (request, response, exception) -> response.setStatus(401)
            ))
            .securityContext(context ->
                context.securityContextRepository(securityContextRepository)
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/api/students",
                    "/api/students/**",
                    "/api/auth/login"
                )
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
            List.of("https://teammatch-frontend.onrender.com")
        );

        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
            List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-CSRF-TOKEN"
            )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
