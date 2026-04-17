package com.be4fe_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_user_aurora_performance.dto.attivita.AssegnaUtenteRequest;
import com.be4fe_user_aurora_performance.dto.attivita.CreateAttivitaRequest;
import com.be4fe_user_aurora_performance.dto.attivita.UpdateAttivitaRequest;
import com.be4fe_user_aurora_performance.principal.UserPrincipal;
import com.be4fe_user_aurora_performance.service.AttivitaService;
import com.be4fe_user_aurora_performance.service.AttivitaService.AttivitaResponse;
import com.be4fe_user_aurora_performance.service.AttivitaService.DeleteAttivitaResponse;
import com.be4fe_user_aurora_performance.service.AttivitaService.ListAttivitaResponse;

import lombok.RequiredArgsConstructor;

/**
 * Controller per la gestione delle Attività (sotto-attività di un Progetto).
 * Include gestione pesi, assegnazioni utenti e timesheet.
 */
@RestController
@RequestMapping("/api/attivita")
@RequiredArgsConstructor
public class AttivitaController {

    private final AttivitaService attivitaService;
    private final UserPrincipal userPrincipal;

    @GetMapping
    public ResponseEntity<ListAttivitaResponse> listAll() {
        return ResponseEntity.ok(attivitaService.listAll(userPrincipal.getRuolo(), userPrincipal.getId().intValue()));
    }

    @GetMapping("/progetto/{progettoId}")
    public ResponseEntity<ListAttivitaResponse> listByProgetto(@PathVariable Long progettoId) {
        return ResponseEntity.ok(attivitaService.listByProgetto(progettoId, userPrincipal.getRuolo()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttivitaResponse> getAttivita(@PathVariable Long id) {
        return ResponseEntity.ok(attivitaService.getAttivita(id, userPrincipal.getRuolo()));
    }

    @PostMapping
    public ResponseEntity<AttivitaResponse> createAttivita(@RequestBody CreateAttivitaRequest body) {
        return ResponseEntity.ok(attivitaService.createAttivita(body, userPrincipal.getRuolo()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttivitaResponse> updateAttivita(@PathVariable Long id, @RequestBody UpdateAttivitaRequest body) {
        return ResponseEntity.ok(attivitaService.updateAttivita(id, body, userPrincipal.getRuolo()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteAttivitaResponse> deleteAttivita(@PathVariable Long id) {
        return ResponseEntity.ok(attivitaService.deleteAttivita(id, userPrincipal.getRuolo()));
    }

    @PostMapping("/{id}/duplica")
    public ResponseEntity<AttivitaResponse> duplicaAttivita(
            @PathVariable Long id,
            @RequestParam(required = false) Integer strutturaId) {
        return ResponseEntity.ok(attivitaService.duplicaAttivita(id, strutturaId, userPrincipal.getRuolo()));
    }

    @PostMapping("/{id}/assegna")
    public ResponseEntity<AttivitaResponse> assegnaUtente(@PathVariable Long id, @RequestBody AssegnaUtenteRequest body) {
        body.setAttivitaId(id);
        return ResponseEntity.ok(attivitaService.assegnaUtente(body, userPrincipal.getRuolo()));
    }

    @DeleteMapping("/{id}/assegna/{utenteId}")
    public ResponseEntity<AttivitaResponse> rimuoviAssegnazione(@PathVariable Long id, @PathVariable Integer utenteId) {
        return ResponseEntity.ok(attivitaService.rimuoviAssegnazione(id, utenteId, userPrincipal.getRuolo()));
    }

    @PutMapping("/{id}/percentuale")
    public ResponseEntity<AttivitaResponse> updatePercentualeCompletamento(
            @PathVariable Long id,
            @RequestParam Integer percentuale) {
        return ResponseEntity.ok(attivitaService.updatePercentualeCompletamento(
                id, percentuale, userPrincipal.getRuolo(), userPrincipal.getId().intValue()));
    }

    @PostMapping("/{id}/steps")
    public ResponseEntity<AttivitaResponse> addStep(
            @PathVariable Long id,
            @RequestParam String titolo,
            @RequestParam(required = false) String descrizione,
            @RequestParam(required = false) Integer peso) {
        return ResponseEntity.ok(attivitaService.addStep(
                id, titolo, descrizione, peso,
                userPrincipal.getRuolo(), userPrincipal.getId().intValue()));
    }

    @PutMapping("/{id}/steps/{stepId}")
    public ResponseEntity<AttivitaResponse> toggleStep(
            @PathVariable Long id,
            @PathVariable Long stepId,
            @RequestParam Boolean completato,
            @RequestParam(required = false) String descrizione) {
        return ResponseEntity.ok(attivitaService.toggleStep(
                id, stepId, completato, descrizione,
                userPrincipal.getRuolo(), userPrincipal.getId().intValue()));
    }

    @DeleteMapping("/{id}/steps/{stepId}")
    public ResponseEntity<AttivitaResponse> removeStep(
            @PathVariable Long id,
            @PathVariable Long stepId) {
        return ResponseEntity.ok(attivitaService.removeStep(
                id, stepId,
                userPrincipal.getRuolo(), userPrincipal.getId().intValue()));
    }
}
