package com.be4fe_user_aurora_performance.dto.admin;

import java.util.List;

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
public class ListUsersResponse {
    private String result;
    private String errorCode;
    private String message;
    private List<UserDto> users;

    public static ListUsersResponse success(List<UserDto> users) {
        return ListUsersResponse.builder()
                .result(RESULT_OK)
                .users(users)
                .build();
    }

    public static ListUsersResponse error(ErrorCode errorCode, String message) {
        return ListUsersResponse.builder()
                .result(RESULT_KO)
                .errorCode(errorCode.getCode())
                .message(message)
                .build();
    }
}
