package com.be4fe_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
import com.be4fe_user_aurora_performance.service.AttivitaService;
import com.be4fe_user_aurora_performance.service.AttivitaService.AttivitaResponse;
import com.be4fe_user_aurora_performance.service.AttivitaService.DeleteAttivitaResponse;
import com.be4fe_user_aurora_performance.service.AttivitaService.ListAttivitaResponse;
import com.be4fe_user_aurora_performance.service.SessionService;

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
    private final SessionService sessionService;

    // ==================== ATTIVITÀ CRUD ====================

    /**
     * Lista tutte le attività
     * Per utenti DB restituisce solo le attività a loro assegnate
     */
    @GetMapping
    public ResponseEntity<ListAttivitaResponse> listAll(Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        Integer userId = sessionService.getUserId(authentication);
        return ResponseEntity.ok(attivitaService.listAll(userRole, userId));
    }

    /**
     * Lista le attività di un progetto
     */
    @GetMapping("/progetto/{progettoId}")
    public ResponseEntity<ListAttivitaResponse> listByProgetto(@PathVariable Long progettoId, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(attivitaService.listByProgetto(progettoId, userRole));
    }

    /**
     * Ottiene una singola attività
     */
    @GetMapping("/{id}")
    public ResponseEntity<AttivitaResponse> getAttivita(@PathVariable Long id, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(attivitaService.getAttivita(id, userRole));
    }

    /**
     * Crea una nuova attività
     */
    @PostMapping
    public ResponseEntity<AttivitaResponse> createAttivita(@RequestBody CreateAttivitaRequest body, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(attivitaService.createAttivita(body, userRole));
    }

    /**
     * Aggiorna un'attività esistente
     */
    @PutMapping("/{id}")
    public ResponseEntity<AttivitaResponse> updateAttivita(@PathVariable Long id, @RequestBody UpdateAttivitaRequest body, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(attivitaService.updateAttivita(id, body, userRole));
    }

    /**
     * Elimina un'attività
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteAttivitaResponse> deleteAttivita(@PathVariable Long id, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(attivitaService.deleteAttivita(id, userRole));
    }

    /**
     * Duplica un'attività, opzionalmente assegnandola a una nuova struttura
     */
    @PostMapping("/{id}/duplica")
    public ResponseEntity<AttivitaResponse> duplicaAttivita(
            @PathVariable Long id,
            @RequestParam(required = false) Integer strutturaId,
            Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(attivitaService.duplicaAttivita(id, strutturaId, userRole));
    }

    // ==================== ASSEGNAZIONI ====================

    /**
     * Assegna un utente a un'attività
     */
    @PostMapping("/{id}/assegna")
    public ResponseEntity<AttivitaResponse> assegnaUtente(@PathVariable Long id, @RequestBody AssegnaUtenteRequest body, Authentication authentication) {
        body.setAttivitaId(id);
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(attivitaService.assegnaUtente(body, userRole));
    }

    /**
     * Rimuove l'assegnazione di un utente da un'attività
     */
    @DeleteMapping("/{id}/assegna/{utenteId}")
    public ResponseEntity<AttivitaResponse> rimuoviAssegnazione(@PathVariable Long id, @PathVariable Integer utenteId, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(attivitaService.rimuoviAssegnazione(id, utenteId, userRole));
    }

    // ==================== PERCENTUALE COMPLETAMENTO ====================

    /**
     * Aggiorna la percentuale di completamento di un'attività.
     * La percentuale è indipendente dalle ore lavorate.
     * Utenti assegnati possono aggiornare la percentuale.
     */
    @PutMapping("/{id}/percentuale")
    public ResponseEntity<AttivitaResponse> updatePercentualeCompletamento(
            @PathVariable Long id,
            @RequestParam Integer percentuale,
            Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        Integer userId = sessionService.getUserId(authentication);
        return ResponseEntity.ok(attivitaService.updatePercentualeCompletamento(id, percentuale, userRole, userId));
    }

    // ==================== STEP (SOTTO-TASK) ====================

    /**
     * Aggiunge un nuovo step (checkbox) a un'attività.
     * La percentuale di completamento viene ricalcolata automaticamente.
     */
    @PostMapping("/{id}/steps")
    public ResponseEntity<AttivitaResponse> addStep(
            @PathVariable Long id,
            @RequestParam String titolo,
            @RequestParam(required = false) String descrizione,
            @RequestParam(required = false) Integer peso,
            Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        Integer userId = sessionService.getUserId(authentication);
        return ResponseEntity.ok(attivitaService.addStep(id, titolo, descrizione, peso, userRole, userId));
    }

    /**
     * Modifica lo stato di completamento di uno step.
     * La percentuale di completamento viene ricalcolata automaticamente.
     * Se viene fornita una descrizione, viene salvata nello step (utile per documentare il lavoro svolto al completamento).
     */
    @PutMapping("/{id}/steps/{stepId}")
    public ResponseEntity<AttivitaResponse> toggleStep(
            @PathVariable Long id,
            @PathVariable Long stepId,
            @RequestParam Boolean completato,
            @RequestParam(required = false) String descrizione,
            Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        Integer userId = sessionService.getUserId(authentication);
        return ResponseEntity.ok(attivitaService.toggleStep(id, stepId, completato, descrizione, userRole, userId));
    }

    /**
     * Rimuove uno step da un'attività.
     * La percentuale di completamento viene ricalcolata automaticamente.
     */
    @DeleteMapping("/{id}/steps/{stepId}")
    public ResponseEntity<AttivitaResponse> removeStep(
            @PathVariable Long id,
            @PathVariable Long stepId,
            Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        Integer userId = sessionService.getUserId(authentication);
        return ResponseEntity.ok(attivitaService.removeStep(id, stepId, userRole, userId));
    }
}
