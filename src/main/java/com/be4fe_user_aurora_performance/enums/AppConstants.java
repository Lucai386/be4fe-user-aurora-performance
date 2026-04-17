package com.be4fe_user_aurora_performance.enums;

/**
 * Costanti per le stringhe comuni usate nell'applicazione.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // Risultati API
    public static final String RESULT_OK = "OK";
    public static final String RESULT_KO = "KO";

    // Messaggi generici
    public static final String MSG_INTERNAL_ERROR = "Errore interno del server";
    public static final String MSG_USER_NOT_AUTHORIZED = "Utente non autorizzato";
    public static final String MSG_NO_ENTE = "Nessun ente selezionato";
    public static final String MSG_USER_NOT_FOUND = "Utente non trovato";
    public static final String MSG_SESSION_INVALID = "Sessione non valida o scaduta";
    public static final String MSG_CODICE_ISTAT_REQUIRED = "Codice ISTAT obbligatorio";
    
    // Messaggi gestione utenti
    public static final String MSG_CF_EXISTS = "Codice fiscale già presente nel sistema";
    public static final String MSG_KEYCLOAK_ERROR = "Errore durante la creazione dell'utente su Keycloak";
    public static final String MSG_CANNOT_DELETE_ADMIN = "Non puoi eliminare un amministratore";
    public static final String MSG_CANNOT_DELETE_OTHER_ENTE = "Non puoi eliminare utenti di altri enti";
    
    // Messaggi strutture
    public static final String MSG_STRUTTURA_NOT_FOUND = "Struttura non trovata";
    public static final String MSG_PARENT_NOT_FOUND = "Struttura padre non trovata";
    public static final String MSG_PARENT_DIFFERENT_ENTE = "La struttura padre appartiene a un altro ente";
    public static final String MSG_INVALID_PARENT = "Una struttura non può essere padre di se stessa";
    public static final String MSG_RESPONSABILE_NOT_FOUND = "Responsabile non trovato";
    public static final String MSG_RESPONSABILE_DIFFERENT_ENTE = "Il responsabile appartiene a un altro ente";
    public static final String MSG_HAS_CHILDREN = "Impossibile eliminare: la struttura ha sottostrutture";
    public static final String MSG_STAFF_ALREADY_EXISTS = "L'utente è già nello staff di questa struttura";
    public static final String MSG_STAFF_NOT_FOUND = "Membro staff non trovato";
    
    // Messaggi LPM
    public static final String MSG_LPM_NOT_FOUND = "LPM non trovata";
    public static final String MSG_INVALID_LPM_ID = "ID LPM non valido";
}
