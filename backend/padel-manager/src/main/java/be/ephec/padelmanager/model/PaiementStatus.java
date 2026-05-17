package be.ephec.padelmanager.model;

/**
 * Valid statut values for the {@code paiement} table.
 * Static constants are used instead of a JPA {@code @Enumerated} enum —
 * see ADR-0002 for the accepted trade-off.
 */
public final class PaiementStatus {

    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String PAYE       = "PAYE";
    public static final String ANNULE     = "ANNULE";
    public static final String REMBOURSE  = "REMBOURSE";

    private PaiementStatus() {}
}
