package com.be4fe_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_user_aurora_performance.dto.dashboard.DashboardResponse;
import com.be4fe_user_aurora_performance.principal.UserPrincipal;
import com.be4fe_user_aurora_performance.service.DashboardService;

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
    private final UserPrincipal userPrincipal;

    @PostMapping("/metrics")
    @Operation(summary = "Ottieni metriche dashboard", 
               description = "Restituisce le statistiche della dashboard. Admin/dirigenti vedono metriche aggregate, dipendenti vedono metriche personali.")
    public ResponseEntity<DashboardResponse> getMetrics() {
        log.debug("Dashboard metrics request received");
        DashboardResponse response = dashboardService.getMetrics(
                userPrincipal.requireCodiceIstat(),
                userPrincipal.getRuolo(),
                userPrincipal.getId().intValue());
        if ("OK".equals(response.getResult())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(400).body(response);
        }
    }
}
