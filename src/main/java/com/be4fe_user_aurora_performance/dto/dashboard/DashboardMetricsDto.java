package com.be4fe_user_aurora_performance.dto.dashboard;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO contenente tutte le metriche per la dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {

    // Contatori generali
    private long totaleProgetti;
    private long totaleDup;
    private long totaleAttivita;
    private long totaleStrutture;
    private long totaleUtenti;
    private long totaleObiettivi;
    private long obiettiviAttivi;
    private long obiettiviCompletati;

    // Metriche attività
    private long attivitaCompletate;
    private long attivitaInCorso;
    private long attivitaNonIniziate;
    private long attivitaInRitardo;

    // Metriche temporali
    private double oreStimate;
    private double oreLavorate;
    private double percentualeCompletamentoMedio;

    // Distribuzione per stato attività
    private List<StatoCount> distribuzioneStatiAttivita;

    // Distribuzione per priorità
    private List<PrioritaCount> distribuzionePriorita;

    // Attività per struttura (top 5)
    private List<StrutturaAttivitaCount> attivitaPerStruttura;

    // Ore lavorate negli ultimi 7 giorni
    private List<OreLavorateGiorno> oreUltimiGiorni;

    // Top 5 progetti con più attività
    private List<ProgettoAttivitaCount> topProgetti;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatoCount {
        private String stato;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrioritaCount {
        private String priorita;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrutturaAttivitaCount {
        private Integer strutturaId;
        private String strutturaNome;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OreLavorateGiorno {
        private String data;
        private double ore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgettoAttivitaCount {
        private Long progettoId;
        private String progettoTitolo;
        private long countAttivita;
        private int percentualeCompletamento;
    }
}
