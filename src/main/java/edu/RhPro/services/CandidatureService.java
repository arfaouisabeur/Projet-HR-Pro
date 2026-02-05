package edu.RhPro.services;

import edu.RhPro.entities.Candidature;
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CandidatureService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    // CREATE
    public void add(Candidature c) throws SQLException {
        if (c.getDateCandidature() == null) throw new IllegalArgumentException("date_candidature obligatoire");
        if (c.getStatut() == null) throw new IllegalArgumentException("statut obligatoire");
        if (c.getCandidatId() <= 0) throw new IllegalArgumentException("candidat_id obligatoire");
        if (c.getOffreEmploiId() <= 0) throw new IllegalArgumentException("offre_emploi_id obligatoire");

        String sql = "INSERT INTO candidature(date_candidature, statut, cv, candidat_id, offre_emploi_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(c.getDateCandidature()));
            ps.setString(2, c.getStatut());

            if (c.getCv() == null) ps.setNull(3, Types.LONGVARCHAR);
            else ps.setString(3, c.getCv());

            ps.setLong(4, c.getCandidatId());
            ps.setLong(5, c.getOffreEmploiId());
            ps.executeUpdate();
        }
    }

    // UPDATE
    public void update(Candidature c) throws SQLException {
        if (c.getId() <= 0) throw new IllegalArgumentException("id obligatoire pour update");
        if (c.getDateCandidature() == null) throw new IllegalArgumentException("date_candidature obligatoire");
        if (c.getStatut() == null) throw new IllegalArgumentException("statut obligatoire");
        if (c.getCandidatId() <= 0) throw new IllegalArgumentException("candidat_id obligatoire");
        if (c.getOffreEmploiId() <= 0) throw new IllegalArgumentException("offre_emploi_id obligatoire");

        String sql = "UPDATE candidature SET date_candidature=?, statut=?, cv=?, candidat_id=?, offre_emploi_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(c.getDateCandidature()));
            ps.setString(2, c.getStatut());

            if (c.getCv() == null) ps.setNull(3, Types.LONGVARCHAR);
            else ps.setString(3, c.getCv());

            ps.setLong(4, c.getCandidatId());
            ps.setLong(5, c.getOffreEmploiId());
            ps.setInt(6, c.getId());

            ps.executeUpdate();
        }
    }

    // DELETE
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM candidature WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // READ BY ID
    public Candidature findById(int id) throws SQLException {
        String sql = "SELECT * FROM candidature WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    // READ ALL
    public List<Candidature> findAll() throws SQLException {
        List<Candidature> list = new ArrayList<>();
        String sql = "SELECT * FROM candidature";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private Candidature map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");

        Date dc = rs.getDate("date_candidature");
        LocalDate dateCandidature = (dc != null) ? dc.toLocalDate() : LocalDate.now();

        String statut = rs.getString("statut");
        String cv = rs.getString("cv");
        long candidatId = rs.getLong("candidat_id");
        long offreEmploiId = rs.getLong("offre_emploi_id");

        return new Candidature(id, dateCandidature, statut, cv, candidatId, offreEmploiId);
    }
}
