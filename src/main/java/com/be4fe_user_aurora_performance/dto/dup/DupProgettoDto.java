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
public class DupProgettoDto {

    private Long id;
    private Long dupId;
    private String codice;
    private String titolo;
    private String descrizione;
    private Long lpmId;
    private String lpmTitolo;
    private Integer responsabileId;
    private String responsabileNome;
    private Integer strutturaId;
    private String strutturaNome;
    private String stato;
    private Integer progresso;
    private String priorita;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private BigDecimal budget;
    private String note;
    private Integer ordine;
}
