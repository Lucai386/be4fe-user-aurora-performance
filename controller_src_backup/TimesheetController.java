package com.bff_user_aurora_performance.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bff_user_aurora_performance.dto.attivita.LogOreLavorateRequest;
import com.bff_user_aurora_performance.service.AttivitaService;
import com.bff_user_aurora_performance.service.AttivitaService.DeleteAttivitaResponse;
import com.bff_user_aurora_performance.service.AttivitaService.ListTimesheetResponse;
import com.bff_user_aurora_performance.service.AttivitaService.TimesheetEntryResponse;
import com.bff_user_aurora_performance.service.SessionService;

import lombok.RequiredArgsConstructor;

/**
 * Controller per la gestione del Timesheet.
 * Registra le ore lavorate dagli utenti sulle attività.
 */
@RestController
@RequestMapping("/api/timesheet")
@RequiredArgsConstructor
public class TimesheetController {

    private final AttivitaService attivitaService;
    private final SessionService sessionService;

    /**
     * Registra ore lavorate
     */
    @PostMapping
    public ResponseEntity<TimesheetEntryResponse> logOreLavorate(@RequestBody LogOreLavorateRequest body, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(attivitaService.logOreLavorate(body, userRole));
    }

    /**
     * Lista le entry di timesheet per un'attività
     */
    @GetMapping("/attivita/{attivitaId}")
    public ResponseEntity<ListTimesheetResponse> getTimesheetByAttivita(@PathVariable Long attivitaId, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(attivitaService.getTimesheetByAttivita(attivitaId, userRole));
    }

    /**
     * Lista le entry di timesheet per un utente (con filtro opzionale per periodo)
     */
    @GetMapping("/utente/{utenteId}")
    public ResponseEntity<ListTimesheetResponse> getTimesheetByUtente(
        @PathVariable Integer utenteId,
        @RequestParam(required = false) String dataInizio,
        @RequestParam(required = false) String dataFine,
        Authentication authentication
    ) {
        String userRole = sessionService.getUserRole(authentication);
        LocalDate inizio = dataInizio != null ? LocalDate.parse(dataInizio) : null;
        LocalDate fine = dataFine != null ? LocalDate.parse(dataFine) : null;
        return ResponseEntity.ok(attivitaService.getTimesheetByUtente(utenteId, inizio, fine, userRole));
    }

    /**
     * Elimina una entry di timesheet
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteAttivitaResponse> deleteTimesheetEntry(@PathVariable Long id, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(attivitaService.deleteTimesheetEntry(id, userRole));
    }
}
