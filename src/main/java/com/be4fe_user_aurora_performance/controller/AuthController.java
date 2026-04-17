package com.be4fe_user_aurora_performance.controller;

import com.be4fe_user_aurora_performance.dto.auth.LoginRequest;
import com.be4fe_user_aurora_performance.dto.auth.LoginResponse;
import com.be4fe_user_aurora_performance.dto.auth.LogoutRequest;
import com.be4fe_user_aurora_performance.dto.auth.RefreshTokenRequest;
import com.be4fe_user_aurora_performance.dto.common.ApiResponse;
import com.be4fe_user_aurora_performance.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API per autenticazione e gestione token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login utente", description = "Autentica l'utente e restituisce i token JWT")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login effettuato con successo"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Rinnova l'access token usando il refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Token rinnovato con successo"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout utente", description = "Invalida la sessione e i token dell'utente")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request) {

        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logout effettuato con successo"));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
