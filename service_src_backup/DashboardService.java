package com.bff_user_aurora_performance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bff_user_aurora_performance.dto.dashboard.DashboardMetricsDto;
import com.bff_user_aurora_performance.dto.dashboard.DashboardMetricsDto.OreLavorateGiorno;
import com.bff_user_aurora_performance.dto.dashboard.DashboardMetricsDto.PrioritaCount;
import com.bff_user_aurora_performance.dto.dashboard.DashboardMetricsDto.ProgettoAttivitaCount;
import com.bff_user_aurora_performance.dto.dashboard.DashboardMetricsDto.StatoCount;
import com.bff_user_aurora_performance.dto.dashboard.DashboardMetricsDto.StrutturaAttivitaCount;
import com.bff_user_aurora_performance.dto.dashboard.DashboardResponse;
import com.bff_user_aurora_performance.dto.dashboard.PersonalMetricsDto;
import com.bff_user_aurora_performance.dto.dashboard.PersonalMetricsDto.ProssimaScadenza;
import com.bff_user_aurora_performance.enums.ErrorCode;
import com.bff_user_aurora_performance.model.Attivita;
import com.bff_user_aurora_performance.model.DupProgetto;
import com.bff_user_aurora_performance.model.Obiettivo;
import com.bff_user_aurora_performance.model.TimesheetEntry;
import com.bff_user_aurora_performance.model.User;
import com.bff_user_aurora_performance.repository.AttivitaRepository;
import com.bff_user_aurora_performance.repository.DupProgettoRepository;
import com.bff_user_aurora_performance.repository.DupRepository;
import com.bff_user_aurora_performance.repository.ObiettivoRepository;
import com.bff_user_aurora_performance.repository.StrutturaRepository;
import com.bff_user_aurora_performance.repository.TimesheetEntryRepository;
import com.bff_user_aurora_performance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service per le metriche della Dashboard.
 * Fornisce statistiche aggregate su attività, progetti, strutture e timesheet.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AttivitaRepository attivitaRepository;
    private final DupProgettoRepository progettoRepository;
    private final DupRepository dupRepository;
    private final StrutturaRepository strutturaRepository;
    private final UserRepository userRepository;
    private final TimesheetEntryRepository timesheetRepository;
    private final ObiettivoRepository obiettivoRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    
    /** Ruoli che vedono la dashboard admin/aggregate */
    private static final Set<String> ADMIN_ROLES = Set.of("AD", "SC", "DR");

    /**
     * Restituisce il valore int di un Integer, o 0 se null.
     */
    private static int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * Restituisce il BigDecimal se non null, altrimenti ZERO.
     */
    private static BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * Metodo principale: instrada alla vista corretta in base al ruolo
     */
    @Transactional(readOnly = true)
    public DashboardResponse getMetrics(String codiceIstat, String userRole, Integer userId) {
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return DashboardResponse.error(ErrorCode.NO_ENTE);
        }

        // Se il ruolo è admin, mostra vista aggregate
        if (userRole != null && ADMIN_ROLES.contains(userRole.toUpperCase())) {
            return getAdminMetrics(codiceIstat);
        } else {
            // Altrimenti vista personale
            return getPersonalMetrics(codiceIstat, userId, userRole);
        }
    }

    /**
     * Metriche aggregate per admin/dirigenti.
     */
    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public DashboardResponse getAdminMetrics(String codiceIstat) {
        log.info("Recupero metriche dashboard ADMIN per codiceIstat={}", codiceIstat);

        // Contatori generali
        long totaleProgetti = progettoRepository.count();
        long totaleDup = dupRepository.count();
        long totaleAttivita = attivitaRepository.count();
        long totaleStrutture = strutturaRepository.findByCodiceIstatComune(codiceIstat).size();
        long totaleUtenti = userRepository.findByCodiceIstat(codiceIstat).size();
        
        // Contatori obiettivi
        long totaleObiettivi = obiettivoRepository.count();
        long obiettiviAttivi = obiettivoRepository.countByStato(Obiettivo.StatoObiettivo.ATTIVO);
        long obiettiviCompletati = obiettivoRepository.countByStato(Obiettivo.StatoObiettivo.COMPLETATO);

        // Carica tutte le attività
        List<Attivita> attivita = attivitaRepository.findAllWithDetails();

        // Metriche attività per stato
        long completate = attivita.stream().filter(a -> a.getStato() == Attivita.Stato.COMPLETATA).count();
        long inCorso = attivita.stream().filter(a -> a.getStato() == Attivita.Stato.IN_CORSO).count();
        long nonIniziate = attivita.stream().filter(a -> a.getStato() == Attivita.Stato.TODO).count();
        
        // Attività in ritardo: data fine stimata passata e non completata
        LocalDate oggi = LocalDate.now();
        long inRitardo = attivita.stream()
            .filter(a -> a.getStato() != Attivita.Stato.COMPLETATA)
            .filter(a -> a.getDataFineStimata() != null && a.getDataFineStimata().isBefore(oggi))
            .count();

        // Metriche temporali aggregate
        BigDecimal totaleOreStimate = BigDecimal.ZERO;
        BigDecimal totaleOreLavorate = BigDecimal.ZERO;
        for (Attivita a : attivita) {
            totaleOreStimate = totaleOreStimate.add(safeBigDecimal(a.getOreStimate()));
            totaleOreLavorate = totaleOreLavorate.add(safeBigDecimal(a.getOreLavorate()));
        }
        double oreStimate = totaleOreStimate.doubleValue();
        double oreLavorate = totaleOreLavorate.doubleValue();

        double percentualeMedia = 0;
        if (!attivita.isEmpty()) {
            int somma = 0;
            for (Attivita a : attivita) {
                somma += safeInt(a.getPercentualeCompletamento());
            }
            percentualeMedia = (double) somma / attivita.size();
        }

        // Distribuzione per stato
        List<StatoCount> distribuzioneStati = new ArrayList<>();
        Map<Attivita.Stato, Long> statiCount = attivita.stream()
            .collect(Collectors.groupingBy(Attivita::getStato, Collectors.counting()));
        for (Attivita.Stato stato : Attivita.Stato.values()) {
            distribuzioneStati.add(StatoCount.builder()
                .stato(stato.name())
                .count(statiCount.getOrDefault(stato, 0L))
                .build());
        }

        // Distribuzione per priorità
        List<PrioritaCount> distribuzionePriorita = new ArrayList<>();
        Map<Attivita.Priorita, Long> prioritaCount = attivita.stream()
            .collect(Collectors.groupingBy(Attivita::getPriorita, Collectors.counting()));
        for (Attivita.Priorita priorita : Attivita.Priorita.values()) {
            distribuzionePriorita.add(PrioritaCount.builder()
                .priorita(priorita.name())
                .count(prioritaCount.getOrDefault(priorita, 0L))
                .build());
        }

        // Attività per struttura (top 5)
        List<StrutturaAttivitaCount> attivitaPerStruttura = attivita.stream()
            .filter(a -> a.getStrutturaId() != null)
            .collect(Collectors.groupingBy(Attivita::getStrutturaId, Collectors.counting()))
            .entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
            .limit(5)
            .map(e -> {
                String nome = strutturaRepository.findById(e.getKey())
                    .map(s -> s.getNome())
                    .orElse("Struttura " + e.getKey());
                return StrutturaAttivitaCount.builder()
                    .strutturaId(e.getKey())
                    .strutturaNome(nome)
                    .count(e.getValue())
                    .build();
            })
            .toList();

        // Ore lavorate ultimi 7 giorni
        List<OreLavorateGiorno> oreUltimiGiorni = getOreUltimiGiorni(7);

        // Top 5 progetti con più attività
        List<ProgettoAttivitaCount> topProgetti = getTopProgetti(attivita, 5);

        DashboardMetricsDto metrics = DashboardMetricsDto.builder()
            .totaleProgetti(totaleProgetti)
            .totaleDup(totaleDup)
            .totaleAttivita(totaleAttivita)
            .totaleStrutture(totaleStrutture)
            .totaleUtenti(totaleUtenti)
            .totaleObiettivi(totaleObiettivi)
            .obiettiviAttivi(obiettiviAttivi)
            .obiettiviCompletati(obiettiviCompletati)
            .attivitaCompletate(completate)
            .attivitaInCorso(inCorso)
            .attivitaNonIniziate(nonIniziate)
            .attivitaInRitardo(inRitardo)
            .oreStimate(oreStimate)
            .oreLavorate(oreLavorate)
            .percentualeCompletamentoMedio(Math.round(percentualeMedia * 10) / 10.0)
            .distribuzioneStatiAttivita(distribuzioneStati)
            .distribuzionePriorita(distribuzionePriorita)
            .attivitaPerStruttura(attivitaPerStruttura)
            .oreUltimiGiorni(oreUltimiGiorni)
            .topProgetti(topProgetti)
            .build();

        log.info("Metriche dashboard ADMIN calcolate: {} attività, {} progetti", totaleAttivita, totaleProgetti);

        return DashboardResponse.successAdmin(metrics);
    }

    /**
     * Metriche personali per dipendenti/collaboratori.
     */
    @Transactional(readOnly = true)
    public DashboardResponse getPersonalMetrics(String codiceIstat, Integer userId, String userRole) {
        if (userId == null) {
            log.warn("UserId non fornito per metriche personali");
            return DashboardResponse.error(ErrorCode.INTERNAL_ERROR);
        }

        log.info("Recupero metriche dashboard PERSONALI per userId={}", userId);

        // Recupero info utente
        User user = userRepository.findById(userId).orElse(null);
        String nomeUtente = user != null ? (user.getNome() + " " + user.getCognome()) : "Utente";
        String ruoloUtente = userRole != null ? userRole : (user != null ? user.getRuolo() : "");

        LocalDate oggi = LocalDate.now();
        LocalDate inizioSettimana = oggi.minusDays(oggi.getDayOfWeek().getValue() - 1);
        LocalDate inizioMese = oggi.withDayOfMonth(1);

        // Le mie attività
        List<Attivita> mieAttivita = attivitaRepository.findByUtenteId(userId);
        long attivitaAssegnate = mieAttivita.size();
        long attivitaCompletate = mieAttivita.stream()
            .filter(a -> a.getStato() == Attivita.Stato.COMPLETATA).count();
        long attivitaInCorso = mieAttivita.stream()
            .filter(a -> a.getStato() == Attivita.Stato.IN_CORSO).count();
        long attivitaInRitardo = mieAttivita.stream()
            .filter(a -> a.getStato() != Attivita.Stato.COMPLETATA)
            .filter(a -> a.getDataFineStimata() != null && a.getDataFineStimata().isBefore(oggi))
            .count();

        // Le mie ore
        List<TimesheetEntry> mieEntries = timesheetRepository.findByUtenteIdOrderByDataDesc(userId);
        double oreLavorateSettimana = mieEntries.stream()
            .filter(e -> !e.getData().isBefore(inizioSettimana) && !e.getData().isAfter(oggi))
            .mapToDouble(e -> safeBigDecimal(e.getOreLavorate()).doubleValue())
            .sum();
        double oreLavorateMese = mieEntries.stream()
            .filter(e -> !e.getData().isBefore(inizioMese) && !e.getData().isAfter(oggi))
            .mapToDouble(e -> safeBigDecimal(e.getOreLavorate()).doubleValue())
            .sum();
        double oreLavorateTotali = mieEntries.stream()
            .mapToDouble(e -> safeBigDecimal(e.getOreLavorate()).doubleValue())
            .sum();
        
        // Ore stimate totali delle mie attività
        double oreStimate = mieAttivita.stream()
            .mapToDouble(a -> safeBigDecimal(a.getOreStimate()).doubleValue())
            .sum();

        // Percentuale completamento medio
        double percentualeMedia = 0;
        if (!mieAttivita.isEmpty()) {
            int somma = 0;
            for (Attivita a : mieAttivita) {
                somma += safeInt(a.getPercentualeCompletamento());
            }
            percentualeMedia = (double) somma / mieAttivita.size();
        }

        // Prossime scadenze (prossimi 7 giorni)
        LocalDate traSetteGiorni = oggi.plusDays(7);
        List<ProssimaScadenza> prossimeScadenze = mieAttivita.stream()
            .filter(a -> a.getStato() != Attivita.Stato.COMPLETATA)
            .filter(a -> a.getDataFineStimata() != null)
            .filter(a -> !a.getDataFineStimata().isBefore(oggi) && !a.getDataFineStimata().isAfter(traSetteGiorni))
            .sorted((a, b) -> a.getDataFineStimata().compareTo(b.getDataFineStimata()))
            .limit(5)
            .map(a -> {
                String progettoTitolo = "";
                if (a.getProgettoId() != null) {
                    progettoTitolo = progettoRepository.findById(a.getProgettoId())
                        .map(DupProgetto::getTitolo)
                        .orElse("");
                }
                int giorniRimanenti = (int) ChronoUnit.DAYS.between(oggi, a.getDataFineStimata());
                return ProssimaScadenza.builder()
                    .attivitaId(a.getId())
                    .attivitaTitolo(a.getTitolo())
                    .progettoTitolo(progettoTitolo)
                    .dataScadenza(a.getDataFineStimata().format(DATE_FORMAT))
                    .giorniRimanenti(giorniRimanenti)
                    .percentualeCompletamento(safeInt(a.getPercentualeCompletamento()))
                    .build();
            })
            .toList();

        // Ore ultimi 7 giorni
        List<PersonalMetricsDto.OreLavorateGiorno> oreUltimiGiorni = getOreUltimiGiorniPersonali(userId, 7);

        // Distribuzione per priorità delle mie attività
        List<PersonalMetricsDto.PrioritaCount> distribuzionePriorita = new ArrayList<>();
        Map<Attivita.Priorita, Long> prioritaCount = mieAttivita.stream()
            .collect(Collectors.groupingBy(Attivita::getPriorita, Collectors.counting()));
        for (Attivita.Priorita priorita : Attivita.Priorita.values()) {
            distribuzionePriorita.add(PersonalMetricsDto.PrioritaCount.builder()
                .priorita(priorita.name())
                .count(prioritaCount.getOrDefault(priorita, 0L))
                .build());
        }

        // Ore per progetto (ultimi 7 giorni)
        List<PersonalMetricsDto.OrePerProgetto> orePerProgetto = getOrePerProgettoPersonali(userId, 7);

        PersonalMetricsDto personalMetrics = PersonalMetricsDto.builder()
            .nomeUtente(nomeUtente)
            .ruoloUtente(ruoloUtente)
            .attivitaAssegnate(attivitaAssegnate)
            .attivitaCompletate(attivitaCompletate)
            .attivitaInCorso(attivitaInCorso)
            .attivitaInRitardo(attivitaInRitardo)
            .oreLavorateSettimana(Math.round(oreLavorateSettimana * 10) / 10.0)
            .oreLavorateMese(Math.round(oreLavorateMese * 10) / 10.0)
            .oreLavorateTotali(Math.round(oreLavorateTotali * 10) / 10.0)
            .oreStimate(oreStimate)
            .percentualeCompletamentoMedio(Math.round(percentualeMedia * 10) / 10.0)
            .prossimeScadenze(prossimeScadenze)
            .oreUltimiGiorni(oreUltimiGiorni)
            .orePerProgetto(orePerProgetto)
            .distribuzionePriorita(distribuzionePriorita)
            .build();

        log.info("Metriche dashboard PERSONALI calcolate per userId={}: {} attività assegnate", userId, attivitaAssegnate);

        return DashboardResponse.successPersonal(personalMetrics);
    }

    /**
     * Calcola le ore lavorate per ogni giorno degli ultimi N giorni per un utente specifico.
     */
    private List<PersonalMetricsDto.OreLavorateGiorno> getOreUltimiGiorniPersonali(Integer userId, int giorni) {
        List<PersonalMetricsDto.OreLavorateGiorno> result = new ArrayList<>();
        LocalDate oggi = LocalDate.now();
        LocalDate dataInizio = oggi.minusDays(giorni - 1);

        List<TimesheetEntry> entries = timesheetRepository.findByUtenteIdAndPeriodo(userId, dataInizio, oggi);

        // Mappa per le ore per giorno
        Map<LocalDate, BigDecimal> orePerGiorno = new HashMap<>();
        for (int i = 0; i < giorni; i++) {
            orePerGiorno.put(oggi.minusDays(i), BigDecimal.ZERO);
        }

        for (TimesheetEntry entry : entries) {
            LocalDate data = entry.getData();
            if (orePerGiorno.containsKey(data)) {
                orePerGiorno.put(data, 
                    orePerGiorno.get(data).add(safeBigDecimal(entry.getOreLavorate())));
            }
        }

        // Ordino per data crescente
        for (int i = giorni - 1; i >= 0; i--) {
            LocalDate data = oggi.minusDays(i);
            result.add(PersonalMetricsDto.OreLavorateGiorno.builder()
                .data(data.format(DATE_FORMAT))
                .ore(orePerGiorno.get(data).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .build());
        }

        return result;
    }

    /**
     * Calcola le ore lavorate per progetto negli ultimi N giorni per un utente specifico.
     */
    private List<PersonalMetricsDto.OrePerProgetto> getOrePerProgettoPersonali(Integer userId, int giorni) {
        LocalDate oggi = LocalDate.now();
        LocalDate dataInizio = oggi.minusDays(giorni - 1);

        List<TimesheetEntry> entries = timesheetRepository.findByUtenteIdAndPeriodo(userId, dataInizio, oggi);

        // Raggruppa ore per progetto
        Map<Long, BigDecimal> orePerProgettoMap = new HashMap<>();
        Map<Long, String> progettoTitoli = new HashMap<>();

        for (TimesheetEntry entry : entries) {
            Attivita attivita = entry.getAttivita();
            if (attivita != null && attivita.getProgetto() != null) {
                Long progettoId = attivita.getProgetto().getId();
                String progettoTitolo = attivita.getProgetto().getTitolo();
                
                orePerProgettoMap.put(progettoId, 
                    orePerProgettoMap.getOrDefault(progettoId, BigDecimal.ZERO)
                        .add(safeBigDecimal(entry.getOreLavorate())));
                progettoTitoli.putIfAbsent(progettoId, progettoTitolo);
            }
        }

        // Converti in lista ordinata per ore decrescenti
        return orePerProgettoMap.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .map(e -> PersonalMetricsDto.OrePerProgetto.builder()
                .progettoId(e.getKey())
                .progettoTitolo(progettoTitoli.get(e.getKey()))
                .ore(e.getValue().setScale(1, RoundingMode.HALF_UP).doubleValue())
                .build())
            .toList();
    }

    /**
     * Calcola le ore lavorate per ogni giorno degli ultimi N giorni
     */
    private List<OreLavorateGiorno> getOreUltimiGiorni(int giorni) {
        List<OreLavorateGiorno> result = new ArrayList<>();
        LocalDate oggi = LocalDate.now();
        
        // Prendo tutti i timesheet entries degli ultimi N giorni
        LocalDate dataInizio = oggi.minusDays(giorni - 1);
        List<TimesheetEntry> entries = timesheetRepository.findAll().stream()
            .filter(e -> !e.getData().isBefore(dataInizio) && !e.getData().isAfter(oggi))
            .toList();

        // Raggruppo per data
        Map<LocalDate, BigDecimal> orePerGiorno = new HashMap<>();
        for (int i = 0; i < giorni; i++) {
            orePerGiorno.put(oggi.minusDays(i), BigDecimal.ZERO);
        }

        for (TimesheetEntry entry : entries) {
            LocalDate data = entry.getData();
            if (orePerGiorno.containsKey(data)) {
                orePerGiorno.put(data, 
                    orePerGiorno.get(data).add(entry.getOreLavorate() != null ? entry.getOreLavorate() : BigDecimal.ZERO));
            }
        }

        // Converto in lista ordinata per data
        for (int i = giorni - 1; i >= 0; i--) {
            LocalDate data = oggi.minusDays(i);
            result.add(OreLavorateGiorno.builder()
                .data(data.format(DATE_FORMAT))
                .ore(orePerGiorno.get(data).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .build());
        }

        return result;
    }

    /**
     * Ritorna i top N progetti con più attività
     */
    @SuppressWarnings("null")
    private List<ProgettoAttivitaCount> getTopProgetti(List<Attivita> attivita, int limit) {
        // Raggruppo attività per progetto
        Map<Long, List<Attivita>> attivitaPerProgetto = attivita.stream()
            .filter(a -> a.getProgettoId() != null)
            .collect(Collectors.groupingBy(Attivita::getProgettoId));

        return attivitaPerProgetto.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
            .limit(limit)
            .map(e -> {
                Long progettoId = e.getKey();
                List<Attivita> progettoAttivita = e.getValue();
                
                // Calcolo percentuale completamento medio del progetto
                int percentualeMedia = 0;
                if (!progettoAttivita.isEmpty()) {
                    int somma = 0;
                    for (Attivita a : progettoAttivita) {
                        somma += safeInt(a.getPercentualeCompletamento());
                    }
                    percentualeMedia = somma / progettoAttivita.size();
                }

                DupProgetto progetto = progettoRepository.findById(progettoId).orElse(null);
                String titolo = progetto != null ? progetto.getTitolo() : "Progetto " + progettoId;

                return ProgettoAttivitaCount.builder()
                    .progettoId(progettoId)
                    .progettoTitolo(titolo)
                    .countAttivita(progettoAttivita.size())
                    .percentualeCompletamento(percentualeMedia)
                    .build();
            })
            .toList();
    }
}
