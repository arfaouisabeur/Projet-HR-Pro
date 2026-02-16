package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
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

            if (conge.getDateDebut() != null) ps.setDate(2, Date.valueOf(conge.getDateDebut()));
            else ps.setNull(2, Types.DATE);

            if (conge.getDateFin() != null) ps.setDate(3, Date.valueOf(conge.getDateFin()));
            else ps.setNull(3, Types.DATE);

            ps.setString(4, conge.getStatut());
            ps.setString(5, conge.getDescription());
            ps.setLong(6, conge.getEmployeeId()); // DB = employe_id

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
        // ✅ FIX column employe_id
        String sql = "UPDATE conge_tt SET type_conge=?, date_debut=?, date_fin=?, statut=?, description=?, employe_id=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, conge.getTypeConge());

            if (conge.getDateDebut() != null) ps.setDate(2, Date.valueOf(conge.getDateDebut()));
            else ps.setNull(2, Types.DATE);

            if (conge.getDateFin() != null) ps.setDate(3, Date.valueOf(conge.getDateFin()));
            else ps.setNull(3, Types.DATE);

            ps.setString(4, conge.getStatut());
            ps.setString(5, conge.getDescription());
            ps.setLong(6, conge.getEmployeeId()); // DB = employe_id
            ps.setLong(7, conge.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public List<Conge> getData() throws SQLException {
        // ✅ FIX column employe_id
        String sql = "SELECT id, type_conge, date_debut, date_fin, statut, description, employe_id FROM conge_tt";
        List<Conge> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Conge getById(long id) throws SQLException {
        String sql = "SELECT id, type_conge, date_debut, date_fin, statut, description, employe_id FROM conge_tt WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    // ✅ Employee view only his requests
    public List<Conge> findByEmployeId(long employeId) throws SQLException {
        String sql = "SELECT id, type_conge, date_debut, date_fin, statut, description, employe_id " +
                "FROM conge_tt WHERE employe_id=? ORDER BY date_debut DESC";
        List<Conge> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, employeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ✅ RH updates status only
    public void updateStatus(long congeId, String newStatus) throws SQLException {
        String sql = "UPDATE conge_tt SET statut=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setLong(2, congeId);
            ps.executeUpdate();
        }
    }

    private Conge map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String type = rs.getString("type_conge");

        Date dd = rs.getDate("date_debut");
        Date df = rs.getDate("date_fin");
        LocalDate dateDebut = dd != null ? dd.toLocalDate() : null;
        LocalDate dateFin = df != null ? df.toLocalDate() : null;

        String statut = rs.getString("statut");
        String desc = rs.getString("description");
        long employeId = rs.getLong("employe_id");

        return new Conge(id, type, dateDebut, dateFin, statut, desc, employeId);
    }
    public List<Conge> findPending() throws SQLException {
        String sql = "SELECT id, type_conge, date_debut, date_fin, statut, description, employe_id " +
                "FROM conge_tt WHERE statut='EN_ATTENTE' ORDER BY date_debut DESC";
        List<Conge> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

}
