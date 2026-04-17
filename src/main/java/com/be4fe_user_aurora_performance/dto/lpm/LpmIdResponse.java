package com.be4fe_user_aurora_performance.dto.lpm;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response per operazioni LPM che restituiscono solo un ID
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LpmIdResponse {
    private String result;
    private String errorCode;
    private String message;
    private String id;

    public static LpmIdResponse ok(String id) {
        LpmIdResponse response = new LpmIdResponse();
        response.setResult("OK");
        response.setId(id);
        return response;
    }

    public static LpmIdResponse error(String errorCode, String message) {
        LpmIdResponse response = new LpmIdResponse();
        response.setResult("KO");
        response.setErrorCode(errorCode);
        response.setMessage(message);
        return response;
    }

    // Getters e Setters
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
