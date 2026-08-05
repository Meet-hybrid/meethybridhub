package com.meethybridhub.identity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bridge between the {@link User} entity and Spring Security's
 * {@link UserDetails}.
 *
 * Unlike Spring's built-in {@code org.springframework.security.core.userdetails.User},
 * this implementation keeps a reference to the entity and exposes
 * {@link #getPasswordVersion()} — the value JwtService embeds as a claim and
 * the JWT filter compares on every request. That is what makes tokens issued
 * before a password change die instantly.
 */
public class AppUser implements UserDetails {

    private final User user;
    private final List<SimpleGrantedAuthority> authorities;

    public AppUser(User user) {
        this.user = user;
        this.authorities = Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .map(role -> "ROLE_" + role) // Spring Security expects ROLE_ prefix
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /** The underlying entity (e.g. to read the user ID for JWT claims). */
    public User getUser() {
        return user;
    }

    /** Current password version, embedded in JWTs at issuance time. */
    public int getPasswordVersion() {
        return user.getPasswordVersion();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != User.UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.canAuthenticate(); // requires ACTIVE + email verified
    }
}
