package edu.RhPro.interfaces;


import edu.RhPro.entities.Prime;
import java.sql.SQLException;
import java.util.List;

public interface IPrimeService {
    void addEntity(Prime prime) throws SQLException;
    void deleteEntity(Prime prime) throws SQLException;
    void updateEntity(Prime prime) throws SQLException;
    List<Prime> getData() throws SQLException;
}
