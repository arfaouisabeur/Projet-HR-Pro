package edu.RhPro.services;

import edu.RhPro.entities.Participation;
import edu.RhPro.interfaces.IParticipationService;
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService implements IParticipationService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Participation p) throws SQLException {
        String sql = "INSERT INTO event_participation (date_inscription, statut, evenement_id, employe_id) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (p.getDateInscription() != null)
                ps.setDate(1, Date.valueOf(p.getDateInscription()));
            else
                ps.setNull(1, Types.DATE);

            ps.setString(2, p.getStatut());
            ps.setLong(3, p.getEvenementId());
            ps.setLong(4, p.getEmployeId());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public void deleteEntity(Participation p) throws SQLException {
        String sql = "DELETE FROM event_participation WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, p.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateEntity(Participation p) throws SQLException {
        String sql = "UPDATE event_participation SET date_inscription=?, statut=?, evenement_id=?, employe_id=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            if (p.getDateInscription() != null)
                ps.setDate(1, Date.valueOf(p.getDateInscription()));
            else
                ps.setNull(1, Types.DATE);

            ps.setString(2, p.getStatut());
            ps.setLong(3, p.getEvenementId());
            ps.setLong(4, p.getEmployeId());
            ps.setLong(5, p.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public List<Participation> getData() throws SQLException {
        String sql = "SELECT * FROM event_participation";
        List<Participation> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Participation p = new Participation();
                p.setId(rs.getLong("id"));

                Date di = rs.getDate("date_inscription");
                if (di != null) p.setDateInscription(di.toLocalDate());

                p.setStatut(rs.getString("statut"));
                p.setEvenementId(rs.getLong("evenement_id"));
                p.setEmployeId(rs.getLong("employe_id"));

                list.add(p);
            }
        }

        return list;
    }
}

