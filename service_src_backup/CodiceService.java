package com.bff_user_aurora_performance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bff_user_aurora_performance.model.CodiceSequence;
import com.bff_user_aurora_performance.model.CodiceSequence.EntityType;
import com.bff_user_aurora_performance.repository.CodiceSequenceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service per la generazione di codici autoincrementali per ente.
 * Ogni ente (codice_istat) ha sequenze separate per DUP, Progetti e Attività.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodiceService {

    private final CodiceSequenceRepository sequenceRepository;

    /**
     * Genera il prossimo codice per DUP.
     * Formato: DUP001, DUP002, ...
     */
    @Transactional
    public String generateNextDupCodice(String codiceIstat) {
        return generateNextCodice(codiceIstat, EntityType.DUP, "DUP");
    }

    /**
     * Genera il prossimo codice per Progetto.
     * Formato: PRJ001, PRJ002, ...
     */
    @Transactional
    public String generateNextProgettoCodice(String codiceIstat) {
        return generateNextCodice(codiceIstat, EntityType.PRJ, "PRJ");
    }

    /**
     * Genera il prossimo codice per Attività.
     * Formato: ATT001, ATT002, ...
     */
    @Transactional
    public String generateNextAttivitaCodice(String codiceIstat) {
        return generateNextCodice(codiceIstat, EntityType.ATT, "ATT");
    }

    /**
     * Genera il prossimo codice per Obiettivo.
     * Formato: OBJ001, OBJ002, ...
     */
    @Transactional
    public String generateNextObiettivoCodice(String codiceIstat) {
        return generateNextCodice(codiceIstat, EntityType.OBJ, "OBJ");
    }

    /**
     * Genera il prossimo codice con lock pessimistico per garantire atomicità.
     */
    private String generateNextCodice(String codiceIstat, EntityType entityType, String prefix) {
        // Trova o crea la sequenza con lock
        CodiceSequence sequence = sequenceRepository
                .findByCodiceIstatAndEntityTypeForUpdate(codiceIstat, entityType)
                .orElseGet(() -> {
                    CodiceSequence newSeq = CodiceSequence.builder()
                            .codiceIstat(codiceIstat)
                            .entityType(entityType)
                            .lastNumber(0L)
                            .build();
                    return sequenceRepository.save(newSeq);
                });

        // Incrementa e salva
        long nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        sequenceRepository.save(sequence);

        String codice = String.format("%s%03d", prefix, nextNumber);
        log.debug("Generato codice {} per ente {} tipo {}", codice, codiceIstat, entityType);

        return codice;
    }
}
