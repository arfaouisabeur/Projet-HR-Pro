package edu.RhPro.interfaces;

import edu.RhPro.entities.Evenement;
import java.sql.SQLException;
import java.util.List;

public interface IEvenementService {
    void addEntity(Evenement evenment) throws SQLException;
    void deleteEntity(Evenement evenment) throws SQLException;
    void updateEntity(Evenement evenment) throws SQLException;
    List<Evenement> getData() throws SQLException;
}
