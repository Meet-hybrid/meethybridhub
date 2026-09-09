package com.meethybridhub.config;

import com.meethybridhub.identity.JwtAuthenticationFilter;
import com.meethybridhub.store.StoreFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final StoreFilter storeFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, StoreFilter storeFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.storeFilter = storeFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()


                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/verify").permitAll()


                .requestMatchers("/api/v1/users/**").authenticated()


                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")


                .anyRequest().permitAll()
            )

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)


            .addFilterAfter(storeFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
