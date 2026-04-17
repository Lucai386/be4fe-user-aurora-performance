package com.be4fe_user_aurora_performance.dto.org;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrutturaDto {

    private Integer id;
    private String nome;
    private String tipo;
    private Integer idParent;
    private String parentNome;
    private Integer idResponsabile;
    private String responsabileNome;
    private String ruoloLabel;
    private String colore;
    private Integer ordine;
    private List<StaffMemberDto> staff;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffMemberDto {
        private Integer id;
        private Integer userId;
        private String nome;
        private String cognome;
        private String nomeCompleto;
        private String ruoloStruttura;
        private Integer ordine;
    }
}
