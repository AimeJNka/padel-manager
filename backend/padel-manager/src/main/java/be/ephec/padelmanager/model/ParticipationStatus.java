package be.ephec.padelmanager.model;

/**
 * Valid statut values for the {@code participation} table.
 * Static constants are used instead of a JPA {@code @Enumerated} enum.
 */
public final class ParticipationStatus {

    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String CONFIRME   = "CONFIRME";
    public static final String ANNULEE    = "ANNULEE";

    private ParticipationStatus() {}
}
