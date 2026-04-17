package com.be4fe_user_aurora_performance.service;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import com.be4fe_user_aurora_performance.dto.valutazioni.*;
import com.be4fe_user_aurora_performance.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BFF User service per le valutazioni delle performance.
 * Sostituisce il ValutazioniService del monolita usando CoreApiClient.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
public class ValutazioniService {

    private final CoreApiClient coreApiClient;
    private final ObiettivoService obiettivoService;

    // ─── Entry point ──────────────────────────────────────────────────────────

    public ValutazioniResponse getValutazioni(String ruolo, Integer userId) {
        try {
            Optional<Map> userOpt = coreApiClient.getUserById((long) userId);
            if (userOpt.isEmpty()) return ValutazioniResponse.ko(ErrorCode.USER_NOT_FOUND);
            Map user = userOpt.get();

            if (isAdminRole(ruolo)) return getValutazioniAdmin(user, ruolo);
            if (isResponsabileRole(ruolo)) return getValutazioniResponsabile(user, ruolo);
            return getValutazioniPersonali(user, ruolo);
        } catch (Exception e) {
            log.error("Errore valutazioni: {}", e.getMessage(), e);
            return ValutazioniResponse.ko(ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    public ValutazioniResponse getValutazioniDipendente(Integer dipendenteId, String ruolo, Integer userId) {
        try {
            if (!isResponsabileRole(ruolo) && !isAdminRole(ruolo)) return ValutazioniResponse.ko(ErrorCode.NOT_AUTHORIZED);
            Optional<Map> dipOpt = coreApiClient.getUserById((long) dipendenteId);
            if (dipOpt.isEmpty()) return ValutazioniResponse.ko(ErrorCode.USER_NOT_FOUND);
            Map dip = dipOpt.get();

            // Responsabile: verifica che dipendente sia nella sua struttura
            if (isResponsabileRole(ruolo) && !isAdminRole(ruolo)) {
                Optional<Map> respOpt = coreApiClient.getUserById((long) userId);
                if (respOpt.isEmpty()) return ValutazioniResponse.ko(ErrorCode.USER_NOT_FOUND);
                Map resp = respOpt.get();
                String codiceIstat = strVal(resp, "codiceIstat");
                Set<Integer> dipStruttura = getDipendentiStruttura(userId, codiceIstat);
                if (!dipStruttura.contains(dipendenteId)) return ValutazioniResponse.ko(ErrorCode.NOT_AUTHORIZED);
            }

            ValutazioniDipendenteDto metrics = buildDipendenteMetrics(dip);
            return ValutazioniResponse.builder().result("OK").viewType("dipendente").metricsDipendente(metrics).build();
        } catch (Exception e) {
            log.error("Errore valutazioni dipendente: {}", e.getMessage(), e);
            return ValutazioniResponse.ko(ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    // ─── View builders ────────────────────────────────────────────────────────

    private ValutazioniResponse getValutazioniAdmin(Map user, String ruolo) {
        String codiceIstat = strVal(user, "codiceIstat");
        ValutazioniStrutturaDto metrics = buildStrutturaMetricsAdmin(codiceIstat);
        List<DipendenteSelectDto> dipendenti = getAllDipendentiSelect(codiceIstat);
        return ValutazioniResponse.builder().result("OK").viewType("admin")
                .metricsStruttura(metrics).dipendentiStruttura(dipendenti).build();
    }

    private ValutazioniResponse getValutazioniResponsabile(Map user, String ruolo) {
        String codiceIstat = strVal(user, "codiceIstat");
        Integer userId = intVal(user, "id");
        // Find user's struttura (responsabile)
        List<Map> strutture = coreApiClient.getStrutture(codiceIstat);
        Optional<Map> strutturaOpt = strutture.stream()
                .filter(s -> Objects.equals(intVal(s, "idResponsabile"), userId))
                .findFirst();
        if (strutturaOpt.isEmpty()) return getValutazioniPersonali(user, ruolo);

        Map struttura = strutturaOpt.get();
        ValutazioniStrutturaDto metrics = buildStrutturaMetrics(struttura, user);
        Integer strutturaId = intVal(struttura, "id");
        List<DipendenteSelectDto> dipendenti = getDipendentiSelectStruttura(struttura, userId);
        return ValutazioniResponse.builder().result("OK").viewType("responsabile")
                .metricsStruttura(metrics).dipendentiStruttura(dipendenti).build();
    }

    private ValutazioniResponse getValutazioniPersonali(Map user, String ruolo) {
        ValutazioniPersonaliDto metrics = buildPersonaliMetrics(user, ruolo);
        return ValutazioniResponse.builder().result("OK").viewType("personal").metricsPersonali(metrics).build();
    }

    // ─── Metrics builders ─────────────────────────────────────────────────────

    private ValutazioniPersonaliDto buildPersonaliMetrics(Map user, String ruolo) {
        Long userId = longVal(user, "id");
        Integer userIdInt = intVal(user, "id");
        String codiceIstat = strVal(user, "codiceIstat");

        // Obiettivi personali
        List<Map> obiettivi = coreApiClient.getObiettivi(null, userId);
        int obTotali = obiettivi.size();
        int obCompletati = countByStato(obiettivi, "COMPLETATO");
        int obInCorso = countByStato(obiettivi, "ATTIVO");
        int obScaduti = countByStato(obiettivi, "SCADUTO");
        double pctObCompletati = obTotali > 0 ? (obCompletati * 100.0 / obTotali) : 0;
        double pctMediaObiettivi = obiettivi.stream()
                .mapToDouble(o -> obiettivoService.computePercentuale(o).doubleValue()).average().orElse(0);

        // Attività assegnate
        List<Map> attivita = coreApiClient.getAttivita(null, userId, null);
        int attTotali = attivita.size();
        int attCompletate = (int) attivita.stream().filter(a -> "COMPLETATA".equals(strVal(a, "stato"))).count();
        int attInCorso = (int) attivita.stream().filter(a -> "IN_CORSO".equals(strVal(a, "stato"))).count();
        int attInRitardo = (int) attivita.stream().filter(a -> isInRitardo(a, LocalDate.now())).count();
        double pctAttCompletate = attTotali > 0 ? (attCompletate * 100.0 / attTotali) : 0;

        // Timesheet
        LocalDate oggi = LocalDate.now();
        LocalDate inizioSettimana = oggi.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate inizioMese = oggi.withDayOfMonth(1);
        List<Map> tsAll = coreApiClient.getTimesheetByUtente(userId, null, null);
        double oreSett = sumOreInRange(tsAll, inizioSettimana, oggi);
        double oreMese = sumOreInRange(tsAll, inizioMese, oggi);
        double oreTotali = tsAll.stream().mapToDouble(e -> safeDouble(bigDecimalVal(e, "oreLavorate"))).sum();
        double oreStimate = attivita.stream().mapToDouble(a -> safeDouble(bigDecimalVal(a, "oreStimate"))).sum();
        double pctOreUtilizzate = oreStimate > 0 ? (oreTotali * 100.0 / oreStimate) : 0;

        Integer score = calcolaScore(obTotali, obCompletati, pctObCompletati, pctMediaObiettivi, attTotali, attCompletate, attInRitardo, pctAttCompletate);

        // Struttura nome: cerca l'utente nelle strutture
        String strutturaNome = strutturaDelUtente(userIdInt, codiceIstat);

        return ValutazioniPersonaliDto.builder()
                .nomeCompleto(strValOrEmpty(user, "nome") + " " + strValOrEmpty(user, "cognome"))
                .ruolo(strValOrEmpty(user, "ruolo"))
                .struttura(strutturaNome)
                .obiettiviAssegnati(obTotali)
                .obiettiviCompletati(obCompletati)
                .obiettiviInCorso(obInCorso)
                .obiettiviScaduti(obScaduti)
                .percentualeObiettiviCompletati(pctObCompletati)
                .percentualeMediaObiettivi(pctMediaObiettivi)
                .attivitaAssegnate(attTotali)
                .attivitaCompletate(attCompletate)
                .attivitaInCorso(attInCorso)
                .attivitaInRitardo(attInRitardo)
                .percentualeAttivitaCompletate(pctAttCompletate)
                .oreLavorateSettimana(oreSett)
                .oreLavorateMese(oreMese)
                .oreLavorateTotali(oreTotali)
                .oreStimate(oreStimate)
                .percentualeOreUtilizzate(pctOreUtilizzate)
                .scorePerformance(score)
                .valutazioneLabel(getLabel(score))
                .obiettiviProgresso(obiettivi.stream().limit(5).map(this::toObiettivoProgressoDto).toList())
                .attivitaPerStato(buildAttivitaPerStato(attivita))
                .oreUltimiGiorni(buildOreUltimiGiorni(tsAll, 7))
                .performanceMensile(buildPerformanceMensile(score))
                .build();
    }

    private ValutazioniStrutturaDto buildStrutturaMetrics(Map struttura, Map responsabile) {
        String codiceIstat = strVal(responsabile, "codiceIstat");
        Integer responsabileId = intVal(responsabile, "id");
        Set<Integer> dipendentiIds = getDipendentiFromStruttura(struttura);

        List<Map> obiettivi = new ArrayList<>();
        List<Map> attivita = new ArrayList<>();
        double oreTotali = 0, oreStimate = 0;
        for (Integer dipId : dipendentiIds) {
            obiettivi.addAll(coreApiClient.getObiettivi(null, (long) dipId));
            attivita.addAll(coreApiClient.getAttivita(null, (long) dipId, null));
            List<Map> ts = coreApiClient.getTimesheetByUtente((long) dipId, null, null);
            oreTotali += ts.stream().mapToDouble(e -> safeDouble(bigDecimalVal(e, "oreLavorate"))).sum();
            oreStimate += attivita.stream().mapToDouble(a -> safeDouble(bigDecimalVal(a, "oreStimate"))).sum();
        }
        // Deduplicate activities
        Set<Long> seenIds = new HashSet<>();
        attivita = attivita.stream().filter(a -> seenIds.add(longVal(a, "id"))).collect(Collectors.toList());
        obiettivi = obiettivi.stream().filter(o -> {
            Long id = longVal(o, "id");
            return id != null && seenIds.add(id + 999999999L);
        }).collect(Collectors.toList());

        return buildStrutturaDto(struttura, responsabile, dipendentiIds, obiettivi, attivita, oreTotali, oreStimate, codiceIstat, false);
    }

    private ValutazioniStrutturaDto buildStrutturaMetricsAdmin(String codiceIstat) {
        List<Map> users = coreApiClient.getUsersByCodiceIstat(codiceIstat);
        List<Map> obiettivi = coreApiClient.getObiettivi(codiceIstat, null);
        List<Map> attivita = coreApiClient.getAttivita(null, null, null); // all activities

        double oreTotali = 0, oreStimate = 0;
        for (Map u : users) {
            Long uid = longVal(u, "id");
            if (uid == null) continue;
            List<Map> ts = coreApiClient.getTimesheetByUtente(uid, null, null);
            oreTotali += ts.stream().mapToDouble(e -> safeDouble(bigDecimalVal(e, "oreLavorate"))).sum();
            oreStimate += coreApiClient.getAttivita(null, uid, null).stream()
                    .mapToDouble(a -> safeDouble(bigDecimalVal(a, "oreStimate"))).sum();
        }

        Set<Integer> userIds = users.stream().map(u -> intVal(u, "id")).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, Object> fakeStruttura = Map.of("id", 0, "nome", "Ente");
        Map<String, Object> fakeResp = Map.of("nome", "Amministratore", "cognome", "");

        int obTotali = obiettivi.size();
        int obCompletati = countByStato(obiettivi, "COMPLETATO");
        int obInCorso = countByStato(obiettivi, "ATTIVO");
        int obScaduti = countByStato(obiettivi, "SCADUTO");
        double pctOb = obTotali > 0 ? (obCompletati * 100.0 / obTotali) : 0;
        double pctMediaOb = obiettivi.stream().mapToDouble(o -> obiettivoService.computePercentuale(o).doubleValue()).average().orElse(0);
        int attTotali = attivita.size();
        int attCompletate = (int) attivita.stream().filter(a -> "COMPLETATA".equals(strVal(a, "stato"))).count();
        int attInCorso = (int) attivita.stream().filter(a -> "IN_CORSO".equals(strVal(a, "stato"))).count();
        int attInRitardo = (int) attivita.stream().filter(a -> isInRitardo(a, LocalDate.now())).count();
        double pctAtt = attTotali > 0 ? (attCompletate * 100.0 / attTotali) : 0;
        double pctOre = oreStimate > 0 ? (oreTotali * 100.0 / oreStimate) : 0;
        Integer score = calcolaScore(obTotali, obCompletati, pctOb, pctMediaOb, attTotali, attCompletate, attInRitardo, pctAtt);

        List<DipendentePerformanceDto> perfDipendenti = users.stream().limit(20)
                .map(u -> toDipendentePerformanceDto(u, codiceIstat)).toList();

        return ValutazioniStrutturaDto.builder()
                .strutturaId(0).strutturaNome("Ente").responsabileNome("Amministratore")
                .numeroDipendenti(users.size())
                .obiettiviTotali(obTotali).obiettiviCompletati(obCompletati)
                .obiettiviInCorso(obInCorso).obiettiviScaduti(obScaduti)
                .percentualeObiettiviCompletati(pctOb).percentualeMediaObiettivi(pctMediaOb)
                .attivitaTotali(attTotali).attivitaCompletate(attCompletate)
                .attivitaInCorso(attInCorso).attivitaInRitardo(attInRitardo)
                .percentualeAttivitaCompletate(pctAtt)
                .oreLavorateTotali(oreTotali).oreStimate(oreStimate)
                .percentualeOreUtilizzate(pctOre)
                .scorePerformance(score).valutazioneLabel(getLabel(score))
                .performanceDipendenti(perfDipendenti)
                .obiettiviProgresso(obiettivi.stream().limit(10).map(this::toObiettivoProgressoDto).toList())
                .attivitaPerStato(buildAttivitaPerStato(attivita))
                .oreUltimiGiorni(buildOreUltimiGiorniStruttura(userIds, 7))
                .performanceMensile(buildPerformanceMensileStruttura())
                .build();
    }

    private ValutazioniStrutturaDto buildStrutturaDto(Map struttura, Map responsabile,
            Set<Integer> dipendentiIds, List<Map> obiettivi, List<Map> attivita,
            double oreTotali, double oreStimate, String codiceIstat, boolean isAdmin) {

        int obTotali = obiettivi.size();
        int obCompletati = countByStato(obiettivi, "COMPLETATO");
        int obInCorso = countByStato(obiettivi, "ATTIVO");
        int obScaduti = countByStato(obiettivi, "SCADUTO");
        double pctOb = obTotali > 0 ? (obCompletati * 100.0 / obTotali) : 0;
        double pctMediaOb = obiettivi.stream().mapToDouble(o -> obiettivoService.computePercentuale(o).doubleValue()).average().orElse(0);
        int attTotali = attivita.size();
        int attCompletate = (int) attivita.stream().filter(a -> "COMPLETATA".equals(strVal(a, "stato"))).count();
        int attInCorso = (int) attivita.stream().filter(a -> "IN_CORSO".equals(strVal(a, "stato"))).count();
        int attInRitardo = (int) attivita.stream().filter(a -> isInRitardo(a, LocalDate.now())).count();
        double pctAtt = attTotali > 0 ? (attCompletate * 100.0 / attTotali) : 0;
        double pctOre = oreStimate > 0 ? (oreTotali * 100.0 / oreStimate) : 0;
        Integer score = calcolaScore(obTotali, obCompletati, pctOb, pctMediaOb, attTotali, attCompletate, attInRitardo, pctAtt);

        List<Map> dipendentiUsers = dipendentiIds.stream()
                .map(id -> coreApiClient.getUserById((long) id).orElse(null))
                .filter(Objects::nonNull).toList();
        List<DipendentePerformanceDto> perfDipendenti = dipendentiUsers.stream()
                .map(u -> toDipendentePerformanceDto(u, codiceIstat)).toList();

        String strutturaId = strVal(struttura, "id");
        String strutturaNome = strVal(struttura, "nome");
        String respNome = strValOrEmpty(responsabile, "nome") + " " + strValOrEmpty(responsabile, "cognome");

        return ValutazioniStrutturaDto.builder()
                .strutturaId(strutturaId != null ? Integer.parseInt(strutturaId) : 0)
                .strutturaNome(strutturaNome)
                .responsabileNome(respNome.trim())
                .numeroDipendenti(dipendentiUsers.size())
                .obiettiviTotali(obTotali).obiettiviCompletati(obCompletati)
                .obiettiviInCorso(obInCorso).obiettiviScaduti(obScaduti)
                .percentualeObiettiviCompletati(pctOb).percentualeMediaObiettivi(pctMediaOb)
                .attivitaTotali(attTotali).attivitaCompletate(attCompletate)
                .attivitaInCorso(attInCorso).attivitaInRitardo(attInRitardo)
                .percentualeAttivitaCompletate(pctAtt)
                .oreLavorateTotali(oreTotali).oreStimate(oreStimate)
                .percentualeOreUtilizzate(pctOre)
                .scorePerformance(score).valutazioneLabel(getLabel(score))
                .performanceDipendenti(perfDipendenti)
                .obiettiviProgresso(obiettivi.stream().limit(10).map(this::toObiettivoProgressoDto).toList())
                .attivitaPerStato(buildAttivitaPerStato(attivita))
                .oreUltimiGiorni(buildOreUltimiGiorniStruttura(dipendentiIds, 7))
                .performanceMensile(buildPerformanceMensileStruttura())
                .build();
    }

    private ValutazioniDipendenteDto buildDipendenteMetrics(Map user) {
        Long userId = longVal(user, "id");
        Integer userIdInt = intVal(user, "id");

        List<Map> obiettivi = coreApiClient.getObiettivi(null, userId);
        List<Map> attivita = coreApiClient.getAttivita(null, userId, null);
        List<Map> tsAll = coreApiClient.getTimesheetByUtente(userId, null, null);

        int obTotali = obiettivi.size();
        int obCompletati = countByStato(obiettivi, "COMPLETATO");
        int obInCorso = countByStato(obiettivi, "ATTIVO");
        int obScaduti = countByStato(obiettivi, "SCADUTO");
        double pctOb = obTotali > 0 ? (obCompletati * 100.0 / obTotali) : 0;
        double pctMediaOb = obiettivi.stream().mapToDouble(o -> obiettivoService.computePercentuale(o).doubleValue()).average().orElse(0);

        int attTotali = attivita.size();
        int attCompletate = (int) attivita.stream().filter(a -> "COMPLETATA".equals(strVal(a, "stato"))).count();
        int attInCorso = (int) attivita.stream().filter(a -> "IN_CORSO".equals(strVal(a, "stato"))).count();
        int attInRitardo = (int) attivita.stream().filter(a -> isInRitardo(a, LocalDate.now())).count();
        double pctAtt = attTotali > 0 ? (attCompletate * 100.0 / attTotali) : 0;

        LocalDate oggi = LocalDate.now();
        LocalDate inizioSett = oggi.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate inizioMese = oggi.withDayOfMonth(1);
        double oreSett = sumOreInRange(tsAll, inizioSett, oggi);
        double oreMese = sumOreInRange(tsAll, inizioMese, oggi);
        double oreTotaliVal = tsAll.stream().mapToDouble(e -> safeDouble(bigDecimalVal(e, "oreLavorate"))).sum();
        double oreStimate = attivita.stream().mapToDouble(a -> safeDouble(bigDecimalVal(a, "oreStimate"))).sum();
        double pctOre = oreStimate > 0 ? (oreTotaliVal * 100.0 / oreStimate) : 0;

        Integer score = calcolaScore(obTotali, obCompletati, pctOb, pctMediaOb, attTotali, attCompletate, attInRitardo, pctAtt);

        return ValutazioniDipendenteDto.builder()
                .utenteId(userIdInt)
                .nomeCompleto(strValOrEmpty(user, "nome") + " " + strValOrEmpty(user, "cognome"))
                .email(strVal(user, "email"))
                .ruolo(strVal(user, "ruolo"))
                .obiettiviAssegnati(obTotali).obiettiviCompletati(obCompletati)
                .obiettiviInCorso(obInCorso).obiettiviScaduti(obScaduti)
                .percentualeObiettiviCompletati(pctOb).percentualeMediaObiettivi(pctMediaOb)
                .attivitaAssegnate(attTotali).attivitaCompletate(attCompletate)
                .attivitaInCorso(attInCorso).attivitaInRitardo(attInRitardo)
                .percentualeAttivitaCompletate(pctAtt)
                .oreLavorateSettimana(oreSett).oreLavorateMese(oreMese)
                .oreLavorateTotali(oreTotaliVal).oreStimate(oreStimate)
                .percentualeOreUtilizzate(pctOre)
                .scorePerformance(score).valutazioneLabel(getLabel(score))
                .obiettiviProgresso(obiettivi.stream().limit(5).map(this::toObiettivoProgressoDto).toList())
                .attivitaPerStato(buildAttivitaPerStato(attivita))
                .oreUltimiGiorni(buildOreUltimiGiorni(tsAll, 7))
                .performanceMensile(buildPerformanceMensile(score))
                .build();
    }

    // ─── Chart/select helpers ─────────────────────────────────────────────────

    private DipendentePerformanceDto toDipendentePerformanceDto(Map user, String codiceIstat) {
        Long uid = longVal(user, "id");
        List<Map> obiettivi = coreApiClient.getObiettivi(null, uid);
        List<Map> attivita = coreApiClient.getAttivita(null, uid, null);
        int obCompletati = countByStato(obiettivi, "COMPLETATO");
        int attCompletate = (int) attivita.stream().filter(a -> "COMPLETATA".equals(strVal(a, "stato"))).count();
        int attInRitardo = (int) attivita.stream().filter(a -> isInRitardo(a, LocalDate.now())).count();
        double pctOb = obiettivi.size() > 0 ? (obCompletati * 100.0 / obiettivi.size()) : 0;
        double pctAtt = attivita.size() > 0 ? (attCompletate * 100.0 / attivita.size()) : 0;
        double pctMediaOb = obiettivi.stream().mapToDouble(o -> obiettivoService.computePercentuale(o).doubleValue()).average().orElse(0);
        Integer score = calcolaScore(obiettivi.size(), obCompletati, pctOb, pctMediaOb, attivita.size(), attCompletate, attInRitardo, pctAtt);
        return DipendentePerformanceDto.builder()
                .utenteId(intVal(user, "id"))
                .nomeCompleto(strValOrEmpty(user, "nome") + " " + strValOrEmpty(user, "cognome"))
                .ruolo(strVal(user, "ruolo"))
                .obiettiviCompletati(obCompletati).obiettiviTotali(obiettivi.size())
                .attivitaCompletate(attCompletate).attivitaTotali(attivita.size())
                .scorePerformance(score).valutazioneLabel(getLabel(score))
                .build();
    }

    private List<DipendenteSelectDto> getAllDipendentiSelect(String codiceIstat) {
        return coreApiClient.getUsersByCodiceIstat(codiceIstat).stream()
                .map(u -> {
                    Long uid = longVal(u, "id");
                    List<Map> obiettivi = coreApiClient.getObiettivi(null, uid);
                    List<Map> attivita = coreApiClient.getAttivita(null, uid, null);
                    int obC = countByStato(obiettivi, "COMPLETATO");
                    int attC = (int) attivita.stream().filter(a -> "COMPLETATA".equals(strVal(a, "stato"))).count();
                    int attR = (int) attivita.stream().filter(a -> isInRitardo(a, LocalDate.now())).count();
                    double pctOb = obiettivi.size() > 0 ? (obC * 100.0 / obiettivi.size()) : 0;
                    double pctAtt = attivita.size() > 0 ? (attC * 100.0 / attivita.size()) : 0;
                    double pctMOb = obiettivi.stream().mapToDouble(o -> obiettivoService.computePercentuale(o).doubleValue()).average().orElse(0);
                    Integer score = calcolaScore(obiettivi.size(), obC, pctOb, pctMOb, attivita.size(), attC, attR, pctAtt);
                    return DipendenteSelectDto.builder()
                            .id(intVal(u, "id")).nomeCompleto(strValOrEmpty(u, "nome") + " " + strValOrEmpty(u, "cognome"))
                            .email(strVal(u, "email")).ruolo(strVal(u, "ruolo")).scorePerformance(score).build();
                }).toList();
    }

    private List<DipendenteSelectDto> getDipendentiSelectStruttura(Map struttura, Integer responsabileId) {
        Set<Integer> dipIds = getDipendentiFromStruttura(struttura);
        return dipIds.stream()
                .map(id -> coreApiClient.getUserById((long) id).orElse(null))
                .filter(Objects::nonNull)
                .map(u -> {
                    Long uid = longVal(u, "id");
                    Integer score = calculateUserScore(uid);
                    return DipendenteSelectDto.builder()
                            .id(intVal(u, "id")).nomeCompleto(strValOrEmpty(u, "nome") + " " + strValOrEmpty(u, "cognome"))
                            .email(strVal(u, "email")).ruolo(strVal(u, "ruolo")).scorePerformance(score).build();
                }).toList();
    }

    private Integer calculateUserScore(Long userId) {
        List<Map> obiettivi = coreApiClient.getObiettivi(null, userId);
        List<Map> attivita = coreApiClient.getAttivita(null, userId, null);
        int obC = countByStato(obiettivi, "COMPLETATO");
        int attC = (int) attivita.stream().filter(a -> "COMPLETATA".equals(strVal(a, "stato"))).count();
        int attR = (int) attivita.stream().filter(a -> isInRitardo(a, LocalDate.now())).count();
        double pctO = obiettivi.size() > 0 ? (obC * 100.0 / obiettivi.size()) : 0;
        double pctA = attivita.size() > 0 ? (attC * 100.0 / attivita.size()) : 0;
        double pmO = obiettivi.stream().mapToDouble(o -> obiettivoService.computePercentuale(o).doubleValue()).average().orElse(0);
        return calcolaScore(obiettivi.size(), obC, pctO, pmO, attivita.size(), attC, attR, pctA);
    }

    private Set<Integer> getDipendentiStruttura(Integer responsabileId, String codiceIstat) {
        List<Map> strutture = coreApiClient.getStrutture(codiceIstat);
        return strutture.stream()
                .filter(s -> Objects.equals(intVal(s, "idResponsabile"), responsabileId))
                .findFirst()
                .map(this::getDipendentiFromStruttura)
                .orElse(Set.of());
    }

    private Set<Integer> getDipendentiFromStruttura(Map struttura) {
        Set<Integer> ids = new HashSet<>();
        Object staffObj = struttura.get("staff");
        if (staffObj instanceof List staffList) {
            for (Object s : staffList) {
                if (s instanceof Map staffMap) {
                    Integer uid = intVal(staffMap, "idUser");
                    if (uid != null) ids.add(uid);
                }
            }
        }
        return ids;
    }

    private String strutturaDelUtente(Integer userId, String codiceIstat) {
        List<Map> strutture = coreApiClient.getStrutture(codiceIstat);
        for (Map s : strutture) {
            if (Objects.equals(intVal(s, "idResponsabile"), userId)) return strValOrEmpty(s, "nome");
            Object staffObj = s.get("staff");
            if (staffObj instanceof List sl) {
                for (Object st : sl) {
                    if (st instanceof Map sm && Objects.equals(intVal(sm, "idUser"), userId)) return strValOrEmpty(s, "nome");
                }
            }
        }
        return "Non assegnato";
    }

    // ─── Chart builders ───────────────────────────────────────────────────────

    private ObiettivoProgressoDto toObiettivoProgressoDto(Map o) {
        String stato = strValOrEmpty(o, "stato");
        BigDecimal target = bigDecimalVal(o, "valoreTarget");
        BigDecimal curr = bigDecimalVal(o, "valoreCorrente");
        BigDecimal init = bigDecimalVal(o, "valoreIniziale");
        BigDecimal peso = bigDecimalVal(o, "peso");
        return ObiettivoProgressoDto.builder()
                .id(longVal(o, "id"))
                .titolo(strVal(o, "titolo"))
                .stato(stato)
                .valoreIniziale(init != null ? init.doubleValue() : 0)
                .valoreTarget(target != null ? target.doubleValue() : 0)
                .valoreCorrente(curr != null ? curr.doubleValue() : 0)
                .percentuale(obiettivoService.computePercentuale(o).doubleValue())
                .peso(peso != null ? peso.intValue() : 100)
                .build();
    }

    private List<AttivitaPerStatoDto> buildAttivitaPerStato(List<Map> attivita) {
        Map<String, Long> m = attivita.stream()
                .collect(Collectors.groupingBy(a -> strValOrEmpty(a, "stato"), Collectors.counting()));
        return m.entrySet().stream()
                .map(e -> AttivitaPerStatoDto.builder().stato(e.getKey()).count(e.getValue().intValue()).build())
                .toList();
    }

    private List<OreLavorateGiornoDto> buildOreUltimiGiorni(List<Map> timesheet, int giorni) {
        LocalDate oggi = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        Map<String, BigDecimal> oreMap = new LinkedHashMap<>();
        for (int i = giorni - 1; i >= 0; i--) oreMap.put(oggi.minusDays(i).format(fmt), BigDecimal.ZERO);
        for (Map e : timesheet) {
            String ds = strVal(e, "data");
            if (ds == null) continue;
            String key = ds.length() >= 10 ? ds.substring(0, 10) : ds;
            if (oreMap.containsKey(key)) {
                BigDecimal ore = bigDecimalVal(e, "oreLavorate");
                if (ore != null) oreMap.merge(key, ore, BigDecimal::add);
            }
        }
        return oreMap.entrySet().stream()
                .map(e -> OreLavorateGiornoDto.builder().data(e.getKey()).ore(e.getValue().doubleValue()).build())
                .toList();
    }

    private List<OreLavorateGiornoDto> buildOreUltimiGiorniStruttura(Set<Integer> userIds, int giorni) {
        LocalDate oggi = LocalDate.now();
        String from = oggi.minusDays(giorni - 1).toString();
        Map<String, BigDecimal> oreMap = new LinkedHashMap<>();
        for (int i = giorni - 1; i >= 0; i--) oreMap.put(oggi.minusDays(i).toString(), BigDecimal.ZERO);
        for (Integer uid : userIds) {
            List<Map> ts = coreApiClient.getTimesheetByUtente((long) uid, from, oggi.toString());
            for (Map e : ts) {
                String ds = strVal(e, "data");
                if (ds == null) continue;
                String key = ds.length() >= 10 ? ds.substring(0, 10) : ds;
                if (oreMap.containsKey(key)) {
                    BigDecimal ore = bigDecimalVal(e, "oreLavorate");
                    if (ore != null) oreMap.merge(key, ore, BigDecimal::add);
                }
            }
        }
        return oreMap.entrySet().stream()
                .map(e -> OreLavorateGiornoDto.builder().data(e.getKey()).ore(e.getValue().doubleValue()).build())
                .toList();
    }

    private List<PerformanceMensileDto> buildPerformanceMensile(Integer score) {
        LocalDate oggi = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        List<PerformanceMensileDto> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            result.add(PerformanceMensileDto.builder()
                    .mese(oggi.minusMonths(i).withDayOfMonth(1).format(fmt))
                    .score(score != null ? score : 0).percentualeObiettivi(0).percentualeAttivita(0).build());
        }
        return result;
    }

    private List<PerformanceMensileDto> buildPerformanceMensileStruttura() {
        LocalDate oggi = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        List<PerformanceMensileDto> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            result.add(PerformanceMensileDto.builder()
                    .mese(oggi.minusMonths(i).withDayOfMonth(1).format(fmt))
                    .score(70).percentualeObiettivi(0).percentualeAttivita(0).build());
        }
        return result;
    }

    // ─── Score ────────────────────────────────────────────────────────────────

    private Integer calcolaScore(int obTotali, int obCompletati, double pctObCompletati, double pctMediaOb,
                                  int attTotali, int attCompletate, int attInRitardo, double pctAttCompletate) {
        boolean haOb = obTotali > 0;
        boolean haAtt = attTotali > 0;
        if (!haOb && !haAtt) return null;
        double score = 0, peso = 0;
        if (haOb) { score += pctObCompletati * 0.5 + pctMediaOb * 0.5; peso += 1.0; }
        if (haAtt) { score += Math.max(0, pctAttCompletate - Math.min(attInRitardo * 2.0, 10.0)); peso += 1.0; }
        int s = (int) Math.round(score / peso);
        return Math.max(0, Math.min(100, s));
    }

    private String getLabel(Integer s) {
        if (s == null) return "N.D.";
        if (s >= 90) return "ECCELLENTE";
        if (s >= 75) return "BUONA";
        if (s >= 60) return "SUFFICIENTE";
        if (s >= 40) return "DA_MIGLIORARE";
        return "INSUFFICIENTE";
    }

    // ─── Utilities ───────────────────────────────────────────────────────────

    private boolean isAdminRole(String r) { return r != null && List.of("AD", "SC", "DR").contains(r); }
    private boolean isResponsabileRole(String r) { return r != null && List.of("CS", "CP").contains(r); }
    private boolean isInRitardo(Map a, LocalDate oggi) {
        if ("COMPLETATA".equals(strVal(a, "stato"))) return false;
        String ds = strVal(a, "dataFineStimata");
        if (ds == null) return false;
        try { return LocalDate.parse(ds.substring(0, 10)).isBefore(oggi); } catch (Exception e) { return false; }
    }
    private int countByStato(List<Map> list, String stato) {
        return (int) list.stream().filter(o -> stato.equals(strVal(o, "stato"))).count();
    }
    private double sumOreInRange(List<Map> ts, LocalDate from, LocalDate to) {
        return ts.stream().filter(e -> {
            String ds = strVal(e, "data");
            if (ds == null) return false;
            try { LocalDate d = LocalDate.parse(ds.substring(0, 10)); return !d.isBefore(from) && !d.isAfter(to); } catch (Exception ex) { return false; }
        }).mapToDouble(e -> safeDouble(bigDecimalVal(e, "oreLavorate"))).sum();
    }
    private static double safeDouble(BigDecimal v) { return v != null ? v.doubleValue() : 0.0; }
    private String strVal(Map<?, ?> m, String k) { Object v = m.get(k); return v != null ? v.toString() : null; }
    private String strValOrEmpty(Map<?, ?> m, String k) { Object v = m.get(k); return v != null ? v.toString() : ""; }
    private Integer intVal(Map<?, ?> m, String k) {
        Object v = m.get(k); if (v == null) return null;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }
    private Long longVal(Map<?, ?> m, String k) {
        Object v = m.get(k); if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }
    private BigDecimal bigDecimalVal(Map<?, ?> m, String k) {
        Object v = m.get(k); if (v == null) return null;
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; }
    }
}
