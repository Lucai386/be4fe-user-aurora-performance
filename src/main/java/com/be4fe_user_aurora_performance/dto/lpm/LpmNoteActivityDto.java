package com.be4fe_user_aurora_performance.dto.lpm;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO per le note LPM che mappa LpmNote del frontend
 */
public class LpmNoteActivityDto {
    private String id;
    private String text;
    private String author;
    
    @JsonProperty("createdAt")
    private String createdAt;

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
