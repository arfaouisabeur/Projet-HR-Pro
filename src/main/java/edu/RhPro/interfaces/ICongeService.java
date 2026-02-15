
package edu.RhPro.interfaces;

import edu.RhPro.entities.Conge;
import java.sql.SQLException;
import java.util.List;

public interface ICongeService {
    void addEntity(Conge conge) throws SQLException;
    void deleteEntity(Conge conge) throws SQLException;
    void updateEntity(Conge conge) throws SQLException;
    List<Conge> getData() throws SQLException;
}
