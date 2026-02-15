package edu.RhPro.services;

import edu.RhPro.entities.Conge;
import edu.RhPro.interfaces.ICongeService;
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CongeService implements ICongeService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Conge conge) throws SQLException {
        String sql = "INSERT INTO conge_tt (type_conge, date_debut, date_fin, statut, description, employe_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, conge.getTypeConge());

            if (conge.getDateDebut() != null) {
                ps.setDate(2, Date.valueOf(conge.getDateDebut()));
            } else {
                ps.setNull(2, Types.DATE);
            }

            if (conge.getDateFin() != null) {
                ps.setDate(3, Date.valueOf(conge.getDateFin()));
            } else {
                ps.setNull(3, Types.DATE);
            }

            ps.setString(4, conge.getStatut());
            ps.setString(5, conge.getDescription());
            ps.setLong(6, conge.getEmployeeId());

            ps.executeUpdate();
        }
    }

    @Override
    public void deleteEntity(Conge conge) throws SQLException {
        String sql = "DELETE FROM conge_tt WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, conge.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateEntity(Conge conge) throws SQLException {
        String sql = "UPDATE conge_tt SET type_conge=?, date_debut=?, date_fin=?, statut=?, description=?, employee_id=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, conge.getTypeConge());

            if (conge.getDateDebut() != null) {
                ps.setDate(2, Date.valueOf(conge.getDateDebut()));
            } else {
                ps.setNull(2, Types.DATE);
            }

            if (conge.getDateFin() != null) {
                ps.setDate(3, Date.valueOf(conge.getDateFin()));
            } else {
                ps.setNull(3, Types.DATE);
            }

            ps.setString(4, conge.getStatut());
            ps.setString(5, conge.getDescription());
            ps.setLong(6, conge.getEmployeeId());
            ps.setLong(7, conge.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public List<Conge> getData() throws SQLException {
        String sql = "SELECT id, type_conge, date_debut, date_fin, statut, description, employee_id FROM conge_tt";
        List<Conge> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Conge conge = new Conge(
                        rs.getString("type_conge"),
                        rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null,
                        rs.getDate("date_fin") != null ? rs.getDate("date_fin").toLocalDate() : null,
                        rs.getString("statut"),
                        rs.getString("description"),
                        rs.getLong("employee_id")
                );
                list.add(conge);
            }
        }
        return list;
    }

    public Conge getById(long id) throws SQLException {
        String sql = "SELECT id, type_conge, date_debut, date_fin, statut, description, employee_id " +
                "FROM conge_tt WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Conge(
                            rs.getString("type_conge"),
                            rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null,
                            rs.getDate("date_fin") != null ? rs.getDate("date_fin").toLocalDate() : null,
                            rs.getString("statut"),
                            rs.getString("description"),
                            rs.getLong("employee_id")
                    );
                }
            }
        }
        return null;
    }
}