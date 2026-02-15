package edu.RhPro.services;

import edu.RhPro.entities.Service; // Importe l'entité Service
import edu.RhPro.interfaces.IServiceService; // Importe l'interface IServiceService
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate; // Pour la conversion entre LocalDate et java.sql.Date
import java.util.ArrayList;
import java.util.List;

public class ServiceService implements IServiceService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Service service) throws SQLException {
        // Le nom de la table dans la base de données est 'demande_service'
        String sql = "INSERT INTO demande_service (titre, description, date_demande, statut, employe_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) { // Remarque: Il manque 'Statement.RETURN_GENERATED_KEYS' ici pour récupérer l'ID
            ps.setString(1, service.getTitre());

            // Gère le champ 'description' qui peut être NULL
            if (service.getDescription() != null) {
                ps.setString(2, service.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR); // Assumant que 'description' est TEXT ou VARCHAR
            }

            // Convertit LocalDate en java.sql.Date
            if (service.getDateDemande() != null) {
                ps.setDate(3, Date.valueOf(service.getDateDemande()));
            } else {
                ps.setNull(3, Types.DATE);
            }

            ps.setString(4, service.getStatut());
            ps.setLong(5, service.getEmployeeId());

            ps.executeUpdate();

            // Remarque: Le code pour récupérer l'ID généré et le set sur l'objet 'service' est absent,
            // conformément à l'exemple de CongeService. Par conséquent, service.getId() restera 0.
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
        String sql = "UPDATE demande_service SET titre=?, description=?, date_demande=?, statut=?, employe_id=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, service.getTitre());

            if (service.getDescription() != null) {
                ps.setString(2, service.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }

            if (service.getDateDemande() != null) {
                ps.setDate(3, Date.valueOf(service.getDateDemande()));
            } else {
                ps.setNull(3, Types.DATE);
            }

            ps.setString(4, service.getStatut());
            ps.setLong(5, service.getEmployeeId());
            ps.setLong(6, service.getId()); // L'ID est utilisé pour la clause WHERE

            ps.executeUpdate();
        }
    }

    @Override
    public List<Service> getData() throws SQLException {
        String sql = "SELECT id, titre, description, date_demande, statut, employe_id FROM demande_service";
        List<Service> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Création d'un nouvel objet Service en utilisant le constructeur sans ID,
                // conformément à l'exemple de CongeService.
                // Remarque: L'attribut 'id' de cet objet restera 0.
                Service service = new Service(
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDate("date_demande") != null ? rs.getDate("date_demande").toLocalDate() : null,
                        rs.getString("statut"),
                        rs.getLong("employe_id") // Utilise le nom de la colonne BDD 'employe_id'
                );
                // Si l'ID doit être populé, il faudrait ajouter : service.setId(rs.getLong("id")); ici
                list.add(service);
            }
        }
        return list;
    }

    // Méthode pour récupérer un service par son ID
    public Service getById(long id) throws SQLException {
        String sql = "SELECT id, titre, description, date_demande, statut, employe_id " +
                "FROM demande_service WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Création d'un nouvel objet Service en utilisant le constructeur sans ID,
                    // conformément à l'exemple de CongeService.
                    // Remarque: L'attribut 'id' de cet objet restera 0.
                    return new Service(
                            rs.getString("titre"),
                            rs.getString("description"),
                            rs.getDate("date_demande") != null ? rs.getDate("date_demande").toLocalDate() : null,
                            rs.getString("statut"),
                            rs.getLong("employe_id") // Utilise le nom de la colonne BDD 'employe_id'
                    );
                    // Si l'ID doit être populé, il faudrait ajouter : service.setId(rs.getLong("id")); ici
                }
            }
        }
        return null;
    }
}