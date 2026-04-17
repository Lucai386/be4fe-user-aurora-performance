package com.be4fe_user_aurora_performance.dto.dashboard;

import com.be4fe_user_aurora_performance.enums.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Risposta per le metriche della dashboard.
 * Contiene sia metriche aggregate (per admin/dirigenti) che personali (per dipendenti).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private String result;
    private String errorCode;
    private String message;
    /** Metriche aggregate per admin/dirigenti */
    private DashboardMetricsDto metrics;
    /** Metriche personali per dipendenti */
    private PersonalMetricsDto personalMetrics;
    /** Tipo di vista: "admin" o "personal" */
    private String viewType;

    public static DashboardResponse successAdmin(DashboardMetricsDto metrics) {
        return DashboardResponse.builder()
                .result("OK")
                .metrics(metrics)
                .viewType("admin")
                .build();
    }

    public static DashboardResponse successPersonal(PersonalMetricsDto personalMetrics) {
        return DashboardResponse.builder()
                .result("OK")
                .personalMetrics(personalMetrics)
                .viewType("personal")
                .build();
    }

    public static DashboardResponse error(ErrorCode code) {
        return DashboardResponse.builder()
                .result("KO")
                .errorCode(code.name())
                .message(code.getDefaultMessage())
                .build();
    }
}
