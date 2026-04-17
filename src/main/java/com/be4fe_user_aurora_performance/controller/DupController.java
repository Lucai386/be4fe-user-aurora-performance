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

import com.be4fe_user_aurora_performance.dto.dup.CreateDupRequest;
import com.be4fe_user_aurora_performance.dto.dup.DeleteDupResponse;
import com.be4fe_user_aurora_performance.dto.dup.DupResponse;
import com.be4fe_user_aurora_performance.dto.dup.ListDupResponse;
import com.be4fe_user_aurora_performance.dto.dup.UpdateDupRequest;
import com.be4fe_user_aurora_performance.principal.UserPrincipal;
import com.be4fe_user_aurora_performance.service.DupService;

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
    private final UserPrincipal userPrincipal;

    @GetMapping
    public ResponseEntity<ListDupResponse> listDup() {
        return ResponseEntity.ok(dupService.listDup(
                userPrincipal.requireCodiceIstat(),
                userPrincipal.getRuolo(),
                userPrincipal.getId().intValue()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DupResponse> getDup(@PathVariable Long id) {
        return ResponseEntity.ok(dupService.getDup(id, userPrincipal.getRuolo()));
    }

    @PostMapping
    public ResponseEntity<DupResponse> createDup(@RequestBody CreateDupRequest body) {
        return ResponseEntity.ok(dupService.createDup(
                body,
                userPrincipal.requireCodiceIstat(),
                userPrincipal.getId().intValue(),
                userPrincipal.getRuolo()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DupResponse> updateDup(@PathVariable Long id, @RequestBody UpdateDupRequest body) {
        return ResponseEntity.ok(dupService.updateDup(
                id, body,
                userPrincipal.getId().intValue(),
                userPrincipal.getRuolo()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteDupResponse> deleteDup(@PathVariable Long id) {
        return ResponseEntity.ok(dupService.deleteDup(id, userPrincipal.getRuolo()));
    }
}
