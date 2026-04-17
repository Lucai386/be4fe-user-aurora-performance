package com.bff_user_aurora_performance.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bff_user_aurora_performance.dto.dup.CreateProgettoRequest;
import com.bff_user_aurora_performance.dto.dup.DupProgettoDto;
import com.bff_user_aurora_performance.dto.dup.UpdateProgettoRequest;
import com.bff_user_aurora_performance.enums.ErrorCode;
import com.bff_user_aurora_performance.model.Dup;
import com.bff_user_aurora_performance.model.DupProgetto;
import com.bff_user_aurora_performance.repository.DupProgettoRepository;
import com.bff_user_aurora_performance.repository.DupRepository;
import com.bff_user_aurora_performance.repository.LpmRepository;
import com.bff_user_aurora_performance.repository.StrutturaRepository;
import com.bff_user_aurora_performance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service per la gestione dei Progetti all'interno di un DUP.
 * I progetti possono essere collegati opzionalmente a una LPM.
 * Prima si crea la LPM, poi la si può agganciare al progetto.
 * 
 * Permessi:
 * - AD (Admin) e SC (Segretario Comunale): CRUD completo
 * - RA (Responsabile Area): solo visualizzazione e modifica
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProgettoService {

    private final DupProgettoRepository progettoRepository;
    private final DupRepository dupRepository;
    private final LpmRepository lpmRepository;
    private final UserRepository userRepository;
    private final StrutturaRepository strutturaRepository;
    private final CodiceService codiceService;

    /** Ruoli che possono creare/eliminare progetti */
    private static final List<String> CRUD_ROLES = List.of("AD", "SC", "DR", "CS", "CP");
    /** Ruoli che possono visualizzare/modificare progetti */
    private static final List<String> VIEW_EDIT_ROLES = List.of("AD", "SC", "RA", "DR", "CS", "CP");

    // ==================== Response DTOs ====================

    public record ProgettoResponse(boolean success, String errorCode, String errorMessage, DupProgettoDto data) {
        public static ProgettoResponse success(DupProgettoDto data) {
            return new ProgettoResponse(true, null, null, data);
        }
        public static ProgettoResponse error(ErrorCode code) {
            return new ProgettoResponse(false, code.name(), code.getDefaultMessage(), null);
        }
    }

    public record ListProgettiResponse(boolean success, String errorCode, String errorMessage, List<DupProgettoDto> data) {
        public static ListProgettiResponse success(List<DupProgettoDto> data) {
            return new ListProgettiResponse(true, null, null, data);
        }
        public static ListProgettiResponse error(ErrorCode code) {
            return new ListProgettiResponse(false, code.name(), code.getDefaultMessage(), null);
        }
    }

    public record DeleteProgettoResponse(boolean success, String errorCode, String errorMessage, Long deletedId) {
        public static DeleteProgettoResponse success(Long id) {
            return new DeleteProgettoResponse(true, null, null, id);
        }
        public static DeleteProgettoResponse error(ErrorCode code) {
            return new DeleteProgettoResponse(false, code.name(), code.getDefaultMessage(), null);
        }
    }

    // ==================== PROGETTI CRUD ====================

    public ListProgettiResponse listByDup(Long dupId, String userRole) {
        if (!canView(userRole)) {
            return ListProgettiResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        if (!dupRepository.existsById(dupId)) {
            return ListProgettiResponse.error(ErrorCode.DUP_NOT_FOUND);
        }

        List<DupProgetto> progetti = progettoRepository.findByDupIdWithDetails(dupId);
        return ListProgettiResponse.success(progetti.stream().map(this::toDto).toList());
    }

    public ProgettoResponse getProgetto(Long progettoId, String userRole) {
        if (!canView(userRole)) {
            return ProgettoResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        DupProgetto progetto = progettoRepository.findByIdWithDetails(progettoId);
        if (progetto == null) {
            return ProgettoResponse.error(ErrorCode.PROGETTO_NOT_FOUND);
        }

        return ProgettoResponse.success(toDto(progetto));
    }

    @Transactional
    public ProgettoResponse createProgetto(CreateProgettoRequest request, String userRole) {
        if (!canCreate(userRole)) {
            return ProgettoResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        if (request.getDupId() == null || !dupRepository.existsById(request.getDupId())) {
            return ProgettoResponse.error(ErrorCode.DUP_NOT_FOUND);
        }

        if (request.getTitolo() == null || request.getTitolo().isBlank()) {
            return ProgettoResponse.error(ErrorCode.PROGETTO_TITOLO_REQUIRED);
        }

        // Valida lpmId se presente
        if (request.getLpmId() != null && !lpmRepository.existsById(request.getLpmId())) {
            return ProgettoResponse.error(ErrorCode.LPM_NOT_FOUND);
        }

        // Valida responsabileId se presente
        if (request.getResponsabileId() != null && !userRepository.existsById(request.getResponsabileId())) {
            return ProgettoResponse.error(ErrorCode.USER_NOT_FOUND);
        }

        // Valida strutturaId se presente
        if (request.getStrutturaId() != null && !strutturaRepository.existsById(request.getStrutturaId())) {
            return ProgettoResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }

        // Ottieni il codiceIstat dal DUP per generare codice autoincrementale per ente
        Dup dup = dupRepository.findById(request.getDupId()).orElse(null);
        String codice = codiceService.generateNextProgettoCodice(dup.getCodiceIstat());

        DupProgetto progetto = DupProgetto.builder()
                .dupId(request.getDupId())
                .codice(codice)
                .titolo(request.getTitolo())
                .descrizione(request.getDescrizione())
                .lpmId(request.getLpmId())
                .responsabileId(request.getResponsabileId())
                .strutturaId(request.getStrutturaId())
                .priorita(parsePriorita(request.getPriorita()))
                .dataInizio(request.getDataInizio())
                .dataFine(request.getDataFine())
                .budget(request.getBudget())
                .note(request.getNote())
                .ordine(request.getOrdine() != null ? request.getOrdine() : 0)
                .build();

        DupProgetto saved = progettoRepository.save(progetto);
        log.info("Progetto creato: id={}, dupId={}, titolo={}", saved.getId(), saved.getDupId(), saved.getTitolo());

        // Ricarica con dettagli
        DupProgetto reloaded = progettoRepository.findByIdWithDetails(saved.getId());
        return ProgettoResponse.success(toDto(reloaded));
    }

    @Transactional
    public ProgettoResponse updateProgetto(Long progettoId, UpdateProgettoRequest request, String userRole) {
        if (!canEdit(userRole)) {
            return ProgettoResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        return progettoRepository.findById(progettoId)
                .map(progetto -> {
                    if (request.getCodice() != null) progetto.setCodice(request.getCodice());
                    if (request.getTitolo() != null) progetto.setTitolo(request.getTitolo());
                    if (request.getDescrizione() != null) progetto.setDescrizione(request.getDescrizione());
                    
                    // LPM: prima si crea LPM, poi si può agganciare
                    if (Boolean.TRUE.equals(request.getRemoveLpm())) {
                        progetto.setLpmId(null);
                    } else if (request.getLpmId() != null) {
                        if (!lpmRepository.existsById(request.getLpmId())) {
                            return ProgettoResponse.error(ErrorCode.LPM_NOT_FOUND);
                        }
                        progetto.setLpmId(request.getLpmId());
                    }

                    // Responsabile
                    if (Boolean.TRUE.equals(request.getRemoveResponsabile())) {
                        progetto.setResponsabileId(null);
                    } else if (request.getResponsabileId() != null) {
                        if (!userRepository.existsById(request.getResponsabileId())) {
                            return ProgettoResponse.error(ErrorCode.USER_NOT_FOUND);
                        }
                        progetto.setResponsabileId(request.getResponsabileId());
                    }

                    // Struttura
                    if (Boolean.TRUE.equals(request.getRemoveStruttura())) {
                        progetto.setStrutturaId(null);
                    } else if (request.getStrutturaId() != null) {
                        if (!strutturaRepository.existsById(request.getStrutturaId())) {
                            return ProgettoResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
                        }
                        progetto.setStrutturaId(request.getStrutturaId());
                    }

                    if (request.getStato() != null) progetto.setStato(DupProgetto.Stato.valueOf(request.getStato()));
                    if (request.getProgresso() != null) progetto.setProgresso(request.getProgresso());
                    if (request.getPriorita() != null) progetto.setPriorita(parsePriorita(request.getPriorita()));
                    if (request.getDataInizio() != null) progetto.setDataInizio(request.getDataInizio());
                    if (request.getDataFine() != null) progetto.setDataFine(request.getDataFine());
                    if (request.getBudget() != null) progetto.setBudget(request.getBudget());
                    if (request.getNote() != null) progetto.setNote(request.getNote());
                    if (request.getOrdine() != null) progetto.setOrdine(request.getOrdine());

                    DupProgetto saved = progettoRepository.save(progetto);
                    log.info("Progetto aggiornato: id={}", saved.getId());

                    DupProgetto reloaded = progettoRepository.findByIdWithDetails(saved.getId());
                    return ProgettoResponse.success(toDto(reloaded));
                })
                .orElse(ProgettoResponse.error(ErrorCode.PROGETTO_NOT_FOUND));
    }

    @Transactional
    public DeleteProgettoResponse deleteProgetto(Long progettoId, String userRole) {
        if (!canDelete(userRole)) {
            return DeleteProgettoResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        return progettoRepository.findById(progettoId)
                .map(progetto -> {
                    progettoRepository.delete(progetto);
                    log.info("Progetto eliminato: id={}", progettoId);
                    return DeleteProgettoResponse.success(progettoId);
                })
                .orElse(DeleteProgettoResponse.error(ErrorCode.PROGETTO_NOT_FOUND));
    }

    // ==================== COLLEGAMENTO LPM ====================

    /**
     * Collega una LPM esistente a un progetto.
     * Prima si crea la LPM, poi la si può agganciare al progetto.
     */
    @Transactional
    public ProgettoResponse linkLpm(Long progettoId, Long lpmId, String userRole) {
        if (!canEdit(userRole)) {
            return ProgettoResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        if (!lpmRepository.existsById(lpmId)) {
            return ProgettoResponse.error(ErrorCode.LPM_NOT_FOUND);
        }

        return progettoRepository.findById(progettoId)
                .map(progetto -> {
                    progetto.setLpmId(lpmId);
                    progettoRepository.save(progetto);
                    log.info("LPM {} collegata al progetto {}", lpmId, progettoId);

                    DupProgetto reloaded = progettoRepository.findByIdWithDetails(progettoId);
                    return ProgettoResponse.success(toDto(reloaded));
                })
                .orElse(ProgettoResponse.error(ErrorCode.PROGETTO_NOT_FOUND));
    }

    /**
     * Scollega una LPM da un progetto.
     */
    @Transactional
    public ProgettoResponse unlinkLpm(Long progettoId, String userRole) {
        if (!canEdit(userRole)) {
            return ProgettoResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        return progettoRepository.findById(progettoId)
                .map(progetto -> {
                    progetto.setLpmId(null);
                    progettoRepository.save(progetto);
                    log.info("LPM scollegata dal progetto {}", progettoId);

                    DupProgetto reloaded = progettoRepository.findByIdWithDetails(progettoId);
                    return ProgettoResponse.success(toDto(reloaded));
                })
                .orElse(ProgettoResponse.error(ErrorCode.PROGETTO_NOT_FOUND));
    }

    // ==================== MAPPING ====================

    private DupProgettoDto toDto(DupProgetto p) {
        return DupProgettoDto.builder()
                .id(p.getId())
                .dupId(p.getDupId())
                .codice(p.getCodice())
                .titolo(p.getTitolo())
                .descrizione(p.getDescrizione())
                .lpmId(p.getLpmId())
                .lpmTitolo(p.getLpm() != null ? p.getLpm().getTitolo() : null)
                .responsabileId(p.getResponsabileId())
                .responsabileNome(p.getResponsabile() != null ? p.getResponsabile().getNome() + " " + p.getResponsabile().getCognome() : null)
                .strutturaId(p.getStrutturaId())
                .strutturaNome(p.getStruttura() != null ? p.getStruttura().getNome() : null)
                .stato(p.getStato() != null ? p.getStato().name() : null)
                .progresso(p.getProgresso())
                .priorita(p.getPriorita() != null ? p.getPriorita().name() : null)
                .dataInizio(p.getDataInizio())
                .dataFine(p.getDataFine())
                .budget(p.getBudget())
                .note(p.getNote())
                .ordine(p.getOrdine())
                .build();
    }

    /** Tutti i ruoli autenticati possono visualizzare */
    private boolean canView(String userRole) {
        return userRole != null && !userRole.isBlank();
    }

    /** AD, SC, RA, DR possono modificare */
    private boolean canEdit(String userRole) {
        return userRole != null && VIEW_EDIT_ROLES.contains(userRole);
    }

    /** Solo AD, SC possono creare */
    private boolean canCreate(String userRole) {
        return userRole != null && CRUD_ROLES.contains(userRole);
    }

    /** Solo AD, SC possono eliminare */
    private boolean canDelete(String userRole) {
        return userRole != null && CRUD_ROLES.contains(userRole);
    }

    /** Converte stringa priorità in enum */
    private DupProgetto.Priorita parsePriorita(String priorita) {
        if (priorita == null) return DupProgetto.Priorita.MEDIA;
        try {
            return DupProgetto.Priorita.valueOf(priorita);
        } catch (IllegalArgumentException e) {
            return DupProgetto.Priorita.MEDIA;
        }
    }
}
