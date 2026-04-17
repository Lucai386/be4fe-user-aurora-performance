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

import com.bff_user_aurora_performance.dto.dup.CreateDupRequest;
import com.bff_user_aurora_performance.dto.dup.DeleteDupResponse;
import com.bff_user_aurora_performance.dto.dup.DupResponse;
import com.bff_user_aurora_performance.dto.dup.ListDupResponse;
import com.bff_user_aurora_performance.dto.dup.UpdateDupRequest;
import com.bff_user_aurora_performance.service.DupService;
import com.bff_user_aurora_performance.service.SessionService;

import lombok.RequiredArgsConstructor;

/**
 * Controller per la gestione dei DUP (Documento Unico di Programmazione).
 * Un DUP è un insieme di progetti.
 */
@RestController
@RequestMapping("/api/dup")
@RequiredArgsConstructor
public class DupController {

    private final DupService dupService;
    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<ListDupResponse> listDup(Authentication authentication) {
        String codiceIstat = sessionService.getCodiceIstat(authentication);
        String userRole = sessionService.getUserRole(authentication);
        Integer userId = sessionService.getUserId(authentication);
        return ResponseEntity.ok(dupService.listDup(codiceIstat, userRole, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DupResponse> getDup(@PathVariable Long id, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(dupService.getDup(id, userRole));
    }

    @PostMapping
    public ResponseEntity<DupResponse> createDup(@RequestBody CreateDupRequest body, Authentication authentication) {
        String codiceIstat = sessionService.getCodiceIstat(authentication);
        Integer userId = sessionService.getUserId(authentication);
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(dupService.createDup(body, codiceIstat, userId, userRole));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DupResponse> updateDup(@PathVariable Long id, @RequestBody UpdateDupRequest body, Authentication authentication) {
        Integer userId = sessionService.getUserId(authentication);
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(dupService.updateDup(id, body, userId, userRole));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteDupResponse> deleteDup(@PathVariable Long id, Authentication authentication) {
        String userRole = sessionService.getUserRole(authentication);
        return ResponseEntity.ok(dupService.deleteDup(id, userRole));
    }
}
