package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjetService implements IProjetService {

    Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addProjet(Projet p) throws SQLException {

        String sql = "INSERT INTO projet (titre, description, statut, rh_id, responsable_employe_id, date_debut, date_fin) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, p.getTitre());
        ps.setString(2, p.getDescription());
        ps.setString(3, p.getStatut());
        ps.setInt(4, p.getRhId());
        ps.setInt(5, p.getResponsableEmployeId());
        ps.setDate(6, Date.valueOf(p.getDateDebut()));
        ps.setDate(7, Date.valueOf(p.getDateFin()));

        ps.executeUpdate();
        System.out.println("✅ Projet ajouté");
    }

    @Override
    public void updateProjet(Projet p) throws SQLException {

        String sql = "UPDATE projet SET titre=?, description=?, statut=?, rh_id=?, responsable_employe_id=?, date_debut=?, date_fin=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, p.getTitre());
        ps.setString(2, p.getDescription());
        ps.setString(3, p.getStatut());
        ps.setInt(4, p.getRhId());
        ps.setInt(5, p.getResponsableEmployeId());
        ps.setDate(6, Date.valueOf(p.getDateDebut()));
        ps.setDate(7, Date.valueOf(p.getDateFin()));
        ps.setInt(8, p.getId());

        ps.executeUpdate();
        System.out.println("✅ Projet mis à jour");
    }

    @Override
    public void deleteProjet(int id) throws SQLException {

        String sql = "DELETE FROM projet WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("✅ Projet supprimé");
    }

    @Override
    public Projet getProjetById(int id) throws SQLException {

        String sql = "SELECT * FROM projet WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new Projet(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("statut"),
                    rs.getInt("rh_id"),
                    rs.getInt("responsable_employe_id"),
                    rs.getDate("date_debut").toLocalDate(),
                    rs.getDate("date_fin").toLocalDate()
            );
        }
        return null;
    }

    @Override
    public List<Projet> getAllProjets() throws SQLException {

        List<Projet> list = new ArrayList<>();
        String sql = "SELECT * FROM projet";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            list.add(new Projet(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("statut"),
                    rs.getInt("rh_id"),
                    rs.getInt("responsable_employe_id"),
                    rs.getDate("date_debut").toLocalDate(),
                    rs.getDate("date_fin").toLocalDate()
            ));
        }
        return list;
    }

    public List<Projet> findByResponsableId(int responsableId) throws SQLException {
        List<Projet> list = new ArrayList<>();
        String sql = "SELECT * FROM projet WHERE responsable_employe_id=? ORDER BY date_debut DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, responsableId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new Projet(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("statut"),
                    rs.getInt("rh_id"),
                    rs.getInt("responsable_employe_id"),
                    rs.getDate("date_debut").toLocalDate(),
                    rs.getDate("date_fin").toLocalDate()
            ));
        }
        return list;
    }

}
