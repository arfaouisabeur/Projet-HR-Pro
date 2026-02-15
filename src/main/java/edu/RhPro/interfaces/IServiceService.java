package edu.RhPro.interfaces;

import edu.RhPro.entities.Service;
import java.sql.SQLException;
import java.util.List;

public interface IServiceService {
    void addEntity(Service service) throws SQLException;
    void deleteEntity(Service service) throws SQLException;
    void updateEntity(Service service) throws SQLException;
    List<Service> getData() throws SQLException;
}
