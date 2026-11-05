package com.fintech.bank.security;

import com.fintech.bank.entity.AuthStatus;
import com.fintech.bank.entity.UserAuth;
import com.fintech.bank.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security UserDetails adapter wrapping the UserAuth entity.
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final UserRole role;
    private final AuthStatus status;
    private final Collection<GrantedAuthority> authorities;

    public UserPrincipal(UUID id, String email, String password, UserRole role, AuthStatus status, Collection<GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
        this.authorities = authorities;
    }

    public static UserPrincipal create(UserAuth userAuth) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(userAuth.getRole().name()));
        return new UserPrincipal(
                userAuth.getId(),
                userAuth.getEmail(),
                userAuth.getPasswordHash(),
                userAuth.getRole(),
                userAuth.getStatus(),
                authorities
        );
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != AuthStatus.LOCKED && status != AuthStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == AuthStatus.ACTIVE;
    }
}
