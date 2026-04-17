package com.bff_user_aurora_performance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bff_user_aurora_performance.dto.obiettivo.CreateObiettivoRequest;
import com.bff_user_aurora_performance.dto.obiettivo.ListObiettiviResponse;
import com.bff_user_aurora_performance.dto.obiettivo.ObiettivoDto;
import com.bff_user_aurora_performance.dto.obiettivo.ObiettivoProgressivoDto;
import com.bff_user_aurora_performance.dto.obiettivo.ObiettivoResponse;
import com.bff_user_aurora_performance.dto.obiettivo.RegistraProgressivoRequest;
import com.bff_user_aurora_performance.dto.obiettivo.UpdateObiettivoRequest;
import com.bff_user_aurora_performance.enums.ErrorCode;
import com.bff_user_aurora_performance.model.Obiettivo;
import com.bff_user_aurora_performance.model.Obiettivo.StatoObiettivo;
import com.bff_user_aurora_performance.model.Obiettivo.TipoObiettivo;
import com.bff_user_aurora_performance.model.ObiettivoProgressivo;
import com.bff_user_aurora_performance.model.Struttura;
import com.bff_user_aurora_performance.model.User;
import com.bff_user_aurora_performance.repository.ObiettivoProgressivoRepository;
import com.bff_user_aurora_performance.repository.ObiettivoRepository;
import com.bff_user_aurora_performance.repository.StrutturaRepository;
import com.bff_user_aurora_performance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service per la gestione degli Obiettivi operativi.
 * 
 * Permessi:
 * - AD, SC: CRUD completo su tutti gli obiettivi dell'ente
 * - DR, RA, CS: possono creare obiettivi e assegnarli a utenti sotto la loro struttura
 * - Tutti i ruoli assegnati: possono registrare progressivi sui propri obiettivi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ObiettivoService {

    private final ObiettivoRepository obiettivoRepository;
    private final ObiettivoProgressivoRepository progressivoRepository;
    private final UserRepository userRepository;
    private final StrutturaRepository strutturaRepository;
    private final CodiceService codiceService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Ruoli che possono creare obiettivi (tutti tranne DB) */
    private static final List<String> CREATE_ROLES = List.of("AD", "SC", "DR", "RA", "CS", "CP");
    /** Ruoli che possono visualizzare tutti gli obiettivi */
    private static final List<String> VIEW_ALL_ROLES = List.of("AD", "SC", "DR", "RA", "CS", "CP");

    // ==================== LIST ====================

    public ListObiettiviResponse listObiettivi(String codiceIstat, Long userId, String userRole) {
        if (!canView(userRole)) {
            return ListObiettiviResponse.error(ErrorCode.OBIETTIVO_NOT_AUTHORIZED);
        }

        if (codiceIstat == null || codiceIstat.isBlank()) {
            return ListObiettiviResponse.error(ErrorCode.NO_ENTE);
        }

        List<Obiettivo> obiettivi;
        
        // Admin e superiori vedono tutti gli obiettivi
        if (VIEW_ALL_ROLES.contains(userRole)) {
            obiettivi = obiettivoRepository.findByCodiceIstatWithDetails(codiceIstat);
        } else {
            // Dipendenti vedono solo i propri obiettivi
            obiettivi = obiettivoRepository.findByUtenteAssegnatoId(userId);
        }

        List<ObiettivoDto> dtos = obiettivi.stream()
                .map(this::toDtoWithProgressivi)
                .toList();

        return ListObiettiviResponse.ok(dtos);
    }

    public ListObiettiviResponse listMieiObiettivi(Long userId) {
        if (userId == null) {
            return ListObiettiviResponse.error(ErrorCode.USER_NOT_FOUND);
        }

        List<Obiettivo> obiettivi = obiettivoRepository.findByUtenteAssegnatoId(userId);
        List<ObiettivoDto> dtos = obiettivi.stream()
                .map(this::toDtoWithProgressivi)
                .toList();

        return ListObiettiviResponse.ok(dtos);
    }

    // ==================== GET ====================

    public ObiettivoResponse getObiettivo(Long obiettivoId, Long userId, String userRole) {
        if (obiettivoId == null) {
            return ObiettivoResponse.error(ErrorCode.INVALID_ID);
        }

        return obiettivoRepository.findById(obiettivoId)
                .map(obiettivo -> {
                    // Verifica permesso: admin vede tutto, altri vedono solo i propri
                    if (!VIEW_ALL_ROLES.contains(userRole) && !obiettivo.getUtenteAssegnatoId().equals(userId)) {
                        return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_AUTHORIZED);
                    }
                    return ObiettivoResponse.ok(toDtoWithProgressivi(obiettivo));
                })
                .orElse(ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_FOUND));
    }

    // ==================== CREATE ====================

    @Transactional
    public ObiettivoResponse createObiettivo(CreateObiettivoRequest request, String codiceIstat, Long userId, String userRole) {
        if (!canCreate(userRole)) {
            return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_AUTHORIZED);
        }

        if (codiceIstat == null || codiceIstat.isBlank()) {
            return ObiettivoResponse.error(ErrorCode.NO_ENTE);
        }

        // Validazione
        if (request.getTitolo() == null || request.getTitolo().isBlank()) {
            return ObiettivoResponse.error(ErrorCode.OBIETTIVO_TITOLO_REQUIRED);
        }
        if (request.getValoreTarget() == null) {
            return ObiettivoResponse.error(ErrorCode.OBIETTIVO_TARGET_REQUIRED);
        }
        if (request.getAnno() == null) {
            return ObiettivoResponse.error(ErrorCode.OBIETTIVO_ANNO_REQUIRED);
        }

        // Genera codice
        String codice = codiceService.generateNextObiettivoCodice(codiceIstat);

        Obiettivo obiettivo = Obiettivo.builder()
                .codiceIstat(codiceIstat)
                .codice(codice)
                .titolo(request.getTitolo())
                .descrizione(request.getDescrizione())
                .unitaMisura(request.getUnitaMisura())
                .tipo(parseType(request.getTipo()))
                .valoreIniziale(request.getValoreIniziale() != null ? request.getValoreIniziale() : BigDecimal.ZERO)
                .valoreTarget(request.getValoreTarget())
                .valoreCorrente(request.getValoreIniziale() != null ? request.getValoreIniziale() : BigDecimal.ZERO)
                .peso(request.getPeso() != null ? request.getPeso() : new BigDecimal("100"))
                .dataInizio(parseDate(request.getDataInizio()))
                .dataFine(parseDate(request.getDataFine()))
                .anno(request.getAnno())
                .strutturaId(request.getStrutturaId())
                .utenteAssegnatoId(request.getUtenteAssegnatoId())
                .creatoDaId(userId)
                .build();

        Obiettivo saved = obiettivoRepository.save(obiettivo);
        log.info("Creato obiettivo {} da utente {}", saved.getCodice(), userId);

        return ObiettivoResponse.ok(toDtoWithProgressivi(saved));
    }

    // ==================== UPDATE ====================

    @Transactional
    public ObiettivoResponse updateObiettivo(UpdateObiettivoRequest request, Long userId, String userRole) {
        if (request.getId() == null) {
            return ObiettivoResponse.error(ErrorCode.INVALID_ID);
        }

        return obiettivoRepository.findById(request.getId())
                .map(obiettivo -> {
                    // Solo chi ha creato o admin può modificare
                    if (!canModify(userRole, userId, obiettivo)) {
                        return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_AUTHORIZED);
                    }

                    if (request.getTitolo() != null && !request.getTitolo().isBlank()) {
                        obiettivo.setTitolo(request.getTitolo());
                    }
                    if (request.getDescrizione() != null) {
                        obiettivo.setDescrizione(request.getDescrizione());
                    }
                    if (request.getUnitaMisura() != null) {
                        obiettivo.setUnitaMisura(request.getUnitaMisura());
                    }
                    if (request.getTipo() != null) {
                        obiettivo.setTipo(parseType(request.getTipo()));
                    }
                    if (request.getStato() != null) {
                        obiettivo.setStato(parseStato(request.getStato()));
                    }
                    if (request.getValoreIniziale() != null) {
                        obiettivo.setValoreIniziale(request.getValoreIniziale());
                    }
                    if (request.getValoreTarget() != null) {
                        obiettivo.setValoreTarget(request.getValoreTarget());
                    }
                    if (request.getPeso() != null) {
                        obiettivo.setPeso(request.getPeso());
                    }
                    if (request.getDataInizio() != null) {
                        obiettivo.setDataInizio(parseDate(request.getDataInizio()));
                    }
                    if (request.getDataFine() != null) {
                        obiettivo.setDataFine(parseDate(request.getDataFine()));
                    }
                    if (request.getUtenteAssegnatoId() != null) {
                        obiettivo.setUtenteAssegnatoId(request.getUtenteAssegnatoId());
                    }

                    Obiettivo saved = obiettivoRepository.save(obiettivo);
                    log.info("Aggiornato obiettivo {} da utente {}", saved.getCodice(), userId);

                    return ObiettivoResponse.ok(toDtoWithProgressivi(saved));
                })
                .orElse(ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_FOUND));
    }

    // ==================== DELETE ====================

    @Transactional
    public ObiettivoResponse deleteObiettivo(Long obiettivoId, Long userId, String userRole) {
        if (obiettivoId == null) {
            return ObiettivoResponse.error(ErrorCode.INVALID_ID);
        }

        return obiettivoRepository.findById(obiettivoId)
                .map(obiettivo -> {
                    // Solo chi ha creato o admin può eliminare
                    if (!canModify(userRole, userId, obiettivo)) {
                        return ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_AUTHORIZED);
                    }

                    obiettivoRepository.delete(obiettivo);
                    log.info("Eliminato obiettivo {} da utente {}", obiettivo.getCodice(), userId);

                    return ObiettivoResponse.ok();
                })
                .orElse(ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_FOUND));
    }

    // ==================== PROGRESSIVI ====================

    @Transactional
    public ObiettivoResponse registraProgressivo(RegistraProgressivoRequest request, Long userId, String userRole) {
        if (request.getObiettivoId() == null) {
            return ObiettivoResponse.error(ErrorCode.INVALID_ID);
        }
        if (request.getNuovoValore() == null) {
            return ObiettivoResponse.error(ErrorCode.PROGRESSIVO_VALORE_REQUIRED);
        }

        return obiettivoRepository.findById(request.getObiettivoId())
                .map(obiettivo -> {
                    // L'utente assegnato o chi ha creato o admin può registrare progressivi
                    if (!canRegistraProgressivo(userRole, userId, obiettivo)) {
                        return ObiettivoResponse.error(ErrorCode.PROGRESSIVO_NOT_AUTHORIZED);
                    }

                    BigDecimal valorePrecedente = obiettivo.getValoreCorrente();

                    // Crea entry progressivo
                    ObiettivoProgressivo progressivo = ObiettivoProgressivo.builder()
                            .obiettivoId(obiettivo.getId())
                            .valoreRegistrato(request.getNuovoValore())
                            .valorePrecedente(valorePrecedente)
                            .note(request.getNote())
                            .registratoDaId(userId)
                            .dataRegistrazione(LocalDateTime.now())
                            .build();

                    progressivoRepository.save(progressivo);

                    // Aggiorna valore corrente obiettivo
                    obiettivo.setValoreCorrente(request.getNuovoValore());
                    
                    // Verifica se obiettivo completato
                    if (obiettivo.calcolaPercentuale().compareTo(new BigDecimal("100")) >= 0) {
                        obiettivo.setStato(StatoObiettivo.COMPLETATO);
                    }

                    Obiettivo saved = obiettivoRepository.save(obiettivo);
                    log.info("Registrato progressivo per obiettivo {} da utente {}: {} -> {}", 
                            saved.getCodice(), userId, valorePrecedente, request.getNuovoValore());

                    return ObiettivoResponse.ok(toDtoWithProgressivi(saved));
                })
                .orElse(ObiettivoResponse.error(ErrorCode.OBIETTIVO_NOT_FOUND));
    }

    // ==================== CONTEGGI DASHBOARD ====================

    public Long countTotale(String codiceIstat) {
        return obiettivoRepository.countByCodiceIstat(codiceIstat);
    }

    public Long countAttivi(String codiceIstat) {
        return obiettivoRepository.countAttiviByCodiceIstat(codiceIstat);
    }

    public Long countCompletati(String codiceIstat) {
        return obiettivoRepository.countCompletatiByCodiceIstat(codiceIstat);
    }

    // ==================== PERMESSI ====================

    /** Tutti i ruoli autenticati possono visualizzare i propri obiettivi */
    private boolean canView(String userRole) {
        return userRole != null && !userRole.isBlank();
    }

    /** Solo ruoli dirigenziali possono creare obiettivi */
    private boolean canCreate(String userRole) {
        return userRole != null && CREATE_ROLES.contains(userRole);
    }

    /** Admin o creatore possono modificare/eliminare */
    private boolean canModify(String userRole, Long userId, Obiettivo obiettivo) {
        if (userRole == null) return false;
        if (List.of("AD", "SC").contains(userRole)) return true;
        return obiettivo.getCreatoDaId() != null && obiettivo.getCreatoDaId().equals(userId);
    }

    /** Utente assegnato, creatore o admin possono registrare progressivi */
    private boolean canRegistraProgressivo(String userRole, Long userId, Obiettivo obiettivo) {
        if (userRole == null) return false;
        if (List.of("AD", "SC").contains(userRole)) return true;
        if (obiettivo.getCreatoDaId() != null && obiettivo.getCreatoDaId().equals(userId)) return true;
        return obiettivo.getUtenteAssegnatoId() != null && obiettivo.getUtenteAssegnatoId().equals(userId);
    }

    // ==================== MAPPING ====================

    private ObiettivoDto toDtoWithProgressivi(Obiettivo o) {
        List<ObiettivoProgressivoDto> progressiviDto = new ArrayList<>();
        
        List<ObiettivoProgressivo> progressivi = progressivoRepository.findByObiettivoIdWithDetails(o.getId());
        for (ObiettivoProgressivo p : progressivi) {
            User registratoDa = p.getRegistratoDa();
            BigDecimal delta = p.getValoreRegistrato().subtract(
                    p.getValorePrecedente() != null ? p.getValorePrecedente() : BigDecimal.ZERO);
            
            progressiviDto.add(ObiettivoProgressivoDto.builder()
                    .id(p.getId())
                    .obiettivoId(p.getObiettivoId())
                    .valoreRegistrato(p.getValoreRegistrato())
                    .valorePrecedente(p.getValorePrecedente())
                    .delta(delta)
                    .note(p.getNote())
                    .registratoDaId(p.getRegistratoDaId())
                    .registratoDaNome(registratoDa != null ? registratoDa.getNome() : null)
                    .registratoDaCognome(registratoDa != null ? registratoDa.getCognome() : null)
                    .dataRegistrazione(p.getDataRegistrazione() != null ? p.getDataRegistrazione().format(DATETIME_FMT) : null)
                    .build());
        }

        User utenteAssegnato = o.getUtenteAssegnato();
        User creatoDa = o.getCreatoDa();
        Struttura struttura = o.getStruttura();

        return ObiettivoDto.builder()
                .id(o.getId())
                .codice(o.getCodice())
                .titolo(o.getTitolo())
                .descrizione(o.getDescrizione())
                .unitaMisura(o.getUnitaMisura())
                .tipo(o.getTipo() != null ? o.getTipo().name() : null)
                .stato(o.getStato() != null ? o.getStato().name() : null)
                .valoreIniziale(o.getValoreIniziale())
                .valoreTarget(o.getValoreTarget())
                .valoreCorrente(o.getValoreCorrente())
                .percentualeCompletamento(o.calcolaPercentuale())
                .peso(o.getPeso())
                .dataInizio(o.getDataInizio() != null ? o.getDataInizio().format(DATE_FMT) : null)
                .dataFine(o.getDataFine() != null ? o.getDataFine().format(DATE_FMT) : null)
                .anno(o.getAnno())
                .strutturaId(o.getStrutturaId())
                .strutturaNome(struttura != null ? struttura.getNome() : null)
                .utenteAssegnatoId(o.getUtenteAssegnatoId())
                .utenteAssegnatoNome(utenteAssegnato != null ? utenteAssegnato.getNome() : null)
                .utenteAssegnatoCognome(utenteAssegnato != null ? utenteAssegnato.getCognome() : null)
                .creatoDaId(o.getCreatoDaId())
                .creatoDaNome(creatoDa != null ? creatoDa.getNome() : null)
                .creatoDaCognome(creatoDa != null ? creatoDa.getCognome() : null)
                .progressivi(progressiviDto)
                .createdAt(o.getCreatedAt() != null ? o.getCreatedAt().format(DATETIME_FMT) : null)
                .updatedAt(o.getUpdatedAt() != null ? o.getUpdatedAt().format(DATETIME_FMT) : null)
                .build();
    }

    private TipoObiettivo parseType(String tipo) {
        if (tipo == null) return TipoObiettivo.CRESCENTE;
        try {
            return TipoObiettivo.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TipoObiettivo.CRESCENTE;
        }
    }

    private StatoObiettivo parseStato(String stato) {
        if (stato == null) return StatoObiettivo.ATTIVO;
        try {
            return StatoObiettivo.valueOf(stato.toUpperCase());
        } catch (IllegalArgumentException e) {
            return StatoObiettivo.ATTIVO;
        }
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            return LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
