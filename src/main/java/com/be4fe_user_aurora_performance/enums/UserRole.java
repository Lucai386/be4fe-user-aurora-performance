package com.be4fe_user_aurora_performance.enums;

/**
 * Enum per i ruoli utente nell'applicazione.
 */
public enum UserRole {
    AD("AD", "Amministratore"),
    SC("SC", "Segretario Comunale"),
    DR("DR", "Dirigente"),
    CS("CS", "Capo Settore"),
    CP("CP", "Capo Progetto"),
    DB("DB", "Dipendente Base");

    private final String code;
    private final String label;

    UserRole(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return code;
    }
}
