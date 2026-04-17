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

import com.be4fe_user_aurora_performance.dto.obiettivo.CreateObiettivoRequest;
import com.be4fe_user_aurora_performance.dto.obiettivo.ListObiettiviResponse;
import com.be4fe_user_aurora_performance.dto.obiettivo.ObiettivoResponse;
import com.be4fe_user_aurora_performance.dto.obiettivo.RegistraProgressivoRequest;
import com.be4fe_user_aurora_performance.dto.obiettivo.UpdateObiettivoRequest;
import com.be4fe_user_aurora_performance.principal.UserPrincipal;
import com.be4fe_user_aurora_performance.service.ObiettivoService;

import lombok.RequiredArgsConstructor;

/**
 * Controller per la gestione degli Obiettivi operativi.
 */
@RestController
@RequestMapping("/api/obiettivi")
@RequiredArgsConstructor
public class ObiettivoController {

    private final ObiettivoService obiettivoService;
    private final UserPrincipal userPrincipal;

    @PostMapping("/list")
    public ResponseEntity<ListObiettiviResponse> listObiettivi() {
        return ResponseEntity.ok(obiettivoService.listObiettivi(
                userPrincipal.requireCodiceIstat(),
                userPrincipal.getId(),
                userPrincipal.getRuolo()));
    }

    @GetMapping("/miei")
    public ResponseEntity<ListObiettiviResponse> listMieiObiettivi() {
        return ResponseEntity.ok(obiettivoService.listMieiObiettivi(userPrincipal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObiettivoResponse> getObiettivo(@PathVariable Long id) {
        return ResponseEntity.ok(obiettivoService.getObiettivo(
                id, userPrincipal.getId(), userPrincipal.getRuolo()));
    }

    @PostMapping("/create")
    public ResponseEntity<ObiettivoResponse> createObiettivo(@RequestBody CreateObiettivoRequest body) {
        return ResponseEntity.ok(obiettivoService.createObiettivo(
                body,
                userPrincipal.requireCodiceIstat(),
                userPrincipal.getId(),
                userPrincipal.getRuolo()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObiettivoResponse> updateObiettivo(@PathVariable Long id, @RequestBody UpdateObiettivoRequest body) {
        body.setId(id);
        return ResponseEntity.ok(obiettivoService.updateObiettivo(
                body, userPrincipal.getId(), userPrincipal.getRuolo()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ObiettivoResponse> deleteObiettivo(@PathVariable Long id) {
        return ResponseEntity.ok(obiettivoService.deleteObiettivo(
                id, userPrincipal.getId(), userPrincipal.getRuolo()));
    }

    @PostMapping("/progressivo")
    public ResponseEntity<ObiettivoResponse> registraProgressivo(@RequestBody RegistraProgressivoRequest body) {
        return ResponseEntity.ok(obiettivoService.registraProgressivo(
                body, userPrincipal.getId(), userPrincipal.getRuolo()));
    }

    @GetMapping("/count/totale")
    public ResponseEntity<Long> countTotale() {
        return ResponseEntity.ok(obiettivoService.countTotale());
    }

    @GetMapping("/count/attivi")
    public ResponseEntity<Long> countAttivi() {
        return ResponseEntity.ok(obiettivoService.countAttivi());
    }

    @GetMapping("/count/completati")
    public ResponseEntity<Long> countCompletati() {
        return ResponseEntity.ok(obiettivoService.countCompletati());
    }
}
