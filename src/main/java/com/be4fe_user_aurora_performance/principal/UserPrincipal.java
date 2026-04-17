package com.be4fe_user_aurora_performance.principal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Bean con scope di richiesta che contiene le informazioni dell'utente corrente.
 *
 * <p>Viene popolato una sola volta per richiesta da {@link UserPrincipalFilter}.
 * I controller iniettano questo bean invece di chiamare {@code SessionService} a ogni metodo,
 * eliminando chiamate ridondanti al Core per recuperare userId / codiceIstat / ruolo.</p>
 */
@Component
@RequestScope
@Getter
@Setter
public class UserPrincipal {

    /** ID interno nel DB (tabella aurora.users). */
    private Long id;

    /** Codice ISTAT del comune dell'utente — identificatore tenant. */
    private String codiceIstat;

    /** Ruolo estratto dal JWT Keycloak (es. SC, DR, CS, CP, DB). */
    private String ruolo;

    /** Subject UUID del JWT Keycloak. */
    private String keycloakId;

    /** {@code true} solo dopo che il filtro ha risolto con successo l'utente dal Core. */
    public boolean isResolved() {
        return id != null && codiceIstat != null;
    }

    /** Codice ISTAT garantito non-null; lancia eccezione se non risolto. */
    public String requireCodiceIstat() {
        if (codiceIstat == null || codiceIstat.isBlank()) {
            throw new IllegalStateException("Tenant (codiceIstat) non risolto per la richiesta corrente");
        }
        return codiceIstat;
    }
}
