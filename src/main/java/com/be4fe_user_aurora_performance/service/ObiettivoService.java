package com.be4fe_user_aurora_performance.service;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import com.be4fe_user_aurora_performance.dto.obiettivo.*;
import com.be4fe_user_aurora_performance.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * BFF User service per gli Obiettivi.
 * Delega CRUD al core, calcola percentualeCompletamento in BFF
 * (il core non espone calcolaPercentuale() come getter Jackson).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ObiettivoService {

    private final CoreApiClient coreApiClient;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<String> CREATE_ROLES = List.of("AD", "SC", "DR", "RA", "CS", "CP");
    private static final List<String> VIEW_ALL_ROLES = List.of("AD", "SC", "DR", "RA", "CS", "CP");
    private static final List<String> ADMIN_ROLES = List.of("AD", "SC");

    // ─── List ─────────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ListObiettiviResponse listObiettivi(String codiceIstat, Long userId, String userRole) {
        if (codiceIstat == null || codiceIstat.isBlank()) return ListObiettiviResponse.error(ErrorCode.NO_ENTE);

        List<Map> raw;
        if (userRole != null && VIEW_ALL_ROLES.contains(userRole)) {
            raw = coreApiClient.getObiettivi(null);
        } else {
            raw = coreApiClient.getObiettivi(userId);
        }
        return ListObiettiviResponse.ok(raw.stream().map(this::toDto).toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ListObiettiviResponse listMieiObiettivi(Long userId) {
        if (userId == null) return ListObiettiviResponse.error(ErrorCode.USER_NOT_FOUND);
        List<Map> raw = coreApiClient.getObiettivi(userId);
        return ListObiettiviResponse.ok(raw.stream().map(this::toDto).toList());
    }

    // ─── Get ──────────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ObiettivoResponse getObiettivo(Long id, Long userId, String userRole) {
        if (id == null) return ObiettivoResponse.error(ErrorCode.INVALID_ID);
        Optional<Map> opt = coreApiClient.getObiettivoById(id);
        if (opt.isEmpty()) return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_FOUND);
        Map m = opt.get();
        // Non-admin can only see their own
        if (userRole != null && !VIEW_ALL_ROLES.contains(userRole)) {
            Long assId = longVal(m, "utenteAssegnatoId");
            if (assId == null || !assId.equals(userId)) return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_AUTHORIZED);
        }
        return ObiettivoResponse.ok(toDto(m));
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ObiettivoResponse createObiettivo(CreateObiettivoRequest request, String codiceIstat, Long userId, String userRole) {
        if (userRole == null || !CREATE_ROLES.contains(userRole)) return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_AUTHORIZED);
        if (codiceIstat == null || codiceIstat.isBlank()) return ObiettivoResponse.error(ErrorCode.NO_ENTE);
        if (request.getTitolo() == null || request.getTitolo().isBlank()) return ObiettivoResponse.error(ErrorCode.OBIETTIVO_TITOLO_REQUIRED);
        if (request.getValoreTarget() == null) return ObiettivoResponse.error(ErrorCode.OBIETTIVO_TARGET_REQUIRED);
        if (request.getAnno() == null) return ObiettivoResponse.error(ErrorCode.OBIETTIVO_ANNO_REQUIRED);

        Map<String, Object> payload = new HashMap<>();
        payload.put("codiceIstat", codiceIstat);
        payload.put("titolo", request.getTitolo());
        if (request.getDescrizione() != null) payload.put("descrizione", request.getDescrizione());
        if (request.getUnitaMisura() != null) payload.put("unitaMisura", request.getUnitaMisura());
        payload.put("tipo", request.getTipo() != null ? request.getTipo() : "CRESCENTE");
        payload.put("valoreIniziale", request.getValoreIniziale() != null ? request.getValoreIniziale() : BigDecimal.ZERO);
        payload.put("valoreTarget", request.getValoreTarget());
        payload.put("peso", request.getPeso() != null ? request.getPeso() : new BigDecimal("100"));
        if (request.getDataInizio() != null) payload.put("dataInizio", request.getDataInizio());
        if (request.getDataFine() != null) payload.put("dataFine", request.getDataFine());
        payload.put("anno", request.getAnno());
        if (request.getStrutturaId() != null) payload.put("strutturaId", request.getStrutturaId());
        if (request.getUtenteAssegnatoId() != null) payload.put("utenteAssegnatoId", request.getUtenteAssegnatoId());
        payload.put("creatoDaId", userId);

        return coreApiClient.createObiettivo(payload)
                .map(m -> ObiettivoResponse.ok(toDto(m)))
                .orElse(ObiettivoResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ObiettivoResponse updateObiettivo(UpdateObiettivoRequest request, Long userId, String userRole) {
        if (request.getId() == null) return ObiettivoResponse.error(ErrorCode.INVALID_ID);
        Optional<Map> opt = coreApiClient.getObiettivoById(request.getId());
        if (opt.isEmpty()) return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_FOUND);
        Map existing = opt.get();
        if (!canModify(userRole, userId, existing)) return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_AUTHORIZED);

        Map<String, Object> payload = new HashMap<>(existing);
        if (request.getTitolo() != null && !request.getTitolo().isBlank()) payload.put("titolo", request.getTitolo());
        if (request.getDescrizione() != null) payload.put("descrizione", request.getDescrizione());
        if (request.getUnitaMisura() != null) payload.put("unitaMisura", request.getUnitaMisura());
        if (request.getTipo() != null) payload.put("tipo", request.getTipo());
        if (request.getStato() != null) payload.put("stato", request.getStato());
        if (request.getValoreIniziale() != null) payload.put("valoreIniziale", request.getValoreIniziale());
        if (request.getValoreTarget() != null) payload.put("valoreTarget", request.getValoreTarget());
        if (request.getPeso() != null) payload.put("peso", request.getPeso());
        if (request.getDataInizio() != null) payload.put("dataInizio", request.getDataInizio());
        if (request.getDataFine() != null) payload.put("dataFine", request.getDataFine());
        if (request.getUtenteAssegnatoId() != null) payload.put("utenteAssegnatoId", request.getUtenteAssegnatoId());

        return coreApiClient.updateObiettivo(request.getId(), payload)
                .map(m -> ObiettivoResponse.ok(toDto(m)))
                .orElse(ObiettivoResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ObiettivoResponse deleteObiettivo(Long id, Long userId, String userRole) {
        if (id == null) return ObiettivoResponse.error(ErrorCode.INVALID_ID);
        Optional<Map> opt = coreApiClient.getObiettivoById(id);
        if (opt.isEmpty()) return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_FOUND);
        if (!canModify(userRole, userId, opt.get())) return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_AUTHORIZED);
        coreApiClient.deleteObiettivo(id);
        return ObiettivoResponse.ok();
    }

    // ─── Registra Progressivo ─────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ObiettivoResponse registraProgressivo(RegistraProgressivoRequest request, Long userId, String userRole) {
        if (request.getObiettivoId() == null) return ObiettivoResponse.error(ErrorCode.INVALID_ID);
        if (request.getNuovoValore() == null) return ObiettivoResponse.error(ErrorCode.PROGRESSIVO_VALORE_REQUIRED);

        Optional<Map> opt = coreApiClient.getObiettivoById(request.getObiettivoId());
        if (opt.isEmpty()) return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_FOUND);
        Map existing = opt.get();
        if (!canRegistraProgressivo(userRole, userId, existing)) return ObiettivoResponse.error(ErrorCode.PROGRESSIVO_NOT_AUTHORIZED);

        Map<String, Object> payload = new HashMap<>();
        payload.put("valore", request.getNuovoValore());
        payload.put("registratoDaId", userId);
        if (request.getNote() != null) payload.put("note", request.getNote());

        return coreApiClient.registraProgressivoObiettivo(request.getObiettivoId(), payload)
                .flatMap(r -> coreApiClient.getObiettivoById(request.getObiettivoId()))
                .map(m -> ObiettivoResponse.ok(toDto(m)))
                .orElse(ObiettivoResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    // ─── Counts ───────────────────────────────────────────────────────────────

    public Map<String, Long> getCounts() {
        return coreApiClient.getObiettiviCounts();
    }

    public long countTotale() {
        return coreApiClient.getObiettiviCounts().getOrDefault("totale", 0L);
    }

    public long countAttivi() {
        return coreApiClient.getObiettiviCounts().getOrDefault("attivi", 0L);
    }

    public long countCompletati() {
        return coreApiClient.getObiettiviCounts().getOrDefault("completati", 0L);
    }

    // ─── Permission helpers ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private boolean canModify(String role, Long userId, Map<?, ?> existing) {
        if (role == null) return false;
        if (ADMIN_ROLES.contains(role)) return true;
        Long creatoDaId = longVal(existing, "creatoDaId");
        return creatoDaId != null && creatoDaId.equals(userId);
    }

    @SuppressWarnings("unchecked")
    private boolean canRegistraProgressivo(String role, Long userId, Map<?, ?> existing) {
        if (role == null) return false;
        if (ADMIN_ROLES.contains(role)) return true;
        Long creatoDaId = longVal(existing, "creatoDaId");
        if (creatoDaId != null && creatoDaId.equals(userId)) return true;
        Long assignedId = longVal(existing, "utenteAssegnatoId");
        return assignedId != null && assignedId.equals(userId);
    }

    // ─── DTO Mapping ──────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    ObiettivoDto toDto(Map m) {
        List<ObiettivoProgressivoDto> progressivi = List.of();
        Object progObj = m.get("progressivi");
        if (progObj instanceof List pl) {
            progressivi = ((List<Map>) pl).stream().map(this::toProgressivoDto).toList();
        }

        return ObiettivoDto.builder()
                .id(longVal(m, "id"))
                .codice(strVal(m, "codice"))
                .titolo(strVal(m, "titolo"))
                .descrizione(strVal(m, "descrizione"))
                .unitaMisura(strVal(m, "unitaMisura"))
                .tipo(strVal(m, "tipo"))
                .stato(strVal(m, "stato"))
                .valoreIniziale(bigDecimalVal(m, "valoreIniziale"))
                .valoreTarget(bigDecimalVal(m, "valoreTarget"))
                .valoreCorrente(bigDecimalVal(m, "valoreCorrente"))
                .percentualeCompletamento(computePercentuale(m))
                .peso(bigDecimalVal(m, "peso"))
                .dataInizio(strVal(m, "dataInizio"))
                .dataFine(strVal(m, "dataFine"))
                .anno(intVal(m, "anno"))
                .strutturaId(intVal(m, "strutturaId"))
                .strutturaNome(nestedStrVal(m, "struttura", "nome"))
                .utenteAssegnatoId(longVal(m, "utenteAssegnatoId"))
                .utenteAssegnatoNome(nestedStrVal(m, "utenteAssegnato", "nome"))
                .utenteAssegnatoCognome(nestedStrVal(m, "utenteAssegnato", "cognome"))
                .creatoDaId(longVal(m, "creatoDaId"))
                .creatoDaNome(nestedStrVal(m, "creatoDa", "nome"))
                .creatoDaCognome(nestedStrVal(m, "creatoDa", "cognome"))
                .progressivi(progressivi)
                .createdAt(strVal(m, "createdAt"))
                .updatedAt(strVal(m, "updatedAt"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private ObiettivoProgressivoDto toProgressivoDto(Map m) {
        BigDecimal valore = bigDecimalVal(m, "valoreRegistrato");
        BigDecimal prec = bigDecimalVal(m, "valorePrecedente");
        BigDecimal delta = valore != null && prec != null ? valore.subtract(prec) : null;
        return ObiettivoProgressivoDto.builder()
                .id(longVal(m, "id"))
                .obiettivoId(longVal(m, "obiettivoId"))
                .valoreRegistrato(valore)
                .valorePrecedente(prec)
                .delta(delta)
                .note(strVal(m, "note"))
                .registratoDaId(longVal(m, "registratoDaId"))
                .registratoDaNome(nestedStrVal(m, "registratoDa", "nome"))
                .registratoDaCognome(nestedStrVal(m, "registratoDa", "cognome"))
                .dataRegistrazione(strVal(m, "dataRegistrazione"))
                .build();
    }

    /**
     * Replicates Obiettivo.calcolaPercentuale() since core doesn't expose it as a Jackson-serialized getter.
     */
    BigDecimal computePercentuale(Map<?, ?> m) {
        BigDecimal target = bigDecimalVal(m, "valoreTarget");
        BigDecimal iniziale = bigDecimalVal(m, "valoreIniziale");
        BigDecimal corrente = bigDecimalVal(m, "valoreCorrente");
        String tipo = strVal(m, "tipo");
        if (target == null) return BigDecimal.ZERO;
        BigDecimal init = iniziale != null ? iniziale : BigDecimal.ZERO;
        BigDecimal curr = corrente != null ? corrente : init;
        BigDecimal range;
        BigDecimal progress;
        if ("DECRESCENTE".equals(tipo)) {
            range = init.subtract(target);
            progress = init.subtract(curr);
        } else {
            range = target.subtract(init);
            progress = curr.subtract(init);
        }
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return curr.compareTo(target) == 0 ? new BigDecimal("100") : BigDecimal.ZERO;
        }
        BigDecimal pct = progress.multiply(new BigDecimal("100")).divide(range, 2, RoundingMode.HALF_UP);
        if (pct.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (pct.compareTo(new BigDecimal("100")) > 0) return new BigDecimal("100");
        return pct;
    }

    // ─── Map helpers ──────────────────────────────────────────────────────────

    private String strVal(Map<?, ?> m, String k) { Object v = m.get(k); return v != null ? v.toString() : null; }

    private Integer intVal(Map<?, ?> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Long longVal(Map<?, ?> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private String nestedStrVal(Map<?, ?> m, String nk, String f) {
        Object n = m.get(nk);
        if (!(n instanceof Map)) return null;
        Object v = ((Map<?, ?>) n).get(f);
        return v != null ? v.toString() : null;
    }

    private BigDecimal bigDecimalVal(Map<?, ?> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; }
    }
}
