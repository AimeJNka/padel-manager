-- date_paiement is now set by the application at payment time (payerParMembre),
-- not at row creation. Drop the NOT NULL constraint; the column is NULL for
-- EN_ATTENTE paiements and populated only when statut transitions to PAYE.
ALTER TABLE paiement ALTER COLUMN date_paiement DROP NOT NULL;
