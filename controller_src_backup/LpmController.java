package com.bff_user_aurora_performance.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bff_user_aurora_performance.dto.lpm.LpmActivityDto;
import com.bff_user_aurora_performance.dto.lpm.LpmCreateRequest;
import com.bff_user_aurora_performance.dto.lpm.LpmDeleteRequest;
import com.bff_user_aurora_performance.dto.lpm.LpmGetRequest;
import com.bff_user_aurora_performance.dto.lpm.LpmIdResponse;
import com.bff_user_aurora_performance.dto.lpm.LpmItemResponse;
import com.bff_user_aurora_performance.dto.lpm.LpmListRequest;
import com.bff_user_aurora_performance.dto.lpm.LpmListResponse;
import com.bff_user_aurora_performance.dto.lpm.LpmUpdateRequest;
import com.bff_user_aurora_performance.repository.UserRepository;
import com.bff_user_aurora_performance.service.LpmService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/lpm")
@Tag(name = "LPM", description = "Linee Programmatiche di Mandato")
@SecurityRequirement(name = "bearerAuth")
public class LpmController {

    private final LpmService lpmService;
    private final UserRepository userRepository;

    public LpmController(LpmService lpmService, UserRepository userRepository) {
        this.lpmService = lpmService;
        this.userRepository = userRepository;
    }

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
            @RequestBody LpmCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer userId = getUserId(jwt);
        LpmActivityDto created = lpmService.create(request, userId);
        return ResponseEntity.ok(LpmItemResponse.ok(created));
    }

    @PostMapping("/update")
    @Operation(summary = "Aggiorna LPM esistente")
    public ResponseEntity<LpmIdResponse> update(
            @RequestBody LpmUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer userId = getUserId(jwt);
        return lpmService.update(request, userId)
                .map(dto -> ResponseEntity.ok(LpmIdResponse.ok(dto.getId())))
                .orElse(ResponseEntity.ok(LpmIdResponse.error("NOT_FOUND", "LPM non trovata")));
    }

    @PostMapping("/delete")
    @Operation(summary = "Elimina LPM (soft delete)")
    public ResponseEntity<LpmIdResponse> delete(
            @RequestBody LpmDeleteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer userId = getUserId(jwt);
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
            @PathVariable Long dupId,
            @AuthenticationPrincipal Jwt jwt) {
        Integer userId = getUserId(jwt);
        return lpmService.linkDup(lpmId, dupId, userId)
                .map(dto -> ResponseEntity.ok(LpmItemResponse.ok(dto)))
                .orElse(ResponseEntity.ok(LpmItemResponse.error("NOT_FOUND", "LPM o DUP non trovato")));
    }

    @DeleteMapping("/{lpmId}/dup")
    @Operation(summary = "Scollega LPM da un DUP")
    public ResponseEntity<LpmItemResponse> unlinkDup(
            @PathVariable Long lpmId,
            @AuthenticationPrincipal Jwt jwt) {
        Integer userId = getUserId(jwt);
        return lpmService.unlinkDup(lpmId, userId)
                .map(dto -> ResponseEntity.ok(LpmItemResponse.ok(dto)))
                .orElse(ResponseEntity.ok(LpmItemResponse.error("NOT_FOUND", "LPM non trovata")));
    }

    private Integer getUserId(Jwt jwt) {
        if (jwt == null) return null;
        String keycloakId = jwt.getSubject();
        return userRepository.findByKeycloakId(keycloakId)
                .map(user -> user.getId())
                .orElse(null);
    }
}
