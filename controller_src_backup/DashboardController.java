package com.bff_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bff_user_aurora_performance.dto.dashboard.DashboardResponse;
import com.bff_user_aurora_performance.service.DashboardService;
import com.bff_user_aurora_performance.service.SessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller per le metriche della Dashboard.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "API per le metriche e statistiche della dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SessionService sessionService;

    @PostMapping("/metrics")
    @Operation(summary = "Ottieni metriche dashboard", 
               description = "Restituisce le statistiche della dashboard. Admin/dirigenti vedono metriche aggregate, dipendenti vedono metriche personali.")
    public ResponseEntity<DashboardResponse> getMetrics(Authentication authentication) {
        log.debug("Dashboard metrics request received");

        String codiceIstat = sessionService.getCodiceIstat(authentication);
        String userRole = sessionService.getUserRole(authentication);
        Integer userId = sessionService.getUserId(authentication);

        DashboardResponse response = dashboardService.getMetrics(codiceIstat, userRole, userId);

        if ("OK".equals(response.getResult())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(400).body(response);
        }
    }
}
