package be.ephec.padelmanager.config;

import java.math.BigDecimal;

/**
 * Central repository for padel match business-rule constants.
 * All numeric invariants that appear in multiple services must reference
 * this class — no magic literals in business logic.
 * CDC references: CF-M-003 (cancellation deadlines), CF-M-006 (payment window),
 * CF-M-007 (solde organisateur), UC-03 (penalty duration).
 */
public final class MatchPolicy {

    private MatchPolicy() {}

    /** CF-M-003 — minimum hours before match start to cancel a PUBLIC match. */
    public static final int DELAI_ANNULATION_PUBLIC_H = 24;

    /** CF-M-003 — minimum hours before match start to cancel a PRIVATE match. */
    public static final int DELAI_ANNULATION_PRIVE_H = 48;

    /** UC-03 — duration in days of a penalty (late cancel / incomplete private match). */
    public static final int DUREE_PENALITE_JOURS = 7;

    /** CF-M-006 — hours before match start within which unpaid slots are released. */
    public static final int DELAI_PAIEMENT_H = 24;

    /** Business invariant — maximum number of players per match (enforced by DB trigger). */
    public static final int NB_JOUEURS_MATCH = 4;

    /** Pricing — cost per player slot in euros. */
    public static final BigDecimal PRIX_PLACE_EUR = BigDecimal.valueOf(15);

    /** Pricing — total match cost derived from slot price × player count (= 60 €). */
    public static final BigDecimal PRIX_TOTAL_MATCH =
            PRIX_PLACE_EUR.multiply(BigDecimal.valueOf(NB_JOUEURS_MATCH));
}
