package be.ephec.padelmanager.model;

/**
 * Valid statut values for the {@code match_padel} table.
 * Static constants are used instead of a JPA {@code @Enumerated} enum —
 * see ADR-0002 for the accepted trade-off.
 */
public final class MatchStatus {

    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String DEMARRE    = "DEMARRE";
    public static final String ANNULE     = "ANNULE";
    public static final String EFFECTUE   = "EFFECTUE";

    private MatchStatus() {}
}
