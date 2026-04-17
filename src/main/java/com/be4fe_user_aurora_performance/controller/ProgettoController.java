package com.be4fe_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_user_aurora_performance.dto.dup.CreateProgettoRequest;
import com.be4fe_user_aurora_performance.dto.dup.UpdateProgettoRequest;
import com.be4fe_user_aurora_performance.principal.UserPrincipal;
import com.be4fe_user_aurora_performance.service.ProgettoService;
import com.be4fe_user_aurora_performance.service.ProgettoService.DeleteProgettoResponse;
import com.be4fe_user_aurora_performance.service.ProgettoService.ListProgettiResponse;
import com.be4fe_user_aurora_performance.service.ProgettoService.ProgettoResponse;

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
    private final UserPrincipal userPrincipal;

    @GetMapping("/dup/{dupId}")
    public ResponseEntity<ListProgettiResponse> listByDup(@PathVariable Long dupId) {
        return ResponseEntity.ok(progettoService.listByDup(dupId, userPrincipal.getRuolo()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgettoResponse> getProgetto(@PathVariable Long id) {
        return ResponseEntity.ok(progettoService.getProgetto(id, userPrincipal.getRuolo()));
    }

    @PostMapping
    public ResponseEntity<ProgettoResponse> createProgetto(@RequestBody CreateProgettoRequest body) {
        return ResponseEntity.ok(progettoService.createProgetto(body, userPrincipal.getRuolo()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProgettoResponse> updateProgetto(@PathVariable Long id, @RequestBody UpdateProgettoRequest body) {
        return ResponseEntity.ok(progettoService.updateProgetto(id, body, userPrincipal.getRuolo()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteProgettoResponse> deleteProgetto(@PathVariable Long id) {
        return ResponseEntity.ok(progettoService.deleteProgetto(id, userPrincipal.getRuolo()));
    }

    @PostMapping("/{id}/lpm/{lpmId}")
    public ResponseEntity<ProgettoResponse> linkLpm(@PathVariable Long id, @PathVariable Long lpmId) {
        return ResponseEntity.ok(progettoService.linkLpm(id, lpmId, userPrincipal.getRuolo()));
    }

    @DeleteMapping("/{id}/lpm")
    public ResponseEntity<ProgettoResponse> unlinkLpm(@PathVariable Long id) {
        return ResponseEntity.ok(progettoService.unlinkLpm(id, userPrincipal.getRuolo()));
    }
}
