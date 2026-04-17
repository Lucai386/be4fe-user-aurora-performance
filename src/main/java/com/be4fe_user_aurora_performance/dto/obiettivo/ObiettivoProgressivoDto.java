package com.be4fe_user_aurora_performance.dto.obiettivo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per un progressivo (entry storico).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObiettivoProgressivoDto {
    private Long id;
    private Long obiettivoId;
    private BigDecimal valoreRegistrato;
    private BigDecimal valorePrecedente;
    private BigDecimal delta;
    private String note;
    private Long registratoDaId;
    private String registratoDaNome;
    private String registratoDaCognome;
    private String dataRegistrazione;
}
