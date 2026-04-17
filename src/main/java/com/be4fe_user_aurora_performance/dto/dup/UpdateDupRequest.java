package com.be4fe_user_aurora_performance.dto.dup;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDupRequest {

    private String titolo;
    private String descrizione;
    private String sezione;
    private String stato; // BOZZA, APPROVATO, IN_CORSO, COMPLETATO
    private LocalDate dataApprovazione;
}
