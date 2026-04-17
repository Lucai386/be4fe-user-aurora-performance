package com.bff_user_aurora_performance.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.bff_user_aurora_performance.dto.session.SessionInfoDto;
import com.bff_user_aurora_performance.dto.session.SessionResponse;
import com.bff_user_aurora_performance.dto.session.SessionUserDto;
import com.bff_user_aurora_performance.enums.ErrorCode;
import com.bff_user_aurora_performance.model.Struttura;
import com.bff_user_aurora_performance.model.StrutturaStaff;
import com.bff_user_aurora_performance.model.User;
import com.bff_user_aurora_performance.repository.StrutturaRepository;
import com.bff_user_aurora_performance.repository.StrutturaStaffRepository;
import com.bff_user_aurora_performance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final UserRepository userRepository;
    private final StrutturaRepository strutturaRepository;
    private final StrutturaStaffRepository strutturaStaffRepository;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault());

    public SessionResponse getSession(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Session request without valid authentication");
            return SessionResponse.error(ErrorCode.SESSION_INVALID);
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();

        Optional<User> userOpt = userRepository.findByKeycloakId(keycloakId);

        if (userOpt.isEmpty()) {
            log.warn("User not found in local DB for keycloakId: {}", keycloakId);
            return SessionResponse.error(ErrorCode.USER_NOT_FOUND);
        }

        User user = userOpt.get();

        SessionUserDto userDto = buildUserDto(user, jwt);
        SessionInfoDto sessionDto = buildSessionDto(jwt);

        return SessionResponse.success(userDto, sessionDto);
    }

    public User getOrCreateUser(String keycloakId, Jwt jwt) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .keycloakId(keycloakId)
                            .nome(jwt.getClaimAsString("given_name"))
                            .cognome(jwt.getClaimAsString("family_name"))
                            .build();
                    return userRepository.save(newUser);
                });
    }

    private SessionUserDto buildUserDto(User user, Jwt jwt) {
        // Trova la struttura principale di cui l'utente è responsabile
        SessionUserDto.AreaCompetenzaDto areaCompetenza = null;
        List<Struttura> strutture = strutturaRepository.findAllByIdResponsabile(user.getId());
        if (!strutture.isEmpty()) {
            // Prendi la prima (radice/più alta in gerarchia, grazie all'ORDER BY)
            Struttura primary = strutture.get(0);
            areaCompetenza = SessionUserDto.AreaCompetenzaDto.builder()
                    .id("struct-" + primary.getId())
                    .nome(primary.getNome())
                    .build();
        } else {
            // Fallback: cerca la struttura a cui l'utente è assegnato come staff
            List<StrutturaStaff> staffEntries = strutturaStaffRepository.findByIdUser(user.getId());
            if (!staffEntries.isEmpty()) {
                Integer strutturaId = staffEntries.get(0).getIdStruttura();
                Optional<Struttura> strutturaOpt = strutturaRepository.findById(strutturaId);
                if (strutturaOpt.isPresent()) {
                    Struttura s = strutturaOpt.get();
                    areaCompetenza = SessionUserDto.AreaCompetenzaDto.builder()
                            .id("struct-" + s.getId())
                            .nome(s.getNome())
                            .build();
                }
            }
        }

        return SessionUserDto.builder()
                .id(user.getId().toString())
                .nome(user.getNome() != null ? user.getNome() : jwt.getClaimAsString("given_name"))
                .cognome(user.getCognome() != null ? user.getCognome() : jwt.getClaimAsString("family_name"))
                .codiceFiscale(user.getCodiceFiscale())
                .codiceIstat(user.getCodiceIstat())
                .ruolo(user.getRuolo())
                .assegnazioni(user.getAssegnazioni())
                .areaCompetenza(areaCompetenza)
                .build();
    }

    private SessionInfoDto buildSessionDto(Jwt jwt) {
        Instant issuedAt = jwt.getIssuedAt();
        Instant expiresAt = jwt.getExpiresAt();

        return SessionInfoDto.builder()
                .issuedAt(issuedAt != null ? ISO_FORMATTER.format(issuedAt) : null)
                .expiresAt(expiresAt != null ? ISO_FORMATTER.format(expiresAt) : null)
                .build();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Ottiene l'utente corrente dalla authentication
     */
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();
        return userRepository.findByKeycloakId(keycloakId).orElse(null);
    }

    /**
     * Ottiene il codice ISTAT dell'utente corrente
     */
    public String getCodiceIstat(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return user != null ? user.getCodiceIstat() : null;
    }

    /**
     * Ottiene l'ID dell'utente corrente
     */
    public Integer getUserId(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return user != null ? user.getId() : null;
    }

    /**
     * Ottiene il ruolo dell'utente corrente
     */
    public String getUserRole(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return user != null ? user.getRuolo() : null;
    }
}
