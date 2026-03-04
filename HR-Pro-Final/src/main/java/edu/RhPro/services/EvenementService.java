package edu.RhPro.services;

import edu.RhPro.entities.Evenement;
import edu.RhPro.interfaces.IEvenementService;
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements IEvenementService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Evenement e) throws SQLException {
        String sql = "INSERT INTO evenement (titre, date_debut, date_fin, lieu, description, image_url, rh_id, latitude, longitude) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, e.getTitre());
            ps.setTimestamp(2, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(3, Timestamp.valueOf(e.getDateFin()));
            ps.setString(4, e.getLieu());
            ps.setString(5, e.getDescription());
            ps.setString(6, e.getImageUrl());
            ps.setLong(7, e.getRhId());
            ps.setDouble(8, e.getLatitude());
            ps.setDouble(9, e.getLongitude());

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
        String sql = "UPDATE evenement SET titre=?, date_debut=?, date_fin=?, lieu=?, description=?, image_url=?, rh_id=?, latitude=?, longitude=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, e.getTitre());
            ps.setTimestamp(2, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(3, Timestamp.valueOf(e.getDateFin()));
            ps.setString(4, e.getLieu());
            ps.setString(5, e.getDescription());
            ps.setString(6, e.getImageUrl());
            ps.setLong(7, e.getRhId());
            ps.setDouble(8, e.getLatitude());
            ps.setDouble(9, e.getLongitude());
            ps.setLong(10, e.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public List<Evenement> getData() throws SQLException {
        String sql = "SELECT * FROM evenement ORDER BY date_debut DESC";
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
                e.setImageUrl(rs.getString("image_url"));
                e.setRhId(rs.getLong("rh_id"));
                e.setLatitude(rs.getDouble("latitude"));
                e.setLongitude(rs.getDouble("longitude"));

                list.add(e);
            }
        }

        return list;
    }

    public List<Evenement> searchByTitre(String keyword) throws SQLException {
        String sql = "SELECT * FROM evenement WHERE titre LIKE ? ORDER BY date_debut DESC";
        List<Evenement> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Evenement e = new Evenement();
                    e.setId(rs.getLong("id"));
                    e.setTitre(rs.getString("titre"));
                    e.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                    e.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
                    e.setLieu(rs.getString("lieu"));
                    e.setDescription(rs.getString("description"));
                    e.setImageUrl(rs.getString("image_url"));
                    e.setRhId(rs.getLong("rh_id"));
                    e.setLatitude(rs.getDouble("latitude"));
                    e.setLongitude(rs.getDouble("longitude"));
                    list.add(e);
                }
            }
        }
        return list;
    }
}