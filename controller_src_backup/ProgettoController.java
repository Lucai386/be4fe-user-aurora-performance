package com.bff_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bff_user_aurora_performance.dto.dup.CreateProgettoRequest;
import com.bff_user_aurora_performance.dto.dup.UpdateProgettoRequest;
import com.bff_user_aurora_performance.service.ProgettoService;
import com.bff_user_aurora_performance.service.ProgettoService.DeleteProgettoResponse;
import com.bff_user_aurora_performance.service.ProgettoService.ListProgettiResponse;
import com.bff_user_aurora_performance.service.ProgettoService.ProgettoResponse;
import com.bff_user_aurora_performance.service.SessionService;

import lombok.RequiredArgsConstructor;

/**
 * Controller per la gestione dei Progetti all'interno di un DUP.
 * I progetti possono essere collegati opzionalmente a una LPM.
 */
@RestController
@RequestMapping("/api/progetti")
@RequiredArgsConstructor
public class ProgettoController {

    private final ProgettoService progettoService;
    private final SessionService sessionService;

    /**
     * Lista tutti i progetti di un DUP
     */
    @GetMapping("/dup/{dupId}")
    public ResponseEntity<ListProgettiResponse> listByDup(@PathVariable Long dupId, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(progettoService.listByDup(dupId, userRole));
    }

    /**
     * Ottiene un singolo progetto
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProgettoResponse> getProgetto(@PathVariable Long id, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(progettoService.getProgetto(id, userRole));
    }

    /**
     * Crea un nuovo progetto in un DUP
     */
    @PostMapping
    public ResponseEntity<ProgettoResponse> createProgetto(@RequestBody CreateProgettoRequest body, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(progettoService.createProgetto(body, userRole));
    }

    /**
     * Aggiorna un progetto esistente
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProgettoResponse> updateProgetto(@PathVariable Long id, @RequestBody UpdateProgettoRequest body, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(progettoService.updateProgetto(id, body, userRole));
    }

    /**
     * Elimina un progetto
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteProgettoResponse> deleteProgetto(@PathVariable Long id, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(progettoService.deleteProgetto(id, userRole));
    }

    /**
     * Collega una LPM esistente a un progetto
     */
    @PostMapping("/{id}/lpm/{lpmId}")
    public ResponseEntity<ProgettoResponse> linkLpm(@PathVariable Long id, @PathVariable Long lpmId, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(progettoService.linkLpm(id, lpmId, userRole));
    }

    /**
     * Scollega la LPM da un progetto
     */
    @DeleteMapping("/{id}/lpm")
    public ResponseEntity<ProgettoResponse> unlinkLpm(@PathVariable Long id, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(progettoService.unlinkLpm(id, userRole));
    }
}
