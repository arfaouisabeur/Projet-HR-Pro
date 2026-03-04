package edu.RhPro.services;

import edu.RhPro.entities.Activite;
import edu.RhPro.interfaces.IActiviteService;
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteService implements IActiviteService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Activite a) throws SQLException {
        String sql = "INSERT INTO activite (titre, description, evenement_id) VALUES (?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, a.getTitre());
            ps.setString(2, a.getDescription());
            ps.setLong(3, a.getEvenementId());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    a.setId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public void deleteEntity(Activite a) throws SQLException {
        String sql = "DELETE FROM activite WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, a.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateEntity(Activite a) throws SQLException {
        String sql = "UPDATE activite SET titre=?, description=?, evenement_id=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, a.getTitre());
            ps.setString(2, a.getDescription());
            ps.setLong(3, a.getEvenementId());
            ps.setLong(4, a.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public List<Activite> getData() throws SQLException {
        String sql = "SELECT * FROM activite";
        List<Activite> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Activite a = new Activite();
                a.setId(rs.getLong("id"));
                a.setTitre(rs.getString("titre"));
                a.setDescription(rs.getString("description"));
                a.setEvenementId(rs.getLong("evenement_id"));

                list.add(a);
            }
        }

        return list;
    }
}


