package com.be4fe_user_aurora_performance.enums;

/**
 * Enum centralizzato per tutti i codici di errore dell'applicazione.
 * Ogni codice è mappato a una chiave i18n nel frontend.
 */
public enum ErrorCode {
    // Errori generici
    INTERNAL_ERROR("INTERNAL_ERROR"),
    VALIDATION_ERROR("VALIDATION_ERROR"),
    NOT_AUTHORIZED("NOT_AUTHORIZED"),
    NOT_FOUND("NOT_FOUND"),
    INVALID_ID("INVALID_ID"),
    
    // Errori sessione
    SESSION_INVALID("SESSION_INVALID"),
    USER_NOT_FOUND("USER_NOT_FOUND"),
    
    // Errori admin/ente
    NO_ENTE("NO_ENTE"),
    CODICE_ISTAT_REQUIRED("CODICE_ISTAT_REQUIRED"),
    
    // Errori gestione utenti
    CF_EXISTS("CF_EXISTS"),
    KEYCLOAK_ERROR("KEYCLOAK_ERROR"),
    CANNOT_DELETE_ADMIN("CANNOT_DELETE_ADMIN"),
    
    // Errori strutture/organigramma
    STRUTTURA_NOT_FOUND("STRUTTURA_NOT_FOUND"),
    PARENT_NOT_FOUND("PARENT_NOT_FOUND"),
    PARENT_DIFFERENT_ENTE("PARENT_DIFFERENT_ENTE"),
    INVALID_PARENT("INVALID_PARENT"),
    RESPONSABILE_NOT_FOUND("RESPONSABILE_NOT_FOUND"),
    RESPONSABILE_DIFFERENT_ENTE("RESPONSABILE_DIFFERENT_ENTE"),
    HAS_CHILDREN("HAS_CHILDREN"),
    STAFF_ALREADY_EXISTS("STAFF_ALREADY_EXISTS"),
    STAFF_NOT_FOUND("STAFF_NOT_FOUND"),
    
    // Errori LPM
    LPM_NOT_FOUND("LPM_NOT_FOUND"),
    
    // Errori DUP
    DUP_NOT_FOUND("DUP_NOT_FOUND"),
    DUP_NOT_AUTHORIZED("DUP_NOT_AUTHORIZED"),
    DUP_ANNO_REQUIRED("DUP_ANNO_REQUIRED"),
    DUP_TITOLO_REQUIRED("DUP_TITOLO_REQUIRED"),
    DUP_HAS_PROGETTI("DUP_HAS_PROGETTI"),
    
    // Errori Progetti
    PROGETTO_NOT_FOUND("PROGETTO_NOT_FOUND"),
    PROGETTO_TITOLO_REQUIRED("PROGETTO_TITOLO_REQUIRED"),
    
    // Errori Attività
    ATTIVITA_NOT_FOUND("ATTIVITA_NOT_FOUND"),
    ATTIVITA_CODICE_REQUIRED("ATTIVITA_CODICE_REQUIRED"),
    ATTIVITA_TITOLO_REQUIRED("ATTIVITA_TITOLO_REQUIRED"),
    ATTIVITA_NOT_AUTHORIZED("ATTIVITA_NOT_AUTHORIZED"),
    ASSEGNAZIONE_NOT_FOUND("ASSEGNAZIONE_NOT_FOUND"),
    ASSEGNAZIONE_ALREADY_EXISTS("ASSEGNAZIONE_ALREADY_EXISTS"),
    TIMESHEET_ENTRY_NOT_FOUND("TIMESHEET_ENTRY_NOT_FOUND"),
    USER_NOT_ASSIGNED("USER_NOT_ASSIGNED"),
    INVALID_PERCENTAGE("INVALID_PERCENTAGE"),
    STEP_NOT_FOUND("STEP_NOT_FOUND"),
    STEP_TITOLO_REQUIRED("STEP_TITOLO_REQUIRED"),
    
    // Errori Obiettivi
    OBIETTIVO_NOT_FOUND("OBIETTIVO_NOT_FOUND"),
    OBIETTIVO_NOT_AUTHORIZED("OBIETTIVO_NOT_AUTHORIZED"),
    OBIETTIVO_TITOLO_REQUIRED("OBIETTIVO_TITOLO_REQUIRED"),
    OBIETTIVO_TARGET_REQUIRED("OBIETTIVO_TARGET_REQUIRED"),
    OBIETTIVO_ANNO_REQUIRED("OBIETTIVO_ANNO_REQUIRED"),
    PROGRESSIVO_NOT_AUTHORIZED("PROGRESSIVO_NOT_AUTHORIZED"),
    PROGRESSIVO_VALORE_REQUIRED("PROGRESSIVO_VALORE_REQUIRED");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return switch (this) {
            case INTERNAL_ERROR -> "Errore interno del server";
            case VALIDATION_ERROR -> "Errore di validazione";
            case NOT_AUTHORIZED -> "Non autorizzato";
            case NOT_FOUND -> "Risorsa non trovata";
            case INVALID_ID -> "ID non valido";
            case SESSION_INVALID -> "Sessione non valida";
            case USER_NOT_FOUND -> "Utente non trovato";
            case NO_ENTE -> "Nessun ente associato";
            case CODICE_ISTAT_REQUIRED -> "Codice ISTAT richiesto";
            case CF_EXISTS -> "Codice fiscale già esistente";
            case KEYCLOAK_ERROR -> "Errore Keycloak";
            case CANNOT_DELETE_ADMIN -> "Impossibile eliminare un admin";
            case STRUTTURA_NOT_FOUND -> "Struttura non trovata";
            case PARENT_NOT_FOUND -> "Struttura padre non trovata";
            case PARENT_DIFFERENT_ENTE -> "La struttura padre appartiene a un altro ente";
            case INVALID_PARENT -> "Parent non valido";
            case RESPONSABILE_NOT_FOUND -> "Responsabile non trovato";
            case RESPONSABILE_DIFFERENT_ENTE -> "Il responsabile appartiene a un altro ente";
            case HAS_CHILDREN -> "La struttura ha sottostrutture";
            case STAFF_ALREADY_EXISTS -> "Staff già presente";
            case STAFF_NOT_FOUND -> "Staff non trovato";
            case LPM_NOT_FOUND -> "Linea programmatica non trovata";
            case DUP_NOT_FOUND -> "DUP non trovato";
            case DUP_NOT_AUTHORIZED -> "Non autorizzato a gestire il DUP";
            case DUP_ANNO_REQUIRED -> "Anno DUP richiesto";
            case DUP_TITOLO_REQUIRED -> "Titolo DUP richiesto";
            case DUP_HAS_PROGETTI -> "Il DUP ha progetti collegati";
            case PROGETTO_NOT_FOUND -> "Progetto non trovato";
            case PROGETTO_TITOLO_REQUIRED -> "Titolo progetto richiesto";
            case ATTIVITA_NOT_FOUND -> "Attività non trovata";
            case ATTIVITA_CODICE_REQUIRED -> "Codice attività richiesto";
            case ATTIVITA_TITOLO_REQUIRED -> "Titolo attività richiesto";
            case ATTIVITA_NOT_AUTHORIZED -> "Non autorizzato a gestire l'attività";
            case ASSEGNAZIONE_NOT_FOUND -> "Assegnazione non trovata";
            case ASSEGNAZIONE_ALREADY_EXISTS -> "Utente già assegnato a questa attività";
            case TIMESHEET_ENTRY_NOT_FOUND -> "Entry timesheet non trovata";
            case USER_NOT_ASSIGNED -> "L'utente non è assegnato a questa attività";
            case INVALID_PERCENTAGE -> "Percentuale non valida (deve essere tra 0 e 100)";
            case STEP_NOT_FOUND -> "Step non trovato";
            case STEP_TITOLO_REQUIRED -> "Titolo step richiesto";
            case OBIETTIVO_NOT_FOUND -> "Obiettivo non trovato";
            case OBIETTIVO_NOT_AUTHORIZED -> "Non autorizzato a gestire l'obiettivo";
            case OBIETTIVO_TITOLO_REQUIRED -> "Titolo obiettivo richiesto";
            case OBIETTIVO_TARGET_REQUIRED -> "Valore target richiesto";
            case OBIETTIVO_ANNO_REQUIRED -> "Anno obiettivo richiesto";
            case PROGRESSIVO_NOT_AUTHORIZED -> "Non autorizzato a registrare progressivi";
            case PROGRESSIVO_VALORE_REQUIRED -> "Valore progressivo richiesto";
        };
    }

    @Override
    public String toString() {
        return code;
    }
}
