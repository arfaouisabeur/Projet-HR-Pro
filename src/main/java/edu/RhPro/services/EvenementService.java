package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements IEvenementService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Evenement e) throws SQLException {
        String sql = "INSERT INTO evenement (titre, date_debut, date_fin, lieu, description, rh_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, e.getTitre());
            ps.setTimestamp(2, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(3, Timestamp.valueOf(e.getDateFin()));
            ps.setString(4, e.getLieu());
            ps.setString(5, e.getDescription());
            ps.setLong(6, e.getRhId());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    e.setId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public void deleteEntity(Evenement e) throws SQLException {
        String sql = "DELETE FROM evenement WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, e.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateEntity(Evenement e) throws SQLException {
        String sql = "UPDATE evenement SET titre=?, date_debut=?, date_fin=?, lieu=?, description=?, rh_id=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, e.getTitre());
            ps.setTimestamp(2, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(3, Timestamp.valueOf(e.getDateFin()));
            ps.setString(4, e.getLieu());
            ps.setString(5, e.getDescription());
            ps.setLong(6, e.getRhId());
            ps.setLong(7, e.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public List<Evenement> getData() throws SQLException {
        String sql = "SELECT * FROM evenement";
        List<Evenement> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Evenement e = new Evenement();
                e.setId(rs.getLong("id"));
                e.setTitre(rs.getString("titre"));
                e.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                e.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
                e.setLieu(rs.getString("lieu"));
                e.setDescription(rs.getString("description"));
                e.setRhId(rs.getLong("rh_id"));

                list.add(e);
            }
        }

        return list;
    }
}
