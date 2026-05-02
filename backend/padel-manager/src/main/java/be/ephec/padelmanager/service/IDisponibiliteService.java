package be.ephec.padelmanager.service;

public interface IDisponibiliteService {

    /**
     * Génère tous les créneaux LIBRE pour un site et une année.
     * Suppose qu'il n'existe pas encore de créneaux pour cette période.
     *
     * @return le nombre de créneaux créés
     */
    int genererCreneaux(Integer siteId, Integer annee);

    /**
     * Supprime les créneaux LIBRE existants pour un site et une année,
     * puis régénère les créneaux en conservant les créneaux RESERVE.
     *
     * @return le nombre de nouveaux créneaux LIBRE créés
     */
    int regenererCreneaux(Integer siteId, Integer annee);
}
