package com.meethybridhub.identity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


public class AppUser implements UserDetails {

    private final User user;
    private final List<SimpleGrantedAuthority> authorities;

    public AppUser(User user) {
        this.user = user;
        this.authorities = Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }


    public User getUser() {
        return user;
    }


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
        return user.canAuthenticate();
    }
}
