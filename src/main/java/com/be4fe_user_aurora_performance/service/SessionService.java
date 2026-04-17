package com.be4fe_user_aurora_performance.service;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import com.be4fe_user_aurora_performance.dto.session.SessionInfoDto;
import com.be4fe_user_aurora_performance.dto.session.SessionResponse;
import com.be4fe_user_aurora_performance.dto.session.SessionUserDto;
import com.be4fe_user_aurora_performance.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final CoreApiClient coreApiClient;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault());

    // ─── Public API ───────────────────────────────────────────────────────────

    public SessionResponse getSession(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return SessionResponse.error(ErrorCode.SESSION_INVALID);
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();

        Optional<Map> userOpt = coreApiClient.getUserByKeycloakId(keycloakId);
        if (userOpt.isEmpty()) {
            log.warn("User not found in core for keycloakId: {}", keycloakId);
            return SessionResponse.error(ErrorCode.USER_NOT_FOUND);
        }

        Map user = userOpt.get();
        SessionUserDto userDto = buildUserDto(user, jwt);
        SessionInfoDto sessionDto = buildSessionDto(jwt);

        return SessionResponse.success(userDto, sessionDto);
    }

    /** Ruolo dell'utente estratto dalle JWT authorities (no core call needed). */
    public String getUserRole(Authentication authentication) {
        if (authentication == null) return null;
        // KeycloakGrantedAuthoritiesConverter adds ROLE_<UPPERCASE> authorities
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_") && isKnownRole(a.substring(5)))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse(null);
    }

    /** ID interno (DB) dell'utente corrente. Chiama il core. */
    @SuppressWarnings("unchecked")
    public Integer getUserId(Authentication authentication) {
        Map user = loadUser(authentication);
        if (user == null) return null;
        Object id = user.get("id");
        return id != null ? Integer.parseInt(id.toString()) : null;
    }

    /** Codice ISTAT dell'ente associato all'utente. Chiama il core. */
    @SuppressWarnings("unchecked")
    public String getCodiceIstat(Authentication authentication) {
        Map user = loadUser(authentication);
        if (user == null) return null;
        return (String) user.get("codiceIstat");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map loadUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return coreApiClient.getUserByKeycloakId(jwt.getSubject()).orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SessionUserDto buildUserDto(Map user, Jwt jwt) {
        Integer userId = user.get("id") != null ? Integer.parseInt(user.get("id").toString()) : null;

        // Trova la struttura principale dell'utente
        SessionUserDto.AreaCompetenzaDto areaCompetenza = null;
        if (userId != null) {
            List<Map> allStrutture = loadStruttureForUser(userId, (String) user.get("codiceIstat"));
            if (!allStrutture.isEmpty()) {
                Map s = allStrutture.get(0);
                areaCompetenza = SessionUserDto.AreaCompetenzaDto.builder()
                        .id("struct-" + s.get("id"))
                        .nome((String) s.get("nome"))
                        .build();
            }
        }

        return SessionUserDto.builder()
                .id(userId != null ? userId.toString() : null)
                .nome(user.get("nome") != null ? (String) user.get("nome") : jwt.getClaimAsString("given_name"))
                .cognome(user.get("cognome") != null ? (String) user.get("cognome") : jwt.getClaimAsString("family_name"))
                .codiceFiscale((String) user.get("codiceFiscale"))
                .codiceIstat((String) user.get("codiceIstat"))
                .ruolo((String) user.get("ruolo"))
                .assegnazioni((Map) user.get("assegnazioni"))
                .areaCompetenza(areaCompetenza)
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map> loadStruttureForUser(Integer userId, String codiceIstat) {
        if (codiceIstat == null || codiceIstat.isBlank()) return List.of();
        List<Map> allStrutture = coreApiClient.getStrutture();

        // Prima cerca come responsabile
        List<Map> asResponsabile = allStrutture.stream()
                .filter(s -> userId.toString().equals(strVal(s, "idResponsabile")))
                .toList();
        if (!asResponsabile.isEmpty()) return asResponsabile;

        // Poi cerca come staff
        return allStrutture.stream()
                .filter(s -> {
                    Object staffObj = s.get("staff");
                    if (!(staffObj instanceof List)) return false;
                    return ((List<?>) staffObj).stream().anyMatch(ss -> {
                        if (!(ss instanceof Map)) return false;
                        return userId.toString().equals(strVal((Map) ss, "idUser"));
                    });
                })
                .toList();
    }

    private SessionInfoDto buildSessionDto(Jwt jwt) {
        Instant issuedAt = jwt.getIssuedAt();
        Instant expiresAt = jwt.getExpiresAt();
        return SessionInfoDto.builder()
                .issuedAt(issuedAt != null ? ISO_FORMATTER.format(issuedAt) : null)
                .expiresAt(expiresAt != null ? ISO_FORMATTER.format(expiresAt) : null)
                .build();
    }

    private String strVal(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private boolean isKnownRole(String r) {
        return switch (r) {
            case "AD", "SC", "DR", "CS", "CP", "DB" -> true;
            default -> false;
        };
    }
}
