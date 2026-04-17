package com.be4fe_user_aurora_performance.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoDto {

    private String issuedAt;
    private String expiresAt;
}
