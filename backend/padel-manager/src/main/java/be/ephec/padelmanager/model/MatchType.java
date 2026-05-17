package be.ephec.padelmanager.model;

/**
 * Valid type_match values for the {@code match_padel} table.
 * Static constants are used instead of a JPA {@code @Enumerated} enum —
 * see ADR-0002 for the accepted trade-off.
 */
public final class MatchType {

    public static final String PRIVE  = "PRIVE";
    public static final String PUBLIC = "PUBLIC";

    private MatchType() {}
}
