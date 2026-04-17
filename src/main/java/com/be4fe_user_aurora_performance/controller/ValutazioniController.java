package com.be4fe_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_user_aurora_performance.dto.valutazioni.ValutazioniResponse;
import com.be4fe_user_aurora_performance.principal.UserPrincipal;
import com.be4fe_user_aurora_performance.service.ValutazioniService;

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
    private final UserPrincipal userPrincipal;

    @PostMapping
    public ResponseEntity<ValutazioniResponse> getValutazioni() {
        log.debug("Recupero valutazioni per utente {} con ruolo {}", userPrincipal.getId(), userPrincipal.getRuolo());
        return ResponseEntity.ok(valutazioniService.getValutazioni(
                userPrincipal.getRuolo(),
                userPrincipal.getId().intValue()));
    }

    @PostMapping("/dipendente/{dipendenteId}")
    public ResponseEntity<ValutazioniResponse> getValutazioniDipendente(
            @PathVariable Integer dipendenteId) {
        log.debug("Recupero valutazioni dipendente {} per utente {} con ruolo {}", dipendenteId, userPrincipal.getId(), userPrincipal.getRuolo());
        return ResponseEntity.ok(valutazioniService.getValutazioniDipendente(
                dipendenteId,
                userPrincipal.getRuolo(),
                userPrincipal.getId().intValue()));
    }
}
