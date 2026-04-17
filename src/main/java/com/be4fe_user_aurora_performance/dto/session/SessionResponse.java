package com.be4fe_user_aurora_performance.dto.session;

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
public class SessionResponse {

    private String result;
    private String errorCode;
    private SessionUserDto user;
    private SessionInfoDto session;

    public static SessionResponse success(SessionUserDto user, SessionInfoDto session) {
        return SessionResponse.builder()
                .result(RESULT_OK)
                .errorCode(null)
                .user(user)
                .session(session)
                .build();
    }

    public static SessionResponse error(ErrorCode errorCode) {
        return SessionResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .user(null)
                .session(null)
                .build();
    }
}
