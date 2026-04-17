package com.be4fe_user_aurora_performance.principal;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Filtro che risolve l'utente corrente dal Core una sola volta per richiesta
 * e lo inserisce in {@link UserPrincipal} (request-scoped bean).
 *
 * <p>Questo elimina le chiamate ridondanti a {@code SessionService.getCodiceIstat()},
 * {@code SessionService.getUserId()} ecc. in ogni metodo dei controller:
 * l'informazione è disponibile tramite {@code UserPrincipal} iniettato direttamente.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class UserPrincipalFilter extends OncePerRequestFilter {

    private final CoreApiClient coreApiClient;
    private final UserPrincipal userPrincipal;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
            String keycloakId = jwt.getSubject();
            userPrincipal.setKeycloakId(keycloakId);

            // Ruolo estratto dalle authorities JWT (nessuna chiamata al Core)
            String ruolo = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_") && isKnownRole(a.substring(5)))
                    .map(a -> a.substring(5))
                    .findFirst()
                    .orElse(null);
            userPrincipal.setRuolo(ruolo);

            // Una sola chiamata al Core per ottenere id + codiceIstat
            Optional<Map> userOpt = coreApiClient.getUserByKeycloakId(keycloakId);
            if (userOpt.isPresent()) {
                Map<?, ?> user = userOpt.get();
                Object rawId = user.get("id");
                if (rawId != null) {
                    userPrincipal.setId(Long.parseLong(rawId.toString()));
                }
                userPrincipal.setCodiceIstat((String) user.get("codiceIstat"));
            } else {
                log.debug("UserPrincipalFilter: utente non trovato nel Core per keycloakId={}", keycloakId);
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
                || path.contains("/actuator")
                || path.contains("/swagger")
                || path.contains("/api-docs")
                || path.contains("/v3/api-docs");
    }

    private boolean isKnownRole(String r) {
        return switch (r) {
            case "AD", "SC", "DR", "CS", "CP", "DB" -> true;
            default -> false;
        };
    }
}
