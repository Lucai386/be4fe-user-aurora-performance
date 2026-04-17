package com.be4fe_user_aurora_performance.dto.attivita;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Request per registrare ore lavorate nel timesheet.
 */
@Data
public class LogOreLavorateRequest {
    private Long attivitaId;
    private Integer utenteId;
    private String data;
    private BigDecimal oreLavorate;
    private String descrizione;
}
