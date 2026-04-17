package com.be4fe_user_aurora_performance.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username è obbligatorio")
    private String username;

    @NotBlank(message = "Password è obbligatoria")
    private String password;
}
