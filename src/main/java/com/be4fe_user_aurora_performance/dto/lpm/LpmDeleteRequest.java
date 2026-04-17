package com.be4fe_user_aurora_performance.dto.lpm;

/**
 * Request per eliminare una LPM - matcha frontend LpmDeleteRequest
 */
public class LpmDeleteRequest {
    private String id;
    private String reason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Long getIdAsLong() { 
        return id != null ? Long.parseLong(id) : null; 
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
