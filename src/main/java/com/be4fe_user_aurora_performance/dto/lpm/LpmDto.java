package com.be4fe_user_aurora_performance.dto.lpm;

import java.time.LocalDateTime;
import java.util.List;

public class LpmDto {
    private Long id;
    private String codiceIstat;
    private Integer annoInizioMandato;
    private Integer annoFineMandato;
    private String titolo;
    private String descrizione;
    private String stato;
    private Integer priorita;
    private Integer progresso;
    private Integer responsabileId;
    private String responsabileNome;
    private Long dupId;
    private LocalDateTime createdAt;
    private List<LpmNoteDto> note;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodiceIstat() { return codiceIstat; }
    public void setCodiceIstat(String codiceIstat) { this.codiceIstat = codiceIstat; }

    public Integer getAnnoInizioMandato() { return annoInizioMandato; }
    public void setAnnoInizioMandato(Integer annoInizioMandato) { this.annoInizioMandato = annoInizioMandato; }

    public Integer getAnnoFineMandato() { return annoFineMandato; }
    public void setAnnoFineMandato(Integer annoFineMandato) { this.annoFineMandato = annoFineMandato; }

    public String getTitolo() { return titolo; }
    public void setTitolo(String titolo) { this.titolo = titolo; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }

    public Integer getPriorita() { return priorita; }
    public void setPriorita(Integer priorita) { this.priorita = priorita; }

    public Integer getProgresso() { return progresso; }
    public void setProgresso(Integer progresso) { this.progresso = progresso; }

    public Integer getResponsabileId() { return responsabileId; }
    public void setResponsabileId(Integer responsabileId) { this.responsabileId = responsabileId; }

    public String getResponsabileNome() { return responsabileNome; }
    public void setResponsabileNome(String responsabileNome) { this.responsabileNome = responsabileNome; }

    public Long getDupId() { return dupId; }
    public void setDupId(Long dupId) { this.dupId = dupId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<LpmNoteDto> getNote() { return note; }
    public void setNote(List<LpmNoteDto> note) { this.note = note; }
}
