package com.meethybridhub.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        SecurityContextHolder.clearContext();
    }


    @Test
    void shouldNotFilterErrorDispatchReturnsTrue() {
        assertThat(filter.shouldNotFilterErrorDispatch()).isTrue();
    }

    @Test
    void shouldNotFilterAsyncDispatchReturnsTrue() {
        assertThat(filter.shouldNotFilterAsyncDispatch()).isTrue();
    }


    @Test
    void skipsFilterForPublicLoginEndpoint() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void skipsFilterForPublicRegisterEndpoint() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/auth/register");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void skipsFilterForPublicVerifyEndpoint() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/auth/verify");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void skipsFilterForPublicRefreshEndpoint() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/auth/refresh");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void skipsFilterForPublicResetPasswordEndpoint() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/auth/reset-password");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void skipsFilterForPublicResendVerificationEndpoint() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/auth/resend-verification");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void skipsFilterForSwaggerUi() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/swagger-ui/index.html");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void skipsFilterForApiDocs() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/v3/api-docs");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }


    @Test
    void passesThroughWhenNoAuthorizationHeader() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void passesThroughWhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void passesThroughWhenAuthorizationHeaderIsJustBearer() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        filter.doFilterInternal(request, response, filterChain);


        when(jwtService.extractUsername("")).thenReturn(null);

        verify(filterChain).doFilter(request, response);
    }


    @Test
    void setsAuthenticationWhenTokenIsValid() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");

        UserDetails user = new org.springframework.security.core.userdetails.User(
                "alice@example.com", "hash", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        when(jwtService.extractUsername("valid-token")).thenReturn("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(user);
        when(jwtService.validateToken("valid-token", user)).thenReturn(true);
        when(jwtService.passwordVersionMatches("valid-token", user)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(user);
    }


    @Test
    void doesNotOverwriteExistingAuthentication() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");


        UsernamePasswordAuthenticationToken existing =
                new UsernamePasswordAuthenticationToken("existing", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);

        when(jwtService.extractUsername("valid-token")).thenReturn("alice@example.com");

        filter.doFilterInternal(request, response, filterChain);


        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existing);
    }


    @Test
    void doesNotSetAuthenticationWhenUserIsDisabled() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");


        UserDetails disabledUser = new org.springframework.security.core.userdetails.User(
                "alice@example.com", "hash", false, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        when(jwtService.extractUsername("valid-token")).thenReturn("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(disabledUser);
        when(jwtService.validateToken("valid-token", disabledUser)).thenReturn(true);
        when(jwtService.passwordVersionMatches("valid-token", disabledUser)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }


    @Test
    void doesNotSetAuthenticationWhenUserIsLocked() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");


        UserDetails lockedUser = new org.springframework.security.core.userdetails.User(
                "alice@example.com", "hash", true, true, true, false,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        when(jwtService.extractUsername("valid-token")).thenReturn("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(lockedUser);
        when(jwtService.validateToken("valid-token", lockedUser)).thenReturn(true);
        when(jwtService.passwordVersionMatches("valid-token", lockedUser)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }


    @Test
    void doesNotSetAuthenticationWhenTokenIsInvalid() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");

        UserDetails user = new org.springframework.security.core.userdetails.User(
                "alice@example.com", "hash", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        when(jwtService.extractUsername("invalid-token")).thenReturn("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(user);
        when(jwtService.validateToken("invalid-token", user)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }


    @Test
    void doesNotSetAuthenticationWhenPasswordVersionMismatch() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        when(request.getHeader("Authorization")).thenReturn("Bearer old-token");

        UserDetails user = new org.springframework.security.core.userdetails.User(
                "alice@example.com", "hash", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        when(jwtService.extractUsername("old-token")).thenReturn("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(user);
        when(jwtService.validateToken("old-token", user)).thenReturn(true);
        when(jwtService.passwordVersionMatches("old-token", user)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }


    @Test
    void clearsContextOnExceptionAndContinues() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtService.extractUsername("bad-token")).thenThrow(new RuntimeException("JWT parse error"));


        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("old", null, List.of()));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }


    @Test
    void doesNotSetAuthenticationWhenExtractUsernameReturnsNull() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/v1/stores");
        when(request.getHeader("Authorization")).thenReturn("Bearer token-no-subject");
        when(jwtService.extractUsername("token-no-subject")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }
}
