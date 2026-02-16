package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrimeService implements IPrimeService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Prime p) throws SQLException {
        String sql = "INSERT INTO prime (montant, date_attribution, description, rh_id, employe_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setBigDecimal(1, p.getMontant());

            if (p.getDateAttribution() != null) ps.setDate(2, Date.valueOf(p.getDateAttribution()));
            else ps.setNull(2, Types.DATE);

            ps.setString(3, p.getDescription());
            ps.setLong(4, p.getRhId());
            ps.setLong(5, p.getEmployeId());

            ps.executeUpdate();
        }
    }

    @Override
    public void deleteEntity(Prime p) throws SQLException {
        String sql = "DELETE FROM prime WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, p.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateEntity(Prime p) throws SQLException {
        String sql = "UPDATE prime SET montant=?, date_attribution=?, description=?, rh_id=?, employe_id=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setBigDecimal(1, p.getMontant());

            if (p.getDateAttribution() != null) ps.setDate(2, Date.valueOf(p.getDateAttribution()));
            else ps.setNull(2, Types.DATE);

            ps.setString(3, p.getDescription());
            ps.setLong(4, p.getRhId());
            ps.setLong(5, p.getEmployeId());
            ps.setLong(6, p.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public List<Prime> getData() throws SQLException {
        String sql = "SELECT id, montant, date_attribution, description, rh_id, employe_id FROM prime";
        List<Prime> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Prime p = new Prime();
                p.setId(rs.getLong("id"));
                p.setMontant(rs.getBigDecimal("montant"));

                Date da = rs.getDate("date_attribution");
                if (da != null) p.setDateAttribution(da.toLocalDate());

                p.setDescription(rs.getString("description"));
                p.setRhId(rs.getLong("rh_id"));
                p.setEmployeId(rs.getLong("employe_id"));

                list.add(p);
            }
        }

        return list;
    }
    public List<Prime> findByEmployeId(long employeId) throws SQLException {
        String sql = "SELECT id, montant, date_attribution, description, rh_id, employe_id " +
                "FROM prime WHERE employe_id = ? ORDER BY date_attribution DESC";
        List<Prime> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, employeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Prime p = new Prime();
                    p.setId(rs.getLong("id"));
                    p.setMontant(rs.getBigDecimal("montant"));

                    Date da = rs.getDate("date_attribution");
                    if (da != null) p.setDateAttribution(da.toLocalDate());

                    p.setDescription(rs.getString("description"));
                    p.setRhId(rs.getLong("rh_id"));
                    p.setEmployeId(rs.getLong("employe_id"));

                    list.add(p);
                }
            }
        }
        return list;
    }

}
