package com.lifedrop.config;

import com.lifedrop.security.CustomUserDetailsService;
import com.lifedrop.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${lifedrop.cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,http://localhost:8080,http://127.0.0.1:8080}")
    private String allowedOrigins;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Link", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)) // For H2 console
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Static frontend & documentation
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/assets/**", "/favicon.ico", "/h2-console/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Public API endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/statistics", "/api/statistics/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/inventory", "/api/inventory/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blood-requests/demanded-groups").permitAll()

                        // Role-specific restrictions (Hospital & Admin)
                        .requestMatchers("/api/donors/compatible").hasAnyRole("HOSPITAL", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/blood-requests").hasAnyRole("HOSPITAL", "ADMIN")
                        .requestMatchers("/api/blood-requests/my-requests").hasAnyRole("HOSPITAL", "ADMIN")
                        .requestMatchers("/api/blood-requests/*/send-to-donor/*").hasAnyRole("HOSPITAL", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/blood-requests/*/status").hasAnyRole("HOSPITAL", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/blood-requests/*").hasAnyRole("HOSPITAL", "ADMIN")

                        // Role-specific restrictions (Donor & Admin)
                        .requestMatchers("/api/blood-requests/incoming").hasAnyRole("DONOR", "ADMIN")
                        .requestMatchers("/api/blood-requests/*/respond").hasAnyRole("DONOR", "ADMIN")

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/inventory/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/donors/**").hasRole("ADMIN")

                        // Publicly readable endpoints with controller-level filters
                        .requestMatchers(HttpMethod.GET, "/api/donors", "/api/donors/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/hospitals", "/api/hospitals/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blood-requests", "/api/blood-requests/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/donations", "/api/donations/**").permitAll()

                        // Authenticated endpoints (Donors, Hospitals, Admin)
                        .requestMatchers("/api/donors/**").authenticated()
                        .requestMatchers("/api/hospitals/**").authenticated()
                        .requestMatchers("/api/blood-requests/**").authenticated()
                        .requestMatchers("/api/donations/**").authenticated()
                        .requestMatchers("/api/referrals/**").authenticated()

                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
