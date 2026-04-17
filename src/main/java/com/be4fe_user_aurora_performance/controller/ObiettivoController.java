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
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_user_aurora_performance.dto.obiettivo.CreateObiettivoRequest;
import com.be4fe_user_aurora_performance.dto.obiettivo.ListObiettiviResponse;
import com.be4fe_user_aurora_performance.dto.obiettivo.ObiettivoResponse;
import com.be4fe_user_aurora_performance.dto.obiettivo.RegistraProgressivoRequest;
import com.be4fe_user_aurora_performance.dto.obiettivo.UpdateObiettivoRequest;
import com.be4fe_user_aurora_performance.service.ObiettivoService;
import com.be4fe_user_aurora_performance.service.SessionService;

import lombok.RequiredArgsConstructor;

/**
 * Controller per la gestione degli Obiettivi operativi.
 */
@RestController
@RequestMapping("/api/obiettivi")
@RequiredArgsConstructor
public class ObiettivoController {

    private final ObiettivoService obiettivoService;
    private final SessionService sessionService;

    @PostMapping("/list")
    public ResponseEntity<ListObiettiviResponse> listObiettivi(Authentication authentication) {
        String codiceIstat = sessionService.getCodiceIstat(authentication);
        Long userId = sessionService.getUserId(authentication).longValue();
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(obiettivoService.listObiettivi(codiceIstat, userId, userRole));
    }

    @GetMapping("/miei")
    public ResponseEntity<ListObiettiviResponse> listMieiObiettivi(Authentication authentication) {
        Long userId = sessionService.getUserId(authentication).longValue();
        return ResponseEntity.ok(obiettivoService.listMieiObiettivi(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObiettivoResponse> getObiettivo(@PathVariable Long id, Authentication authentication) {
        Long userId = sessionService.getUserId(authentication).longValue();
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(obiettivoService.getObiettivo(id, userId, userRole));
    }

    @PostMapping("/create")
    public ResponseEntity<ObiettivoResponse> createObiettivo(@RequestBody CreateObiettivoRequest body, Authentication authentication) {
        String codiceIstat = sessionService.getCodiceIstat(authentication);
        Long userId = sessionService.getUserId(authentication).longValue();
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(obiettivoService.createObiettivo(body, codiceIstat, userId, userRole));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObiettivoResponse> updateObiettivo(@PathVariable Long id, @RequestBody UpdateObiettivoRequest body, Authentication authentication) {
        Long userId = sessionService.getUserId(authentication).longValue();
        String userRole = sessionService.getUserRole(authentication);
        body.setId(id);
        return ResponseEntity.ok(obiettivoService.updateObiettivo(body, userId, userRole));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ObiettivoResponse> deleteObiettivo(@PathVariable Long id, Authentication authentication) {
        Long userId = sessionService.getUserId(authentication).longValue();
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(obiettivoService.deleteObiettivo(id, userId, userRole));
    }

    @PostMapping("/progressivo")
    public ResponseEntity<ObiettivoResponse> registraProgressivo(@RequestBody RegistraProgressivoRequest body, Authentication authentication) {
        Long userId = sessionService.getUserId(authentication).longValue();
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(obiettivoService.registraProgressivo(body, userId, userRole));
    }

    // Conteggi per dashboard
    @GetMapping("/count/totale")
    public ResponseEntity<Long> countTotale(Authentication authentication) {
        String codiceIstat = sessionService.getCodiceIstat(authentication);
        return ResponseEntity.ok(obiettivoService.countTotale(codiceIstat));
    }

    @GetMapping("/count/attivi")
    public ResponseEntity<Long> countAttivi(Authentication authentication) {
        String codiceIstat = sessionService.getCodiceIstat(authentication);
        return ResponseEntity.ok(obiettivoService.countAttivi(codiceIstat));
    }

    @GetMapping("/count/completati")
    public ResponseEntity<Long> countCompletati(Authentication authentication) {
        String codiceIstat = sessionService.getCodiceIstat(authentication);
        return ResponseEntity.ok(obiettivoService.countCompletati(codiceIstat));
    }
}
