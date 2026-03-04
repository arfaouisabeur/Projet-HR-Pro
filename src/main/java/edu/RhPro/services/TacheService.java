package edu.RhPro.services;

import edu.RhPro.entities.Tache;
import edu.RhPro.interfaces.ITacheService;
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TacheService implements ITacheService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addTache(Tache t) throws SQLException {
        String sql = "INSERT INTO tache (titre, description, statut, date_debut, date_fin, level, projet_id, employe_id, prime_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, t.getTitre());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getStatut());

            ps.setDate(4, t.getDateDebut() == null ? null : Date.valueOf(t.getDateDebut()));
            ps.setDate(5, t.getDateFin() == null ? null : Date.valueOf(t.getDateFin()));

            ps.setInt(6, t.getLevel());
            ps.setInt(7, t.getProjetId());
            ps.setInt(8, t.getEmployeId());

            if (t.getPrimeId() == null) ps.setNull(9, Types.INTEGER);
            else ps.setInt(9, t.getPrimeId());

            ps.executeUpdate();
        }
    }

    @Override
    public void updateTache(Tache t) throws SQLException {
        String sql = "UPDATE tache SET statut=?, prime_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, t.getStatut());
            if (t.getPrimeId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, t.getPrimeId());
            ps.setInt(3, t.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteTache(int id) throws SQLException {
        String sql = "DELETE FROM tache WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Tache getTacheById(int id) throws SQLException {
        String sql = "SELECT * FROM tache WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    @Override
    public List<Tache> getAllTaches() throws SQLException {
        List<Tache> list = new ArrayList<>();
        String sql = "SELECT * FROM tache";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Tache> findByProjetId(int projetId) throws SQLException {
        List<Tache> list = new ArrayList<>();
        String sql = "SELECT * FROM tache WHERE projet_id=? ORDER BY id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, projetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Tache> findByEmployeId(int employeId) throws SQLException {
        List<Tache> list = new ArrayList<>();
        String sql = "SELECT * FROM tache WHERE employe_id=? ORDER BY id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, employeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ✅ ONLY DONE tasks with prime_id NULL
    public List<Tache> findDoneWithoutPrimeByEmployeId(int employeId) throws SQLException {
        List<Tache> list = new ArrayList<>();
        String sql = "SELECT * FROM tache WHERE employe_id=? AND statut='DONE' AND prime_id IS NULL ORDER BY id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, employeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ✅ NEW: assign prime to selected tasks (so they disappear next time)
    public void assignPrimeToTaches(int primeId, List<Integer> tacheIds) throws SQLException {
        if (tacheIds == null || tacheIds.isEmpty()) return;

        String sql = "UPDATE tache SET prime_id=? WHERE id=? AND prime_id IS NULL";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (Integer id : tacheIds) {
                if (id == null) continue;
                ps.setInt(1, primeId);
                ps.setInt(2, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Tache map(ResultSet rs) throws SQLException {
        Date dd = rs.getDate("date_debut");
        Date df = rs.getDate("date_fin");

        Object primeObj = rs.getObject("prime_id");
        Integer primeId = (primeObj == null) ? null : ((Number) primeObj).intValue();

        return new Tache(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getString("statut"),
                dd == null ? null : dd.toLocalDate(),
                df == null ? null : df.toLocalDate(),
                rs.getInt("level"),
                rs.getInt("projet_id"),
                rs.getInt("employe_id"),
                primeId
        );
    }

    public List<Tache> findDoneByEmployeId(int employeId) throws SQLException {
        List<Tache> list = new ArrayList<>();
        String sql = "SELECT * FROM tache " +
                "WHERE employe_id=? AND statut='DONE' AND prime_id IS NULL " +   // ✅ only done + not paid yet
                "ORDER BY date_fin DESC, id DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, employeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }
}