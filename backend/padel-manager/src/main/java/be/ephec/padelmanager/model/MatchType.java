package be.ephec.padelmanager.model;

/**
 * Valid type_match values for the {@code match_padel} table.
 * Static constants are used instead of a JPA {@code @Enumerated} enum.
 */
public final class MatchType {

    public static final String PRIVE  = "PRIVE";
    public static final String PUBLIC = "PUBLIC";

    private MatchType() {}
}
