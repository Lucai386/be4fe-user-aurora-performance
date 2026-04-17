package com.be4fe_user_aurora_performance.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 50, message = "Il nome non può superare 50 caratteri")
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Size(max = 50, message = "Il cognome non può superare 50 caratteri")
    private String cognome;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Email non valida")
    private String email;

    @NotBlank(message = "Il codice fiscale è obbligatorio")
    @Size(min = 16, max = 16, message = "Il codice fiscale deve essere di 16 caratteri")
    @Pattern(regexp = "^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$", message = "Codice fiscale non valido")
    private String codiceFiscale;

    @NotBlank(message = "Il ruolo è obbligatorio")
    @Pattern(regexp = "^(SC|DR|CS|CP|DB)$", message = "Ruolo non valido")
    private String ruolo;

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 8, message = "La password deve essere di almeno 8 caratteri")
    private String password;
}
