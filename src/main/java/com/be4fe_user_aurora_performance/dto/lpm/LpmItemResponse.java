package com.be4fe_user_aurora_performance.dto.lpm;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response per operazioni LPM che restituiscono un singolo item
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LpmItemResponse {
    private String result;
    private String errorCode;
    private String message;
    private LpmActivityDto item;

    public static LpmItemResponse ok(LpmActivityDto item) {
        LpmItemResponse response = new LpmItemResponse();
        response.setResult("OK");
        response.setItem(item);
        return response;
    }

    public static LpmItemResponse error(String errorCode, String message) {
        LpmItemResponse response = new LpmItemResponse();
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

    public LpmActivityDto getItem() { return item; }
    public void setItem(LpmActivityDto item) { this.item = item; }
}
