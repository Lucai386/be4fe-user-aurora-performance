package com.be4fe_user_aurora_performance.dto.dashboard;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per le metriche personali del dipendente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalMetricsDto {

    // Info utente
    private String nomeUtente;
    private String ruoloUtente;

    // Le mie attività
    private long attivitaAssegnate;
    private long attivitaCompletate;
    private long attivitaInCorso;
    private long attivitaInRitardo;

    // Le mie ore
    private double oreLavorateSettimana;
    private double oreLavorateMese;
    private double oreLavorateTotali;
    private double oreStimate;

    // Percentuale completamento medio delle mie attività
    private double percentualeCompletamentoMedio;

    // Prossime scadenze (attività in scadenza nei prossimi 7 giorni)
    private List<ProssimaScadenza> prossimeScadenze;

    // Ore per giorno (ultimi 7 giorni)
    private List<OreLavorateGiorno> oreUltimiGiorni;

    // Ore per progetto (ultimi 7 giorni)
    private List<OrePerProgetto> orePerProgetto;

    // Le mie attività per priorità
    private List<PrioritaCount> distribuzionePriorita;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProssimaScadenza {
        private Long attivitaId;
        private String attivitaTitolo;
        private String progettoTitolo;
        private String dataScadenza;
        private int giorniRimanenti;
        private int percentualeCompletamento;
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
    public static class PrioritaCount {
        private String priorita;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrePerProgetto {
        private Long progettoId;
        private String progettoTitolo;
        private double ore;
    }
}
