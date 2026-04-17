package com.be4fe_user_aurora_performance.service;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import com.be4fe_user_aurora_performance.dto.attivita.*;
import com.be4fe_user_aurora_performance.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * BFF User service per Attività.
 * Tutta la business logic su step/ore/assegnazioni è delegata al core.
 * Qui manteniamo: role-based access control e mapping DTO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttivitaService {

    private final CoreApiClient coreApiClient;

    private static final List<String> CRUD_ROLES = List.of("AD", "SC", "DR", "CS", "CP");
    private static final List<String> EDIT_ROLES = List.of("AD", "SC", "DR", "CS", "CP");

    // ─── Response records ─────────────────────────────────────────────────────

    public record AttivitaResponse(String result, String errorCode, String message, AttivitaDto attivita) {
        public static AttivitaResponse success(AttivitaDto a) { return new AttivitaResponse("OK", null, null, a); }
        public static AttivitaResponse error(ErrorCode c) { return new AttivitaResponse("KO", c.name(), c.getDefaultMessage(), null); }
    }

    public record ListAttivitaResponse(String result, String errorCode, String message, List<AttivitaDto> attivitaList) {
        public static ListAttivitaResponse success(List<AttivitaDto> l) { return new ListAttivitaResponse("OK", null, null, l); }
        public static ListAttivitaResponse error(ErrorCode c) { return new ListAttivitaResponse("KO", c.name(), c.getDefaultMessage(), null); }
    }

    public record DeleteAttivitaResponse(String result, String errorCode, String message) {
        public static DeleteAttivitaResponse success() { return new DeleteAttivitaResponse("OK", null, null); }
        public static DeleteAttivitaResponse error(ErrorCode c) { return new DeleteAttivitaResponse("KO", c.name(), c.getDefaultMessage()); }
    }

    public record TimesheetEntryResponse(String result, String errorCode, String message, TimesheetEntryDto entry) {
        public static TimesheetEntryResponse success(TimesheetEntryDto e) { return new TimesheetEntryResponse("OK", null, null, e); }
        public static TimesheetEntryResponse error(ErrorCode c) { return new TimesheetEntryResponse("KO", c.name(), c.getDefaultMessage(), null); }
    }

    public record ListTimesheetResponse(String result, String errorCode, String message, List<TimesheetEntryDto> entries) {
        public static ListTimesheetResponse success(List<TimesheetEntryDto> e) { return new ListTimesheetResponse("OK", null, null, e); }
        public static ListTimesheetResponse error(ErrorCode c) { return new ListTimesheetResponse("KO", c.name(), c.getDefaultMessage(), null); }
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ListAttivitaResponse listAll(String userRole, Integer userId) {
        List<Map> all;
        if ("DB".equalsIgnoreCase(userRole) && userId != null) {
            all = coreApiClient.getAttivita(null, (long) userId, null);
        } else {
            all = coreApiClient.getAttivita(null, null, null);
        }
        return ListAttivitaResponse.success(all.stream().map(this::toDto).toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ListAttivitaResponse listByProgetto(Long progettoId, String userRole) {
        if (coreApiClient.getProgettoById(progettoId).isEmpty()) {
            return ListAttivitaResponse.error(ErrorCode.PROGETTO_NOT_FOUND);
        }
        List<Map> list = coreApiClient.getAttivita(progettoId, null, null);
        return ListAttivitaResponse.success(list.stream().map(this::toDto).toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AttivitaResponse getAttivita(Long id, String userRole) {
        return coreApiClient.getAttivitaById(id)
                .map(m -> AttivitaResponse.success(toDto(m)))
                .orElse(AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AttivitaResponse createAttivita(CreateAttivitaRequest request, String userRole) {
        if (!canCreate(userRole)) return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        if (request.getTitolo() == null || request.getTitolo().isBlank()) return AttivitaResponse.error(ErrorCode.ATTIVITA_TITOLO_REQUIRED);
        if (coreApiClient.getProgettoById(request.getProgettoId()).isEmpty()) return AttivitaResponse.error(ErrorCode.PROGETTO_NOT_FOUND);

        Map<String, Object> payload = new HashMap<>();
        payload.put("progettoId", request.getProgettoId());
        payload.put("titolo", request.getTitolo());
        if (request.getDescrizione() != null) payload.put("descrizione", request.getDescrizione());
        if (request.getPriorita() != null) payload.put("priorita", request.getPriorita());
        payload.put("peso", request.getPeso() != null ? request.getPeso() : 100);
        payload.put("oreStimate", request.getOreStimate() != null ? request.getOreStimate() : BigDecimal.ZERO);
        if (request.getDataInizio() != null) payload.put("dataInizio", request.getDataInizio());
        if (request.getDataFineStimata() != null) payload.put("dataFineStimata", request.getDataFineStimata());
        if (request.getNote() != null) payload.put("note", request.getNote());
        payload.put("ordine", request.getOrdine() != null ? request.getOrdine() : 0);
        if (request.getStrutturaId() != null) payload.put("strutturaId", request.getStrutturaId());

        return coreApiClient.createAttivita(payload)
                .map(m -> AttivitaResponse.success(toDto(m)))
                .orElse(AttivitaResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AttivitaResponse updateAttivita(Long id, UpdateAttivitaRequest request, String userRole) {
        if (!canEdit(userRole)) return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        if (coreApiClient.getAttivitaById(id).isEmpty()) return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);

        Map<String, Object> payload = new HashMap<>();
        if (request.getCodice() != null) payload.put("codice", request.getCodice());
        if (request.getTitolo() != null) payload.put("titolo", request.getTitolo());
        if (request.getDescrizione() != null) payload.put("descrizione", request.getDescrizione());
        if (request.getStato() != null) payload.put("stato", request.getStato());
        if (request.getPriorita() != null) payload.put("priorita", request.getPriorita());
        if (request.getPeso() != null) payload.put("peso", request.getPeso());
        if (request.getOreStimate() != null) payload.put("oreStimate", request.getOreStimate());
        if (request.getDataInizio() != null) payload.put("dataInizio", request.getDataInizio());
        if (request.getDataFineStimata() != null) payload.put("dataFineStimata", request.getDataFineStimata());
        if (request.getDataFineEffettiva() != null) payload.put("dataFineEffettiva", request.getDataFineEffettiva());
        if (request.getNote() != null) payload.put("note", request.getNote());
        if (request.getOrdine() != null) payload.put("ordine", request.getOrdine());
        if (Boolean.TRUE.equals(request.getRemoveStruttura())) payload.put("strutturaId", null);
        else if (request.getStrutturaId() != null) payload.put("strutturaId", request.getStrutturaId());

        return coreApiClient.updateAttivita(id, payload)
                .map(m -> AttivitaResponse.success(toDto(m)))
                .orElse(AttivitaResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    public DeleteAttivitaResponse deleteAttivita(Long id, String userRole) {
        if (!canCreate(userRole)) return DeleteAttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        if (coreApiClient.getAttivitaById(id).isEmpty()) return DeleteAttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        coreApiClient.deleteAttivita(id);
        return DeleteAttivitaResponse.success();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AttivitaResponse duplicaAttivita(Long id, Integer nuovaStrutturaId, String userRole) {
        if (!canCreate(userRole)) return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        if (coreApiClient.getAttivitaById(id).isEmpty()) return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        return coreApiClient.duplicaAttivita(id)
                .map(m -> AttivitaResponse.success(toDto(m)))
                .orElse(AttivitaResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    // ─── Assegnazioni ─────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AttivitaResponse assegnaUtente(AssegnaUtenteRequest request, String userRole) {
        if (!canEdit(userRole)) return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        if (coreApiClient.getAttivitaById(request.getAttivitaId()).isEmpty()) return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        if (coreApiClient.getUserById((long) request.getUtenteId()).isEmpty()) return AttivitaResponse.error(ErrorCode.USER_NOT_FOUND);

        Map<String, Object> payload = new HashMap<>();
        payload.put("utenteId", request.getUtenteId());
        if (request.getRuolo() != null) payload.put("ruolo", request.getRuolo());
        if (request.getOreStimate() != null) payload.put("oreStimate", request.getOreStimate());
        if (request.getDataInizio() != null) payload.put("dataInizio", request.getDataInizio());
        if (request.getDataFine() != null) payload.put("dataFine", request.getDataFine());
        if (request.getNote() != null) payload.put("note", request.getNote());

        Optional<Map> result = coreApiClient.addAssegnazione(request.getAttivitaId(), payload);
        if (result.isEmpty()) return AttivitaResponse.error(ErrorCode.INTERNAL_ERROR);
        return coreApiClient.getAttivitaById(request.getAttivitaId())
                .map(m -> AttivitaResponse.success(toDto(m)))
                .orElse(AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND));
    }

    public AttivitaResponse rimuoviAssegnazione(Long attivitaId, Integer utenteId, String userRole) {
        if (!canEdit(userRole)) return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        coreApiClient.removeAssegnazione(attivitaId, (long) utenteId);
        return coreApiClient.getAttivitaById(attivitaId)
                .map(m -> AttivitaResponse.success(toDto(m)))
                .orElse(AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND));
    }

    // ─── Percentuale ──────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AttivitaResponse updatePercentualeCompletamento(Long attivitaId, Integer percentuale, String userRole, Integer userId) {
        if (percentuale == null || percentuale < 0 || percentuale > 100) return AttivitaResponse.error(ErrorCode.INVALID_PERCENTAGE);
        return coreApiClient.updatePercentualeAttivita(attivitaId, percentuale)
                .map(m -> AttivitaResponse.success(toDto(m)))
                .orElse(AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND));
    }

    // ─── Steps ────────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AttivitaResponse addStep(Long attivitaId, String titolo, String descrizione, Integer peso, String userRole, Integer userId) {
        if (!canEdit(userRole)) return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        Map<String, Object> payload = new HashMap<>();
        payload.put("titolo", titolo);
        if (descrizione != null) payload.put("descrizione", descrizione);
        payload.put("peso", peso != null ? peso : 0);
        payload.put("completato", false);
        coreApiClient.addStep(attivitaId, payload);
        return coreApiClient.getAttivitaById(attivitaId)
                .map(m -> AttivitaResponse.success(toDto(m)))
                .orElse(AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AttivitaResponse toggleStep(Long attivitaId, Long stepId, Boolean completato, String descrizione, String userRole, Integer userId) {
        if (!canEdit(userRole)) return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        coreApiClient.toggleStep(attivitaId, stepId);
        return coreApiClient.getAttivitaById(attivitaId)
                .map(m -> AttivitaResponse.success(toDto(m)))
                .orElse(AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND));
    }

    public AttivitaResponse removeStep(Long attivitaId, Long stepId, String userRole, Integer userId) {
        if (!canEdit(userRole)) return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        coreApiClient.removeStep(attivitaId, stepId);
        return coreApiClient.getAttivitaById(attivitaId)
                .map(m -> AttivitaResponse.success(toDto(m)))
                .orElse(AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND));
    }

    // ─── Timesheet ────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public TimesheetEntryResponse logOreLavorate(LogOreLavorateRequest request, String userRole) {
        if (coreApiClient.getAttivitaById(request.getAttivitaId()).isEmpty()) return TimesheetEntryResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attivitaId", request.getAttivitaId());
        payload.put("utenteId", request.getUtenteId());
        payload.put("data", request.getData());
        payload.put("oreLavorate", request.getOreLavorate());
        if (request.getDescrizione() != null) payload.put("descrizione", request.getDescrizione());

        return coreApiClient.logOre(payload)
                .map(m -> TimesheetEntryResponse.success(toTimesheetDto(m)))
                .orElse(TimesheetEntryResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ListTimesheetResponse getTimesheetByAttivita(Long attivitaId, String userRole) {
        List<Map> entries = coreApiClient.getTimesheetByAttivita(attivitaId);
        return ListTimesheetResponse.success(entries.stream().map(this::toTimesheetDto).toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ListTimesheetResponse getTimesheetByUtente(Integer utenteId, LocalDate dataInizio, LocalDate dataFine, String userRole) {
        String from = dataInizio != null ? dataInizio.toString() : null;
        String to = dataFine != null ? dataFine.toString() : null;
        List<Map> entries = coreApiClient.getTimesheetByUtente((long) utenteId, from, to);
        return ListTimesheetResponse.success(entries.stream().map(this::toTimesheetDto).toList());
    }

    public DeleteAttivitaResponse deleteTimesheetEntry(Long id, String userRole) {
        coreApiClient.deleteTimesheetEntry(id);
        return DeleteAttivitaResponse.success();
    }

    // ─── DTO Mapping ──────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    AttivitaDto toDto(Map m) {
        // Struttura: usa quella dell'attività, altrimenti eredita dal progetto
        Integer strutturaId = intVal(m, "strutturaId");
        String strutturaNome = nestedStrVal(m, "struttura", "nome");
        if (strutturaId == null) {
            Object prog = m.get("progetto");
            if (prog instanceof Map pm) {
                strutturaId = intVal(pm, "strutturaId");
                strutturaNome = nestedStrVal(pm, "struttura", "nome");
            }
        }

        // Assegnazioni
        List<AttivitaAssegnazioneDto> assegnazioni = List.of();
        Object assObj = m.get("assegnazioni");
        if (assObj instanceof List al) {
            assegnazioni = ((List<Map>) al).stream().map(this::toAssegnazioneDto).toList();
        }

        // Steps
        List<AttivitaStepDto> steps = List.of();
        Object stObj = m.get("steps");
        if (stObj instanceof List sl) {
            steps = ((List<Map>) sl).stream().map(this::toStepDto).toList();
        }

        return AttivitaDto.builder()
                .id(longVal(m, "id"))
                .progettoId(longVal(m, "progettoId"))
                .progettoTitolo(nestedStrVal(m, "progetto", "titolo"))
                .codice(strVal(m, "codice"))
                .titolo(strVal(m, "titolo"))
                .descrizione(strVal(m, "descrizione"))
                .stato(strVal(m, "stato"))
                .priorita(strVal(m, "priorita"))
                .strutturaId(strutturaId)
                .strutturaNome(strutturaNome)
                .pesi(AttivitaPesiDto.builder().peso(intVal(m, "peso")).build())
                .metricaTemporale(MetricaTemporaleDto.builder()
                        .oreStimate(bigDecimalVal(m, "oreStimate"))
                        .oreLavorate(bigDecimalVal(m, "oreLavorate"))
                        .oreMancanti(bigDecimalVal(m, "oreMancanti"))
                        .percentualeCompletamento(intVal(m, "percentualeCompletamento"))
                        .percentualeOreLavorate(intVal(m, "percentualeOreLavorate"))
                        .dataInizio(strVal(m, "dataInizio"))
                        .dataFineStimata(strVal(m, "dataFineStimata"))
                        .dataFineEffettiva(strVal(m, "dataFineEffettiva"))
                        .scostamentoGiorni(intVal(m, "scostamentoGiorni"))
                        .build())
                .assegnazioni(assegnazioni)
                .steps(steps)
                .createdAt(strVal(m, "createdAt"))
                .updatedAt(strVal(m, "updatedAt"))
                .note(strVal(m, "note"))
                .ordine(intVal(m, "ordine"))
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private AttivitaAssegnazioneDto toAssegnazioneDto(Map m) {
        return AttivitaAssegnazioneDto.builder()
                .id(longVal(m, "id"))
                .attivitaId(longVal(m, "attivitaId"))
                .utenteId(intVal(m, "utenteId"))
                .utenteNome(nestedStrVal(m, "utente", "nome"))
                .utenteCognome(nestedStrVal(m, "utente", "cognome"))
                .ruolo(strVal(m, "ruolo"))
                .oreStimate(bigDecimalVal(m, "oreStimate"))
                .oreLavorate(bigDecimalVal(m, "oreLavorate"))
                .dataAssegnazione(strVal(m, "dataAssegnazione"))
                .dataInizio(strVal(m, "dataInizio"))
                .dataFine(strVal(m, "dataFine"))
                .note(strVal(m, "note"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private AttivitaStepDto toStepDto(Map m) {
        return AttivitaStepDto.builder()
                .id(longVal(m, "id"))
                .attivitaId(longVal(m, "attivitaId"))
                .titolo(strVal(m, "titolo"))
                .descrizione(strVal(m, "descrizione"))
                .completato(m.get("completato") instanceof Boolean b ? b : false)
                .peso(intVal(m, "peso"))
                .ordine(intVal(m, "ordine"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private TimesheetEntryDto toTimesheetDto(Map m) {
        return TimesheetEntryDto.builder()
                .id(longVal(m, "id"))
                .attivitaId(longVal(m, "attivitaId"))
                .utenteId(intVal(m, "utenteId"))
                .data(strVal(m, "data"))
                .oreLavorate(bigDecimalVal(m, "oreLavorate"))
                .descrizione(strVal(m, "descrizione"))
                .createdAt(strVal(m, "createdAt"))
                .build();
    }

    // ─── Permission helpers ────────────────────────────────────────────────────
    private boolean canCreate(String r) { return r != null && CRUD_ROLES.contains(r); }
    private boolean canEdit(String r) { return r != null && EDIT_ROLES.contains(r); }

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
