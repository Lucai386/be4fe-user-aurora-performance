package com.be4fe_user_aurora_performance.dto.lpm;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO che mappa LpmActivity del frontend
 */
public class LpmActivityDto {
    private String id;
    private String title;
    private String description;
    private String status;
    private Integer priority;
    private Integer progress;
    
    @JsonProperty("responsibleName")
    private String responsibleName;
    
    @JsonProperty("dupId")
    private String dupId;
    
    @JsonProperty("dupTitle")
    private String dupTitle;
    
    private List<LpmNoteActivityDto> notes;

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public String getResponsibleName() { return responsibleName; }
    public void setResponsibleName(String responsibleName) { this.responsibleName = responsibleName; }

    public String getDupId() { return dupId; }
    public void setDupId(String dupId) { this.dupId = dupId; }

    public String getDupTitle() { return dupTitle; }
    public void setDupTitle(String dupTitle) { this.dupTitle = dupTitle; }

    public List<LpmNoteActivityDto> getNotes() { return notes; }
    public void setNotes(List<LpmNoteActivityDto> notes) { this.notes = notes; }
}
