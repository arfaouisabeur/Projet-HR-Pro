package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceService implements IServiceService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Service service) throws SQLException {
        String sql = "INSERT INTO demande_service (titre, description, date_demande, statut, employe_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, service.getTitre());

            if (service.getDescription() != null && !service.getDescription().isBlank())
                ps.setString(2, service.getDescription());
            else
                ps.setNull(2, Types.LONGVARCHAR);

            if (service.getDateDemande() != null) ps.setDate(3, Date.valueOf(service.getDateDemande()));
            else ps.setNull(3, Types.DATE);

            ps.setString(4, service.getStatut());
            ps.setLong(5, service.getEmployeeId()); // DB = employe_id

            ps.executeUpdate();
        }
    }

    @Override
    public void deleteEntity(Service service) throws SQLException {
        String sql = "DELETE FROM demande_service WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, service.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateEntity(Service service) throws SQLException {
        String sql = "UPDATE demande_service SET titre=?, description=?, date_demande=?, statut=?, employe_id=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, service.getTitre());

            if (service.getDescription() != null && !service.getDescription().isBlank())
                ps.setString(2, service.getDescription());
            else
                ps.setNull(2, Types.LONGVARCHAR);

            if (service.getDateDemande() != null) ps.setDate(3, Date.valueOf(service.getDateDemande()));
            else ps.setNull(3, Types.DATE);

            ps.setString(4, service.getStatut());
            ps.setLong(5, service.getEmployeeId());
            ps.setLong(6, service.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public List<Service> getData() throws SQLException {
        String sql = "SELECT id, titre, description, date_demande, statut, employe_id FROM demande_service";
        List<Service> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Service getById(long id) throws SQLException {
        String sql = "SELECT id, titre, description, date_demande, statut, employe_id FROM demande_service WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    // ✅ Employee view only his requests
    public List<Service> findByEmployeId(long employeId) throws SQLException {
        String sql = "SELECT id, titre, description, date_demande, statut, employe_id " +
                "FROM demande_service WHERE employe_id=? ORDER BY date_demande DESC";
        List<Service> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, employeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ✅ RH updates status only
    public void updateStatus(long serviceId, String newStatus) throws SQLException {
        String sql = "UPDATE demande_service SET statut=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setLong(2, serviceId);
            ps.executeUpdate();
        }
    }

    private Service map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String titre = rs.getString("titre");
        String desc = rs.getString("description");

        Date dd = rs.getDate("date_demande");
        LocalDate date = dd != null ? dd.toLocalDate() : null;

        String statut = rs.getString("statut");
        long employeId = rs.getLong("employe_id");

        return new Service(id, titre, desc, date, statut, employeId);
    }
    public List<Service> findPending() throws SQLException {
        String sql = "SELECT id, titre, description, date_demande, statut, employe_id " +
                "FROM demande_service WHERE statut='EN_ATTENTE' ORDER BY date_demande DESC";
        List<Service> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

}
