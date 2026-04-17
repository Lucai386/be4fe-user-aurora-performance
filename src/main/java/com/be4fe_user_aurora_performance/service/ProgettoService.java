package com.be4fe_user_aurora_performance.service;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import com.be4fe_user_aurora_performance.dto.dup.*;
import com.be4fe_user_aurora_performance.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * BFF User service per i Progetti (DupProgetto).
 * Delega al core la persistenza, mantiene logica di ruolo e mapping DTO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProgettoService {

    private final CoreApiClient coreApiClient;

    private static final List<String> CRUD_ROLES = List.of("AD", "SC", "DR", "CS", "CP");
    private static final List<String> VIEW_EDIT_ROLES = List.of("AD", "SC", "RA", "DR", "CS", "CP");

    // ─── Response types (same as monolith) ────────────────────────────────────

    public record ProgettoResponse(boolean success, String errorCode, String errorMessage, DupProgettoDto data) {
        public static ProgettoResponse success(DupProgettoDto d) { return new ProgettoResponse(true, null, null, d); }
        public static ProgettoResponse error(ErrorCode c) { return new ProgettoResponse(false, c.name(), c.getDefaultMessage(), null); }
    }

    public record ListProgettiResponse(boolean success, String errorCode, String errorMessage, List<DupProgettoDto> data) {
        public static ListProgettiResponse success(List<DupProgettoDto> d) { return new ListProgettiResponse(true, null, null, d); }
        public static ListProgettiResponse error(ErrorCode c) { return new ListProgettiResponse(false, c.name(), c.getDefaultMessage(), null); }
    }

    public record DeleteProgettoResponse(boolean success, String errorCode, String errorMessage, Long deletedId) {
        public static DeleteProgettoResponse success(Long id) { return new DeleteProgettoResponse(true, null, null, id); }
        public static DeleteProgettoResponse error(ErrorCode c) { return new DeleteProgettoResponse(false, c.name(), c.getDefaultMessage(), null); }
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ListProgettiResponse listByDup(Long dupId, String userRole) {
        if (coreApiClient.getDupById(dupId).isEmpty()) return ListProgettiResponse.error(ErrorCode.DUP_NOT_FOUND);
        List<Map> progetti = coreApiClient.getProgetti(dupId);
        return ListProgettiResponse.success(progetti.stream().map(this::toDto).toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ProgettoResponse getProgetto(Long progettoId, String userRole) {
        Optional<Map> opt = coreApiClient.getProgettoById(progettoId);
        return opt.map(p -> ProgettoResponse.success(toDto(p)))
                .orElse(ProgettoResponse.error(ErrorCode.PROGETTO_NOT_FOUND));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ProgettoResponse createProgetto(CreateProgettoRequest request, String userRole) {
        if (!canCreate(userRole)) return ProgettoResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        if (request.getDupId() == null || coreApiClient.getDupById(request.getDupId()).isEmpty()) {
            return ProgettoResponse.error(ErrorCode.DUP_NOT_FOUND);
        }
        if (request.getTitolo() == null || request.getTitolo().isBlank()) {
            return ProgettoResponse.error(ErrorCode.PROGETTO_TITOLO_REQUIRED);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("dupId", request.getDupId());
        payload.put("titolo", request.getTitolo());
        if (request.getDescrizione() != null) payload.put("descrizione", request.getDescrizione());
        if (request.getLpmId() != null) payload.put("lpmId", request.getLpmId());
        if (request.getResponsabileId() != null) payload.put("responsabileId", request.getResponsabileId());
        if (request.getStrutturaId() != null) payload.put("strutturaId", request.getStrutturaId());
        if (request.getPriorita() != null) payload.put("priorita", request.getPriorita());
        if (request.getDataInizio() != null) payload.put("dataInizio", request.getDataInizio());
        if (request.getDataFine() != null) payload.put("dataFine", request.getDataFine());
        if (request.getBudget() != null) payload.put("budget", request.getBudget());
        if (request.getNote() != null) payload.put("note", request.getNote());
        payload.put("ordine", request.getOrdine() != null ? request.getOrdine() : 0);

        Optional<Map> saved = coreApiClient.createProgetto(payload);
        if (saved.isEmpty()) return ProgettoResponse.error(ErrorCode.INTERNAL_ERROR);
        return ProgettoResponse.success(toDto(saved.get()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ProgettoResponse updateProgetto(Long progettoId, UpdateProgettoRequest request, String userRole) {
        if (!canEdit(userRole)) return ProgettoResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        if (coreApiClient.getProgettoById(progettoId).isEmpty()) return ProgettoResponse.error(ErrorCode.PROGETTO_NOT_FOUND);

        Map<String, Object> payload = new HashMap<>();
        if (request.getCodice() != null) payload.put("codice", request.getCodice());
        if (request.getTitolo() != null) payload.put("titolo", request.getTitolo());
        if (request.getDescrizione() != null) payload.put("descrizione", request.getDescrizione());
        if (Boolean.TRUE.equals(request.getRemoveLpm())) payload.put("lpmId", null);
        else if (request.getLpmId() != null) payload.put("lpmId", request.getLpmId());
        if (Boolean.TRUE.equals(request.getRemoveResponsabile())) payload.put("responsabileId", null);
        else if (request.getResponsabileId() != null) payload.put("responsabileId", request.getResponsabileId());
        if (Boolean.TRUE.equals(request.getRemoveStruttura())) payload.put("strutturaId", null);
        else if (request.getStrutturaId() != null) payload.put("strutturaId", request.getStrutturaId());
        if (request.getStato() != null) payload.put("stato", request.getStato());
        if (request.getProgresso() != null) payload.put("progresso", request.getProgresso());
        if (request.getPriorita() != null) payload.put("priorita", request.getPriorita());
        if (request.getDataInizio() != null) payload.put("dataInizio", request.getDataInizio());
        if (request.getDataFine() != null) payload.put("dataFine", request.getDataFine());
        if (request.getBudget() != null) payload.put("budget", request.getBudget());
        if (request.getNote() != null) payload.put("note", request.getNote());
        if (request.getOrdine() != null) payload.put("ordine", request.getOrdine());

        Optional<Map> saved = coreApiClient.updateProgetto(progettoId, payload);
        if (saved.isEmpty()) return ProgettoResponse.error(ErrorCode.INTERNAL_ERROR);
        return ProgettoResponse.success(toDto(saved.get()));
    }

    public DeleteProgettoResponse deleteProgetto(Long progettoId, String userRole) {
        if (!canDelete(userRole)) return DeleteProgettoResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        if (coreApiClient.getProgettoById(progettoId).isEmpty()) return DeleteProgettoResponse.error(ErrorCode.PROGETTO_NOT_FOUND);
        coreApiClient.deleteProgetto(progettoId);
        return DeleteProgettoResponse.success(progettoId);
    }

    public ProgettoResponse linkLpm(Long progettoId, Long lpmId, String userRole) {
        if (!canEdit(userRole)) return ProgettoResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        coreApiClient.linkLpmToProgetto(progettoId, lpmId);
        return coreApiClient.getProgettoById(progettoId)
                .map(p -> ProgettoResponse.success(toDto(p)))
                .orElse(ProgettoResponse.error(ErrorCode.PROGETTO_NOT_FOUND));
    }

    public ProgettoResponse unlinkLpm(Long progettoId, String userRole) {
        if (!canEdit(userRole)) return ProgettoResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        coreApiClient.unlinkLpmFromProgetto(progettoId);
        return coreApiClient.getProgettoById(progettoId)
                .map(p -> ProgettoResponse.success(toDto(p)))
                .orElse(ProgettoResponse.error(ErrorCode.PROGETTO_NOT_FOUND));
    }

    // ─── DTO mapping ──────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private DupProgettoDto toDto(Map p) {
        return DupProgettoDto.builder()
                .id(longVal(p, "id"))
                .dupId(longVal(p, "dupId"))
                .codice(strVal(p, "codice"))
                .titolo(strVal(p, "titolo"))
                .descrizione(strVal(p, "descrizione"))
                .lpmId(longVal(p, "lpmId"))
                .lpmTitolo(nestedStrVal(p, "lpm", "titolo"))
                .responsabileId(intVal(p, "responsabileId"))
                .responsabileNome(buildFullName(p, "responsabile"))
                .strutturaId(intVal(p, "strutturaId"))
                .strutturaNome(nestedStrVal(p, "struttura", "nome"))
                .stato(strVal(p, "stato"))
                .progresso(intVal(p, "progresso"))
                .priorita(strVal(p, "priorita"))
                .dataInizio(p.get("dataInizio") != null ? LocalDate.parse(p.get("dataInizio").toString()) : null)
                .dataFine(p.get("dataFine") != null ? LocalDate.parse(p.get("dataFine").toString()) : null)
                .budget(p.get("budget") != null ? new BigDecimal(p.get("budget").toString()) : null)
                .note(strVal(p, "note"))
                .ordine(intVal(p, "ordine"))
                .build();
    }

    // ─── Permission helpers ────────────────────────────────────────────────────
    private boolean canCreate(String r) { return r != null && CRUD_ROLES.contains(r); }
    private boolean canEdit(String r) { return r != null && VIEW_EDIT_ROLES.contains(r); }
    private boolean canDelete(String r) { return r != null && CRUD_ROLES.contains(r); }

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

    @SuppressWarnings("unchecked")
    private String buildFullName(Map<?, ?> m, String pk) {
        Object p = m.get(pk);
        if (!(p instanceof Map pm)) return null;
        String n = strVal(pm, "nome"), c = strVal(pm, "cognome");
        return (n != null && c != null) ? n + " " + c : (n != null ? n : c);
    }
}
