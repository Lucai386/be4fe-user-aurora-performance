package com.be4fe_user_aurora_performance.dto.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddStaffRequest {

    private Integer idUser;
    private String ruoloStruttura;
    private Integer ordine;
}
