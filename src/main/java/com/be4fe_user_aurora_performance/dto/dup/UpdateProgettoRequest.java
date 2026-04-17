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
public class UpdateProgettoRequest {

    private String codice;
    private String titolo;
    private String descrizione;
    private Long lpmId;
    private Boolean removeLpm;
    private Integer responsabileId;
    private Boolean removeResponsabile;
    private Integer strutturaId;
    private Boolean removeStruttura;
    private String stato; // TODO, IN_CORSO, COMPLETATO
    private Integer progresso;
    private String priorita; // BASSA, MEDIA, ALTA, CRITICA
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private BigDecimal budget;
    private String note;
    private Integer ordine;
}
