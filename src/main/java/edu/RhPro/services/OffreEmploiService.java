package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OffreEmploiService implements IOffreEmploiService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void add(offreEmploi o) throws SQLException {

        // ✅ Bloquer directement si rhId est null (car la colonne est NOT NULL)
        if (o.getRhId() == null) {
            throw new IllegalArgumentException("rh_id est obligatoire. Mets un RH existant (ex: 1).");
        }

        String sql = "INSERT INTO offre_emploi(titre, description, localisation, type_contrat, date_publication, date_expiration, statut, rh_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setString(3, o.getLocalisation());
            ps.setString(4, o.getTypeContrat());
            ps.setDate(5, Date.valueOf(o.getDatePublication()));
            ps.setDate(6, Date.valueOf(o.getDateExpiration()));
            ps.setString(7, o.getStatut());
            ps.setInt(8, o.getRhId()); // ✅ toujours une valeur

            ps.executeUpdate();
        }
    }

    @Override
    public void update(offreEmploi o) throws SQLException {

        // ✅ Même logique : si rh_id est NOT NULL, on ne met jamais null
        if (o.getRhId() == null) {
            throw new IllegalArgumentException("rh_id est obligatoire. Mets un RH existant (ex: 1).");
        }

        String sql = "UPDATE offre_emploi SET titre=?, description=?, localisation=?, type_contrat=?, date_publication=?, date_expiration=?, statut=?, rh_id=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setString(3, o.getLocalisation());
            ps.setString(4, o.getTypeContrat());
            ps.setDate(5, Date.valueOf(o.getDatePublication()));
            ps.setDate(6, Date.valueOf(o.getDateExpiration()));
            ps.setString(7, o.getStatut());
            ps.setInt(8, o.getRhId());  // ✅ jamais null
            ps.setInt(9, o.getId());

            ps.executeUpdate();
        }
    }

    // le reste de ta classe reste identique ✅

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM offre_emploi WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void fermer(int id) throws SQLException {
        String sql = "UPDATE offre_emploi SET statut='FERMEE' WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public offreEmploi findById(int id) throws SQLException {
        String sql = "SELECT * FROM offre_emploi WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    @Override
    public List<offreEmploi> findAll() throws SQLException {
        List<offreEmploi> list = new ArrayList<>();
        String sql = "SELECT * FROM offre_emploi";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    @Override
    public List<offreEmploi> findActives() throws SQLException {
        List<offreEmploi> list = new ArrayList<>();
        String sql = "SELECT * FROM offre_emploi WHERE statut='ACTIVE'";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private offreEmploi map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String titre = rs.getString("titre");
        String description = rs.getString("description");
        String localisation = rs.getString("localisation");
        String typeContrat = rs.getString("type_contrat");

        Date dp = rs.getDate("date_publication");
        Date de = rs.getDate("date_expiration");
        LocalDate datePublication = (dp != null) ? dp.toLocalDate() : LocalDate.now();
        LocalDate dateExpiration = (de != null) ? de.toLocalDate() : LocalDate.now();

        String statut = rs.getString("statut");

        int rh = rs.getInt("rh_id");
        Integer rhId = rs.wasNull() ? null : rh;

        return new offreEmploi(id, titre, description, localisation, typeContrat, datePublication, dateExpiration, statut, rhId);
    }
}
