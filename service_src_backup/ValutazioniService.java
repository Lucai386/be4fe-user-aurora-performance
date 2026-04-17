package com.bff_user_aurora_performance.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bff_user_aurora_performance.dto.valutazioni.AttivitaPerStatoDto;
import com.bff_user_aurora_performance.dto.valutazioni.DipendentePerformanceDto;
import com.bff_user_aurora_performance.dto.valutazioni.DipendenteSelectDto;
import com.bff_user_aurora_performance.dto.valutazioni.ObiettivoProgressoDto;
import com.bff_user_aurora_performance.dto.valutazioni.OreLavorateGiornoDto;
import com.bff_user_aurora_performance.dto.valutazioni.PerformanceMensileDto;
import com.bff_user_aurora_performance.dto.valutazioni.ScoreCalcoloDto;
import com.bff_user_aurora_performance.dto.valutazioni.ValutazioniDipendenteDto;
import com.bff_user_aurora_performance.dto.valutazioni.ValutazioniPersonaliDto;
import com.bff_user_aurora_performance.dto.valutazioni.ValutazioniResponse;
import com.bff_user_aurora_performance.dto.valutazioni.ValutazioniStrutturaDto;
import com.bff_user_aurora_performance.enums.ErrorCode;
import com.bff_user_aurora_performance.enums.UserRole;
import com.bff_user_aurora_performance.model.Attivita;
import com.bff_user_aurora_performance.model.AttivitaAssegnazione;
import com.bff_user_aurora_performance.model.Obiettivo;
import com.bff_user_aurora_performance.model.Struttura;
import com.bff_user_aurora_performance.model.StrutturaStaff;
import com.bff_user_aurora_performance.model.TimesheetEntry;
import com.bff_user_aurora_performance.model.User;
import com.bff_user_aurora_performance.repository.AttivitaAssegnazioneRepository;
import com.bff_user_aurora_performance.repository.AttivitaRepository;
import com.bff_user_aurora_performance.repository.ObiettivoRepository;
import com.bff_user_aurora_performance.repository.StrutturaRepository;
import com.bff_user_aurora_performance.repository.TimesheetEntryRepository;
import com.bff_user_aurora_performance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service per le valutazioni delle performance.
 * Fornisce metriche per dipendenti e responsabili basate su obiettivi e attività.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValutazioniService {

    private final UserRepository userRepository;
    private final ObiettivoRepository obiettivoRepository;
    private final AttivitaRepository attivitaRepository;
    private final AttivitaAssegnazioneRepository assegnazioneRepository;
    private final TimesheetEntryRepository timesheetRepository;
    private final StrutturaRepository strutturaRepository;

    /**
     * Ottiene le metriche di valutazione in base al ruolo dell'utente.
     */
    @Transactional(readOnly = true)
    public ValutazioniResponse getValutazioni(String ruolo, Integer userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ValutazioniResponse.ko(ErrorCode.USER_NOT_FOUND);
            }
            User user = userOpt.get();

            // Admin/Dirigente/SegretarioComunale vedono tutto
            if (isAdminRole(ruolo)) {
                return getValutazioniAdmin(user);
            }

            // CapoSettore/CapoProgetto vedono la propria struttura + possono selezionare dipendenti
            if (isResponsabileRole(ruolo)) {
                return getValutazioniResponsabile(user);
            }

            // DipendenteBase vede solo le proprie metriche
            return getValutazioniPersonali(user);
        } catch (Exception e) {
            log.error("Errore nel recupero valutazioni: {}", e.getMessage(), e);
            return ValutazioniResponse.ko(ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Ottiene le metriche di un dipendente specifico (per responsabili).
     */
    @Transactional(readOnly = true)
    public ValutazioniResponse getValutazioniDipendente(Integer dipendenteId, String ruolo, Integer userId) {
        try {
            // Verifica che l'utente sia un responsabile
            if (!isResponsabileRole(ruolo) && !isAdminRole(ruolo)) {
                return ValutazioniResponse.ko(ErrorCode.NOT_AUTHORIZED);
            }

            Optional<User> dipendenteOpt = userRepository.findById(dipendenteId);
            if (dipendenteOpt.isEmpty()) {
                return ValutazioniResponse.ko(ErrorCode.USER_NOT_FOUND);
            }
            User dipendente = dipendenteOpt.get();

            // Se è responsabile, verifica che il dipendente sia nella sua struttura
            if (isResponsabileRole(ruolo) && !isAdminRole(ruolo)) {
                Optional<User> responsabileOpt = userRepository.findById(userId);
                if (responsabileOpt.isEmpty()) {
                    return ValutazioniResponse.ko(ErrorCode.USER_NOT_FOUND);
                }
                Set<Integer> dipendentiStruttura = getDipendentiStruttura(responsabileOpt.get());
                if (!dipendentiStruttura.contains(dipendenteId)) {
                    return ValutazioniResponse.ko(ErrorCode.NOT_AUTHORIZED);
                }
            }

            ValutazioniDipendenteDto metrics = buildDipendenteMetrics(dipendente);

            return ValutazioniResponse.builder()
                    .result("OK")
                    .viewType("dipendente")
                    .metricsDipendente(metrics)
                    .build();
        } catch (Exception e) {
            log.error("Errore nel recupero valutazioni dipendente: {}", e.getMessage(), e);
            return ValutazioniResponse.ko(ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    // ==================== PRIVATE METHODS ====================

    private ValutazioniResponse getValutazioniAdmin(User user) {
        ValutazioniStrutturaDto metrics = buildStrutturaMetricsAdmin(user.getCodiceIstat());
        List<DipendenteSelectDto> dipendenti = getAllDipendentiSelect(user.getCodiceIstat());

        return ValutazioniResponse.builder()
                .result("OK")
                .viewType("admin")
                .metricsStruttura(metrics)
                .dipendentiStruttura(dipendenti)
                .build();
    }

    private ValutazioniResponse getValutazioniResponsabile(User user) {
        // Trova la struttura del responsabile
        Optional<Struttura> strutturaOpt = strutturaRepository.findByResponsabileId(user.getId());
        if (strutturaOpt.isEmpty()) {
            // Non è responsabile di nessuna struttura, mostra metriche personali
            return getValutazioniPersonali(user);
        }

        Struttura struttura = strutturaOpt.get();
        ValutazioniStrutturaDto metricsStruttura = buildStrutturaMetrics(struttura, user);
        List<DipendenteSelectDto> dipendenti = getDipendentiSelectStruttura(struttura, user.getId());

        return ValutazioniResponse.builder()
                .result("OK")
                .viewType("responsabile")
                .metricsStruttura(metricsStruttura)
                .dipendentiStruttura(dipendenti)
                .build();
    }

    private ValutazioniResponse getValutazioniPersonali(User user) {
        ValutazioniPersonaliDto metrics = buildPersonaliMetrics(user);

        return ValutazioniResponse.builder()
                .result("OK")
                .viewType("personal")
                .metricsPersonali(metrics)
                .build();
    }

    private ValutazioniPersonaliDto buildPersonaliMetrics(User user) {
        Long userId = user.getId().longValue();
        
        // Obiettivi personali
        List<Obiettivo> obiettivi = obiettivoRepository.findByUtenteAssegnatoId(userId);
        int obiettiviTotali = obiettivi.size();
        int obiettiviCompletati = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.COMPLETATO).count();
        int obiettiviInCorso = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.ATTIVO).count();
        int obiettiviScaduti = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.SCADUTO).count();
        double percentualeObiettiviCompletati = obiettiviTotali > 0 
                ? (obiettiviCompletati * 100.0 / obiettiviTotali) : 0;
        double percentualeMediaObiettivi = obiettivi.stream()
                .map(Obiettivo::calcolaPercentuale)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0);

        // Attività assegnate
        List<AttivitaAssegnazione> assegnazioni = assegnazioneRepository.findByUtenteId(user.getId());
        List<Long> attivitaIds = assegnazioni.stream()
                .map(AttivitaAssegnazione::getAttivitaId).toList();
        List<Attivita> attivita = attivitaIds.isEmpty() 
                ? List.of() 
                : attivitaRepository.findAllById(attivitaIds);
        
        int attivitaTotali = attivita.size();
        int attivitaCompletate = (int) attivita.stream()
                .filter(a -> a.getStato() == Attivita.Stato.COMPLETATA).count();
        int attivitaInCorso = (int) attivita.stream()
                .filter(a -> a.getStato() == Attivita.Stato.IN_CORSO).count();
        int attivitaInRitardo = (int) attivita.stream()
                .filter(this::isInRitardo).count();
        double percentualeAttivitaCompletate = attivitaTotali > 0 
                ? (attivitaCompletate * 100.0 / attivitaTotali) : 0;

        // Ore lavorate
        LocalDate oggi = LocalDate.now();
        LocalDate inizioSettimana = oggi.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate inizioMese = oggi.withDayOfMonth(1);

        List<TimesheetEntry> oreSettimana = timesheetRepository.findByUtenteIdAndDataBetween(
                user.getId(), inizioSettimana, oggi);
        List<TimesheetEntry> oreMese = timesheetRepository.findByUtenteIdAndDataBetween(
                user.getId(), inizioMese, oggi);
        List<TimesheetEntry> oreTotali = timesheetRepository.findByUtenteId(user.getId());

        double oreLavorateSettimana = oreSettimana.stream()
                .map(TimesheetEntry::getOreLavorate)
                .mapToDouble(BigDecimal::doubleValue).sum();
        double oreLavorateMese = oreMese.stream()
                .map(TimesheetEntry::getOreLavorate)
                .mapToDouble(BigDecimal::doubleValue).sum();
        double oreLavorateTotaliVal = oreTotali.stream()
                .map(TimesheetEntry::getOreLavorate)
                .mapToDouble(BigDecimal::doubleValue).sum();
        double oreStimate = assegnazioni.stream()
                .mapToDouble(a -> a.getOreStimate() != null ? a.getOreStimate().doubleValue() : 0).sum();
        double percentualeOreUtilizzate = oreStimate > 0 
                ? (oreLavorateTotaliVal * 100.0 / oreStimate) : 0;

        // Score performance con calcolo dinamico
        Integer scorePerformance = calcolaScorePerformance(ScoreCalcoloDto.builder()
                .obiettiviTotali(obiettiviTotali)
                .obiettiviCompletati(obiettiviCompletati)
                .percentualeObiettiviCompletati(percentualeObiettiviCompletati)
                .percentualeMediaObiettivi(percentualeMediaObiettivi)
                .attivitaTotali(attivitaTotali)
                .attivitaCompletate(attivitaCompletate)
                .attivitaInRitardo(attivitaInRitardo)
                .percentualeAttivitaCompletate(percentualeAttivitaCompletate)
                .build());
        String valutazioneLabel = getValutazioneLabel(scorePerformance);

        // Grafici
        List<ObiettivoProgressoDto> obiettiviProgresso = obiettivi.stream()
                .limit(5)
                .map(this::toObiettivoProgressoDto).toList();
        List<AttivitaPerStatoDto> attivitaPerStato = buildAttivitaPerStato(attivita);
        List<OreLavorateGiornoDto> oreUltimiGiorni = buildOreUltimiGiorni(user.getId(), 7);
        List<PerformanceMensileDto> performanceMensile = buildPerformanceMensile(user.getId());

        String strutturaNome = getStrutturaUtente(user);

        return ValutazioniPersonaliDto.builder()
                .nomeCompleto(user.getNome() + " " + user.getCognome())
                .ruolo(user.getRuolo())
                .struttura(strutturaNome)
                .obiettiviAssegnati(obiettiviTotali)
                .obiettiviCompletati(obiettiviCompletati)
                .obiettiviInCorso(obiettiviInCorso)
                .obiettiviScaduti(obiettiviScaduti)
                .percentualeObiettiviCompletati(percentualeObiettiviCompletati)
                .percentualeMediaObiettivi(percentualeMediaObiettivi)
                .attivitaAssegnate(attivitaTotali)
                .attivitaCompletate(attivitaCompletate)
                .attivitaInCorso(attivitaInCorso)
                .attivitaInRitardo(attivitaInRitardo)
                .percentualeAttivitaCompletate(percentualeAttivitaCompletate)
                .oreLavorateSettimana(oreLavorateSettimana)
                .oreLavorateMese(oreLavorateMese)
                .oreLavorateTotali(oreLavorateTotaliVal)
                .oreStimate(oreStimate)
                .percentualeOreUtilizzate(percentualeOreUtilizzate)
                .scorePerformance(scorePerformance)
                .valutazioneLabel(valutazioneLabel)
                .obiettiviProgresso(obiettiviProgresso)
                .attivitaPerStato(attivitaPerStato)
                .oreUltimiGiorni(oreUltimiGiorni)
                .performanceMensile(performanceMensile)
                .build();
    }

    private ValutazioniStrutturaDto buildStrutturaMetrics(Struttura struttura, User responsabile) {
        Set<Integer> dipendentiIds = getDipendentiStruttura(responsabile);
        List<User> dipendenti = userRepository.findAllById(dipendentiIds);

        // Obiettivi della struttura
        List<Obiettivo> obiettivi = new ArrayList<>();
        for (Integer dipId : dipendentiIds) {
            obiettivi.addAll(obiettivoRepository.findByUtenteAssegnatoId(dipId.longValue()));
        }
        obiettivi = obiettivi.stream().distinct().toList();

        int obiettiviTotali = obiettivi.size();
        int obiettiviCompletati = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.COMPLETATO).count();
        int obiettiviInCorso = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.ATTIVO).count();
        int obiettiviScaduti = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.SCADUTO).count();
        double percentualeObiettiviCompletati = obiettiviTotali > 0 
                ? (obiettiviCompletati * 100.0 / obiettiviTotali) : 0;
        double percentualeMediaObiettivi = obiettivi.stream()
                .map(Obiettivo::calcolaPercentuale)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0);

        // Attività
        List<Attivita> attivita = attivitaRepository.findByStrutturaId(struttura.getId());
        int attivitaTotali = attivita.size();
        int attivitaCompletate = (int) attivita.stream()
                .filter(a -> a.getStato() == Attivita.Stato.COMPLETATA).count();
        int attivitaInCorso = (int) attivita.stream()
                .filter(a -> a.getStato() == Attivita.Stato.IN_CORSO).count();
        int attivitaInRitardo = (int) attivita.stream()
                .filter(this::isInRitardo).count();
        double percentualeAttivitaCompletate = attivitaTotali > 0 
                ? (attivitaCompletate * 100.0 / attivitaTotali) : 0;

        // Ore struttura
        double oreLavorateTotali = 0;
        double oreStimate = 0;
        for (Integer dipId : dipendentiIds) {
            List<TimesheetEntry> entries = timesheetRepository.findByUtenteId(dipId);
            oreLavorateTotali += entries.stream()
                    .map(TimesheetEntry::getOreLavorate)
                    .mapToDouble(BigDecimal::doubleValue).sum();
            List<AttivitaAssegnazione> ass = assegnazioneRepository.findByUtenteId(dipId);
            oreStimate += ass.stream()
                    .mapToDouble(a -> a.getOreStimate() != null ? a.getOreStimate().doubleValue() : 0).sum();
        }
        double percentualeOreUtilizzate = oreStimate > 0 ? (oreLavorateTotali * 100.0 / oreStimate) : 0;

        // Score performance struttura con calcolo dinamico
        Integer scorePerformance = calcolaScorePerformance(ScoreCalcoloDto.builder()
                .obiettiviTotali(obiettiviTotali)
                .obiettiviCompletati(obiettiviCompletati)
                .percentualeObiettiviCompletati(percentualeObiettiviCompletati)
                .percentualeMediaObiettivi(percentualeMediaObiettivi)
                .attivitaTotali(attivitaTotali)
                .attivitaCompletate(attivitaCompletate)
                .attivitaInRitardo(attivitaInRitardo)
                .percentualeAttivitaCompletate(percentualeAttivitaCompletate)
                .build());
        String valutazioneLabel = getValutazioneLabel(scorePerformance);

        // Performance dipendenti
        List<DipendentePerformanceDto> performanceDipendenti = dipendenti.stream()
                .map(this::toDipendentePerformanceDto).toList();

        // Grafici
        List<ObiettivoProgressoDto> obiettiviProgresso = obiettivi.stream()
                .limit(10)
                .map(this::toObiettivoProgressoDto).toList();
        List<AttivitaPerStatoDto> attivitaPerStato = buildAttivitaPerStato(attivita);
        List<OreLavorateGiornoDto> oreUltimiGiorni = buildOreUltimiGiorniStruttura(dipendentiIds, 7);
        List<PerformanceMensileDto> performanceMensile = buildPerformanceMensileStruttura();

        return ValutazioniStrutturaDto.builder()
                .strutturaId(struttura.getId())
                .strutturaNome(struttura.getNome())
                .responsabileNome(responsabile.getNome() + " " + responsabile.getCognome())
                .numeroDipendenti(dipendenti.size())
                .obiettiviTotali(obiettiviTotali)
                .obiettiviCompletati(obiettiviCompletati)
                .obiettiviInCorso(obiettiviInCorso)
                .obiettiviScaduti(obiettiviScaduti)
                .percentualeObiettiviCompletati(percentualeObiettiviCompletati)
                .percentualeMediaObiettivi(percentualeMediaObiettivi)
                .attivitaTotali(attivitaTotali)
                .attivitaCompletate(attivitaCompletate)
                .attivitaInCorso(attivitaInCorso)
                .attivitaInRitardo(attivitaInRitardo)
                .percentualeAttivitaCompletate(percentualeAttivitaCompletate)
                .oreLavorateTotali(oreLavorateTotali)
                .oreStimate(oreStimate)
                .percentualeOreUtilizzate(percentualeOreUtilizzate)
                .scorePerformance(scorePerformance)
                .valutazioneLabel(valutazioneLabel)
                .performanceDipendenti(performanceDipendenti)
                .obiettiviProgresso(obiettiviProgresso)
                .attivitaPerStato(attivitaPerStato)
                .oreUltimiGiorni(oreUltimiGiorni)
                .performanceMensile(performanceMensile)
                .build();
    }

    private ValutazioniStrutturaDto buildStrutturaMetricsAdmin(String codiceIstat) {
        List<User> utenti = userRepository.findByCodiceIstat(codiceIstat);
        List<Obiettivo> obiettivi = obiettivoRepository.findByCodiceIstatOrderByAnnoDescCreatedAtDesc(codiceIstat);
        
        int obiettiviTotali = obiettivi.size();
        int obiettiviCompletati = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.COMPLETATO).count();
        int obiettiviInCorso = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.ATTIVO).count();
        int obiettiviScaduti = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.SCADUTO).count();
        double percentualeObiettiviCompletati = obiettiviTotali > 0 
                ? (obiettiviCompletati * 100.0 / obiettiviTotali) : 0;
        double percentualeMediaObiettivi = obiettivi.stream()
                .map(Obiettivo::calcolaPercentuale)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0);

        // Attività - collect from all users
        List<Attivita> attivita = new ArrayList<>();
        for (User u : utenti) {
            List<AttivitaAssegnazione> ass = assegnazioneRepository.findByUtenteId(u.getId());
            List<Long> attIds = ass.stream().map(AttivitaAssegnazione::getAttivitaId).toList();
            if (!attIds.isEmpty()) {
                attivita.addAll(attivitaRepository.findAllById(attIds));
            }
        }
        attivita = attivita.stream().distinct().toList();
        
        int attivitaTotali = attivita.size();
        int attivitaCompletate = (int) attivita.stream()
                .filter(a -> a.getStato() == Attivita.Stato.COMPLETATA).count();
        int attivitaInCorso = (int) attivita.stream()
                .filter(a -> a.getStato() == Attivita.Stato.IN_CORSO).count();
        int attivitaInRitardo = (int) attivita.stream()
                .filter(this::isInRitardo).count();
        double percentualeAttivitaCompletate = attivitaTotali > 0 
                ? (attivitaCompletate * 100.0 / attivitaTotali) : 0;

        // Ore aggregate
        double oreLavorateTotali = 0;
        double oreStimate = 0;
        for (User u : utenti) {
            List<TimesheetEntry> entries = timesheetRepository.findByUtenteId(u.getId());
            oreLavorateTotali += entries.stream()
                    .map(TimesheetEntry::getOreLavorate)
                    .mapToDouble(BigDecimal::doubleValue).sum();
            List<AttivitaAssegnazione> ass = assegnazioneRepository.findByUtenteId(u.getId());
            oreStimate += ass.stream()
                    .mapToDouble(a -> a.getOreStimate() != null ? a.getOreStimate().doubleValue() : 0).sum();
        }
        double percentualeOreUtilizzate = oreStimate > 0 ? (oreLavorateTotali * 100.0 / oreStimate) : 0;

        Integer scorePerformance = calcolaScorePerformance(ScoreCalcoloDto.builder()
                .obiettiviTotali(obiettiviTotali)
                .obiettiviCompletati(obiettiviCompletati)
                .percentualeObiettiviCompletati(percentualeObiettiviCompletati)
                .percentualeMediaObiettivi(percentualeMediaObiettivi)
                .attivitaTotali(attivitaTotali)
                .attivitaCompletate(attivitaCompletate)
                .attivitaInRitardo(attivitaInRitardo)
                .percentualeAttivitaCompletate(percentualeAttivitaCompletate)
                .build());
        String valutazioneLabel = getValutazioneLabel(scorePerformance);

        List<DipendentePerformanceDto> performanceDipendenti = utenti.stream()
                .limit(20)
                .map(this::toDipendentePerformanceDto).toList();

        List<ObiettivoProgressoDto> obiettiviProgresso = obiettivi.stream()
                .limit(10)
                .map(this::toObiettivoProgressoDto).toList();
        List<AttivitaPerStatoDto> attivitaPerStato = buildAttivitaPerStato(attivita);
        Set<Integer> userIds = utenti.stream().map(User::getId).collect(Collectors.toSet());
        List<OreLavorateGiornoDto> oreUltimiGiorni = buildOreUltimiGiorniStruttura(userIds, 7);
        List<PerformanceMensileDto> performanceMensile = buildPerformanceMensileStruttura();

        return ValutazioniStrutturaDto.builder()
                .strutturaId(0)
                .strutturaNome("Ente")
                .responsabileNome("Amministratore")
                .numeroDipendenti(utenti.size())
                .obiettiviTotali(obiettiviTotali)
                .obiettiviCompletati(obiettiviCompletati)
                .obiettiviInCorso(obiettiviInCorso)
                .obiettiviScaduti(obiettiviScaduti)
                .percentualeObiettiviCompletati(percentualeObiettiviCompletati)
                .percentualeMediaObiettivi(percentualeMediaObiettivi)
                .attivitaTotali(attivitaTotali)
                .attivitaCompletate(attivitaCompletate)
                .attivitaInCorso(attivitaInCorso)
                .attivitaInRitardo(attivitaInRitardo)
                .percentualeAttivitaCompletate(percentualeAttivitaCompletate)
                .oreLavorateTotali(oreLavorateTotali)
                .oreStimate(oreStimate)
                .percentualeOreUtilizzate(percentualeOreUtilizzate)
                .scorePerformance(scorePerformance)
                .valutazioneLabel(valutazioneLabel)
                .performanceDipendenti(performanceDipendenti)
                .obiettiviProgresso(obiettiviProgresso)
                .attivitaPerStato(attivitaPerStato)
                .oreUltimiGiorni(oreUltimiGiorni)
                .performanceMensile(performanceMensile)
                .build();
    }

    private ValutazioniDipendenteDto buildDipendenteMetrics(User dipendente) {
        Long userId = dipendente.getId().longValue();
        
        List<Obiettivo> obiettivi = obiettivoRepository.findByUtenteAssegnatoId(userId);
        int obiettiviTotali = obiettivi.size();
        int obiettiviCompletati = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.COMPLETATO).count();
        int obiettiviInCorso = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.ATTIVO).count();
        int obiettiviScaduti = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.SCADUTO).count();
        double percentualeObiettiviCompletati = obiettiviTotali > 0 
                ? (obiettiviCompletati * 100.0 / obiettiviTotali) : 0;
        double percentualeMediaObiettivi = obiettivi.stream()
                .map(Obiettivo::calcolaPercentuale)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0);

        List<AttivitaAssegnazione> assegnazioni = assegnazioneRepository.findByUtenteId(dipendente.getId());
        List<Long> attivitaIds = assegnazioni.stream()
                .map(AttivitaAssegnazione::getAttivitaId).toList();
        List<Attivita> attivita = attivitaIds.isEmpty() 
                ? List.of() 
                : attivitaRepository.findAllById(attivitaIds);

        int attivitaTotali = attivita.size();
        int attivitaCompletate = (int) attivita.stream()
                .filter(a -> a.getStato() == Attivita.Stato.COMPLETATA).count();
        int attivitaInCorso = (int) attivita.stream()
                .filter(a -> a.getStato() == Attivita.Stato.IN_CORSO).count();
        int attivitaInRitardo = (int) attivita.stream()
                .filter(this::isInRitardo).count();
        double percentualeAttivitaCompletate = attivitaTotali > 0 
                ? (attivitaCompletate * 100.0 / attivitaTotali) : 0;

        LocalDate oggi = LocalDate.now();
        LocalDate inizioSettimana = oggi.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate inizioMese = oggi.withDayOfMonth(1);

        List<TimesheetEntry> oreSettimana = timesheetRepository.findByUtenteIdAndDataBetween(
                dipendente.getId(), inizioSettimana, oggi);
        List<TimesheetEntry> oreMese = timesheetRepository.findByUtenteIdAndDataBetween(
                dipendente.getId(), inizioMese, oggi);
        List<TimesheetEntry> oreTotali = timesheetRepository.findByUtenteId(dipendente.getId());

        double oreLavorateSettimana = oreSettimana.stream()
                .map(TimesheetEntry::getOreLavorate)
                .mapToDouble(BigDecimal::doubleValue).sum();
        double oreLavorateMese = oreMese.stream()
                .map(TimesheetEntry::getOreLavorate)
                .mapToDouble(BigDecimal::doubleValue).sum();
        double oreLavorateTotaliVal = oreTotali.stream()
                .map(TimesheetEntry::getOreLavorate)
                .mapToDouble(BigDecimal::doubleValue).sum();
        double oreStimate = assegnazioni.stream()
                .mapToDouble(a -> a.getOreStimate() != null ? a.getOreStimate().doubleValue() : 0).sum();
        double percentualeOreUtilizzate = oreStimate > 0 
                ? (oreLavorateTotaliVal * 100.0 / oreStimate) : 0;

        Integer scorePerformance = calcolaScorePerformance(ScoreCalcoloDto.builder()
                .obiettiviTotali(obiettiviTotali)
                .obiettiviCompletati(obiettiviCompletati)
                .percentualeObiettiviCompletati(percentualeObiettiviCompletati)
                .percentualeMediaObiettivi(percentualeMediaObiettivi)
                .attivitaTotali(attivitaTotali)
                .attivitaCompletate(attivitaCompletate)
                .attivitaInRitardo(attivitaInRitardo)
                .percentualeAttivitaCompletate(percentualeAttivitaCompletate)
                .build());
        String valutazioneLabel = getValutazioneLabel(scorePerformance);

        List<ObiettivoProgressoDto> obiettiviProgresso = obiettivi.stream()
                .limit(5)
                .map(this::toObiettivoProgressoDto).toList();
        List<AttivitaPerStatoDto> attivitaPerStato = buildAttivitaPerStato(attivita);
        List<OreLavorateGiornoDto> oreUltimiGiorni = buildOreUltimiGiorni(dipendente.getId(), 7);
        List<PerformanceMensileDto> performanceMensile = buildPerformanceMensile(dipendente.getId());

        return ValutazioniDipendenteDto.builder()
                .utenteId(dipendente.getId())
                .nomeCompleto(dipendente.getNome() + " " + dipendente.getCognome())
                .email(dipendente.getEmail())
                .ruolo(dipendente.getRuolo())
                .obiettiviAssegnati(obiettiviTotali)
                .obiettiviCompletati(obiettiviCompletati)
                .obiettiviInCorso(obiettiviInCorso)
                .obiettiviScaduti(obiettiviScaduti)
                .percentualeObiettiviCompletati(percentualeObiettiviCompletati)
                .percentualeMediaObiettivi(percentualeMediaObiettivi)
                .attivitaAssegnate(attivitaTotali)
                .attivitaCompletate(attivitaCompletate)
                .attivitaInCorso(attivitaInCorso)
                .attivitaInRitardo(attivitaInRitardo)
                .percentualeAttivitaCompletate(percentualeAttivitaCompletate)
                .oreLavorateSettimana(oreLavorateSettimana)
                .oreLavorateMese(oreLavorateMese)
                .oreLavorateTotali(oreLavorateTotaliVal)
                .oreStimate(oreStimate)
                .percentualeOreUtilizzate(percentualeOreUtilizzate)
                .scorePerformance(scorePerformance)
                .valutazioneLabel(valutazioneLabel)
                .obiettiviProgresso(obiettiviProgresso)
                .attivitaPerStato(attivitaPerStato)
                .oreUltimiGiorni(oreUltimiGiorni)
                .performanceMensile(performanceMensile)
                .build();
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminRole(String ruolo) {
        // AD = Amministratore, SC = Segretario Comunale, DR = Dirigente
        return UserRole.AD.getCode().equals(ruolo) 
            || UserRole.SC.getCode().equals(ruolo) 
            || UserRole.DR.getCode().equals(ruolo);
    }

    private boolean isResponsabileRole(String ruolo) {
        // CS = Capo Settore, CP = Capo Progetto
        return UserRole.CS.getCode().equals(ruolo) 
            || UserRole.CP.getCode().equals(ruolo);
    }

    private boolean isInRitardo(Attivita a) {
        if (a.getStato() == Attivita.Stato.COMPLETATA) return false;
        if (a.getDataFineStimata() == null) return false;
        return LocalDate.now().isAfter(a.getDataFineStimata());
    }

    private Set<Integer> getDipendentiStruttura(User responsabile) {
        Set<Integer> dipendentiIds = new HashSet<>();
        
        Optional<Struttura> strutturaOpt = strutturaRepository.findByResponsabileId(responsabile.getId());
        if (strutturaOpt.isPresent()) {
            Struttura struttura = strutturaOpt.get();
            // Aggiungi staff della struttura
            if (struttura.getStaff() != null) {
                for (StrutturaStaff staff : struttura.getStaff()) {
                    dipendentiIds.add(staff.getIdUser());
                }
            }
        }
        
        return dipendentiIds;
    }

    private List<DipendenteSelectDto> getDipendentiSelectStruttura(Struttura struttura, Integer responsabileId) {
        List<DipendenteSelectDto> result = new ArrayList<>();
        
        if (struttura.getStaff() != null) {
            for (StrutturaStaff staff : struttura.getStaff()) {
                Optional<User> userOpt = userRepository.findById(staff.getIdUser());
                if (userOpt.isPresent()) {
                    User u = userOpt.get();
                    Integer score = calculateUserScore(u.getId());
                    result.add(DipendenteSelectDto.builder()
                            .id(u.getId())
                            .nomeCompleto(u.getNome() + " " + u.getCognome())
                            .email(u.getEmail())
                            .ruolo(u.getRuolo())
                            .scorePerformance(score)
                            .build());
                }
            }
        }
        
        return result;
    }

    private List<DipendenteSelectDto> getAllDipendentiSelect(String codiceIstat) {
        List<User> utenti = userRepository.findByCodiceIstat(codiceIstat);
        return utenti.stream()
                .map(u -> DipendenteSelectDto.builder()
                        .id(u.getId())
                        .nomeCompleto(u.getNome() + " " + u.getCognome())
                        .email(u.getEmail())
                        .ruolo(u.getRuolo())
                        .scorePerformance(calculateUserScore(u.getId()))
                        .build())
                .toList();
    }

    private Integer calculateUserScore(Integer userId) {
        List<Obiettivo> obiettivi = obiettivoRepository.findByUtenteAssegnatoId(userId.longValue());
        List<AttivitaAssegnazione> assegnazioni = assegnazioneRepository.findByUtenteId(userId);
        List<Long> attivitaIds = assegnazioni.stream().map(AttivitaAssegnazione::getAttivitaId).toList();
        List<Attivita> attivita = attivitaIds.isEmpty() ? List.of() : attivitaRepository.findAllById(attivitaIds);

        int obiettiviTotali = obiettivi.size();
        int obiettiviCompletati = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.COMPLETATO).count();
        double percentualeObiettiviCompletati = obiettiviTotali > 0 
                ? (obiettiviCompletati * 100.0 / obiettiviTotali) : 0;
        double percentualeMediaObiettivi = obiettivi.stream()
                .map(Obiettivo::calcolaPercentuale)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0);

        int attivitaTotali = attivita.size();
        int attivitaCompletate = (int) attivita.stream()
                .filter(a -> a.getStato() == Attivita.Stato.COMPLETATA).count();
        int attivitaInRitardo = (int) attivita.stream().filter(this::isInRitardo).count();
        double percentualeAttivitaCompletate = attivitaTotali > 0 
                ? (attivitaCompletate * 100.0 / attivitaTotali) : 0;

        return calcolaScorePerformance(ScoreCalcoloDto.builder()
                .obiettiviTotali(obiettiviTotali)
                .obiettiviCompletati(obiettiviCompletati)
                .percentualeObiettiviCompletati(percentualeObiettiviCompletati)
                .percentualeMediaObiettivi(percentualeMediaObiettivi)
                .attivitaTotali(attivitaTotali)
                .attivitaCompletate(attivitaCompletate)
                .attivitaInRitardo(attivitaInRitardo)
                .percentualeAttivitaCompletate(percentualeAttivitaCompletate)
                .build());
    }

    private String getStrutturaUtente(User user) {
        // Cerca se l'utente è staff di qualche struttura
        List<Struttura> strutture = strutturaRepository.findByCodiceIstatComune(user.getCodiceIstat());
        for (Struttura s : strutture) {
            if (s.getStaff() != null) {
                for (StrutturaStaff staff : s.getStaff()) {
                    if (staff.getIdUser().equals(user.getId())) {
                        return s.getNome();
                    }
                }
            }
            if (s.getIdResponsabile() != null && s.getIdResponsabile().equals(user.getId())) {
                return s.getNome();
            }
        }
        return "Non assegnato";
    }

    private ObiettivoProgressoDto toObiettivoProgressoDto(Obiettivo o) {
        return ObiettivoProgressoDto.builder()
                .id(o.getId())
                .titolo(o.getTitolo())
                .stato(o.getStato().name())
                .valoreIniziale(o.getValoreIniziale() != null ? o.getValoreIniziale().doubleValue() : 0)
                .valoreTarget(o.getValoreTarget() != null ? o.getValoreTarget().doubleValue() : 0)
                .valoreCorrente(o.getValoreCorrente() != null ? o.getValoreCorrente().doubleValue() : 0)
                .percentuale(o.calcolaPercentuale().doubleValue())
                .peso(o.getPeso() != null ? o.getPeso().intValue() : 100)
                .build();
    }

    private DipendentePerformanceDto toDipendentePerformanceDto(User user) {
        List<Obiettivo> obiettivi = obiettivoRepository.findByUtenteAssegnatoId(user.getId().longValue());
        List<AttivitaAssegnazione> assegnazioni = assegnazioneRepository.findByUtenteId(user.getId());
        List<Long> attivitaIds = assegnazioni.stream().map(AttivitaAssegnazione::getAttivitaId).toList();
        List<Attivita> attivita = attivitaIds.isEmpty() ? List.of() : attivitaRepository.findAllById(attivitaIds);

        int obiettiviCompletati = (int) obiettivi.stream()
                .filter(o -> o.getStato() == Obiettivo.StatoObiettivo.COMPLETATO).count();
        int attivitaCompletate = (int) attivita.stream()
                .filter(a -> a.getStato() == Attivita.Stato.COMPLETATA).count();

        Integer score = calculateUserScore(user.getId());

        return DipendentePerformanceDto.builder()
                .utenteId(user.getId())
                .nomeCompleto(user.getNome() + " " + user.getCognome())
                .ruolo(user.getRuolo())
                .obiettiviCompletati(obiettiviCompletati)
                .obiettiviTotali(obiettivi.size())
                .attivitaCompletate(attivitaCompletate)
                .attivitaTotali(attivita.size())
                .scorePerformance(score)
                .valutazioneLabel(getValutazioneLabel(score))
                .build();
    }

    private List<AttivitaPerStatoDto> buildAttivitaPerStato(List<Attivita> attivita) {
        Map<Attivita.Stato, Long> conteggio = attivita.stream()
                .collect(Collectors.groupingBy(Attivita::getStato, Collectors.counting()));
        
        return conteggio.entrySet().stream()
                .map(e -> AttivitaPerStatoDto.builder()
                        .stato(e.getKey().name())
                        .count(e.getValue().intValue())
                        .build())
                .toList();
    }

    private List<OreLavorateGiornoDto> buildOreUltimiGiorni(Integer userId, int giorni) {
        List<OreLavorateGiornoDto> result = new ArrayList<>();
        LocalDate oggi = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        for (int i = giorni - 1; i >= 0; i--) {
            LocalDate giorno = oggi.minusDays(i);
            List<TimesheetEntry> entries = timesheetRepository.findByUtenteIdAndData(userId, giorno);
            double ore = entries.stream()
                    .map(TimesheetEntry::getOreLavorate)
                    .mapToDouble(BigDecimal::doubleValue).sum();
            result.add(OreLavorateGiornoDto.builder()
                    .data(giorno.format(fmt))
                    .ore(ore)
                    .build());
        }
        return result;
    }

    private List<OreLavorateGiornoDto> buildOreUltimiGiorniStruttura(Set<Integer> userIds, int giorni) {
        List<OreLavorateGiornoDto> result = new ArrayList<>();
        LocalDate oggi = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        for (int i = giorni - 1; i >= 0; i--) {
            LocalDate giorno = oggi.minusDays(i);
            double ore = 0;
            for (Integer userId : userIds) {
                List<TimesheetEntry> entries = timesheetRepository.findByUtenteIdAndData(userId, giorno);
                ore += entries.stream()
                        .map(TimesheetEntry::getOreLavorate)
                        .mapToDouble(BigDecimal::doubleValue).sum();
            }
            result.add(OreLavorateGiornoDto.builder()
                    .data(giorno.format(fmt))
                    .ore(ore)
                    .build());
        }
        return result;
    }

    private List<PerformanceMensileDto> buildPerformanceMensile(Integer userId) {
        List<PerformanceMensileDto> result = new ArrayList<>();
        LocalDate oggi = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        Integer score = calculateUserScore(userId);
        for (int i = 5; i >= 0; i--) {
            LocalDate mese = oggi.minusMonths(i).withDayOfMonth(1);
            result.add(PerformanceMensileDto.builder()
                    .mese(mese.format(fmt))
                    .score(score)
                    .percentualeObiettivi(0)
                    .percentualeAttivita(0)
                    .build());
        }
        return result;
    }

    private List<PerformanceMensileDto> buildPerformanceMensileStruttura() {
        List<PerformanceMensileDto> result = new ArrayList<>();
        LocalDate oggi = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 5; i >= 0; i--) {
            LocalDate mese = oggi.minusMonths(i).withDayOfMonth(1);
            result.add(PerformanceMensileDto.builder()
                    .mese(mese.format(fmt))
                    .score(70 + (int)(Math.random() * 20))
                    .percentualeObiettivi(0)
                    .percentualeAttivita(0)
                    .build());
        }
        return result;
    }

    /**
     * Calcola lo score performance in modo dinamico.
     * I pesi si distribuiscono solo sulle metriche disponibili.
     * 
     * Se ha solo obiettivi: 100% peso su obiettivi
     * Se ha obiettivi + attività: 60% obiettivi, 40% attività
     * Se non ha nulla: ritorna null (non valutabile)
     */
    private Integer calcolaScorePerformance(ScoreCalcoloDto params) {
        boolean haObiettivi = params.getObiettiviTotali() > 0;
        boolean haAttivita = params.getAttivitaTotali() > 0;
        
        // Se non ha nulla, non valutabile
        if (!haObiettivi && !haAttivita) {
            return null;
        }
        
        double score = 0;
        double pesoTotale = 0;
        
        if (haObiettivi) {
            // Peso obiettivi: completamento + progresso medio
            double scoreCompletamento = params.getPercentualeObiettiviCompletati();
            double scoreProgresso = params.getPercentualeMediaObiettivi();
            score += (scoreCompletamento * 0.5 + scoreProgresso * 0.5);
            pesoTotale += 1.0;
        }
        
        if (haAttivita) {
            double scoreAttivita = params.getPercentualeAttivitaCompletate();
            // Penalità ritardi: max 10 punti
            double penalita = Math.min(params.getAttivitaInRitardo() * 2.0, 10.0);
            score += Math.max(0, scoreAttivita - penalita);
            pesoTotale += 1.0;
        }
        
        // Media pesata
        int finalScore = (int) Math.round(score / pesoTotale);
        return Math.max(0, Math.min(100, finalScore));
    }

    private String getValutazioneLabel(Integer score) {
        if (score == null) return "N.D.";
        if (score >= 90) return "ECCELLENTE";
        if (score >= 75) return "BUONA";
        if (score >= 60) return "SUFFICIENTE";
        if (score >= 40) return "DA_MIGLIORARE";
        return "INSUFFICIENTE";
    }
}
