package com.be4fe_user_aurora_performance.dto.lpm;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response per operazioni LPM che restituiscono una lista
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LpmListResponse {
    private String result;
    private String errorCode;
    private String message;
    private List<LpmActivityDto> items;

    public static LpmListResponse ok(List<LpmActivityDto> items) {
        LpmListResponse response = new LpmListResponse();
        response.setResult("OK");
        response.setItems(items);
        return response;
    }

    public static LpmListResponse error(String errorCode, String message) {
        LpmListResponse response = new LpmListResponse();
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

    public List<LpmActivityDto> getItems() { return items; }
    public void setItems(List<LpmActivityDto> items) { this.items = items; }
}
