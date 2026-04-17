package com.be4fe_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_user_aurora_performance.dto.session.SessionResponse;
import com.be4fe_user_aurora_performance.service.SessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Session", description = "API per la gestione della sessione utente")
@SecurityRequirement(name = "bearerAuth")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @Operation(summary = "Ottieni dati sessione", description = "Restituisce i dati dell'utente corrente e info sulla sessione JWT")
    public ResponseEntity<SessionResponse> getSession(Authentication authentication) {
        log.debug("Session request received");
        SessionResponse response = sessionService.getSession(authentication);
        
        if ("OK".equals(response.getResult())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }
}
