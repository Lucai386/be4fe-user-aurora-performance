package com.be4fe_user_aurora_performance.dto.lpm;

public class LpmListRequest {
    private String codiceIstat;
    private Integer annoInizioMandato;
    private Integer annoFineMandato;

    public String getCodiceIstat() { return codiceIstat; }
    public void setCodiceIstat(String codiceIstat) { this.codiceIstat = codiceIstat; }

    public Integer getAnnoInizioMandato() { return annoInizioMandato; }
    public void setAnnoInizioMandato(Integer annoInizioMandato) { this.annoInizioMandato = annoInizioMandato; }

    public Integer getAnnoFineMandato() { return annoFineMandato; }
    public void setAnnoFineMandato(Integer annoFineMandato) { this.annoFineMandato = annoFineMandato; }
}
