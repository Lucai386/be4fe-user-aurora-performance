package com.bff_user_aurora_performance.service;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.bff_user_aurora_performance.dto.auth.LoginRequest;
import com.bff_user_aurora_performance.dto.auth.LoginResponse;
import com.bff_user_aurora_performance.dto.auth.LogoutRequest;
import com.bff_user_aurora_performance.dto.auth.RefreshTokenRequest;
import com.bff_user_aurora_performance.exception.AuthenticationException;
import com.bff_user_aurora_performance.model.User;
import com.bff_user_aurora_performance.model.UserLog;
import com.bff_user_aurora_performance.repository.UserLogRepository;
import com.bff_user_aurora_performance.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserLogRepository userLogRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret:}")
    private String clientSecret;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("username", request.getUsername());
            body.add("password", request.getPassword());

            if (clientSecret != null && !clientSecret.isBlank()) {
                body.add("client_secret", clientSecret);
            }

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode tokenResponse = objectMapper.readTree(response.getBody());

            String accessToken = tokenResponse.get("access_token").asText();
            String refreshToken = tokenResponse.get("refresh_token").asText();
            long expiresIn = tokenResponse.get("expires_in").asLong();
            long refreshExpiresIn = tokenResponse.get("refresh_expires_in").asLong();
            String scope = tokenResponse.has("scope") ? tokenResponse.get("scope").asText() : "";

            // Estrai info utente dal token JWT
            UserInfo userInfo = extractUserInfoFromToken(accessToken);

            // Sincronizza utente nel DB locale
            User user = syncUserFromKeycloak(userInfo);

            // Log login
            logUserAction(user.getId(), "LOGIN", ipAddress, userAgent);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn)
                    .refreshExpiresIn(refreshExpiresIn)
                    .scope(scope)
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("Keycloak authentication failed: {}", e.getResponseBodyAsString());
            throw new AuthenticationException("AUTH_FAILED", "Credenziali non valide");
        } catch (Exception e) {
            log.error("Login error", e);
            throw new AuthenticationException("AUTH_ERROR", "Errore durante l'autenticazione");
        }
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("refresh_token", request.getRefreshToken());

            if (clientSecret != null && !clientSecret.isBlank()) {
                body.add("client_secret", clientSecret);
            }

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode tokenResponse = objectMapper.readTree(response.getBody());

            return LoginResponse.builder()
                    .accessToken(tokenResponse.get("access_token").asText())
                    .refreshToken(tokenResponse.get("refresh_token").asText())
                    .tokenType("Bearer")
                    .expiresIn(tokenResponse.get("expires_in").asLong())
                    .refreshExpiresIn(tokenResponse.get("refresh_expires_in").asLong())
                    .scope(tokenResponse.has("scope") ? tokenResponse.get("scope").asText() : "")
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("Token refresh failed: {}", e.getResponseBodyAsString());
            throw new AuthenticationException("REFRESH_FAILED", "Refresh token non valido o scaduto");
        } catch (Exception e) {
            log.error("Refresh token error", e);
            throw new AuthenticationException("REFRESH_ERROR", "Errore durante il refresh del token");
        }
    }

    @Transactional
    public void logout(LogoutRequest request) {
        try {
            String logoutUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("refresh_token", request.getRefreshToken());

            if (clientSecret != null && !clientSecret.isBlank()) {
                body.add("client_secret", clientSecret);
            }

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(logoutUrl, HttpMethod.POST, entity, String.class);

        } catch (Exception e) {
            log.error("Logout error", e);
        }
    }

    private UserInfo extractUserInfoFromToken(String accessToken) throws Exception {
        String[] parts = accessToken.split("\\.");
        if (parts.length < 2) {
            throw new AuthenticationException("INVALID_TOKEN", "Token JWT non valido");
        }

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        JsonNode claims = objectMapper.readTree(payload);

        Set<String> roles = new HashSet<>();

        if (claims.has("realm_access") && claims.get("realm_access").has("roles")) {
            claims.get("realm_access").get("roles").forEach(role -> roles.add(role.asText()));
        }

        if (claims.has("resource_access") && claims.get("resource_access").has(clientId)) {
            JsonNode clientRoles = claims.get("resource_access").get(clientId);
            if (clientRoles.has("roles")) {
                clientRoles.get("roles").forEach(role -> roles.add(role.asText()));
            }
        }

        return UserInfo.builder()
                .keycloakId(claims.get("sub").asText())
                .firstName(claims.has("given_name") ? claims.get("given_name").asText() : null)
                .lastName(claims.has("family_name") ? claims.get("family_name").asText() : null)
                .roles(roles)
                .build();
    }

    private User syncUserFromKeycloak(UserInfo userInfo) {
        String ruoloCode = resolveRuoloFromRoles(userInfo.getRoles());
        
        return userRepository.findByKeycloakId(userInfo.getKeycloakId())
                .map(existingUser -> {
                    // Aggiorna nome/cognome se presenti in Keycloak
                    if (userInfo.getFirstName() != null) {
                        existingUser.setNome(userInfo.getFirstName());
                    }
                    if (userInfo.getLastName() != null) {
                        existingUser.setCognome(userInfo.getLastName());
                    }
                    // Aggiorna ruolo
                    if (ruoloCode != null) {
                        existingUser.setRuolo(ruoloCode);
                    }
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // Crea nuovo utente
                    User newUser = User.builder()
                            .keycloakId(userInfo.getKeycloakId())
                            .nome(userInfo.getFirstName())
                            .cognome(userInfo.getLastName())
                            .ruolo(ruoloCode)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    /**
     * Mappa i ruoli Keycloak al codice ruolo DB
     * Ordine di priorità: admin > segretario_comunale > dirigente > capo_settore > capo_progetto > dipendente_base
     */
    private String resolveRuoloFromRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) return null;
        
        if (roles.contains("admin")) return "AD";
        if (roles.contains("segretario_comunale")) return "SC";
        if (roles.contains("dirigente")) return "DR";
        if (roles.contains("capo_settore")) return "CS";
        if (roles.contains("capo_progetto")) return "CP";
        if (roles.contains("dipendente_base")) return "DB";
        
        return null;
    }

    private void logUserAction(Integer userId, String azione, String ipAddress, String userAgent) {
        UserLog log = new UserLog();
        log.setUserId(userId);
        log.setAzione(azione);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        userLogRepository.save(log);
    }

    @lombok.Data
    @lombok.Builder
    private static class UserInfo {
        private String keycloakId;
        private String firstName;
        private String lastName;
        private Set<String> roles;
    }
}
