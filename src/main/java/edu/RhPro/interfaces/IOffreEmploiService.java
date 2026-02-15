package edu.RhPro.interfaces;

import edu.RhPro.entities.offreEmploi;
import java.sql.SQLException;
import java.util.List;

public interface IOffreEmploiService {
    void add(offreEmploi o) throws SQLException;
    void update(offreEmploi o) throws SQLException;
    void delete(int id) throws SQLException;
    void fermer(int id) throws SQLException;

    offreEmploi findById(int id) throws SQLException;
    List<offreEmploi> findAll() throws SQLException;
    List<offreEmploi> findActives() throws SQLException;
}
