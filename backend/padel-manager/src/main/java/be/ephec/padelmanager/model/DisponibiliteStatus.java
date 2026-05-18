package be.ephec.padelmanager.model;

/**
 * Valid statut values for the {@code disponibilite} table.
 * Static constants are used instead of a JPA {@code @Enumerated} enum —
 * see ADR-0002 for the accepted trade-off.
 */
public final class DisponibiliteStatus {

    public static final String LIBRE   = "LIBRE";
    public static final String RESERVE = "RESERVE";

    private DisponibiliteStatus() {}
}
