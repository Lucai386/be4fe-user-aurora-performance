package com.be4fe_user_aurora_performance.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_user_aurora_performance.dto.lpm.LpmActivityDto;
import com.be4fe_user_aurora_performance.dto.lpm.LpmCreateRequest;
import com.be4fe_user_aurora_performance.dto.lpm.LpmDeleteRequest;
import com.be4fe_user_aurora_performance.dto.lpm.LpmGetRequest;
import com.be4fe_user_aurora_performance.dto.lpm.LpmIdResponse;
import com.be4fe_user_aurora_performance.dto.lpm.LpmItemResponse;
import com.be4fe_user_aurora_performance.dto.lpm.LpmListRequest;
import com.be4fe_user_aurora_performance.dto.lpm.LpmListResponse;
import com.be4fe_user_aurora_performance.dto.lpm.LpmUpdateRequest;
import com.be4fe_user_aurora_performance.principal.UserPrincipal;
import com.be4fe_user_aurora_performance.service.LpmService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/lpm")
@Tag(name = "LPM", description = "Linee Programmatiche di Mandato")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class LpmController {

    private final LpmService lpmService;
    private final UserPrincipal userPrincipal;

    @PostMapping("/list")
    @Operation(summary = "Lista tutte le LPM attive")
    public ResponseEntity<LpmListResponse> list(@RequestBody(required = false) LpmListRequest request) {
        List<LpmActivityDto> items = lpmService.list(request);
        return ResponseEntity.ok(LpmListResponse.ok(items));
    }

    @PostMapping("/get")
    @Operation(summary = "Dettaglio singola LPM")
    public ResponseEntity<LpmItemResponse> get(@RequestBody LpmGetRequest request) {
        Long id = request.getIdAsLong();
        if (id == null) {
            return ResponseEntity.ok(LpmItemResponse.error("INVALID_ID", "ID non valido"));
        }
        return lpmService.get(id)
                .map(dto -> ResponseEntity.ok(LpmItemResponse.ok(dto)))
                .orElse(ResponseEntity.ok(LpmItemResponse.error("NOT_FOUND", "LPM non trovata")));
    }

    @PostMapping("/create")
    @Operation(summary = "Crea nuova LPM")
    public ResponseEntity<LpmItemResponse> create(
            @RequestBody LpmCreateRequest request) {
        Integer userId = userPrincipal.getId().intValue();
        LpmActivityDto created = lpmService.create(request, userId);
        return ResponseEntity.ok(LpmItemResponse.ok(created));
    }

    @PostMapping("/update")
    @Operation(summary = "Aggiorna LPM esistente")
    public ResponseEntity<LpmIdResponse> update(
            @RequestBody LpmUpdateRequest request) {
        Integer userId = userPrincipal.getId().intValue();
        return lpmService.update(request, userId)
                .map(dto -> ResponseEntity.ok(LpmIdResponse.ok(dto.getId())))
                .orElse(ResponseEntity.ok(LpmIdResponse.error("NOT_FOUND", "LPM non trovata")));
    }

    @PostMapping("/delete")
    @Operation(summary = "Elimina LPM (soft delete)")
    public ResponseEntity<LpmIdResponse> delete(
            @RequestBody LpmDeleteRequest request) {
        Integer userId = userPrincipal.getId().intValue();
        boolean deleted = lpmService.delete(request, userId);
        if (deleted) {
            return ResponseEntity.ok(LpmIdResponse.ok(request.getId()));
        }
        return ResponseEntity.ok(LpmIdResponse.error("NOT_FOUND", "LPM non trovata"));
    }

    @PostMapping("/{lpmId}/dup/{dupId}")
    @Operation(summary = "Collega LPM a un DUP")
    public ResponseEntity<LpmItemResponse> linkDup(
            @PathVariable Long lpmId,
            @PathVariable Long dupId) {
        Integer userId = userPrincipal.getId().intValue();
        return lpmService.linkDup(lpmId, dupId, userId)
                .map(dto -> ResponseEntity.ok(LpmItemResponse.ok(dto)))
                .orElse(ResponseEntity.ok(LpmItemResponse.error("NOT_FOUND", "LPM o DUP non trovato")));
    }

    @DeleteMapping("/{lpmId}/dup")
    @Operation(summary = "Scollega LPM da un DUP")
    public ResponseEntity<LpmItemResponse> unlinkDup(
            @PathVariable Long lpmId) {
        Integer userId = userPrincipal.getId().intValue();
        return lpmService.unlinkDup(lpmId, userId)
                .map(dto -> ResponseEntity.ok(LpmItemResponse.ok(dto)))
                .orElse(ResponseEntity.ok(LpmItemResponse.error("NOT_FOUND", "LPM non trovata")));
    }
}
