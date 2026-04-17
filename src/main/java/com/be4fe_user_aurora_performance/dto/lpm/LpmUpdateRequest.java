package com.be4fe_user_aurora_performance.dto.lpm;

/**
 * Request per aggiornare una LPM - matcha frontend LpmUpdateRequest
 */
public class LpmUpdateRequest {
    private String id;  // FE usa string
    private String title;
    private String notes;  // FE usa string
    private String status;
    
    // Campi extra opzionali
    private Integer priority;
    private Integer progress;
    private String description;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Long getIdAsLong() { 
        return id != null ? Long.parseLong(id) : null; 
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
