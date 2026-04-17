package com.bff_user_aurora_performance.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bff_user_aurora_performance.dto.dup.CreateDupRequest;
import com.bff_user_aurora_performance.dto.dup.DeleteDupResponse;
import com.bff_user_aurora_performance.dto.dup.DupDto;
import com.bff_user_aurora_performance.dto.dup.DupProgettoDto;
import com.bff_user_aurora_performance.dto.dup.DupResponse;
import com.bff_user_aurora_performance.dto.dup.ListDupResponse;
import com.bff_user_aurora_performance.dto.dup.UpdateDupRequest;
import com.bff_user_aurora_performance.enums.ErrorCode;
import com.bff_user_aurora_performance.model.Dup;
import com.bff_user_aurora_performance.model.DupProgetto;
import com.bff_user_aurora_performance.repository.AttivitaAssegnazioneRepository;
import com.bff_user_aurora_performance.repository.AttivitaRepository;
import com.bff_user_aurora_performance.repository.DupProgettoRepository;
import com.bff_user_aurora_performance.repository.DupRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service per la gestione dei DUP (Documento Unico di Programmazione).
 * Un DUP è un insieme di progetti.
 * 
 * Permessi:
 * - AD (Admin) e SC (Segretario Comunale): CRUD completo
 * - RA (Responsabile Area): solo visualizzazione e modifica
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DupService {

    private final DupRepository dupRepository;
    private final DupProgettoRepository progettoRepository;
    private final AttivitaRepository attivitaRepository;
    private final AttivitaAssegnazioneRepository assegnazioneRepository;
    private final CodiceService codiceService;

    /** Ruoli che possono creare/eliminare DUP */
    private static final List<String> CRUD_ROLES = List.of("AD", "SC", "DR", "CS", "CP");
    /** Ruoli che possono visualizzare/modificare DUP */
    private static final List<String> VIEW_EDIT_ROLES = List.of("AD", "SC", "RA", "DR", "CS", "CP");

    // ==================== DUP CRUD ====================

    public ListDupResponse listDup(String codiceIstat, String userRole, Integer userId) {
        // Per dipendenti base, permettiamo la visualizzazione ma filtriamo i risultati
        if (!canView(userRole) && !"DB".equalsIgnoreCase(userRole)) {
            return ListDupResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        if (codiceIstat == null || codiceIstat.isBlank()) {
            return ListDupResponse.error(ErrorCode.NO_ENTE);
        }

        List<Dup> dups = dupRepository.findByCodiceIstatOrderByAnnoDesc(codiceIstat);
        
        // Se il ruolo è DB, filtra solo i DUP che contengono progetti collegati alle attività dell'utente
        if ("DB".equalsIgnoreCase(userRole) && userId != null) {
            // Ottieni gli ID dei progetti collegati alle attività assegnate all'utente
            Set<Long> progettiIds = assegnazioneRepository.findByUtenteId(userId).stream()
                .map(a -> attivitaRepository.findById(a.getAttivitaId()).orElse(null))
                .filter(att -> att != null)
                .map(att -> att.getProgettoId())
                .collect(Collectors.toSet());
            
            if (progettiIds.isEmpty()) {
                return ListDupResponse.success(List.of());
            }
            
            // Filtra DUP e progetti
            List<DupDto> filteredDups = dups.stream()
                .map(dup -> toDtoWithFilteredProgetti(dup, progettiIds))
                .filter(dto -> !dto.getProgetti().isEmpty())
                .toList();
            
            return ListDupResponse.success(filteredDups);
        }
        
        List<DupDto> dupDtos = dups.stream().map(this::toDtoWithProgetti).toList();

        return ListDupResponse.success(dupDtos);
    }

    public DupResponse getDup(Long dupId, String userRole) {
        if (!canView(userRole)) {
            return DupResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        return dupRepository.findById(dupId)
                .map(dup -> DupResponse.success(toDtoWithProgetti(dup)))
                .orElse(DupResponse.error(ErrorCode.DUP_NOT_FOUND));
    }

    @Transactional
    public DupResponse createDup(CreateDupRequest request, String codiceIstat, Integer userId, String userRole) {
        if (!canCreate(userRole)) {
            return DupResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        if (codiceIstat == null || codiceIstat.isBlank()) {
            return DupResponse.error(ErrorCode.NO_ENTE);
        }

        if (request.getAnno() == null) {
            return DupResponse.error(ErrorCode.DUP_ANNO_REQUIRED);
        }

        if (request.getTitolo() == null || request.getTitolo().isBlank()) {
            return DupResponse.error(ErrorCode.DUP_TITOLO_REQUIRED);
        }

        // Genera codice autoincrementale per ente (DUP001, DUP002, ...)
        String codice = codiceService.generateNextDupCodice(codiceIstat);

        Dup dup = Dup.builder()
                .codiceIstat(codiceIstat)
                .codice(codice)
                .anno(request.getAnno())
                .titolo(request.getTitolo())
                .descrizione(request.getDescrizione())
                .sezione(request.getSezione() != null ? Dup.Sezione.valueOf(request.getSezione()) : Dup.Sezione.STRATEGICA)
                .stato(Dup.Stato.BOZZA)
                .createdBy(userId)
                .build();

        Dup saved = dupRepository.save(dup);
        log.info("DUP creato: id={}, anno={}, titolo={}", saved.getId(), saved.getAnno(), saved.getTitolo());

        return DupResponse.success(toDto(saved));
    }

    @Transactional
    public DupResponse updateDup(Long dupId, UpdateDupRequest request, Integer userId, String userRole) {
        if (!canEdit(userRole)) {
            return DupResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        return dupRepository.findById(dupId)
                .map(dup -> {
                    if (request.getTitolo() != null) dup.setTitolo(request.getTitolo());
                    if (request.getDescrizione() != null) dup.setDescrizione(request.getDescrizione());
                    if (request.getSezione() != null) dup.setSezione(Dup.Sezione.valueOf(request.getSezione()));
                    if (request.getStato() != null) dup.setStato(Dup.Stato.valueOf(request.getStato()));
                    if (request.getDataApprovazione() != null) dup.setDataApprovazione(request.getDataApprovazione());
                    dup.setUpdatedBy(userId);

                    Dup saved = dupRepository.save(dup);
                    log.info("DUP aggiornato: id={}", saved.getId());

                    return DupResponse.success(toDtoWithProgetti(saved));
                })
                .orElse(DupResponse.error(ErrorCode.DUP_NOT_FOUND));
    }

    @Transactional
    public DeleteDupResponse deleteDup(Long dupId, String userRole) {
        if (!canDelete(userRole)) {
            return DeleteDupResponse.error(ErrorCode.DUP_NOT_AUTHORIZED);
        }

        return dupRepository.findById(dupId)
                .map(dup -> {
                    dupRepository.delete(dup);
                    log.info("DUP eliminato: id={}", dupId);
                    return DeleteDupResponse.success();
                })
                .orElse(DeleteDupResponse.error(ErrorCode.DUP_NOT_FOUND));
    }

    // ==================== MAPPING ====================

    private DupDto toDto(Dup dup) {
        return DupDto.builder()
                .id(dup.getId())
                .codiceIstat(dup.getCodiceIstat())
                .codice(dup.getCodice())
                .anno(dup.getAnno())
                .titolo(dup.getTitolo())
                .descrizione(dup.getDescrizione())
                .sezione(dup.getSezione().name())
                .stato(dup.getStato().name())
                .dataApprovazione(dup.getDataApprovazione())
                .createdAt(dup.getCreatedAt() != null ? dup.getCreatedAt().toString() : null)
                .updatedAt(dup.getUpdatedAt() != null ? dup.getUpdatedAt().toString() : null)
                .progetti(new ArrayList<>())
                .build();
    }

    private DupDto toDtoWithProgetti(Dup dup) {
        DupDto dto = toDto(dup);
        List<DupProgetto> progetti = progettoRepository.findByDupIdWithDetails(dup.getId());
        dto.setProgetti(progetti.stream().map(this::toProgettoDto).toList());
        return dto;
    }

    /**
     * Converte un DUP in DTO filtrando solo i progetti specificati.
     * Usato per utenti DB che possono vedere solo i progetti collegati alle loro attività.
     */
    private DupDto toDtoWithFilteredProgetti(Dup dup, Set<Long> progettiIds) {
        DupDto dto = toDto(dup);
        List<DupProgetto> progetti = progettoRepository.findByDupIdWithDetails(dup.getId());
        dto.setProgetti(progetti.stream()
            .filter(p -> progettiIds.contains(p.getId()))
            .map(this::toProgettoDto)
            .toList());
        return dto;
    }

    private DupProgettoDto toProgettoDto(DupProgetto p) {
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
}
