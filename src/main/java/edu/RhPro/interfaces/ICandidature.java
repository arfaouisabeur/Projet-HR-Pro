package edu.RhPro.interfaces;

import edu.RhPro.entities.Candidature;
import java.sql.SQLException;
import java.util.List;

public interface ICandidature {

    // Ajouter une candidature (postuler)
    void add(Candidature candidature) throws SQLException;

    // Consulter toutes les candidatures
    List<Candidature> findAll() throws SQLException;

    // Consulter les candidatures d’une offre
    List<Candidature> findByOffre(int offreId) throws SQLException;

    // Consulter les candidatures d’un candidat
    List<Candidature> findByCandidat(int candidatId) throws SQLException;

    // Mettre à jour le statut (ACCEPTEE / REFUSEE / EN_ATTENTE)
    void updateStatut(int idCandidature, String statut) throws SQLException;

    // Supprimer une candidature (optionnel)
    void delete(int idCandidature) throws SQLException;
}
