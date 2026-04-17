package com.be4fe_user_aurora_performance.dto.admin;

import com.be4fe_user_aurora_performance.dto.session.SessionInfoDto;
import com.be4fe_user_aurora_performance.dto.session.SessionUserDto;
import com.be4fe_user_aurora_performance.enums.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.be4fe_user_aurora_performance.enums.AppConstants.RESULT_OK;
import static com.be4fe_user_aurora_performance.enums.AppConstants.RESULT_KO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchEnteResponse {
    private String result;
    private String errorCode;
    private SessionUserDto user;
    private SessionInfoDto session;

    public static SwitchEnteResponse success(SessionUserDto user, SessionInfoDto session) {
        return SwitchEnteResponse.builder()
                .result(RESULT_OK)
                .user(user)
                .session(session)
                .build();
    }

    public static SwitchEnteResponse error(ErrorCode errorCode) {
        return SwitchEnteResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .build();
    }
}
