package edu.RhPro.interfaces;

import edu.RhPro.entities.Reponse;
import java.sql.SQLException;
import java.util.List;

public interface IReponseService {
    // Méthodes CRUD de base (comme dans vos autres interfaces)
    void addEntity(Reponse reponse) throws SQLException;
    void updateEntity(Reponse reponse) throws SQLException;
    void deleteEntity(Reponse reponse) throws SQLException;
    List<Reponse> getData() throws SQLException;
    Reponse getById(long id) throws SQLException;

    // Méthodes spécifiques pour les réponses
    List<Reponse> getReponsesByRhId(long rhId) throws SQLException;
    List<Reponse> getReponsesByEmployeId(long employeId) throws SQLException;

    // Méthodes pour récupérer les réponses par type de demande
    List<Reponse> getReponsesByCongeId(long congeId) throws SQLException;
    List<Reponse> getReponsesByServiceId(long serviceId) throws SQLException;

    // Méthode pour vérifier si une réponse existe pour une demande
    boolean hasReponseForConge(long congeId) throws SQLException;
    boolean hasReponseForService(long serviceId) throws SQLException;
}