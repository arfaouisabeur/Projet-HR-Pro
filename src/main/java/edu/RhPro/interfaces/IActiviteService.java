package edu.RhPro.interfaces;

import edu.RhPro.entities.Activite;
import java.sql.SQLException;
import java.util.List;

public interface IActiviteService {
    void addEntity(Activite activite) throws SQLException;
    void deleteEntity(Activite activite) throws SQLException;
    void updateEntity(Activite activite) throws SQLException;
    List<Activite> getData() throws SQLException;
}
