package com.be4fe_user_aurora_performance.dto.lpm;

/**
 * Request per creare una LPM - matcha frontend LpmCreateRequest
 */
public class LpmCreateRequest {
    private String title;
    private String notes;  // Nel FE è una stringa, non una lista
    private String status;
    
    // Campi opzionali extra per il backend
    private String codiceIstat;
    private Integer annoInizioMandato;
    private Integer annoFineMandato;
    private Integer priority;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCodiceIstat() { return codiceIstat; }
    public void setCodiceIstat(String codiceIstat) { this.codiceIstat = codiceIstat; }

    public Integer getAnnoInizioMandato() { return annoInizioMandato; }
    public void setAnnoInizioMandato(Integer annoInizioMandato) { this.annoInizioMandato = annoInizioMandato; }

    public Integer getAnnoFineMandato() { return annoFineMandato; }
    public void setAnnoFineMandato(Integer annoFineMandato) { this.annoFineMandato = annoFineMandato; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
