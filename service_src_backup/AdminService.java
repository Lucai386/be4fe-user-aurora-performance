package com.bff_user_aurora_performance.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bff_user_aurora_performance.dto.admin.SwitchEnteRequest;
import com.bff_user_aurora_performance.dto.admin.SwitchEnteResponse;
import com.bff_user_aurora_performance.dto.session.SessionInfoDto;
import com.bff_user_aurora_performance.dto.session.SessionUserDto;
import com.bff_user_aurora_performance.enums.ErrorCode;
import com.bff_user_aurora_performance.model.User;
import com.bff_user_aurora_performance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault());

    @Transactional
    public SwitchEnteResponse switchEnte(Authentication authentication, SwitchEnteRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Switch ente request without valid authentication");
            return SwitchEnteResponse.error(ErrorCode.SESSION_INVALID);
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();

        Optional<User> userOpt = userRepository.findByKeycloakId(keycloakId);

        if (userOpt.isEmpty()) {
            log.warn("User not found in local DB for keycloakId: {}", keycloakId);
            return SwitchEnteResponse.error(ErrorCode.USER_NOT_FOUND);
        }

        User user = userOpt.get();

        // Verifica che l'utente sia admin
        if (!"AD".equals(user.getRuolo())) {
            log.warn("Non-admin user {} attempted to switch ente", user.getId());
            return SwitchEnteResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = request.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return SwitchEnteResponse.error(ErrorCode.CODICE_ISTAT_REQUIRED);
        }

        // Aggiorna il codiceIstat dell'utente admin
        user.setCodiceIstat(codiceIstat.trim());
        userRepository.save(user);

        log.info("Admin user {} switched to codiceIstat: {}", user.getId(), codiceIstat);

        SessionUserDto userDto = buildUserDto(user, jwt);
        SessionInfoDto sessionDto = buildSessionDto(jwt);

        return SwitchEnteResponse.success(userDto, sessionDto);
    }

    private SessionUserDto buildUserDto(User user, Jwt jwt) {
        return SessionUserDto.builder()
                .id(user.getId().toString())
                .nome(user.getNome() != null ? user.getNome() : jwt.getClaimAsString("given_name"))
                .cognome(user.getCognome() != null ? user.getCognome() : jwt.getClaimAsString("family_name"))
                .codiceFiscale(user.getCodiceFiscale())
                .codiceIstat(user.getCodiceIstat())
                .ruolo(user.getRuolo())
                .assegnazioni(user.getAssegnazioni())
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
}
