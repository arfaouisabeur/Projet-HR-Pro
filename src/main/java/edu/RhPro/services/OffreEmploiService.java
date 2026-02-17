package edu.RhPro.services;

import edu.RhPro.entities.offreEmploi;
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OffreEmploiService {

    private final Connection cnx;

    public OffreEmploiService() {
        this(MyConnection.getInstance().getCnx());
    }

    public OffreEmploiService(Connection cnx) {
        this.cnx = cnx;
    }

    private void validate(offreEmploi o, boolean isUpdate) {
        if (o == null) throw new IllegalArgumentException("Offre obligatoire");
        if (isUpdate && o.getId() <= 0) throw new IllegalArgumentException("ID obligatoire pour update");

        if (o.getTitre() == null || o.getTitre().trim().length() < 5)
            throw new IllegalArgumentException("Titre: minimum 5 caractères");

        if (o.getLocalisation() == null || o.getLocalisation().trim().isEmpty())
            throw new IllegalArgumentException("Localisation obligatoire");

        if (o.getTypeContrat() == null || o.getTypeContrat().trim().length() < 2)
            throw new IllegalArgumentException("Type contrat obligatoire");

        if (o.getDatePublication() == null)
            throw new IllegalArgumentException("Date publication obligatoire");

        if (o.getDateExpiration() == null)
            throw new IllegalArgumentException("Date expiration obligatoire");

        if (!o.getDateExpiration().isAfter(o.getDatePublication()))
            throw new IllegalArgumentException("Expiration doit être après publication");

        if (o.getStatut() == null || o.getStatut().trim().isEmpty())
            throw new IllegalArgumentException("Statut obligatoire");

        if (o.getDescription() == null || o.getDescription().trim().length() < 10)
            throw new IllegalArgumentException("Description: minimum 10 caractères");

        if (o.getDescription().matches(".*\\d.*"))
            throw new IllegalArgumentException("Description: chiffres interdits");

        // ✅ rh_id obligatoire (type long)
        if (o.getRhId() <= 0)
            throw new IllegalArgumentException("RH (rh_id) obligatoire");
    }

    public List<offreEmploi> getAll() throws SQLException {
        List<offreEmploi> list = new ArrayList<>();
        String sql = "SELECT * FROM offre_emploi ORDER BY id DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void add(offreEmploi o) throws SQLException {
        validate(o, false);

        String sql = "INSERT INTO offre_emploi " +
                "(titre, description, localisation, type_contrat, date_publication, date_expiration, statut, rh_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setString(3, o.getLocalisation());
            ps.setString(4, o.getTypeContrat());
            ps.setDate(5, Date.valueOf(o.getDatePublication()));
            ps.setDate(6, Date.valueOf(o.getDateExpiration()));
            ps.setString(7, o.getStatut());
            ps.setLong(8, o.getRhId());
            ps.executeUpdate();
        }
    }

    public void update(offreEmploi o) throws SQLException {
        validate(o, true);

        String sql = "UPDATE offre_emploi SET " +
                "titre=?, description=?, localisation=?, type_contrat=?, " +
                "date_publication=?, date_expiration=?, statut=?, rh_id=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setString(3, o.getLocalisation());
            ps.setString(4, o.getTypeContrat());
            ps.setDate(5, Date.valueOf(o.getDatePublication()));
            ps.setDate(6, Date.valueOf(o.getDateExpiration()));
            ps.setString(7, o.getStatut());
            ps.setLong(8, o.getRhId());
            ps.setInt(9, o.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM offre_emploi WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private offreEmploi map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String titre = rs.getString("titre");
        String loc = rs.getString("localisation");
        String type = rs.getString("type_contrat");

        LocalDate pub = rs.getDate("date_publication").toLocalDate();
        LocalDate exp = rs.getDate("date_expiration").toLocalDate();

        String statut = rs.getString("statut");
        String desc = rs.getString("description");

        long rhId = rs.getLong("rh_id");
        offreEmploi o = new offreEmploi(id, titre, loc, type, pub, exp, statut, desc);
        o.setRhId(rhId);
        return o;

    }

    public List<offreEmploi> findActives() throws SQLException {
        List<offreEmploi> list = new ArrayList<>();
        String sql = "SELECT * FROM offre_emploi WHERE statut=? ORDER BY id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, "OUVERTE");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM offre_emploi";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countByStatut(String statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM offre_emploi WHERE statut=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
e