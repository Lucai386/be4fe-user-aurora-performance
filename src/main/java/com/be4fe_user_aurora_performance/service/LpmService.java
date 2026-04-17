package com.be4fe_user_aurora_performance.service;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import com.be4fe_user_aurora_performance.dto.lpm.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * BFF User service per LPM (Linee Programmatiche di Mandato).
 * Delega al core la persistenza.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LpmService {

    private final CoreApiClient coreApiClient;

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<LpmActivityDto> list(LpmListRequest request) {
        List<Map> items = coreApiClient.getLpm();
        return items.stream().map(this::toActivityDto).toList();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Optional<LpmActivityDto> get(Long id) {
        return coreApiClient.getLpmById(id).map(m -> toActivityDtoWithNotes(m, id));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public LpmActivityDto create(LpmCreateRequest request, Integer userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("titolo", request.getTitle());
        payload.put("stato", request.getStatus() != null ? request.getStatus() : "todo");
        payload.put("codiceIstat", request.getCodiceIstat() != null ? request.getCodiceIstat() : "000000");
        payload.put("annoInizioMandato", request.getAnnoInizioMandato() != null
                ? request.getAnnoInizioMandato() : java.time.Year.now().getValue());
        payload.put("annoFineMandato", request.getAnnoFineMandato() != null
                ? request.getAnnoFineMandato() : java.time.Year.now().getValue() + 5);
        payload.put("priorita", request.getPriority() != null ? request.getPriority() : 0);
        payload.put("createdBy", userId);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            payload.put("firstNote", request.getNotes());
        }

        Optional<Map> saved = coreApiClient.createLpm(payload);
        return saved.map(m -> toActivityDtoWithNotes(m, longVal(m, "id"))).orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Optional<LpmActivityDto> update(LpmUpdateRequest request, Integer userId) {
        Long id = request.getIdAsLong();
        if (id == null) return Optional.empty();

        Optional<Map> existing = coreApiClient.getLpmById(id);
        if (existing.isEmpty()) return Optional.empty();

        Map<String, Object> payload = new HashMap<>();
        if (request.getTitle() != null) payload.put("titolo", request.getTitle());
        if (request.getDescription() != null) payload.put("descrizione", request.getDescription());
        if (request.getStatus() != null) payload.put("stato", request.getStatus());
        if (request.getPriority() != null) payload.put("priorita", request.getPriority());
        if (request.getProgress() != null) payload.put("progresso", request.getProgress());
        payload.put("updatedBy", userId);

        Optional<Map> saved = coreApiClient.updateLpm(id, payload);

        // Aggiunge nota se presente
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            coreApiClient.addLpmNote(id, Map.of("testo", request.getNotes(), "autoreId", userId));
        }

        return saved.map(m -> toActivityDtoWithNotes(m, id));
    }

    public boolean delete(LpmDeleteRequest request, Integer userId) {
        Long id = request.getIdAsLong();
        if (id == null) return false;
        coreApiClient.deleteLpm(id, (long) userId);
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Optional<LpmActivityDto> linkDup(Long lpmId, Long dupId, Integer userId) {
        coreApiClient.linkDupToLpm(lpmId, dupId, userId);
        return coreApiClient.getLpmById(lpmId).map(m -> toActivityDtoWithNotes(m, lpmId));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Optional<LpmActivityDto> unlinkDup(Long lpmId, Integer userId) {
        coreApiClient.unlinkDupFromLpm(lpmId, userId);
        return coreApiClient.getLpmById(lpmId).map(m -> toActivityDtoWithNotes(m, lpmId));
    }

    // ─── DTO Mapping ──────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LpmActivityDto toActivityDto(Map m) {
        LpmActivityDto dto = new LpmActivityDto();
        Object id = m.get("id");
        if (id != null) dto.setId(id.toString());
        dto.setTitle(strVal(m, "titolo"));
        dto.setDescription(strVal(m, "descrizione"));
        dto.setStatus(strVal(m, "stato"));
        Object prio = m.get("priorita");
        dto.setPriority(prio instanceof Number n ? n.intValue() : 0);
        Object prog = m.get("progresso");
        dto.setProgress(prog instanceof Number n ? n.intValue() : 0);
        Object dupId = m.get("dupId");
        if (dupId != null) dto.setDupId(dupId.toString());
        dto.setDupTitle(strVal(m, "dupTitolo"));

        Object respObj = m.get("responsabile");
        if (respObj instanceof Map rm) {
            String nome = strVal(rm, "nome"), cognome = strVal(rm, "cognome");
            dto.setResponsibleName((nome != null && cognome != null) ? nome + " " + cognome
                    : (nome != null ? nome : cognome));
        }
        return dto;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LpmActivityDto toActivityDtoWithNotes(Map m, Long lpmId) {
        LpmActivityDto dto = toActivityDto(m);
        if (lpmId != null) {
            List<Map> notes = coreApiClient.getLpmNotes(lpmId);
            dto.setNotes(notes.stream().map(this::toNoteDto).toList());
        }
        return dto;
    }

    @SuppressWarnings("unchecked")
    private LpmNoteActivityDto toNoteDto(Map n) {
        LpmNoteActivityDto dto = new LpmNoteActivityDto();
        Object id = n.get("id");
        if (id != null) dto.setId(id.toString());
        dto.setText(strVal(n, "testo"));
        dto.setCreatedAt(strVal(n, "createdAt"));
        Object authorObj = n.get("autore");
        if (authorObj instanceof Map am) {
            String nome = strVal(am, "nome"), cognome = strVal(am, "cognome");
            dto.setAuthor((nome != null && cognome != null) ? nome + " " + cognome
                    : (nome != null ? nome : cognome));
        }
        return dto;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private String strVal(Map<?, ?> m, String k) { Object v = m.get(k); return v != null ? v.toString() : null; }
    private Long longVal(Map<?, ?> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }
}
