package com.be4fe_user_aurora_performance.dto.dup;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProgettoRequest {

    private Long dupId;
    private String codice;
    private String titolo;
    private String descrizione;
    private Long lpmId; // Collegamento opzionale a LPM (prima si crea LPM, poi si aggancia)
    private Integer responsabileId;
    private Integer strutturaId;
    private String priorita;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private BigDecimal budget;
    private String note;
    private Integer ordine;
}
