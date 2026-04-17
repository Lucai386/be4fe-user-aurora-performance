package com.be4fe_user_aurora_performance.service;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import com.be4fe_user_aurora_performance.dto.auth.LoginRequest;
import com.be4fe_user_aurora_performance.dto.auth.LoginResponse;
import com.be4fe_user_aurora_performance.dto.auth.LogoutRequest;
import com.be4fe_user_aurora_performance.dto.auth.RefreshTokenRequest;
import com.be4fe_user_aurora_performance.exception.AuthenticationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CoreApiClient coreApiClient;

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret:}")
    private String clientSecret;

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
            if (clientSecret != null && !clientSecret.isBlank()) body.add("client_secret", clientSecret);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            JsonNode tokenResponse = objectMapper.readTree(response.getBody());

            String accessToken = tokenResponse.get("access_token").asText();
            String refreshToken = tokenResponse.get("refresh_token").asText();
            long expiresIn = tokenResponse.get("expires_in").asLong();
            long refreshExpiresIn = tokenResponse.get("refresh_expires_in").asLong();
            String scope = tokenResponse.has("scope") ? tokenResponse.get("scope").asText() : "";

            UserInfo userInfo = extractUserInfoFromToken(accessToken);
            syncUserInCore(userInfo);

            // Log login nel core
            coreApiClient.saveUserLog(Map.of(
                    "userId", userInfo.getKeycloakId(),
                    "azione", "LOGIN",
                    "ipAddress", ipAddress != null ? ipAddress : "",
                    "userAgent", userAgent != null ? userAgent : "",
                    "createdAt", LocalDateTime.now().toString()
            ));

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

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("refresh_token", request.getRefreshToken());
            if (clientSecret != null && !clientSecret.isBlank()) body.add("client_secret", clientSecret);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

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

    public void logout(LogoutRequest request) {
        try {
            String logoutUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("refresh_token", request.getRefreshToken());
            if (clientSecret != null && !clientSecret.isBlank()) body.add("client_secret", clientSecret);

            restTemplate.exchange(logoutUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        } catch (Exception e) {
            log.warn("Logout warning: {}", e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private UserInfo extractUserInfoFromToken(String accessToken) throws Exception {
        String[] parts = accessToken.split("\\.");
        if (parts.length < 2) throw new AuthenticationException("INVALID_TOKEN", "Token JWT non valido");

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        JsonNode claims = objectMapper.readTree(payload);

        Set<String> roles = new HashSet<>();
        if (claims.has("realm_access") && claims.get("realm_access").has("roles")) {
            claims.get("realm_access").get("roles").forEach(r -> roles.add(r.asText()));
        }
        if (claims.has("resource_access") && claims.get("resource_access").has(clientId)) {
            JsonNode cr = claims.get("resource_access").get(clientId);
            if (cr.has("roles")) cr.get("roles").forEach(r -> roles.add(r.asText()));
        }

        return UserInfo.builder()
                .keycloakId(claims.get("sub").asText())
                .firstName(claims.has("given_name") ? claims.get("given_name").asText() : null)
                .lastName(claims.has("family_name") ? claims.get("family_name").asText() : null)
                .email(claims.has("email") ? claims.get("email").asText() : null)
                .roles(roles)
                .build();
    }

    @SuppressWarnings("unchecked")
    private void syncUserInCore(UserInfo userInfo) {
        String ruolo = resolveRuolo(userInfo.getRoles());
        Optional<Map> existingOpt = coreApiClient.getUserByKeycloakId(userInfo.getKeycloakId());

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("keycloakId", userInfo.getKeycloakId());
        if (userInfo.getFirstName() != null) userPayload.put("nome", userInfo.getFirstName());
        if (userInfo.getLastName() != null) userPayload.put("cognome", userInfo.getLastName());
        if (userInfo.getEmail() != null) userPayload.put("email", userInfo.getEmail());
        if (ruolo != null) userPayload.put("ruolo", ruolo);

        if (existingOpt.isPresent()) {
            Object userId = existingOpt.get().get("id");
            if (userId != null) coreApiClient.updateUser(Long.parseLong(userId.toString()), userPayload);
        } else {
            coreApiClient.saveUser(userPayload);
        }
    }

    private String resolveRuolo(Set<String> roles) {
        if (roles == null || roles.isEmpty()) return null;
        if (roles.contains("admin")) return "AD";
        if (roles.contains("segretario_comunale")) return "SC";
        if (roles.contains("dirigente")) return "DR";
        if (roles.contains("capo_settore")) return "CS";
        if (roles.contains("capo_progetto")) return "CP";
        if (roles.contains("dipendente_base")) return "DB";
        return null;
    }

    @Data
    @Builder
    private static class UserInfo {
        private String keycloakId;
        private String firstName;
        private String lastName;
        private String email;
        private Set<String> roles;
    }
}
