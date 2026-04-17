package com.be4fe_user_aurora_performance.service;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import com.be4fe_user_aurora_performance.dto.dashboard.DashboardMetricsDto;
import com.be4fe_user_aurora_performance.dto.dashboard.DashboardMetricsDto.*;
import com.be4fe_user_aurora_performance.dto.dashboard.DashboardResponse;
import com.be4fe_user_aurora_performance.dto.dashboard.PersonalMetricsDto;
import com.be4fe_user_aurora_performance.dto.dashboard.PersonalMetricsDto.ProssimaScadenza;
import com.be4fe_user_aurora_performance.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BFF User service per le metriche della Dashboard.
 * Sostituisce il DashboardService del monolita delegando al core via HTTP.
 * Per la vista admin usa getAttivita(null,null,null) che restituisce tutte le attività.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
public class DashboardService {

    private final CoreApiClient coreApiClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Set<String> ADMIN_ROLES = Set.of("AD", "SC", "DR");

    // ─── Entry point ──────────────────────────────────────────────────────────

    public DashboardResponse getMetrics(String codiceIstat, String userRole, Integer userId) {
        if (codiceIstat == null || codiceIstat.isBlank()) return DashboardResponse.error(ErrorCode.NO_ENTE);
        if (userRole != null && ADMIN_ROLES.contains(userRole.toUpperCase())) {
            return getAdminMetrics(codiceIstat);
        }
        return getPersonalMetrics(codiceIstat, userId, userRole);
    }

    // ─── Admin metrics ────────────────────────────────────────────────────────

    private DashboardResponse getAdminMetrics(String codiceIstat) {
        log.info("Dashboard ADMIN per codiceIstat={}", codiceIstat);

        // Fetch all needed data
        List<Map> attivita = coreApiClient.getAttivita(null, null, null);
        List<Map> users = coreApiClient.getUsers();
        List<Map> strutture = coreApiClient.getStrutture();
        List<Map> dups = coreApiClient.getDup();
        Map<String, Long> obiettiviCounts = coreApiClient.getObiettiviCounts();

        long totaleProgetti = dups.stream()
                .mapToLong(d -> {
                    Object pList = d.get("progetti");
                    return pList instanceof List ? ((List<?>) pList).size() : 0L;
                }).sum();

        long totaleAttivita = attivita.size();
        long totaleStrutture = strutture.size();
        long totaleUtenti = users.size();
        long totaleDup = dups.size();
        long totaleObiettivi = obiettiviCounts.getOrDefault("totale", 0L);
        long obiettiviAttivi = obiettiviCounts.getOrDefault("attivi", 0L);
        long obiettiviCompletati = obiettiviCounts.getOrDefault("completati", 0L);

        LocalDate oggi = LocalDate.now();

        long completate = attivita.stream().filter(a -> "COMPLETATA".equals(strVal(a, "stato"))).count();
        long inCorso = attivita.stream().filter(a -> "IN_CORSO".equals(strVal(a, "stato"))).count();
        long nonIniziate = attivita.stream().filter(a -> "TODO".equals(strVal(a, "stato"))).count();
        long inRitardo = attivita.stream().filter(a -> isInRitardo(a, oggi)).count();

        double oreStimate = attivita.stream().mapToDouble(a -> safeDouble(bigDecimalVal(a, "oreStimate"))).sum();
        double oreLavorate = attivita.stream().mapToDouble(a -> safeDouble(bigDecimalVal(a, "oreLavorate"))).sum();

        double percentualeMedia = 0;
        if (!attivita.isEmpty()) {
            int somma = attivita.stream().mapToInt(a -> safeInt(intVal(a, "percentualeCompletamento"))).sum();
            percentualeMedia = (double) somma / attivita.size();
        }

        // Distribuzione stati
        Map<String, Long> statiMap = attivita.stream()
                .collect(Collectors.groupingBy(a -> strValOrEmpty(a, "stato"), Collectors.counting()));
        List<String> allStati = List.of("TODO", "IN_CORSO", "SOSPESA", "COMPLETATA");
        List<StatoCount> distribuzioneStati = allStati.stream()
                .map(s -> StatoCount.builder().stato(s).count(statiMap.getOrDefault(s, 0L)).build())
                .collect(Collectors.toList());

        // Distribuzione priorità
        Map<String, Long> prioritaMap = attivita.stream()
                .collect(Collectors.groupingBy(a -> strValOrEmpty(a, "priorita"), Collectors.counting()));
        List<String> allPriorita = List.of("BASSA", "MEDIA", "ALTA", "URGENTE");
        List<PrioritaCount> distribuzionePriorita = allPriorita.stream()
                .map(p -> PrioritaCount.builder().priorita(p).count(prioritaMap.getOrDefault(p, 0L)).build())
                .collect(Collectors.toList());

        // Attività per struttura (top 5)
        Map<Integer, Long> perStruttura = attivita.stream()
                .filter(a -> intVal(a, "strutturaId") != null)
                .collect(Collectors.groupingBy(a -> intVal(a, "strutturaId"), Collectors.counting()));
        Map<Integer, String> strutturaNomi = strutture.stream()
                .filter(s -> intVal(s, "id") != null)
                .collect(Collectors.toMap(s -> intVal(s, "id"), s -> strValOrEmpty(s, "nome"), (a, b) -> a));
        List<StrutturaAttivitaCount> attivitaPerStruttura = perStruttura.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .map(e -> StrutturaAttivitaCount.builder()
                        .strutturaId(e.getKey())
                        .strutturaNome(strutturaNomi.getOrDefault(e.getKey(), "Struttura " + e.getKey()))
                        .count(e.getValue())
                        .build())
                .toList();

        // Ore ultimi 7 giorni (admin = somma tutti gli utenti per i giorni richiesti)
        List<OreLavorateGiorno> oreUltimiGiorni = buildOreUltimiGiorniAdmin(attivita, 7);

        // Top 5 progetti
        List<ProgettoAttivitaCount> topProgetti = buildTopProgetti(attivita, 5);

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
                .oreStimate(round1(oreStimate))
                .oreLavorate(round1(oreLavorate))
                .percentualeCompletamentoMedio(round1(percentualeMedia))
                .distribuzioneStatiAttivita(distribuzioneStati)
                .distribuzionePriorita(distribuzionePriorita)
                .attivitaPerStruttura(attivitaPerStruttura)
                .oreUltimiGiorni(oreUltimiGiorni)
                .topProgetti(topProgetti)
                .build();

        return DashboardResponse.successAdmin(metrics);
    }

    // ─── Personal metrics ─────────────────────────────────────────────────────

    private DashboardResponse getPersonalMetrics(String codiceIstat, Integer userId, String userRole) {
        if (userId == null) return DashboardResponse.error(ErrorCode.INTERNAL_ERROR);
        log.info("Dashboard PERSONALE per userId={}", userId);

        // User info
        Optional<Map> userOpt = coreApiClient.getUserById((long) userId);
        String nomeUtente = userOpt.map(u -> strValOrEmpty(u, "nome") + " " + strValOrEmpty(u, "cognome")).orElse("Utente");
        String ruoloUtente = userRole != null ? userRole : userOpt.map(u -> strValOrEmpty(u, "ruolo")).orElse("");

        LocalDate oggi = LocalDate.now();
        LocalDate inizioSettimana = oggi.minusDays(oggi.getDayOfWeek().getValue() - 1L);
        LocalDate inizioMese = oggi.withDayOfMonth(1);

        // User activities
        List<Map> mieAttivita = coreApiClient.getAttivita(null, (long) userId, null);
        long attivitaAssegnate = mieAttivita.size();
        long attivitaCompletate = mieAttivita.stream().filter(a -> "COMPLETATA".equals(strVal(a, "stato"))).count();
        long attivitaInCorso = mieAttivita.stream().filter(a -> "IN_CORSO".equals(strVal(a, "stato"))).count();
        long attivitaInRitardo = mieAttivita.stream().filter(a -> isInRitardo(a, oggi)).count();

        // Timesheet
        List<Map> timesheetAll = coreApiClient.getTimesheetByUtente((long) userId, null, null);
        List<Map> timesheetSettimana = timesheetAll.stream()
                .filter(e -> isDateInRange(strVal(e, "data"), inizioSettimana, oggi))
                .toList();
        List<Map> timesheetMese = timesheetAll.stream()
                .filter(e -> isDateInRange(strVal(e, "data"), inizioMese, oggi))
                .toList();

        double oreLavorateSettimana = timesheetSettimana.stream().mapToDouble(e -> safeDouble(bigDecimalVal(e, "oreLavorate"))).sum();
        double oreLavorateMese = timesheetMese.stream().mapToDouble(e -> safeDouble(bigDecimalVal(e, "oreLavorate"))).sum();
        double oreLavorateTotali = timesheetAll.stream().mapToDouble(e -> safeDouble(bigDecimalVal(e, "oreLavorate"))).sum();
        double oreStimate = mieAttivita.stream().mapToDouble(a -> safeDouble(bigDecimalVal(a, "oreStimate"))).sum();

        double percentualeMedia = 0;
        if (!mieAttivita.isEmpty()) {
            int somma = mieAttivita.stream().mapToInt(a -> safeInt(intVal(a, "percentualeCompletamento"))).sum();
            percentualeMedia = (double) somma / mieAttivita.size();
        }

        // Prossime scadenze (prossimi 7 giorni)
        LocalDate traSetteGiorni = oggi.plusDays(7);
        List<ProssimaScadenza> prossimeScadenze = mieAttivita.stream()
                .filter(a -> !"COMPLETATA".equals(strVal(a, "stato")))
                .filter(a -> {
                    String ds = strVal(a, "dataFineStimata");
                    if (ds == null) return false;
                    try {
                        LocalDate d = LocalDate.parse(ds.substring(0, 10));
                        return !d.isBefore(oggi) && !d.isAfter(traSetteGiorni);
                    } catch (Exception ex) { return false; }
                })
                .sorted(Comparator.comparing(a -> {
                    try { return LocalDate.parse(strVal(a, "dataFineStimata").substring(0, 10)); } catch (Exception e) { return oggi; }
                }))
                .limit(5)
                .map(a -> {
                    String ds = strVal(a, "dataFineStimata");
                    LocalDate d = LocalDate.parse(ds.substring(0, 10));
                    int giorniRimanenti = (int) ChronoUnit.DAYS.between(oggi, d);
                    return ProssimaScadenza.builder()
                            .attivitaId(longVal(a, "id"))
                            .attivitaTitolo(strVal(a, "titolo"))
                            .progettoTitolo(nestedStrVal(a, "progetto", "titolo"))
                            .dataScadenza(d.format(DATE_FMT))
                            .giorniRimanenti(giorniRimanenti)
                            .percentualeCompletamento(safeInt(intVal(a, "percentualeCompletamento")))
                            .build();
                })
                .toList();

        // Ore ultimi 7 giorni personali
        List<PersonalMetricsDto.OreLavorateGiorno> oreUltimiGiorni = buildOreUltimiGiorniPersonali(timesheetAll, 7);

        // Distribuzione priorità personale
        Map<String, Long> prioritaMap = mieAttivita.stream()
                .collect(Collectors.groupingBy(a -> strValOrEmpty(a, "priorita"), Collectors.counting()));
        List<String> allPriorita = List.of("BASSA", "MEDIA", "ALTA", "URGENTE");
        List<PersonalMetricsDto.PrioritaCount> distribuzionePriorita = allPriorita.stream()
                .map(p -> PersonalMetricsDto.PrioritaCount.builder().priorita(p).count(prioritaMap.getOrDefault(p, 0L)).build())
                .collect(Collectors.toList());

        // Ore per progetto (ultimi 7 giorni)
        List<PersonalMetricsDto.OrePerProgetto> orePerProgetto = buildOrePerProgetto(timesheetSettimana);

        PersonalMetricsDto personalMetrics = PersonalMetricsDto.builder()
                .nomeUtente(nomeUtente)
                .ruoloUtente(ruoloUtente)
                .attivitaAssegnate(attivitaAssegnate)
                .attivitaCompletate(attivitaCompletate)
                .attivitaInCorso(attivitaInCorso)
                .attivitaInRitardo(attivitaInRitardo)
                .oreLavorateSettimana(round1(oreLavorateSettimana))
                .oreLavorateMese(round1(oreLavorateMese))
                .oreLavorateTotali(round1(oreLavorateTotali))
                .oreStimate(oreStimate)
                .percentualeCompletamentoMedio(round1(percentualeMedia))
                .prossimeScadenze(prossimeScadenze)
                .oreUltimiGiorni(oreUltimiGiorni)
                .orePerProgetto(orePerProgetto)
                .distribuzionePriorita(distribuzionePriorita)
                .build();

        return DashboardResponse.successPersonal(personalMetrics);
    }

    // ─── Chart helpers ────────────────────────────────────────────────────────

    /** Calcola ore lavorate per giorno negli ultimi N giorni aggregando le attività (admin). */
    private List<OreLavorateGiorno> buildOreUltimiGiorniAdmin(List<Map> attivita, int giorni) {
        // Admin: usa i timesheetEntries embedded in ogni attività (se presenti)
        // Raggruppa per data
        Map<String, BigDecimal> orePerGiorno = new LinkedHashMap<>();
        LocalDate oggi = LocalDate.now();
        for (int i = giorni - 1; i >= 0; i--) {
            orePerGiorno.put(oggi.minusDays(i).format(DATE_FMT), BigDecimal.ZERO);
        }
        LocalDate dataInizio = oggi.minusDays(giorni - 1);

        for (Map a : attivita) {
            Object tsObj = a.get("timesheetEntries");
            if (!(tsObj instanceof List)) continue;
            for (Map e : (List<Map>) tsObj) {
                String ds = strVal(e, "data");
                if (ds == null) continue;
                String giorno = ds.length() >= 10 ? ds.substring(0, 10) : ds;
                if (orePerGiorno.containsKey(giorno)) {
                    BigDecimal ore = bigDecimalVal(e, "oreLavorate");
                    if (ore != null) orePerGiorno.merge(giorno, ore, BigDecimal::add);
                }
            }
        }

        return orePerGiorno.entrySet().stream()
                .map(e -> OreLavorateGiorno.builder()
                        .data(e.getKey())
                        .ore(e.getValue().setScale(1, RoundingMode.HALF_UP).doubleValue())
                        .build())
                .toList();
    }

    private List<PersonalMetricsDto.OreLavorateGiorno> buildOreUltimiGiorniPersonali(List<Map> timesheet, int giorni) {
        LocalDate oggi = LocalDate.now();
        Map<String, BigDecimal> orePerGiorno = new LinkedHashMap<>();
        for (int i = giorni - 1; i >= 0; i--) {
            orePerGiorno.put(oggi.minusDays(i).format(DATE_FMT), BigDecimal.ZERO);
        }
        for (Map e : timesheet) {
            String ds = strVal(e, "data");
            if (ds == null) continue;
            String giorno = ds.length() >= 10 ? ds.substring(0, 10) : ds;
            if (orePerGiorno.containsKey(giorno)) {
                BigDecimal ore = bigDecimalVal(e, "oreLavorate");
                if (ore != null) orePerGiorno.merge(giorno, ore, BigDecimal::add);
            }
        }
        return orePerGiorno.entrySet().stream()
                .map(e -> PersonalMetricsDto.OreLavorateGiorno.builder()
                        .data(e.getKey())
                        .ore(e.getValue().setScale(1, RoundingMode.HALF_UP).doubleValue())
                        .build())
                .toList();
    }

    private List<PersonalMetricsDto.OrePerProgetto> buildOrePerProgetto(List<Map> timesheet) {
        Map<Long, BigDecimal> orePerProgettoMap = new HashMap<>();
        Map<Long, String> progettoTitoli = new HashMap<>();

        for (Map entry : timesheet) {
            Long attId = longVal(entry, "attivitaId");
            if (attId == null) continue;
            // We don't have project info in timesheet entries from getTimesheetByUtente
            // Use attivitaId as proxy key until we can join
            BigDecimal ore = bigDecimalVal(entry, "oreLavorate");
            if (ore != null) orePerProgettoMap.merge(attId, ore, BigDecimal::add);
        }

        return orePerProgettoMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> PersonalMetricsDto.OrePerProgetto.builder()
                        .progettoId(e.getKey())
                        .progettoTitolo(progettoTitoli.getOrDefault(e.getKey(), "Attività " + e.getKey()))
                        .ore(e.getValue().setScale(1, RoundingMode.HALF_UP).doubleValue())
                        .build())
                .toList();
    }

    private List<ProgettoAttivitaCount> buildTopProgetti(List<Map> attivita, int limit) {
        Map<Long, List<Map>> perProgetto = attivita.stream()
                .filter(a -> longVal(a, "progettoId") != null)
                .collect(Collectors.groupingBy(a -> longVal(a, "progettoId")));

        return perProgetto.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .limit(limit)
                .map(e -> {
                    List<Map> progettoAttivita = e.getValue();
                    int pct = 0;
                    if (!progettoAttivita.isEmpty()) {
                        int somma = progettoAttivita.stream().mapToInt(a -> safeInt(intVal(a, "percentualeCompletamento"))).sum();
                        pct = somma / progettoAttivita.size();
                    }
                    String titolo = progettoAttivita.stream()
                            .map(a -> nestedStrVal(a, "progetto", "titolo"))
                            .filter(Objects::nonNull).findFirst().orElse("Progetto " + e.getKey());
                    return ProgettoAttivitaCount.builder()
                            .progettoId(e.getKey())
                            .progettoTitolo(titolo)
                            .countAttivita(progettoAttivita.size())
                            .percentualeCompletamento(pct)
                            .build();
                })
                .toList();
    }

    // ─── Utilities ───────────────────────────────────────────────────────────

    private boolean isInRitardo(Map a, LocalDate oggi) {
        if ("COMPLETATA".equals(strVal(a, "stato"))) return false;
        String ds = strVal(a, "dataFineStimata");
        if (ds == null) return false;
        try {
            return LocalDate.parse(ds.substring(0, 10)).isBefore(oggi);
        } catch (Exception e) { return false; }
    }

    private boolean isDateInRange(String ds, LocalDate from, LocalDate to) {
        if (ds == null) return false;
        try {
            LocalDate d = LocalDate.parse(ds.substring(0, 10));
            return !d.isBefore(from) && !d.isAfter(to);
        } catch (Exception e) { return false; }
    }

    private double round1(double v) { return Math.round(v * 10) / 10.0; }
    private static int safeInt(Integer v) { return v != null ? v : 0; }
    private static double safeDouble(BigDecimal v) { return v != null ? v.doubleValue() : 0.0; }
    private String strVal(Map<?, ?> m, String k) { Object v = m.get(k); return v != null ? v.toString() : null; }
    private String strValOrEmpty(Map<?, ?> m, String k) { Object v = m.get(k); return v != null ? v.toString() : ""; }
    private Integer intVal(Map<?, ?> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }
    private Long longVal(Map<?, ?> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }
    private String nestedStrVal(Map<?, ?> m, String nk, String f) {
        Object n = m.get(nk);
        if (!(n instanceof Map)) return null;
        Object v = ((Map<?, ?>) n).get(f);
        return v != null ? v.toString() : null;
    }
    private BigDecimal bigDecimalVal(Map<?, ?> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; }
    }
}
