package edu.RhPro.interfaces;

import edu.RhPro.entities.Projet;
import java.sql.SQLException;
import java.util.List;

public interface IProjetService {

    void addProjet(Projet p) throws SQLException;
    void updateProjet(Projet p) throws SQLException;
    void deleteProjet(int id) throws SQLException;
    Projet getProjetById(int id) throws SQLException;
    List<Projet> getAllProjets() throws SQLException;
}
