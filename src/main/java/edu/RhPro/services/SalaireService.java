package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalaireService implements ISalaireService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Salaire s) throws SQLException {
        String sql = "INSERT INTO salaire (mois, annee, montant, date_paiement, statut, rh_id, employe_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, s.getMois());
            ps.setInt(2, s.getAnnee());
            ps.setBigDecimal(3, s.getMontant());

            if (s.getDatePaiement() != null) ps.setDate(4, Date.valueOf(s.getDatePaiement()));
            else ps.setNull(4, Types.DATE);

            ps.setString(5, s.getStatut());
            ps.setLong(6, s.getRhId());
            ps.setLong(7, s.getEmployeId());

            ps.executeUpdate();


        }
    }

    @Override
    public void deleteEntity(Salaire s) throws SQLException {
        String sql = "DELETE FROM salaire WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, s.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateEntity(Salaire s) throws SQLException {
        String sql = "UPDATE salaire SET mois=?, annee=?, montant=?, date_paiement=?, statut=?, rh_id=?, employe_id=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, s.getMois());
            ps.setInt(2, s.getAnnee());
            ps.setBigDecimal(3, s.getMontant());

            if (s.getDatePaiement() != null) ps.setDate(4, Date.valueOf(s.getDatePaiement()));
            else ps.setNull(4, Types.DATE);

            ps.setString(5, s.getStatut());
            ps.setLong(6, s.getRhId());
            ps.setLong(7, s.getEmployeId());
            ps.setLong(8, s.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public List<Salaire> getData() throws SQLException {
        String sql = "SELECT id, mois, annee, montant, date_paiement, statut, rh_id, employe_id FROM salaire";
        List<Salaire> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Salaire s = new Salaire();
                s.setId(rs.getLong("id"));
                s.setMois(rs.getInt("mois"));
                s.setAnnee(rs.getInt("annee"));
                s.setMontant(rs.getBigDecimal("montant"));

                Date dp = rs.getDate("date_paiement");
                if (dp != null) s.setDatePaiement(dp.toLocalDate());

                s.setStatut(rs.getString("statut"));
                s.setRhId(rs.getLong("rh_id"));
                s.setEmployeId(rs.getLong("employe_id"));

                list.add(s);
            }
        }

        return list;
    }
    public List<Salaire> findByEmployeId(long employeId) throws SQLException {
        String sql = "SELECT id, mois, annee, montant, date_paiement, statut, rh_id, employe_id " +
                "FROM salaire WHERE employe_id = ? ORDER BY annee DESC, mois DESC";
        List<Salaire> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, employeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Salaire s = new Salaire();
                    s.setId(rs.getLong("id"));
                    s.setMois(rs.getInt("mois"));
                    s.setAnnee(rs.getInt("annee"));
                    s.setMontant(rs.getBigDecimal("montant"));

                    Date dp = rs.getDate("date_paiement");
                    if (dp != null) s.setDatePaiement(dp.toLocalDate());

                    s.setStatut(rs.getString("statut"));
                    s.setRhId(rs.getLong("rh_id"));
                    s.setEmployeId(rs.getLong("employe_id"));

                    list.add(s);
                }
            }
        }
        return list;
    }

}
