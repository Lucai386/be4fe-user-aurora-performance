package com.be4fe_user_aurora_performance.dto.dup;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per il DUP (Documento Unico di Programmazione).
 * Un DUP è un insieme di progetti.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DupDto {

    private Long id;
    private String codiceIstat;
    private String codice;
    private Integer anno;
    private String titolo;
    private String descrizione;
    private String sezione;
    private String stato;
    private LocalDate dataApprovazione;
    private String createdAt;
    private String updatedAt;
    /** Lista dei progetti nel DUP */
    private List<DupProgettoDto> progetti;
}
