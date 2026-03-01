package edu.RhPro.services;

import edu.RhPro.entities.Service;
import edu.RhPro.interfaces.IServiceService;
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceService implements IServiceService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Service service) throws SQLException {
        String sql = "INSERT INTO demande_service (titre, description, date_demande, statut, " +
                "employe_id, etape_workflow, date_derniere_etape, priorite, deadline_reponse, sla_depasse) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, service.getTitre());
            ps.setString(2, service.getDescription());
            ps.setDate(3, Date.valueOf(service.getDateDemande()));
            ps.setString(4, service.getStatut());
            ps.setLong(5, service.getEmployeeId());
            ps.setString(6, service.getEtapeWorkflow() != null ? service.getEtapeWorkflow() : "SOUMISE");
            ps.setDate(7, Date.valueOf(LocalDate.now()));
            ps.setString(8, service.getPriorite() != null ? service.getPriorite() : "NORMAL");

            // Calculer deadline selon priorité
            LocalDate deadline = calculerDeadline(service.getPriorite());
            ps.setDate(9, Date.valueOf(deadline));
            ps.setBoolean(10, false);

            ps.executeUpdate();
        }
    }
    public static LocalDate calculerDeadline(String priorite) {
        return switch (priorite != null ? priorite : "NORMAL") {
            case "URGENT" -> LocalDate.now().plusDays(1);   // 24h
            case "NORMAL" -> LocalDate.now().plusDays(3);   // 72h
            case "FAIBLE" -> LocalDate.now().plusDays(7);   // 7 jours
            default       -> LocalDate.now().plusDays(3);
        };
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
        String sql = "UPDATE demande_service SET titre=?, description=?, date_demande=?, statut=?, " +
                "employe_id=?, etape_workflow=?, date_derniere_etape=?, priorite=?, " +
                "deadline_reponse=?, sla_depasse=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, service.getTitre());
            ps.setString(2, service.getDescription());
            ps.setDate(3, Date.valueOf(service.getDateDemande()));
            ps.setString(4, service.getStatut());
            ps.setLong(5, service.getEmployeeId());
            ps.setString(6, service.getEtapeWorkflow());
            ps.setDate(7, service.getDateDerniereEtape() != null
                    ? Date.valueOf(service.getDateDerniereEtape()) : Date.valueOf(LocalDate.now()));
            ps.setString(8, service.getPriorite());
            ps.setDate(9, service.getDeadlineReponse() != null
                    ? Date.valueOf(service.getDeadlineReponse()) : null);
            ps.setBoolean(10, service.isSlaDepasse());
            ps.setLong(11, service.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Service> getData() throws SQLException {
        String sql = "SELECT id, titre, description, date_demande, statut, employe_id, " +
                "etape_workflow, date_derniere_etape, priorite, deadline_reponse, " +
                "sla_depasse, pdf_path FROM demande_service";
        List<Service> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Service getById(long id) throws SQLException {
        String sql = "SELECT id, titre, description, date_demande, statut, employe_id, " +
                "etape_workflow, date_derniere_etape, priorite, deadline_reponse, " +
                "sla_depasse, pdf_path FROM demande_service WHERE id=?";
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
        String sql = "SELECT id, titre, description, date_demande, statut, employe_id, " +
                "etape_workflow, date_derniere_etape, priorite, deadline_reponse, " +
                "sla_depasse, pdf_path " +
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
        Service s = new Service();
        s.setId(rs.getLong("id"));
        s.setTitre(rs.getString("titre"));
        s.setDescription(rs.getString("description"));
        Date dd = rs.getDate("date_demande");
        s.setDateDemande(dd != null ? dd.toLocalDate() : null);
        s.setStatut(rs.getString("statut"));
        s.setEmployeeId(rs.getLong("employe_id"));
        s.setEtapeWorkflow(rs.getString("etape_workflow"));
        Date dle = rs.getDate("date_derniere_etape");
        s.setDateDerniereEtape(dle != null ? dle.toLocalDate() : null);
        s.setPriorite(rs.getString("priorite"));
        Date dl = rs.getDate("deadline_reponse");
        s.setDeadlineReponse(dl != null ? dl.toLocalDate() : null);
        s.setSlaDepasse(rs.getBoolean("sla_depasse"));
        s.setPdfPath(rs.getString("pdf_path"));
        return s;
    }
    public List<Service> findPending() throws SQLException {
        String sql = "SELECT id, titre, description, date_demande, statut, employe_id, " +
                "etape_workflow, date_derniere_etape, priorite, deadline_reponse, " +
                "sla_depasse, pdf_path " +
                "FROM demande_service WHERE statut='EN_ATTENTE' ORDER BY date_demande DESC";
        List<Service> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }




    // Vérifier et mettre à jour les SLA dépassés
    public void verifierSlaDepasses() throws SQLException {
        String sql = "UPDATE demande_service SET sla_depasse=TRUE " +
                "WHERE deadline_reponse < CURDATE() " +
                "AND statut NOT IN ('RESOLUE','REJETEE') " +
                "AND sla_depasse=FALSE";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }


    // Score récurrence — combien de demandes ce mois par employé
    public int getScoreRecurrence(long employeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM demande_service " +
                "WHERE employe_id=? " +
                "AND MONTH(date_demande)=MONTH(CURDATE()) " +
                "AND YEAR(date_demande)=YEAR(CURDATE())";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, employeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }




    // Stats par statut
    public java.util.Map<String, Integer> getStatsByStatut() throws SQLException {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT statut, COUNT(*) as total FROM demande_service GROUP BY statut";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getString("statut"), rs.getInt("total"));
        }
        return map;
    }

    // Stats par mois
    public java.util.Map<String, Integer> getStatsByMois() throws SQLException {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(date_demande,'%Y-%m') as mois, COUNT(*) as total " +
                "FROM demande_service GROUP BY mois ORDER BY mois DESC LIMIT 6";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getString("mois"), rs.getInt("total"));
        }
        return map;
    }

    // Délai moyen de traitement (en jours)
    public double getDelaiMoyenTraitement() throws SQLException {
        String sql = "SELECT AVG(DATEDIFF(date_derniere_etape, date_demande)) " +
                "FROM demande_service WHERE statut IN ('RESOLUE','REJETEE')";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    // Taux de résolution
    public double getTauxResolution() throws SQLException {
        String sql = "SELECT " +
                "COUNT(CASE WHEN statut='RESOLUE' THEN 1 END) * 100.0 / COUNT(*) " +
                "FROM demande_service";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    // Stats par priorité (URGENT / NORMAL / FAIBLE)
    public java.util.Map<String, Integer> getStatsByPriorite() throws SQLException {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        // Pré-remplir dans l'ordre souhaité pour l'affichage
        map.put("URGENT", 0);
        map.put("NORMAL", 0);
        map.put("FAIBLE", 0);
        String sql = "SELECT priorite, COUNT(*) as total FROM demande_service " +
                "WHERE priorite IS NOT NULL GROUP BY priorite";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String p = rs.getString("priorite");
                if (p != null) map.put(p, rs.getInt("total"));
            }
        }
        return map;
    }

    // Stats SLA : dépassés vs respectés
    public java.util.Map<String, Integer> getStatsSla() throws SQLException {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT " +
                "SUM(CASE WHEN sla_depasse = TRUE  THEN 1 ELSE 0 END) AS depasses, " +
                "SUM(CASE WHEN sla_depasse = FALSE THEN 1 ELSE 0 END) AS respectes " +
                "FROM demande_service";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                map.put("SLA Dépassés",  rs.getInt("depasses"));
                map.put("SLA Respectés", rs.getInt("respectes"));
            }
        }
        return map;
    }

    // Nombre de demandes urgentes en attente (pour alerte KPI)
    public int getNbUrgentesEnAttente() throws SQLException {
        String sql = "SELECT COUNT(*) FROM demande_service " +
                "WHERE priorite='URGENT' AND statut NOT IN ('RESOLUE','REJETEE')";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

}
