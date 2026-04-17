package com.be4fe_user_aurora_performance.dto.attivita;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per il peso di un'attività.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttivitaPesiDto {
    private Integer peso;
}
