package com.bff_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bff_user_aurora_performance.dto.valutazioni.ValutazioniResponse;
import com.bff_user_aurora_performance.service.SessionService;
import com.bff_user_aurora_performance.service.ValutazioniService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller per le valutazioni delle performance.
 * Fornisce endpoint per visualizzare metriche e grafici di valutazione.
 */
@RestController
@RequestMapping("/api/valutazioni")
@RequiredArgsConstructor
@Slf4j
public class ValutazioniController {

    private final ValutazioniService valutazioniService;
    private final SessionService sessionService;

    /**
     * Ottiene le metriche di valutazione in base al ruolo dell'utente.
     * - Dipendente: metriche personali
     * - Responsabile: metriche struttura + lista dipendenti
     * - Admin: metriche aggregate ente + tutti i dipendenti
     */
    @PostMapping
    public ResponseEntity<ValutazioniResponse> getValutazioni(Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        Integer userId = sessionService.getUserId(authentication);
        log.debug("Recupero valutazioni per utente {} con ruolo {}", userId, userRole);
        return ResponseEntity.ok(valutazioniService.getValutazioni(userRole, userId));
    }

    /**
     * Ottiene le metriche di valutazione di un dipendente specifico.
     * Solo per responsabili e admin.
     */
    @PostMapping("/dipendente/{dipendenteId}")
    public ResponseEntity<ValutazioniResponse> getValutazioniDipendente(
            @PathVariable Integer dipendenteId,
            Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        Integer userId = sessionService.getUserId(authentication);
        log.debug("Recupero valutazioni dipendente {} per utente {} con ruolo {}", dipendenteId, userId, userRole);
        return ResponseEntity.ok(valutazioniService.getValutazioniDipendente(dipendenteId, userRole, userId));
    }
}
