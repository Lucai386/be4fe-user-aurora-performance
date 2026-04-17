package com.be4fe_user_aurora_performance.dto.obiettivo;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per un obiettivo completo con progressivi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObiettivoDto {
    private Long id;
    private String codice;
    private String titolo;
    private String descrizione;
    private String unitaMisura;
    /** CRESCENTE o DECRESCENTE */
    private String tipo;
    /** ATTIVO, COMPLETATO, SCADUTO, SOSPESO */
    private String stato;
    private BigDecimal valoreIniziale;
    private BigDecimal valoreTarget;
    private BigDecimal valoreCorrente;
    private BigDecimal percentualeCompletamento;
    private BigDecimal peso;
    private String dataInizio;
    private String dataFine;
    private Integer anno;
    
    // Struttura
    private Integer strutturaId;
    private String strutturaNome;
    
    // Utente assegnato
    private Long utenteAssegnatoId;
    private String utenteAssegnatoNome;
    private String utenteAssegnatoCognome;
    
    // Creatore
    private Long creatoDaId;
    private String creatoDaNome;
    private String creatoDaCognome;
    
    // Storico progressivi
    private List<ObiettivoProgressivoDto> progressivi;
    
    private String createdAt;
    private String updatedAt;
}
