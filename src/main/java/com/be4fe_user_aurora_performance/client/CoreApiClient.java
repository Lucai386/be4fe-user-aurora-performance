package com.be4fe_user_aurora_performance.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client HTTP per comunicare con core-aurora-performance.
 *
 * <p>Per comunicazioni service-to-service il BFF User ottiene un access_token
 * tramite client_credentials da Keycloak e lo include come Bearer token.</p>
 *
 * <p>In questa implementazione il token viene gestito con un semplice cache
 * in-memory (rinnovato 30s prima della scadenza).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoreApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.core-url}")
    private String coreBaseUrl;

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${spring.security.oauth2.client.registration.core-client.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.core-client.client-secret}")
    private String clientSecret;

    // ─── Token cache ──────────────────────────────────────────────────────────

    private volatile String cachedToken;
    private volatile long tokenExpiresAt = 0;

    private synchronized String getServiceToken() {
        long now = System.currentTimeMillis() / 1000;
        if (cachedToken != null && now < tokenExpiresAt - 30) {
            return cachedToken;
        }
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
        ResponseEntity<Map> resp = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(body, headers), Map.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            cachedToken = (String) resp.getBody().get("access_token");
            Integer expiresIn = (Integer) resp.getBody().getOrDefault("expires_in", 300);
            tokenExpiresAt = now + expiresIn;
        }
        return cachedToken;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(getServiceToken());
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private <T> Optional<T> getOne(String path, Class<T> type) {
        try {
            ResponseEntity<T> resp = restTemplate.exchange(
                    coreBaseUrl + path, HttpMethod.GET, new HttpEntity<>(authHeaders()), type);
            return Optional.ofNullable(resp.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    private <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> typeRef) {
        try {
            ResponseEntity<List<T>> resp = restTemplate.exchange(
                    coreBaseUrl + path, HttpMethod.GET, new HttpEntity<>(authHeaders()), typeRef);
            return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("GET {} failed: {}", path, e.getMessage());
            return Collections.emptyList();
        }
    }

    private <T> Optional<T> post(String path, Object body, Class<T> type) {
        try {
            ResponseEntity<T> resp = restTemplate.exchange(
                    coreBaseUrl + path, HttpMethod.POST,
                    new HttpEntity<>(body, authHeaders()), type);
            return Optional.ofNullable(resp.getBody());
        } catch (Exception e) {
            log.error("POST {} failed: {}", path, e.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<T> put(String path, Object body, Class<T> type) {
        try {
            ResponseEntity<T> resp = restTemplate.exchange(
                    coreBaseUrl + path, HttpMethod.PUT,
                    new HttpEntity<>(body, authHeaders()), type);
            return Optional.ofNullable(resp.getBody());
        } catch (Exception e) {
            log.error("PUT {} failed: {}", path, e.getMessage());
            return Optional.empty();
        }
    }

    private void delete(String path) {
        try {
            restTemplate.exchange(coreBaseUrl + path, HttpMethod.DELETE,
                    new HttpEntity<>(authHeaders()), Void.class);
        } catch (Exception e) {
            log.warn("DELETE {} failed: {}", path, e.getMessage());
        }
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    public Optional<Map> getUserByKeycloakId(String keycloakId) {
        return getOne("/internal/users/keycloak/" + keycloakId, Map.class);
    }

    public Optional<Map> getUserById(Long id) {
        return getOne("/internal/users/" + id, Map.class);
    }

    public List<Map> getUsersByCodiceIstat(String codiceIstat) {
        return getList("/internal/users?codiceIstat=" + codiceIstat,
                new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> saveUser(Map<String, Object> user) {
        return post("/internal/users", user, Map.class);
    }

    public Optional<Map> updateUser(Long id, Map<String, Object> user) {
        return put("/internal/users/" + id, user, Map.class);
    }

    public Optional<Map> patchCodiceIstat(Long id, String codiceIstat) {
        return post("/internal/users/" + id + "/codice-istat",
                Map.of("codiceIstat", codiceIstat), Map.class);
    }

    public void deleteUser(Long id) {
        delete("/internal/users/" + id);
    }

    public Optional<Map> saveUserLog(Map<String, Object> log) {
        return post("/internal/users/logs", log, Map.class);
    }

    // ─── Strutture ────────────────────────────────────────────────────────────

    public List<Map> getStrutture(String codiceIstat) {
        return getList("/internal/strutture?codiceIstat=" + codiceIstat,
                new ParameterizedTypeReference<>() {});
    }

    public List<Map> getUtentiStruttura(Integer strutturaId) {
        return getList("/internal/strutture/" + strutturaId + "/utenti",
                new ParameterizedTypeReference<>() {});
    }

    // ─── DUP ──────────────────────────────────────────────────────────────────

    public List<Map> getDup(String codiceIstat) {
        return getList("/internal/dup?codiceIstat=" + codiceIstat,
                new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> getDupById(Long id) {
        return getOne("/internal/dup/" + id, Map.class);
    }

    public Optional<Map> createDup(Map<String, Object> dup) {
        return post("/internal/dup", dup, Map.class);
    }

    public Optional<Map> updateDup(Long id, Map<String, Object> dup) {
        return put("/internal/dup/" + id, dup, Map.class);
    }

    public void deleteDup(Long id) {
        delete("/internal/dup/" + id);
    }

    // ─── Progetti ─────────────────────────────────────────────────────────────

    public List<Map> getProgetti(Long dupId) {
        return getList("/internal/progetti?dupId=" + dupId,
                new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> getProgettoById(Long id) {
        return getOne("/internal/progetti/" + id, Map.class);
    }

    public Optional<Map> createProgetto(Map<String, Object> progetto) {
        return post("/internal/progetti", progetto, Map.class);
    }

    public Optional<Map> updateProgetto(Long id, Map<String, Object> progetto) {
        return put("/internal/progetti/" + id, progetto, Map.class);
    }

    public void deleteProgetto(Long id) {
        delete("/internal/progetti/" + id);
    }

    public void linkLpmToProgetto(Long progettoId, Long lpmId) {
        post("/internal/progetti/" + progettoId + "/lpm/" + lpmId, null, Void.class);
    }

    public void unlinkLpmFromProgetto(Long progettoId) {
        delete("/internal/progetti/" + progettoId + "/lpm");
    }

    // ─── LPM ──────────────────────────────────────────────────────────────────

    public List<Map> getLpm(String codiceIstat) {
        return getList("/internal/lpm?codiceIstat=" + codiceIstat,
                new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> getLpmById(Long id) {
        return getOne("/internal/lpm/" + id, Map.class);
    }

    public Optional<Map> createLpm(Map<String, Object> lpm) {
        return post("/internal/lpm", lpm, Map.class);
    }

    public Optional<Map> updateLpm(Long id, Map<String, Object> lpm) {
        return put("/internal/lpm/" + id, lpm, Map.class);
    }

    public void deleteLpm(Long id, Long deletedBy) {
        delete("/internal/lpm/" + id + "?deletedBy=" + deletedBy);
    }

    public void linkDupToLpm(Long lpmId, Long dupId, Integer userId) {
        post("/internal/lpm/" + lpmId + "/dup/" + dupId, null, Void.class);
    }

    public void unlinkDupFromLpm(Long lpmId, Integer userId) {
        delete("/internal/lpm/" + lpmId + "/dup");
    }

    public List<Map> getLpmNotes(Long lpmId) {
        return getList("/internal/lpm/" + lpmId + "/note", new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> addLpmNote(Long lpmId, Map<String, Object> note) {
        return post("/internal/lpm/" + lpmId + "/note", note, Map.class);
    }

    // ─── Attività ─────────────────────────────────────────────────────────────

    public List<Map> getAttivita(Long progettoId, Long utenteId, Long strutturaId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(coreBaseUrl + "/internal/attivita");
        if (progettoId != null) builder.queryParam("progettoId", progettoId);
        if (utenteId != null) builder.queryParam("utenteId", utenteId);
        if (strutturaId != null) builder.queryParam("strutturaId", strutturaId);
        return getList(builder.build().toUriString().replace(coreBaseUrl, ""),
                new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> getAttivitaById(Long id) {
        return getOne("/internal/attivita/" + id, Map.class);
    }

    public Optional<Map> createAttivita(Map<String, Object> attivita) {
        return post("/internal/attivita", attivita, Map.class);
    }

    public Optional<Map> updateAttivita(Long id, Map<String, Object> attivita) {
        return put("/internal/attivita/" + id, attivita, Map.class);
    }

    public void deleteAttivita(Long id) {
        delete("/internal/attivita/" + id);
    }

    public Optional<Map> duplicaAttivita(Long id) {
        return post("/internal/attivita/" + id + "/duplica", null, Map.class);
    }

    public Optional<Map> updatePercentualeAttivita(Long id, Object percentuale) {
        return put("/internal/attivita/" + id + "/percentuale",
                Map.of("percentuale", percentuale), Map.class);
    }

    public Optional<Map> addAssegnazione(Long attivitaId, Map<String, Object> payload) {
        return post("/internal/attivita/" + attivitaId + "/assegnazioni", payload, Map.class);
    }

    public void removeAssegnazione(Long attivitaId, Long utenteId) {
        delete("/internal/attivita/" + attivitaId + "/assegnazioni/" + utenteId);
    }

    public Optional<Map> addStep(Long attivitaId, Map<String, Object> step) {
        return post("/internal/attivita/" + attivitaId + "/steps", step, Map.class);
    }

    public Optional<Map> toggleStep(Long attivitaId, Long stepId) {
        return put("/internal/attivita/" + attivitaId + "/steps/" + stepId, null, Map.class);
    }

    public void removeStep(Long attivitaId, Long stepId) {
        delete("/internal/attivita/" + attivitaId + "/steps/" + stepId);
    }

    // ─── Timesheet ────────────────────────────────────────────────────────────

    public Optional<Map> logOre(Map<String, Object> entry) {
        return post("/internal/attivita/timesheet", entry, Map.class);
    }

    public List<Map> getTimesheetByAttivita(Long attivitaId) {
        return getList("/internal/attivita/timesheet/attivita/" + attivitaId,
                new ParameterizedTypeReference<>() {});
    }

    public List<Map> getTimesheetByUtente(Long utenteId, String from, String to) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                coreBaseUrl + "/internal/attivita/timesheet/utente/" + utenteId);
        if (from != null) builder.queryParam("from", from);
        if (to != null) builder.queryParam("to", to);
        return getList(builder.build().toUriString().replace(coreBaseUrl, ""),
                new ParameterizedTypeReference<>() {});
    }

    public void deleteTimesheetEntry(Long id) {
        delete("/internal/attivita/timesheet/" + id);
    }

    // ─── Obiettivi ────────────────────────────────────────────────────────────

    public List<Map> getObiettivi(String codiceIstat, Long utenteId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(coreBaseUrl + "/internal/obiettivi");
        if (codiceIstat != null) builder.queryParam("codiceIstat", codiceIstat);
        if (utenteId != null) builder.queryParam("utenteId", utenteId);
        return getList(builder.build().toUriString().replace(coreBaseUrl, ""),
                new ParameterizedTypeReference<>() {});
    }

    public Optional<Map> getObiettivoById(Long id) {
        return getOne("/internal/obiettivi/" + id, Map.class);
    }

    public Optional<Map> createObiettivo(Map<String, Object> obiettivo) {
        return post("/internal/obiettivi", obiettivo, Map.class);
    }

    public Optional<Map> updateObiettivo(Long id, Map<String, Object> obiettivo) {
        return put("/internal/obiettivi/" + id, obiettivo, Map.class);
    }

    public void deleteObiettivo(Long id) {
        delete("/internal/obiettivi/" + id);
    }

    public Optional<Map> registraProgressivoObiettivo(Long id, Map<String, Object> payload) {
        return post("/internal/obiettivi/" + id + "/progressivi", payload, Map.class);
    }

    public Map<String, Long> getObiettiviCounts(String codiceIstat) {
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    coreBaseUrl + "/internal/obiettivi/count?codiceIstat=" + codiceIstat,
                    HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
            return resp.getBody() != null ? (Map<String, Long>) resp.getBody() : Map.of();
        } catch (Exception e) {
            log.error("GET counts failed: {}", e.getMessage());
            return Map.of();
        }
    }
}
