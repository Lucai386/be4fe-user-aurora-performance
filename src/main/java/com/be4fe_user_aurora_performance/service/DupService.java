package com.be4fe_user_aurora_performance.service;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import com.be4fe_user_aurora_performance.dto.dup.*;
import com.be4fe_user_aurora_performance.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BFF User service per DUP.
 * Delega al core la persistenza, mantiene qui la logica di ruolo e mapping DTO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DupService {

    private final CoreApiClient coreApiClient;

    /** Ruoli che possono creare/eliminare DUP */
    private static final List<String> CRUD_ROLES = List.of("AD", "SC", "DR", "CS", "CP");
    /** Tutti i ruoli autenticati possono visualizzare */
    private static final List<String> VIEW_EDIT_ROLES = List.of("AD", "SC", "RA", "DR", "CS", "CP");

    // ==================== DUP CRUD ====================

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ListDupResponse listDup(String codiceIstat, String userRole, Integer userId) {
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return ListDupResponse.error(ErrorCode.NO_ENTE);
        }

        List<Map> dups = coreApiClient.getDup();

        if ("DB".equalsIgnoreCase(userRole) && userId != null) {
            // Per dipendenti base: filtra solo DUP con progetti collegati alle attività dell'utente
            List<Map> myAttivita = coreApiClient.getAttivita(null, (long) userId, null);
            Set<Object> progettiIds = myAttivita.stream()
                    .map(a -> a.get("progettoId"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (progettiIds.isEmpty()) return ListDupResponse.success(List.of());

            List<DupDto> filteredDups = dups.stream()
                    .map(dup -> {
                        Long dupId = longVal(dup, "id");
                        List<Map> progetti = dupId != null ? coreApiClient.getProgetti(dupId) : List.of();
                        List<DupProgettoDto> filteredProgetti = progetti.stream()
                                .filter(p -> progettiIds.contains(p.get("id")) || progettiIds.contains(longVal(p, "id")))
                                .map(this::toProgettoDto)
                                .toList();
                        DupDto dto = toDupDto(dup);
                        dto.setProgetti(filteredProgetti);
                        return dto;
                    })
                    .filter(dto -> !dto.getProgetti().isEmpty())
                    .toList();
            return ListDupResponse.success(filteredDups);
        }

        List<DupDto> dupDtos = dups.stream()
                .map(dup -> {
                    Long dupId = longVal(dup, "id");
                    List<Map> progetti = dupId != null ? coreApiClient.getProgetti(dupId) : List.of();
                    DupDto dto = toDupDto(dup);
                    dto.setProgetti(progetti.stream().map(this::toProgettoDto).toList());
                    return dto;
                })
                .toList();
        return ListDupResponse.success(dupDtos);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DupResponse getDup(Long dupId, String userRole) {
        Optional<Map> dupOpt = coreApiClient.getDupById(dupId);
        if (dupOpt.isEmpty()) return DupResponse.error(ErrorCode.DUP_NOT_FOUND);

        List<Map> progetti = coreApiClient.getProgetti(dupId);
        DupDto dto = toDupDto(dupOpt.get());
        dto.setProgetti(progetti.stream().map(this::toProgettoDto).toList());
        return DupResponse.success(dto);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DupResponse createDup(CreateDupRequest request, String codiceIstat, Integer userId, String userRole) {
        if (!canCreate(userRole)) return DupResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        if (codiceIstat == null || codiceIstat.isBlank()) return DupResponse.error(ErrorCode.NO_ENTE);
        if (request.getAnno() == null) return DupResponse.error(ErrorCode.DUP_ANNO_REQUIRED);
        if (request.getTitolo() == null || request.getTitolo().isBlank()) return DupResponse.error(ErrorCode.DUP_TITOLO_REQUIRED);

        Map<String, Object> payload = new HashMap<>();
        payload.put("codiceIstat", codiceIstat);
        payload.put("anno", request.getAnno());
        payload.put("titolo", request.getTitolo());
        if (request.getDescrizione() != null) payload.put("descrizione", request.getDescrizione());
        payload.put("sezione", request.getSezione() != null ? request.getSezione() : "STRATEGICA");
        payload.put("stato", "BOZZA");
        payload.put("createdBy", userId);

        Optional<Map> saved = coreApiClient.createDup(payload);
        if (saved.isEmpty()) return DupResponse.error(ErrorCode.INTERNAL_ERROR);
        return DupResponse.success(toDupDto(saved.get()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DupResponse updateDup(Long dupId, UpdateDupRequest request, Integer userId, String userRole) {
        if (!canEdit(userRole)) return DupResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        if (coreApiClient.getDupById(dupId).isEmpty()) return DupResponse.error(ErrorCode.DUP_NOT_FOUND);

        Map<String, Object> payload = new HashMap<>();
        if (request.getTitolo() != null) payload.put("titolo", request.getTitolo());
        if (request.getDescrizione() != null) payload.put("descrizione", request.getDescrizione());
        if (request.getSezione() != null) payload.put("sezione", request.getSezione());
        if (request.getStato() != null) payload.put("stato", request.getStato());
        if (request.getDataApprovazione() != null) payload.put("dataApprovazione", request.getDataApprovazione());
        payload.put("updatedBy", userId);

        Optional<Map> saved = coreApiClient.updateDup(dupId, payload);
        if (saved.isEmpty()) return DupResponse.error(ErrorCode.INTERNAL_ERROR);
        return DupResponse.success(toDupDto(saved.get()));
    }

    public DeleteDupResponse deleteDup(Long dupId, String userRole) {
        if (!canDelete(userRole)) return DeleteDupResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        if (coreApiClient.getDupById(dupId).isEmpty()) return DeleteDupResponse.error(ErrorCode.DUP_NOT_FOUND);
        coreApiClient.deleteDup(dupId);
        log.info("DUP eliminato: id={}", dupId);
        return DeleteDupResponse.success();
    }

    // ==================== DTO MAPPING ====================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private DupDto toDupDto(Map m) {
        return DupDto.builder()
                .id(longVal(m, "id"))
                .codiceIstat(strVal(m, "codiceIstat"))
                .codice(strVal(m, "codice"))
                .anno(intVal(m, "anno"))
                .titolo(strVal(m, "titolo"))
                .descrizione(strVal(m, "descrizione"))
                .sezione(strVal(m, "sezione"))
                .stato(strVal(m, "stato"))
                .dataApprovazione(m.get("dataApprovazione") != null
                        ? java.time.LocalDate.parse(m.get("dataApprovazione").toString()) : null)
                .createdAt(strVal(m, "createdAt"))
                .updatedAt(strVal(m, "updatedAt"))
                .progetti(new ArrayList<>())
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private DupProgettoDto toProgettoDto(Map p) {
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
                .dataInizio(p.get("dataInizio") != null
                        ? java.time.LocalDate.parse(p.get("dataInizio").toString()) : null)
                .dataFine(p.get("dataFine") != null
                        ? java.time.LocalDate.parse(p.get("dataFine").toString()) : null)
                .budget(p.get("budget") != null
                        ? new java.math.BigDecimal(p.get("budget").toString()) : null)
                .note(strVal(p, "note"))
                .ordine(intVal(p, "ordine"))
                .build();
    }

    // ─── Permission helpers ────────────────────────────────────────────────────

    private boolean canView(String r) { return r != null && !r.isBlank(); }
    private boolean canEdit(String r) { return r != null && VIEW_EDIT_ROLES.contains(r); }
    private boolean canCreate(String r) { return r != null && CRUD_ROLES.contains(r); }
    private boolean canDelete(String r) { return r != null && CRUD_ROLES.contains(r); }

    // ─── Map helpers ──────────────────────────────────────────────────────────

    private String strVal(Map<?, ?> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }

    private Integer intVal(Map<?, ?> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Long longVal(Map<?, ?> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        try {
            if (v instanceof Number n) return n.longValue();
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private String nestedStrVal(Map<?, ?> m, String nestedKey, String field) {
        Object nested = m.get(nestedKey);
        if (!(nested instanceof Map)) return null;
        Object v = ((Map<?, ?>) nested).get(field);
        return v != null ? v.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private String buildFullName(Map<?, ?> m, String personKey) {
        Object personObj = m.get(personKey);
        if (!(personObj instanceof Map person)) return null;
        String n = strVal(person, "nome"), c = strVal(person, "cognome");
        if (n != null && c != null) return n + " " + c;
        return n != null ? n : c;
    }
}
