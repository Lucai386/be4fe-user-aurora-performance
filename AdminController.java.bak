package com.bff_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bff_user_aurora_performance.dto.admin.CreateUserRequest;
import com.bff_user_aurora_performance.dto.admin.CreateUserResponse;
import com.bff_user_aurora_performance.dto.admin.DeleteUserResponse;
import com.bff_user_aurora_performance.dto.admin.ListUsersResponse;
import com.bff_user_aurora_performance.dto.admin.SwitchEnteRequest;
import com.bff_user_aurora_performance.dto.admin.SwitchEnteResponse;
import com.bff_user_aurora_performance.service.AdminService;
import com.bff_user_aurora_performance.service.UserManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "API per funzionalità amministrative")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final UserManagementService userManagementService;

    @PostMapping("/switch-ente")
    @Operation(summary = "Switch ente", description = "Permette all'admin di cambiare il contesto dell'ente usando il codice ISTAT")
    public ResponseEntity<SwitchEnteResponse> switchEnte(
            Authentication authentication,
            @RequestBody SwitchEnteRequest request) {
        log.debug("Switch ente request received for codiceIstat: {}", request.getCodiceIstat());
        SwitchEnteResponse response = adminService.switchEnte(authentication, request);

        if ("OK".equals(response.getResult())) {
            return ResponseEntity.ok(response);
        } else if ("NOT_AUTHORIZED".equals(response.getErrorCode())) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/users")
    @Operation(summary = "Lista utenti", description = "Elenca gli utenti dell'ente corrente")
    public ResponseEntity<ListUsersResponse> listUsers(Authentication authentication) {
        log.debug("List users request received");
        ListUsersResponse response = userManagementService.listUsers(authentication);

        if ("OK".equals(response.getResult())) {
            return ResponseEntity.ok(response);
        } else if ("NOT_AUTHORIZED".equals(response.getErrorCode())) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/users")
    @Operation(summary = "Crea utente", description = "Crea un nuovo utente per l'ente corrente")
    public ResponseEntity<CreateUserResponse> createUser(
            Authentication authentication,
            @Valid @RequestBody CreateUserRequest request) {
        log.debug("Create user request received for: {} {}", request.getNome(), request.getCognome());
        CreateUserResponse response = userManagementService.createUser(authentication, request);

        if ("OK".equals(response.getResult())) {
            return ResponseEntity.status(201).body(response);
        } else if ("NOT_AUTHORIZED".equals(response.getErrorCode())) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Elimina utente", description = "Elimina un utente dall'ente corrente")
    public ResponseEntity<DeleteUserResponse> deleteUser(
            Authentication authentication,
            @PathVariable Integer userId) {
        log.debug("Delete user request received for userId: {}", userId);
        DeleteUserResponse response = userManagementService.deleteUser(authentication, userId);

        if ("OK".equals(response.getResult())) {
            return ResponseEntity.ok(response);
        } else if ("NOT_AUTHORIZED".equals(response.getErrorCode())) {
            return ResponseEntity.status(403).body(response);
        } else if ("USER_NOT_FOUND".equals(response.getErrorCode())) {
            return ResponseEntity.status(404).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
