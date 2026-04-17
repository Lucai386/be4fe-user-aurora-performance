package com.be4fe_user_aurora_performance.dto.lpm;

import java.time.LocalDateTime;

public class LpmNoteDto {
    private Long id;
    private String testo;
    private Integer autoreId;
    private String autoreNome;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTesto() { return testo; }
    public void setTesto(String testo) { this.testo = testo; }

    public Integer getAutoreId() { return autoreId; }
    public void setAutoreId(Integer autoreId) { this.autoreId = autoreId; }

    public String getAutoreNome() { return autoreNome; }
    public void setAutoreNome(String autoreNome) { this.autoreNome = autoreNome; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
