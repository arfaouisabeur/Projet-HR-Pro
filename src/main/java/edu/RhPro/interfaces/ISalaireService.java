package edu.RhPro.interfaces;

import edu.RhPro.entities.Salaire;
import java.sql.SQLException;
import java.util.List;

public interface ISalaireService {
    void addEntity(Salaire salaire) throws SQLException;
    void deleteEntity(Salaire salaire) throws SQLException;
    void updateEntity(Salaire salaire) throws SQLException;
    List<Salaire> getData() throws SQLException;
}
