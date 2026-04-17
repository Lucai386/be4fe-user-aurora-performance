package com.bff_user_aurora_performance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bff_user_aurora_performance.dto.attivita.AssegnaUtenteRequest;
import com.bff_user_aurora_performance.dto.attivita.AttivitaAssegnazioneDto;
import com.bff_user_aurora_performance.dto.attivita.AttivitaDto;
import com.bff_user_aurora_performance.dto.attivita.AttivitaPesiDto;
import com.bff_user_aurora_performance.dto.attivita.AttivitaStepDto;
import com.bff_user_aurora_performance.dto.attivita.CreateAttivitaRequest;
import com.bff_user_aurora_performance.dto.attivita.LogOreLavorateRequest;
import com.bff_user_aurora_performance.dto.attivita.MetricaTemporaleDto;
import com.bff_user_aurora_performance.dto.attivita.TimesheetEntryDto;
import com.bff_user_aurora_performance.dto.attivita.UpdateAttivitaRequest;
import com.bff_user_aurora_performance.enums.ErrorCode;
import com.bff_user_aurora_performance.model.Attivita;
import com.bff_user_aurora_performance.model.AttivitaAssegnazione;
import com.bff_user_aurora_performance.model.AttivitaStep;
import com.bff_user_aurora_performance.model.DupProgetto;
import com.bff_user_aurora_performance.model.TimesheetEntry;
import com.bff_user_aurora_performance.model.User;
import com.bff_user_aurora_performance.repository.AttivitaAssegnazioneRepository;
import com.bff_user_aurora_performance.repository.AttivitaRepository;
import com.bff_user_aurora_performance.repository.AttivitaStepRepository;
import com.bff_user_aurora_performance.repository.DupProgettoRepository;
import com.bff_user_aurora_performance.repository.DupRepository;
import com.bff_user_aurora_performance.repository.TimesheetEntryRepository;
import com.bff_user_aurora_performance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service per la gestione delle Attività (sotto-attività di un Progetto).
 * Include gestione pesi di merito/completamento, assegnazioni utenti e timesheet.
 * 
 * Permessi:
 * - AD (Admin), SC (Segretario Comunale), DR (Dirigente): CRUD completo
 * - CP (Capo Progetto): visualizzazione e modifica
 * - Tutti gli altri: solo visualizzazione
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttivitaService {

    private final AttivitaRepository attivitaRepository;
    private final AttivitaAssegnazioneRepository assegnazioneRepository;
    private final AttivitaStepRepository stepRepository;
    private final TimesheetEntryRepository timesheetRepository;
    private final DupProgettoRepository progettoRepository;
    private final DupRepository dupRepository;
    private final UserRepository userRepository;
    private final CodiceService codiceService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** Ruoli che possono creare/eliminare attività */
    private static final List<String> CRUD_ROLES = List.of("AD", "SC", "DR", "CS", "CP");
    /** Ruoli che possono modificare attività */
    private static final List<String> EDIT_ROLES = List.of("AD", "SC", "DR", "CS", "CP");

    // ==================== Response DTOs ====================

    public record AttivitaResponse(String result, String errorCode, String message, AttivitaDto attivita) {
        public static AttivitaResponse success(AttivitaDto attivita) {
            return new AttivitaResponse("OK", null, null, attivita);
        }
        public static AttivitaResponse error(ErrorCode code) {
            return new AttivitaResponse("KO", code.name(), code.getDefaultMessage(), null);
        }
    }

    public record ListAttivitaResponse(String result, String errorCode, String message, List<AttivitaDto> attivitaList) {
        public static ListAttivitaResponse success(List<AttivitaDto> list) {
            return new ListAttivitaResponse("OK", null, null, list);
        }
        public static ListAttivitaResponse error(ErrorCode code) {
            return new ListAttivitaResponse("KO", code.name(), code.getDefaultMessage(), null);
        }
    }

    public record DeleteAttivitaResponse(String result, String errorCode, String message) {
        public static DeleteAttivitaResponse success() {
            return new DeleteAttivitaResponse("OK", null, null);
        }
        public static DeleteAttivitaResponse error(ErrorCode code) {
            return new DeleteAttivitaResponse("KO", code.name(), code.getDefaultMessage());
        }
    }

    public record TimesheetEntryResponse(String result, String errorCode, String message, TimesheetEntryDto entry) {
        public static TimesheetEntryResponse success(TimesheetEntryDto entry) {
            return new TimesheetEntryResponse("OK", null, null, entry);
        }
        public static TimesheetEntryResponse error(ErrorCode code) {
            return new TimesheetEntryResponse("KO", code.name(), code.getDefaultMessage(), null);
        }
    }

    public record ListTimesheetResponse(String result, String errorCode, String message, List<TimesheetEntryDto> entries) {
        public static ListTimesheetResponse success(List<TimesheetEntryDto> entries) {
            return new ListTimesheetResponse("OK", null, null, entries);
        }
        public static ListTimesheetResponse error(ErrorCode code) {
            return new ListTimesheetResponse("KO", code.name(), code.getDefaultMessage(), null);
        }
    }

    // ==================== ATTIVITÀ CRUD ====================

    public ListAttivitaResponse listAll(String userRole, Integer userId) {
        // Se il ruolo è DB (Dipendente Base), filtra solo le attività assegnate all'utente
        if ("DB".equalsIgnoreCase(userRole) && userId != null) {
            // Ottieni tutti gli ID delle attività assegnate a questo utente
            List<Long> attivitaIds = assegnazioneRepository.findByUtenteId(userId).stream()
                .map(AttivitaAssegnazione::getAttivitaId)
                .distinct()
                .toList();
            
            if (attivitaIds.isEmpty()) {
                return ListAttivitaResponse.success(List.of());
            }
            
            List<Attivita> attivita = attivitaRepository.findAllWithDetails().stream()
                .filter(a -> attivitaIds.contains(a.getId()))
                .toList();
            return ListAttivitaResponse.success(attivita.stream().map(this::toDto).toList());
        }
        
        List<Attivita> attivita = attivitaRepository.findAllWithDetails();
        return ListAttivitaResponse.success(attivita.stream().map(this::toDto).toList());
    }

    public ListAttivitaResponse listByProgetto(Long progettoId, String userRole) {
        if (!progettoRepository.existsById(progettoId)) {
            return ListAttivitaResponse.error(ErrorCode.PROGETTO_NOT_FOUND);
        }

        List<Attivita> attivita = attivitaRepository.findByProgettoIdWithDetails(progettoId);
        return ListAttivitaResponse.success(attivita.stream().map(this::toDto).toList());
    }

    public AttivitaResponse getAttivita(Long id, String userRole) {
        Attivita attivita = attivitaRepository.findByIdWithDetails(id);
        if (attivita == null) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        }
        return AttivitaResponse.success(toDto(attivita));
    }

    @Transactional
    public AttivitaResponse createAttivita(CreateAttivitaRequest request, String userRole) {
        if (!canCreate(userRole)) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        }

        if (request.getTitolo() == null || request.getTitolo().isBlank()) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_TITOLO_REQUIRED);
        }
        if (!progettoRepository.existsById(request.getProgettoId())) {
            return AttivitaResponse.error(ErrorCode.PROGETTO_NOT_FOUND);
        }

        // Ottieni il codiceIstat risalendo dal progetto al DUP
        var progetto = progettoRepository.findById(request.getProgettoId()).orElse(null);
        var dup = dupRepository.findById(progetto.getDupId()).orElse(null);
        String codice = codiceService.generateNextAttivitaCodice(dup.getCodiceIstat());

        Attivita attivita = Attivita.builder()
            .progettoId(request.getProgettoId())
            .codice(codice)
            .titolo(request.getTitolo())
            .descrizione(request.getDescrizione())
            .priorita(parsePriorita(request.getPriorita()))
            .peso(request.getPeso() != null ? request.getPeso() : 100)
            .oreStimate(request.getOreStimate() != null ? request.getOreStimate() : BigDecimal.ZERO)
            .dataInizio(parseDate(request.getDataInizio()))
            .dataFineStimata(parseDate(request.getDataFineStimata()))
            .note(request.getNote())
            .ordine(request.getOrdine() != null ? request.getOrdine() : 0)
            .strutturaId(request.getStrutturaId())
            .build();

        attivita = attivitaRepository.save(attivita);
        log.info("Creata attività {} per progetto {}", attivita.getId(), request.getProgettoId());

        return AttivitaResponse.success(toDto(attivita));
    }

    @Transactional
    public AttivitaResponse updateAttivita(Long id, UpdateAttivitaRequest request, String userRole) {
        if (!canEdit(userRole)) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        }

        Attivita attivita = attivitaRepository.findById(id).orElse(null);
        if (attivita == null) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        }

        if (request.getCodice() != null) attivita.setCodice(request.getCodice());
        if (request.getTitolo() != null) attivita.setTitolo(request.getTitolo());
        if (request.getDescrizione() != null) attivita.setDescrizione(request.getDescrizione());
        if (request.getStato() != null) attivita.setStato(parseStato(request.getStato()));
        if (request.getPriorita() != null) attivita.setPriorita(parsePriorita(request.getPriorita()));
        if (request.getPeso() != null) attivita.setPeso(request.getPeso());
        if (request.getOreStimate() != null) attivita.setOreStimate(request.getOreStimate());
        if (request.getDataInizio() != null) attivita.setDataInizio(parseDate(request.getDataInizio()));
        if (request.getDataFineStimata() != null) attivita.setDataFineStimata(parseDate(request.getDataFineStimata()));
        if (request.getDataFineEffettiva() != null) attivita.setDataFineEffettiva(parseDate(request.getDataFineEffettiva()));
        if (request.getNote() != null) attivita.setNote(request.getNote());
        if (request.getOrdine() != null) attivita.setOrdine(request.getOrdine());
        
        // Gestione strutturaId
        if (Boolean.TRUE.equals(request.getRemoveStruttura())) {
            attivita.setStrutturaId(null);
        } else if (request.getStrutturaId() != null) {
            attivita.setStrutturaId(request.getStrutturaId());
        }

        attivita = attivitaRepository.save(attivita);
        log.info("Aggiornata attività {}", id);

        return AttivitaResponse.success(toDto(attivitaRepository.findByIdWithDetails(id)));
    }

    @Transactional
    public DeleteAttivitaResponse deleteAttivita(Long id, String userRole) {
        if (!canCreate(userRole)) {
            return DeleteAttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        }

        if (!attivitaRepository.existsById(id)) {
            return DeleteAttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        }

        attivitaRepository.deleteById(id);
        log.info("Eliminata attività {}", id);

        return DeleteAttivitaResponse.success();
    }

    /**
     * Duplica un'attività esistente, opzionalmente assegnandola a una nuova struttura.
     */
    @Transactional
    public AttivitaResponse duplicaAttivita(Long attivitaId, Integer nuovaStrutturaId, String userRole) {
        if (!canCreate(userRole)) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        }

        Attivita originale = attivitaRepository.findByIdWithDetails(attivitaId);
        if (originale == null) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        }

        // Ottieni codiceIstat dal progetto
        var progetto = progettoRepository.findById(originale.getProgettoId()).orElse(null);
        if (progetto == null) {
            return AttivitaResponse.error(ErrorCode.PROGETTO_NOT_FOUND);
        }
        var dup = dupRepository.findById(progetto.getDupId()).orElse(null);
        String codiceIstat = dup != null ? dup.getCodiceIstat() : null;

        // Genera nuovo codice
        String nuovoCodice = codiceService.generateNextAttivitaCodice(codiceIstat);

        // Determina la struttura: usa quella nuova se specificata, altrimenti copia dall'originale
        Integer strutturaId = nuovaStrutturaId != null ? nuovaStrutturaId : originale.getStrutturaId();

        // Crea copia dell'attività
        Attivita copia = Attivita.builder()
                .progettoId(originale.getProgettoId())
                .codice(nuovoCodice)
                .titolo(originale.getTitolo() + " (copia)")
                .descrizione(originale.getDescrizione())
                .stato(Attivita.Stato.TODO)
                .priorita(originale.getPriorita())
                .peso(originale.getPeso())
                .oreStimate(originale.getOreStimate())
                .dataInizio(originale.getDataInizio())
                .dataFineStimata(originale.getDataFineStimata())
                .strutturaId(strutturaId)
                .build();

        copia = attivitaRepository.save(copia);
        log.info("Duplicata attività {} -> {} con struttura {}", attivitaId, copia.getId(), strutturaId);

        return AttivitaResponse.success(toDto(attivitaRepository.findByIdWithDetails(copia.getId())));
    }

    // ==================== ASSEGNAZIONI ====================

    @Transactional
    public AttivitaResponse assegnaUtente(AssegnaUtenteRequest request, String userRole) {
        if (!canEdit(userRole)) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        }

        Attivita attivita = attivitaRepository.findById(request.getAttivitaId()).orElse(null);
        if (attivita == null) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        }

        User utente = userRepository.findById(request.getUtenteId()).orElse(null);
        if (utente == null) {
            return AttivitaResponse.error(ErrorCode.USER_NOT_FOUND);
        }

        if (assegnazioneRepository.existsByAttivitaIdAndUtenteId(request.getAttivitaId(), request.getUtenteId())) {
            return AttivitaResponse.error(ErrorCode.ASSEGNAZIONE_ALREADY_EXISTS);
        }

        AttivitaAssegnazione assegnazione = AttivitaAssegnazione.builder()
            .attivitaId(request.getAttivitaId())
            .utenteId(request.getUtenteId())
            .ruolo(request.getRuolo())
            .oreStimate(request.getOreStimate() != null ? request.getOreStimate() : BigDecimal.ZERO)
            .dataInizio(parseDate(request.getDataInizio()))
            .dataFine(parseDate(request.getDataFine()))
            .note(request.getNote())
            .build();

        assegnazioneRepository.save(assegnazione);
        log.info("Assegnato utente {} all'attività {}", request.getUtenteId(), request.getAttivitaId());

        return AttivitaResponse.success(toDto(attivitaRepository.findByIdWithDetails(request.getAttivitaId())));
    }

    @Transactional
    public AttivitaResponse rimuoviAssegnazione(Long attivitaId, Integer utenteId, String userRole) {
        if (!canEdit(userRole)) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        }

        AttivitaAssegnazione assegnazione = assegnazioneRepository
            .findByAttivitaIdAndUtenteId(attivitaId, utenteId)
            .orElse(null);

        if (assegnazione == null) {
            return AttivitaResponse.error(ErrorCode.ASSEGNAZIONE_NOT_FOUND);
        }

        assegnazioneRepository.delete(assegnazione);
        log.info("Rimossa assegnazione utente {} dall'attività {}", utenteId, attivitaId);

        return AttivitaResponse.success(toDto(attivitaRepository.findByIdWithDetails(attivitaId)));
    }

    // ==================== PERCENTUALE COMPLETAMENTO ====================

    /**
     * Aggiorna la percentuale di completamento di un'attività.
     * La percentuale è indipendente dalle ore lavorate.
     * Utenti assegnati possono aggiornare la percentuale.
     */
    @Transactional
    public AttivitaResponse updatePercentualeCompletamento(Long attivitaId, Integer percentuale, String userRole, Integer userId) {
        // Verifica se l'utente può aggiornare: o ha ruolo adeguato, o è assegnato all'attività
        boolean isAssigned = userId != null && assegnazioneRepository.existsByAttivitaIdAndUtenteId(attivitaId, userId);
        if (!canEdit(userRole) && !isAssigned) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        }

        Attivita attivita = attivitaRepository.findById(attivitaId).orElse(null);
        if (attivita == null) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        }

        if (percentuale == null || percentuale < 0 || percentuale > 100) {
            return AttivitaResponse.error(ErrorCode.INVALID_PERCENTAGE);
        }

        // Se l'attività ha degli step, la percentuale è calcolata dai pesi degli step completati
        // e non può essere sovrascritta manualmente
        List<AttivitaStep> steps = stepRepository.findByAttivitaIdOrderByOrdineAsc(attivitaId);
        if (!steps.isEmpty()) {
            log.warn("Tentativo di aggiornamento manuale percentuale attività {} ignorato: la percentuale è gestita dagli step", attivitaId);
            updatePercentualeFromSteps(attivitaId);
            return AttivitaResponse.success(toDto(attivitaRepository.findByIdWithDetails(attivitaId)));
        }

        attivita.setPercentualeCompletamento(percentuale);
        attivitaRepository.save(attivita);

        log.info("Aggiornata percentuale completamento attività {} a {}%", attivitaId, percentuale);

        return AttivitaResponse.success(toDto(attivitaRepository.findByIdWithDetails(attivitaId)));
    }

    // ==================== TIMESHEET ====================

    @Transactional
    public TimesheetEntryResponse logOreLavorate(LogOreLavorateRequest request, String userRole) {
        Attivita attivita = attivitaRepository.findById(request.getAttivitaId()).orElse(null);
        if (attivita == null) {
            return TimesheetEntryResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        }

        User utente = userRepository.findById(request.getUtenteId()).orElse(null);
        if (utente == null) {
            return TimesheetEntryResponse.error(ErrorCode.USER_NOT_FOUND);
        }

        // Verifica che l'utente sia assegnato all'attività
        if (!assegnazioneRepository.existsByAttivitaIdAndUtenteId(request.getAttivitaId(), request.getUtenteId())) {
            return TimesheetEntryResponse.error(ErrorCode.USER_NOT_ASSIGNED);
        }

        TimesheetEntry entry = TimesheetEntry.builder()
            .attivitaId(request.getAttivitaId())
            .utenteId(request.getUtenteId())
            .data(parseDate(request.getData()))
            .oreLavorate(request.getOreLavorate())
            .descrizione(request.getDescrizione())
            .build();

        entry = timesheetRepository.save(entry);

        // Aggiorna le ore lavorate sull'attività
        updateOreLavorateAttivita(request.getAttivitaId());

        // Aggiorna le ore lavorate sull'assegnazione
        updateOreLavorateAssegnazione(request.getAttivitaId(), request.getUtenteId());

        log.info("Registrate {} ore per attività {} da utente {}", request.getOreLavorate(), request.getAttivitaId(), request.getUtenteId());

        return TimesheetEntryResponse.success(toTimesheetDto(entry));
    }

    public ListTimesheetResponse getTimesheetByAttivita(Long attivitaId, String userRole) {
        if (!attivitaRepository.existsById(attivitaId)) {
            return ListTimesheetResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        }

        List<TimesheetEntry> entries = timesheetRepository.findByAttivitaIdOrderByDataDesc(attivitaId);
        return ListTimesheetResponse.success(entries.stream().map(this::toTimesheetDto).toList());
    }

    public ListTimesheetResponse getTimesheetByUtente(Integer utenteId, LocalDate dataInizio, LocalDate dataFine, String userRole) {
        if (!userRepository.existsById(utenteId)) {
            return ListTimesheetResponse.error(ErrorCode.USER_NOT_FOUND);
        }

        List<TimesheetEntry> entries;
        if (dataInizio != null && dataFine != null) {
            entries = timesheetRepository.findByUtenteIdAndPeriodo(utenteId, dataInizio, dataFine);
        } else {
            entries = timesheetRepository.findByUtenteIdOrderByDataDesc(utenteId);
        }

        return ListTimesheetResponse.success(entries.stream().map(this::toTimesheetDto).toList());
    }

    @Transactional
    public DeleteAttivitaResponse deleteTimesheetEntry(Long id, String userRole) {
        TimesheetEntry entry = timesheetRepository.findById(id).orElse(null);
        if (entry == null) {
            return DeleteAttivitaResponse.error(ErrorCode.TIMESHEET_ENTRY_NOT_FOUND);
        }

        Long attivitaId = entry.getAttivitaId();
        Integer utenteId = entry.getUtenteId();

        timesheetRepository.delete(entry);

        // Ricalcola le ore
        updateOreLavorateAttivita(attivitaId);
        updateOreLavorateAssegnazione(attivitaId, utenteId);

        log.info("Eliminata entry timesheet {}", id);

        return DeleteAttivitaResponse.success();
    }

    // ==================== HELPERS ====================

    private void updateOreLavorateAttivita(Long attivitaId) {
        BigDecimal totale = timesheetRepository.sumOreLavorateByAttivitaId(attivitaId);
        Attivita attivita = attivitaRepository.findById(attivitaId).orElse(null);
        if (attivita != null) {
            attivita.setOreLavorate(totale != null ? totale : BigDecimal.ZERO);
            attivitaRepository.save(attivita);
        }
    }

    private void updateOreLavorateAssegnazione(Long attivitaId, Integer utenteId) {
        BigDecimal totale = timesheetRepository.sumOreLavorateByAttivitaIdAndUtenteId(attivitaId, utenteId);
        assegnazioneRepository.findByAttivitaIdAndUtenteId(attivitaId, utenteId).ifPresent(ass -> {
            ass.setOreLavorate(totale != null ? totale : BigDecimal.ZERO);
            assegnazioneRepository.save(ass);
        });
    }

    private boolean canCreate(String role) {
        return role != null && CRUD_ROLES.contains(role);
    }

    private boolean canEdit(String role) {
        return role != null && EDIT_ROLES.contains(role);
    }

    private Attivita.Stato parseStato(String stato) {
        if (stato == null) return Attivita.Stato.TODO;
        try {
            return Attivita.Stato.valueOf(stato);
        } catch (IllegalArgumentException e) {
            return Attivita.Stato.TODO;
        }
    }

    private Attivita.Priorita parsePriorita(String priorita) {
        if (priorita == null) return Attivita.Priorita.MEDIA;
        try {
            return Attivita.Priorita.valueOf(priorita);
        } catch (IllegalArgumentException e) {
            return Attivita.Priorita.MEDIA;
        }
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            return LocalDate.parse(date, DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== DTO MAPPING ====================

    private AttivitaDto toDto(Attivita a) {
        // Determina la struttura: usa quella dell'attività, altrimenti eredita dal progetto
        Integer strutturaId = a.getStrutturaId();
        String strutturaNome = null;
        if (a.getStruttura() != null) {
            strutturaNome = a.getStruttura().getNome();
        } else if (strutturaId == null && a.getProgetto() != null && a.getProgetto().getStruttura() != null) {
            strutturaId = a.getProgetto().getStrutturaId();
            strutturaNome = a.getProgetto().getStruttura().getNome();
        }

        return AttivitaDto.builder()
            .id(a.getId())
            .progettoId(a.getProgettoId())
            .progettoTitolo(a.getProgetto() != null ? a.getProgetto().getTitolo() : null)
            .codice(a.getCodice())
            .titolo(a.getTitolo())
            .descrizione(a.getDescrizione())
            .stato(a.getStato() != null ? a.getStato().name() : null)
            .priorita(a.getPriorita() != null ? a.getPriorita().name() : null)
            .strutturaId(strutturaId)
            .strutturaNome(strutturaNome)
            .pesi(AttivitaPesiDto.builder()
                .peso(a.getPeso())
                .build())
            .metricaTemporale(MetricaTemporaleDto.builder()
                .oreStimate(a.getOreStimate())
                .oreLavorate(a.getOreLavorate())
                .oreMancanti(a.getOreMancanti())
                .percentualeCompletamento(a.getPercentualeCompletamento())
                .percentualeOreLavorate(a.getPercentualeOreLavorate())
                .dataInizio(a.getDataInizio() != null ? a.getDataInizio().format(DATE_FORMAT) : null)
                .dataFineStimata(a.getDataFineStimata() != null ? a.getDataFineStimata().format(DATE_FORMAT) : null)
                .dataFineEffettiva(a.getDataFineEffettiva() != null ? a.getDataFineEffettiva().format(DATE_FORMAT) : null)
                .scostamentoGiorni(a.getScostamentoGiorni())
                .build())
            .assegnazioni(a.getAssegnazioni() != null ? 
                a.getAssegnazioni().stream().map(this::toAssegnazioneDto).toList() : 
                List.of())
            .steps(a.getSteps() != null ?
                a.getSteps().stream().map(this::toStepDto).toList() :
                List.of())
            .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().format(DATETIME_FORMAT) : null)
            .updatedAt(a.getUpdatedAt() != null ? a.getUpdatedAt().format(DATETIME_FORMAT) : null)
            .note(a.getNote())
            .ordine(a.getOrdine())
            .build();
    }

    private AttivitaAssegnazioneDto toAssegnazioneDto(AttivitaAssegnazione ass) {
        return AttivitaAssegnazioneDto.builder()
            .id(ass.getId())
            .attivitaId(ass.getAttivitaId())
            .utenteId(ass.getUtenteId())
            .utenteNome(ass.getUtente() != null ? ass.getUtente().getNome() : null)
            .utenteCognome(ass.getUtente() != null ? ass.getUtente().getCognome() : null)
            .ruolo(ass.getRuolo())
            .oreStimate(ass.getOreStimate())
            .oreLavorate(ass.getOreLavorate())
            .dataAssegnazione(ass.getDataAssegnazione() != null ? ass.getDataAssegnazione().format(DATE_FORMAT) : null)
            .dataInizio(ass.getDataInizio() != null ? ass.getDataInizio().format(DATE_FORMAT) : null)
            .dataFine(ass.getDataFine() != null ? ass.getDataFine().format(DATE_FORMAT) : null)
            .note(ass.getNote())
            .build();
    }

    private TimesheetEntryDto toTimesheetDto(TimesheetEntry t) {
        return TimesheetEntryDto.builder()
            .id(t.getId())
            .attivitaId(t.getAttivitaId())
            .utenteId(t.getUtenteId())
            .data(t.getData() != null ? t.getData().format(DATE_FORMAT) : null)
            .oreLavorate(t.getOreLavorate())
            .descrizione(t.getDescrizione())
            .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().format(DATETIME_FORMAT) : null)
            .build();
    }

    private AttivitaStepDto toStepDto(AttivitaStep s) {
        return AttivitaStepDto.builder()
            .id(s.getId())
            .attivitaId(s.getAttivitaId())
            .titolo(s.getTitolo())
            .descrizione(s.getDescrizione())
            .completato(s.getCompletato())
            .peso(s.getPeso())
            .ordine(s.getOrdine())
            .build();
    }

    // ==================== STEP MANAGEMENT ====================

    /**
     * Aggiunge un nuovo step a un'attività.
     * @param peso Il peso dello step (0-100) che contribuisce alla percentuale di completamento
     */
    @Transactional
    public AttivitaResponse addStep(Long attivitaId, String titolo, String descrizione, Integer peso, String userRole, Integer userId) {
        // Verifica permessi: ruolo adeguato o assegnato all'attività
        boolean isAssigned = userId != null && assegnazioneRepository.existsByAttivitaIdAndUtenteId(attivitaId, userId);
        if (!canEdit(userRole) && !isAssigned) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        }

        Attivita attivita = attivitaRepository.findById(attivitaId).orElse(null);
        if (attivita == null) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_FOUND);
        }

        int maxOrdine = stepRepository.findByAttivitaIdOrderByOrdineAsc(attivitaId).stream()
            .mapToInt(AttivitaStep::getOrdine)
            .max()
            .orElse(-1);

        AttivitaStep step = AttivitaStep.builder()
            .attivitaId(attivitaId)
            .titolo(titolo)
            .descrizione(descrizione)
            .completato(false)
            .peso(peso != null ? peso : 0)
            .ordine(maxOrdine + 1)
            .build();

        stepRepository.save(step);
        log.info("Aggiunto step '{}' con peso {} all'attività {}", titolo, peso, attivitaId);

        // Ricalcola percentuale: se ci sono step, la percentuale deve derivare da essi
        updatePercentualeFromSteps(attivitaId);

        return AttivitaResponse.success(toDto(attivitaRepository.findByIdWithDetails(attivitaId)));
    }

    /**
     * Aggiorna lo stato di completamento di uno step e ricalcola la percentuale dell'attività.
     * Se viene fornita una descrizione, viene salvata nello step (documenta il lavoro svolto).
     */
    @Transactional
    public AttivitaResponse toggleStep(Long attivitaId, Long stepId, Boolean completato, String descrizione, String userRole, Integer userId) {
        // Verifica permessi: ruolo adeguato o assegnato all'attività
        boolean isAssigned = userId != null && assegnazioneRepository.existsByAttivitaIdAndUtenteId(attivitaId, userId);
        if (!canEdit(userRole) && !isAssigned) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        }

        AttivitaStep step = stepRepository.findById(stepId).orElse(null);
        if (step == null || !step.getAttivitaId().equals(attivitaId)) {
            return AttivitaResponse.error(ErrorCode.STEP_NOT_FOUND);
        }

        step.setCompletato(completato);
        if (descrizione != null) {
            step.setDescrizione(descrizione);
        }
        stepRepository.save(step);

        // Ricalcola percentuale completamento basata sugli step
        updatePercentualeFromSteps(attivitaId);

        log.info("Step {} dell'attività {} impostato a completato={}", stepId, attivitaId, completato);

        return AttivitaResponse.success(toDto(attivitaRepository.findByIdWithDetails(attivitaId)));
    }

    /**
     * Rimuove uno step da un'attività.
     */
    @Transactional
    public AttivitaResponse removeStep(Long attivitaId, Long stepId, String userRole, Integer userId) {
        // Verifica permessi: ruolo adeguato o assegnato all'attività
        boolean isAssigned = userId != null && assegnazioneRepository.existsByAttivitaIdAndUtenteId(attivitaId, userId);
        if (!canEdit(userRole) && !isAssigned) {
            return AttivitaResponse.error(ErrorCode.ATTIVITA_NOT_AUTHORIZED);
        }

        AttivitaStep step = stepRepository.findById(stepId).orElse(null);
        if (step == null || !step.getAttivitaId().equals(attivitaId)) {
            return AttivitaResponse.error(ErrorCode.STEP_NOT_FOUND);
        }

        stepRepository.delete(step);

        // Ricalcola percentuale completamento
        updatePercentualeFromSteps(attivitaId);

        log.info("Rimosso step {} dall'attività {}", stepId, attivitaId);

        return AttivitaResponse.success(toDto(attivitaRepository.findByIdWithDetails(attivitaId)));
    }

    /**
     * Ricalcola la percentuale di completamento basandosi sui pesi degli step completati.
     * La percentuale è la somma dei pesi degli step completati.
     */
    private void updatePercentualeFromSteps(Long attivitaId) {
        List<AttivitaStep> steps = stepRepository.findByAttivitaIdOrderByOrdineAsc(attivitaId);
        if (steps.isEmpty()) {
            return; // Mantieni la percentuale a 0 se non ci sono step
        }

        // Calcola la somma dei pesi degli step completati
        int percentuale = steps.stream()
            .filter(AttivitaStep::getCompletato)
            .mapToInt(step -> step.getPeso() != null ? step.getPeso() : 0)
            .sum();

        // Limita a 100
        percentuale = Math.min(percentuale, 100);

        Attivita attivita = attivitaRepository.findById(attivitaId).orElse(null);
        if (attivita != null) {
            attivita.setPercentualeCompletamento(percentuale);
            
            // Aggiorna automaticamente lo stato se completata al 100%
            if (percentuale == 100 && attivita.getStato() != Attivita.Stato.COMPLETATA) {
                attivita.setStato(Attivita.Stato.COMPLETATA);
                attivita.setDataFineEffettiva(java.time.LocalDate.now());
            } else if (percentuale > 0 && percentuale < 100 && attivita.getStato() == Attivita.Stato.TODO) {
                attivita.setStato(Attivita.Stato.IN_CORSO);
            }
            
            attivitaRepository.save(attivita);
            
            // Aggiorna anche la percentuale del progetto
            updateProgettoProgresso(attivita.getProgettoId());
        }
    }

    /**
     * Ricalcola il progresso del progetto basandosi sulle percentuali delle attività.
     * Il progresso è la media pesata delle percentuali di completamento delle attività.
     */
    private void updateProgettoProgresso(Long progettoId) {
        List<Attivita> attivita = attivitaRepository.findByProgettoIdOrderByOrdine(progettoId);
        if (attivita.isEmpty()) {
            return;
        }

        // Calcola la media pesata delle percentuali
        int sommaPesi = attivita.stream()
            .mapToInt(a -> a.getPeso() != null ? a.getPeso() : 100)
            .sum();

        if (sommaPesi == 0) {
            return;
        }

        int progressoPesato = attivita.stream()
            .mapToInt(a -> {
                int peso = a.getPeso() != null ? a.getPeso() : 100;
                int percentuale = a.getPercentualeCompletamento() != null ? a.getPercentualeCompletamento() : 0;
                return peso * percentuale;
            })
            .sum();

        int progresso = progressoPesato / sommaPesi;

        progettoRepository.findById(progettoId).ifPresent(progetto -> {
            progetto.setProgresso(progresso);
            
            // Aggiorna automaticamente lo stato del progetto
            if (progresso == 100 && progetto.getStato() != DupProgetto.Stato.COMPLETATO) {
                progetto.setStato(DupProgetto.Stato.COMPLETATO);
                progetto.setDataFine(java.time.LocalDate.now());
            } else if (progresso > 0 && progresso < 100 && progetto.getStato() == DupProgetto.Stato.TODO) {
                progetto.setStato(DupProgetto.Stato.IN_CORSO);
            }
            
            progettoRepository.save(progetto);
        });
    }
}
