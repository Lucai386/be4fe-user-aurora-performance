package com.be4fe_user_aurora_performance.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_user_aurora_performance.dto.attivita.LogOreLavorateRequest;
import com.be4fe_user_aurora_performance.principal.UserPrincipal;
import com.be4fe_user_aurora_performance.service.AttivitaService;
import com.be4fe_user_aurora_performance.service.AttivitaService.DeleteAttivitaResponse;
import com.be4fe_user_aurora_performance.service.AttivitaService.ListTimesheetResponse;
import com.be4fe_user_aurora_performance.service.AttivitaService.TimesheetEntryResponse;

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
    private final UserPrincipal userPrincipal;

    @PostMapping
    public ResponseEntity<TimesheetEntryResponse> logOreLavorate(@RequestBody LogOreLavorateRequest body) {
        return ResponseEntity.ok(attivitaService.logOreLavorate(body, userPrincipal.getRuolo()));
    }

    @GetMapping("/attivita/{attivitaId}")
    public ResponseEntity<ListTimesheetResponse> getTimesheetByAttivita(@PathVariable Long attivitaId) {
        return ResponseEntity.ok(attivitaService.getTimesheetByAttivita(attivitaId, userPrincipal.getRuolo()));
    }

    @GetMapping("/utente/{utenteId}")
    public ResponseEntity<ListTimesheetResponse> getTimesheetByUtente(
        @PathVariable Integer utenteId,
        @RequestParam(required = false) String dataInizio,
        @RequestParam(required = false) String dataFine
    ) {
        LocalDate inizio = dataInizio != null ? LocalDate.parse(dataInizio) : null;
        LocalDate fine = dataFine != null ? LocalDate.parse(dataFine) : null;
        return ResponseEntity.ok(attivitaService.getTimesheetByUtente(utenteId, inizio, fine, userPrincipal.getRuolo()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteAttivitaResponse> deleteTimesheetEntry(@PathVariable Long id) {
        return ResponseEntity.ok(attivitaService.deleteTimesheetEntry(id, userPrincipal.getRuolo()));
    }
}
