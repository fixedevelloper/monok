package com.monokek.identity.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Two kinds of tokens reach this app's resource server: end-user tokens from the
 * password grant (carry a "roles" claim — see monokek-identity's
 * UserClaimsTokenCustomizer, the same shape pms-modulith's JwtAuthoritiesConverter
 * reads) and client_credentials service tokens calling {@code /internal/**} (scope-
 * based authorities only — a client_credentials {@code sub} is the client id, not a
 * numeric user id, so it can't become a {@link JwtCurrentUser}).
 */
@Component
public class JwtToCurrentUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        if (jwt.getClaimAsStringList("roles") == null) {
            return new JwtAuthenticationToken(jwt, scopeAuthoritiesConverter.convert(jwt));
        }

        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Le claim 'sub' du token n'est pas un identifiant utilisateur valide.");
        }

        JwtCurrentUser principal = new JwtCurrentUser(id, jwt.getClaimAsString("name"), branchId(jwt));
        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities(jwt));
    }

    private static Long branchId(Jwt jwt) {
        Object raw = jwt.getClaim("branch_id");
        return raw == null ? null : ((Number) raw).longValue();
    }

    private static Set<GrantedAuthority> authorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT))));
        }
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        if (permissions != null) {
            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        }
        return authorities;
    }
}
