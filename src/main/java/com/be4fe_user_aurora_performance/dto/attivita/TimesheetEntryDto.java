package com.be4fe_user_aurora_performance.dto.attivita;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per una entry di timesheet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetEntryDto {
    private Long id;
    private Long attivitaId;
    private Integer utenteId;
    private String data;
    private BigDecimal oreLavorate;
    private String descrizione;
    private String createdAt;
}
