package edu.RhPro.interfaces;

import edu.RhPro.entities.Tache;
import java.sql.SQLException;
import java.util.List;

public interface ITacheService {
    void addTache(Tache tache) throws SQLException;
    void updateTache(Tache tache) throws SQLException;
    void deleteTache(int id) throws SQLException;
    Tache getTacheById(int id) throws SQLException;
    List<Tache> getAllTaches() throws SQLException;
}
