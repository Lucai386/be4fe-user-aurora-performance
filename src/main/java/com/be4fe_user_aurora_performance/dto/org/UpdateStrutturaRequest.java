package com.be4fe_user_aurora_performance.dto.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStrutturaRequest {

    private String nome;

    private String tipo;

    private Integer idParent;

    /** Se true, rimuove il parent (rende la struttura radice) */
    private Boolean removeParent;

    private Integer idResponsabile;

    /** Se true, rimuove il responsabile */
    private Boolean removeResponsabile;

    private String ruoloLabel;

    private String colore;

    private Integer ordine;
}
