package com.bff_user_aurora_performance.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bff_user_aurora_performance.dto.lpm.LpmActivityDto;
import com.bff_user_aurora_performance.dto.lpm.LpmCreateRequest;
import com.bff_user_aurora_performance.dto.lpm.LpmDeleteRequest;
import com.bff_user_aurora_performance.dto.lpm.LpmListRequest;
import com.bff_user_aurora_performance.dto.lpm.LpmNoteActivityDto;
import com.bff_user_aurora_performance.dto.lpm.LpmUpdateRequest;
import com.bff_user_aurora_performance.model.Lpm;
import com.bff_user_aurora_performance.model.LpmNote;
import com.bff_user_aurora_performance.repository.DupRepository;
import com.bff_user_aurora_performance.repository.LpmNoteRepository;
import com.bff_user_aurora_performance.repository.LpmRepository;
import com.bff_user_aurora_performance.repository.UserRepository;

@Service
public class LpmService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final LpmRepository lpmRepository;
    private final LpmNoteRepository lpmNoteRepository;
    private final UserRepository userRepository;
    private final DupRepository dupRepository;

    public LpmService(LpmRepository lpmRepository, LpmNoteRepository lpmNoteRepository, UserRepository userRepository, DupRepository dupRepository) {
        this.lpmRepository = lpmRepository;
        this.lpmNoteRepository = lpmNoteRepository;
        this.userRepository = userRepository;
        this.dupRepository = dupRepository;
    }

    /**
     * Lista tutte le LPM attive
     */
    public List<LpmActivityDto> list(LpmListRequest request) {
        List<Lpm> items;
        
        if (request != null && request.getCodiceIstat() != null && request.getAnnoInizioMandato() != null && request.getAnnoFineMandato() != null) {
            items = lpmRepository.findByMandato(request.getCodiceIstat(), request.getAnnoInizioMandato(), request.getAnnoFineMandato());
        } else if (request != null && request.getCodiceIstat() != null) {
            items = lpmRepository.findByCodiceIstatActive(request.getCodiceIstat());
        } else {
            items = lpmRepository.findAllActive();
        }
        
        return items.stream()
                .map(this::toActivityDto)
                .collect(Collectors.toList());
    }

    /**
     * Ottiene una singola LPM per ID
     */
    public Optional<LpmActivityDto> get(Long id) {
        return lpmRepository.findByIdActive(id)
                .map(this::toActivityDtoWithNotes);
    }

    /**
     * Crea una nuova LPM
     */
    @Transactional
    public LpmActivityDto create(LpmCreateRequest request, Integer userId) {
        Lpm lpm = new Lpm();
        
        // Campi dal frontend
        lpm.setTitolo(request.getTitle());
        lpm.setStato(request.getStatus() != null ? request.getStatus() : "todo");
        
        // Campi opzionali backend
        lpm.setCodiceIstat(request.getCodiceIstat() != null ? request.getCodiceIstat() : "000000");
        lpm.setAnnoInizioMandato(request.getAnnoInizioMandato() != null ? request.getAnnoInizioMandato() : LocalDateTime.now().getYear());
        lpm.setAnnoFineMandato(request.getAnnoFineMandato() != null ? request.getAnnoFineMandato() : LocalDateTime.now().getYear() + 5);
        lpm.setPriorita(request.getPriority() != null ? request.getPriority() : 0);
        lpm.setCreatedBy(userId);

        Lpm saved = lpmRepository.save(lpm);
        
        // Se c'è una nota nel request, la salvo come prima nota
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            LpmNote note = new LpmNote();
            note.setLpmId(saved.getId());
            note.setTesto(request.getNotes());
            note.setAutoreId(userId);
            lpmNoteRepository.save(note);
        }

        return toActivityDtoWithNotes(saved);
    }

    /**
     * Aggiorna una LPM esistente
     */
    @Transactional
    public Optional<LpmActivityDto> update(LpmUpdateRequest request, Integer userId) {
        Long id = request.getIdAsLong();
        if (id == null) return Optional.empty();
        
        return lpmRepository.findByIdActive(id)
                .map(lpm -> {
                    if (request.getTitle() != null) {
                        lpm.setTitolo(request.getTitle());
                    }
                    if (request.getDescription() != null) {
                        lpm.setDescrizione(request.getDescription());
                    }
                    if (request.getStatus() != null) {
                        lpm.setStato(request.getStatus());
                    }
                    if (request.getPriority() != null) {
                        lpm.setPriorita(request.getPriority());
                    }
                    if (request.getProgress() != null) {
                        lpm.setProgresso(request.getProgress());
                    }
                    lpm.setUpdatedBy(userId);
                    lpm.setUpdatedAt(LocalDateTime.now());
                    
                    Lpm saved = lpmRepository.save(lpm);
                    
                    // Se c'è una nota nel request, la aggiungo
                    if (request.getNotes() != null && !request.getNotes().isBlank()) {
                        LpmNote note = new LpmNote();
                        note.setLpmId(saved.getId());
                        note.setTesto(request.getNotes());
                        note.setAutoreId(userId);
                        lpmNoteRepository.save(note);
                    }
                    
                    return toActivityDtoWithNotes(saved);
                });
    }

    /**
     * Elimina una LPM (soft delete)
     */
    @Transactional
    public boolean delete(LpmDeleteRequest request, Integer userId) {
        Long id = request.getIdAsLong();
        if (id == null) return false;
        
        return lpmRepository.findByIdActive(id)
                .map(lpm -> {
                    lpm.setDeletedAt(LocalDateTime.now());
                    lpm.setDeletedBy(userId);
                    lpm.setDeletedReason(request.getReason());
                    lpmRepository.save(lpm);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Collega una LPM a un DUP
     */
    @Transactional
    public Optional<LpmActivityDto> linkDup(Long lpmId, Long dupId, Integer userId) {
        return lpmRepository.findByIdActive(lpmId)
                .flatMap(lpm -> dupRepository.findById(dupId)
                        .map(dup -> {
                            lpm.setDupId(dupId);
                            lpm.setDupTitolo(dup.getTitolo());
                            lpm.setUpdatedBy(userId);
                            lpm.setUpdatedAt(LocalDateTime.now());
                            Lpm saved = lpmRepository.save(lpm);
                            return toActivityDtoWithNotes(saved);
                        }));
    }

    /**
     * Scollega una LPM da un DUP
     */
    @Transactional
    public Optional<LpmActivityDto> unlinkDup(Long lpmId, Integer userId) {
        return lpmRepository.findByIdActive(lpmId)
                .map(lpm -> {
                    lpm.setDupId(null);
                    lpm.setDupTitolo(null);
                    lpm.setUpdatedBy(userId);
                    lpm.setUpdatedAt(LocalDateTime.now());
                    Lpm saved = lpmRepository.save(lpm);
                    return toActivityDtoWithNotes(saved);
                });
    }

    /**
     * Converte Lpm entity in LpmActivityDto (senza note)
     */
    private LpmActivityDto toActivityDto(Lpm lpm) {
        LpmActivityDto dto = new LpmActivityDto();
        dto.setId(String.valueOf(lpm.getId()));
        dto.setTitle(lpm.getTitolo());
        dto.setDescription(lpm.getDescrizione());
        dto.setStatus(lpm.getStato());
        dto.setPriority(lpm.getPriorita());
        dto.setProgress(lpm.getProgresso());
        
        if (lpm.getDupId() != null) {
            dto.setDupId(String.valueOf(lpm.getDupId()));
        }
        dto.setDupTitle(lpm.getDupTitolo());

        if (lpm.getResponsabileId() != null) {
            userRepository.findById(lpm.getResponsabileId())
                    .ifPresent(user -> dto.setResponsibleName(user.getNomeCompleto()));
        }

        return dto;
    }

    /**
     * Converte Lpm entity in LpmActivityDto (con note)
     */
    private LpmActivityDto toActivityDtoWithNotes(Lpm lpm) {
        LpmActivityDto dto = toActivityDto(lpm);
        
        List<LpmNoteActivityDto> notes = lpmNoteRepository.findByLpmIdOrderByCreatedAtDesc(lpm.getId())
                .stream()
                .map(this::toNoteActivityDto)
                .collect(Collectors.toList());
        dto.setNotes(notes);
        
        return dto;
    }

    /**
     * Converte LpmNote entity in LpmNoteActivityDto
     */
    private LpmNoteActivityDto toNoteActivityDto(LpmNote note) {
        LpmNoteActivityDto dto = new LpmNoteActivityDto();
        dto.setId(String.valueOf(note.getId()));
        dto.setText(note.getTesto());
        
        if (note.getCreatedAt() != null) {
            dto.setCreatedAt(note.getCreatedAt().format(ISO_FORMATTER));
        }

        if (note.getAutoreId() != null) {
            userRepository.findById(note.getAutoreId())
                    .ifPresent(user -> dto.setAuthor(user.getNomeCompleto()));
        }

        return dto;
    }
}
