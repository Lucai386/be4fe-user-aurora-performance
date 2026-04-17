package com.be4fe_user_aurora_performance.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Integer id;
    private String nome;
    private String cognome;
    private String email;
    private String codiceFiscale;
    private String codiceIstat;
    private String ruolo;
    private String createdAt;
}
