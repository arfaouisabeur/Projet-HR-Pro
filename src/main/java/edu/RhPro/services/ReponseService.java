package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReponseService implements IReponseService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Reponse reponse) throws SQLException {
        String sql = "INSERT INTO reponse (decision, commentaire, rh_id, employe_id, conge_tt_id, demande_service_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, reponse.getDecision());
            ps.setString(2, reponse.getCommentaire());
            ps.setLong(3, reponse.getRhId());

            // Gestion des valeurs NULL
            ps.setObject(4, reponse.getEmployeId());
            ps.setObject(5, reponse.getCongeTtId());
            ps.setObject(6, reponse.getDemandeServiceId());

            ps.executeUpdate();
        }
    }

    @Override
    public void updateEntity(Reponse reponse) throws SQLException {
        String sql = "UPDATE reponse SET decision=?, commentaire=?, rh_id=?, employe_id=?, " +
                "conge_tt_id=?, demande_service_id=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, reponse.getDecision());
            ps.setString(2, reponse.getCommentaire());
            ps.setLong(3, reponse.getRhId());

            // Gestion des valeurs NULL
            ps.setObject(4, reponse.getEmployeId());
            ps.setObject(5, reponse.getCongeTtId());
            ps.setObject(6, reponse.getDemandeServiceId());
            ps.setLong(7, reponse.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void deleteEntity(Reponse reponse) throws SQLException {
        String sql = "DELETE FROM reponse WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, reponse.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Reponse> getData() throws SQLException {
        String sql = "SELECT id, decision, commentaire, rh_id, employe_id, conge_tt_id, demande_service_id FROM reponse";
        List<Reponse> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Reponse reponse = new Reponse(
                        rs.getLong("id"),
                        rs.getString("decision"),
                        rs.getString("commentaire"),
                        rs.getLong("rh_id"),
                        rs.getObject("employe_id", Long.class),
                        rs.getObject("conge_tt_id", Long.class),
                        rs.getObject("demande_service_id", Long.class)
                );
                list.add(reponse);
            }
        }
        return list;
    }

    @Override
    public Reponse getById(long id) throws SQLException {
        String sql = "SELECT id, decision, commentaire, rh_id, employe_id, conge_tt_id, demande_service_id " +
                "FROM reponse WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Reponse(
                            rs.getLong("id"),
                            rs.getString("decision"),
                            rs.getString("commentaire"),
                            rs.getLong("rh_id"),
                            rs.getObject("employe_id", Long.class),
                            rs.getObject("conge_tt_id", Long.class),
                            rs.getObject("demande_service_id", Long.class)
                    );
                }
            }
        }
        return null;
    }

    @Override
    public List<Reponse> getReponsesByRhId(long rhId) throws SQLException {
        String sql = "SELECT id, decision, commentaire, rh_id, employe_id, conge_tt_id, demande_service_id " +
                "FROM reponse WHERE rh_id = ?";

        List<Reponse> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, rhId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reponse reponse = new Reponse(
                            rs.getLong("id"),
                            rs.getString("decision"),
                            rs.getString("commentaire"),
                            rs.getLong("rh_id"),
                            rs.getObject("employe_id", Long.class),
                            rs.getObject("conge_tt_id", Long.class),
                            rs.getObject("demande_service_id", Long.class)
                    );
                    list.add(reponse);
                }
            }
        }
        return list;
    }

    @Override
    public List<Reponse> getReponsesByEmployeId(long employeId) throws SQLException {
        String sql = "SELECT id, decision, commentaire, rh_id, employe_id, conge_tt_id, demande_service_id " +
                "FROM reponse WHERE employe_id = ?";

        List<Reponse> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, employeId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reponse reponse = new Reponse(
                            rs.getLong("id"),
                            rs.getString("decision"),
                            rs.getString("commentaire"),
                            rs.getLong("rh_id"),
                            rs.getObject("employe_id", Long.class),
                            rs.getObject("conge_tt_id", Long.class),
                            rs.getObject("demande_service_id", Long.class)
                    );
                    list.add(reponse);
                }
            }
        }
        return list;
    }

    @Override
    public List<Reponse> getReponsesByCongeId(long congeId) throws SQLException {
        String sql = "SELECT id, decision, commentaire, rh_id, employe_id, conge_tt_id, demande_service_id " +
                "FROM reponse WHERE conge_tt_id = ?";

        List<Reponse> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, congeId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reponse reponse = new Reponse(
                            rs.getLong("id"),
                            rs.getString("decision"),
                            rs.getString("commentaire"),
                            rs.getLong("rh_id"),
                            rs.getObject("employe_id", Long.class),
                            rs.getObject("conge_tt_id", Long.class),
                            rs.getObject("demande_service_id", Long.class)
                    );
                    list.add(reponse);
                }
            }
        }
        return list;
    }

    @Override
    public List<Reponse> getReponsesByServiceId(long serviceId) throws SQLException {
        String sql = "SELECT id, decision, commentaire, rh_id, employe_id, conge_tt_id, demande_service_id " +
                "FROM reponse WHERE demande_service_id = ?";

        List<Reponse> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, serviceId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reponse reponse = new Reponse(
                            rs.getLong("id"),
                            rs.getString("decision"),
                            rs.getString("commentaire"),
                            rs.getLong("rh_id"),
                            rs.getObject("employe_id", Long.class),
                            rs.getObject("conge_tt_id", Long.class),
                            rs.getObject("demande_service_id", Long.class)
                    );
                    list.add(reponse);
                }
            }
        }
        return list;
    }
    public Reponse getOneByCongeId(long congeId) throws SQLException {
        String sql = "SELECT id, decision, commentaire, rh_id, employe_id, conge_tt_id, demande_service_id " +
                "FROM reponse WHERE conge_tt_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, congeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Reponse(
                            rs.getLong("id"),
                            rs.getString("decision"),
                            rs.getString("commentaire"),
                            rs.getLong("rh_id"),
                            rs.getObject("employe_id", Long.class),
                            rs.getObject("conge_tt_id", Long.class),
                            rs.getObject("demande_service_id", Long.class)
                    );
                }
            }
        }
        return null;
    }

    public Reponse getOneByServiceId(long serviceId) throws SQLException {
        String sql = "SELECT id, decision, commentaire, rh_id, employe_id, conge_tt_id, demande_service_id " +
                "FROM reponse WHERE demande_service_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Reponse(
                            rs.getLong("id"),
                            rs.getString("decision"),
                            rs.getString("commentaire"),
                            rs.getLong("rh_id"),
                            rs.getObject("employe_id", Long.class),
                            rs.getObject("conge_tt_id", Long.class),
                            rs.getObject("demande_service_id", Long.class)
                    );
                }
            }
        }
        return null;
    }


    @Override
    public boolean hasReponseForConge(long congeId) throws SQLException {
        String sql = "SELECT 1 FROM reponse WHERE conge_tt_id = ? LIMIT 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, congeId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean hasReponseForService(long serviceId) throws SQLException {
        String sql = "SELECT 1 FROM reponse WHERE demande_service_id = ? LIMIT 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, serviceId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}