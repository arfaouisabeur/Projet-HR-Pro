package edu.RhPro.services;

import edu.RhPro.entities.Tache;
import edu.RhPro.interfaces.ITacheService;
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TacheService implements ITacheService {

    Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addTache(Tache t) throws SQLException {

        String sql = "INSERT INTO tache (titre, description, statut, projet_id, employe_id, prime_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getTitre());
        ps.setString(2, t.getDescription());
        ps.setString(3, t.getStatut());
        ps.setInt(4, t.getProjetId());
        ps.setInt(5, t.getEmployeId());

        if (t.getPrimeId() == null)
            ps.setNull(6, Types.INTEGER);
        else
            ps.setInt(6, t.getPrimeId());

        ps.executeUpdate();
        System.out.println("✅ Tâche ajoutée");
    }

    @Override
    public void updateTache(Tache t) throws SQLException {

        String sql = "UPDATE tache SET statut=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getStatut());
        ps.setInt(2, t.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteTache(int id) throws SQLException {

        String sql = "DELETE FROM tache WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public Tache getTacheById(int id) throws SQLException {

        String sql = "SELECT * FROM tache WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new Tache(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("statut"),
                    rs.getInt("projet_id"),
                    rs.getInt("employe_id"),
                    (Integer) rs.getObject("prime_id")
            );
        }
        return null;
    }

    @Override
    public List<Tache> getAllTaches() throws SQLException {

        List<Tache> list = new ArrayList<>();
        String sql = "SELECT * FROM tache";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            list.add(new Tache(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("statut"),
                    rs.getInt("projet_id"),
                    rs.getInt("employe_id"),
                    (Integer) rs.getObject("prime_id")
            ));
        }
        return list;
    }
}
