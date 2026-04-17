package com.be4fe_user_aurora_performance.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchEnteRequest {
    private String codiceIstat;
}
