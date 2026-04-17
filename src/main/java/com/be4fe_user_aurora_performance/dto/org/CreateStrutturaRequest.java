package com.be4fe_user_aurora_performance.dto.org;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStrutturaRequest {

    @NotBlank(message = "Il nome è obbligatorio")
    private String nome;

    private String tipo;

    private Integer idParent;

    private Integer idResponsabile;

    private String ruoloLabel;

    private String colore;

    private Integer ordine;
}
