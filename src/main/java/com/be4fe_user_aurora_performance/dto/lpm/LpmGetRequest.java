package com.be4fe_user_aurora_performance.dto.lpm;

/**
 * Request per ottenere una LPM - matcha frontend LpmGetRequest
 */
public class LpmGetRequest {
    private String id;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Long getIdAsLong() { 
        return id != null ? Long.parseLong(id) : null; 
    }
}
