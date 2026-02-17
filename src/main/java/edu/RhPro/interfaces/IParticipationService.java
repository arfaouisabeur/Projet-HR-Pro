package edu.RhPro.interfaces;


import edu.RhPro.entities.Participation;
import java.sql.SQLException;
import java.util.List;

public interface IParticipationService {
    void addEntity(Participation participation) throws SQLException;
    void deleteEntity(Participation participation) throws SQLException;
    void updateEntity(Participation participation) throws SQLException;
    List<Participation> getData() throws SQLException;
}
