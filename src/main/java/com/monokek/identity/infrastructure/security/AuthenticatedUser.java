package com.monokek.identity.infrastructure.security;

import com.monokek.identity.CurrentUser;
import com.monokek.identity.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapts {@link User} to Spring Security. Authorities are the union of:
 * <ul>
 *   <li>{@code ROLE_<NAME>} for each role — checked with {@code hasRole(...)},
 *       mirroring the Laravel {@code role:admin|manager} middleware</li>
 *   <li>the bare permission name for each role- or directly-granted
 *       permission — checked with {@code hasAuthority(...)}, mirroring
 *       {@code $user->can('manage_products')}</li>
 * </ul>
 * Also implements {@link CurrentUser} so other modules' controllers can bind
 * {@code @AuthenticationPrincipal CurrentUser} without depending on this
 * (internal) class.
 */
public class AuthenticatedUser implements UserDetails, CurrentUser {

    private final User user;

    public AuthenticatedUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Long id() {
        return user.getId();
    }

    @Override
    public String name() {
        return user.getName();
    }

    @Override
    public Long branchId() {
        return user.getBranchId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase())));
        user.getAllPermissionNames().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
