package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Reponse;
import edu.RhPro.entities.Service;
import edu.RhPro.services.ReponseService;
import edu.RhPro.services.ServiceService;
import edu.RhPro.tools.MyConnection;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

public class ServicesManageController {

    @FXML private TableView<Service> table;
    @FXML private TableColumn<Service, Long> colId;
    @FXML private TableColumn<Service, Long> colEmploye;
    @FXML private TableColumn<Service, String> colTitre;
    @FXML private TableColumn<Service, LocalDate> colDate;
    @FXML private TableColumn<Service, String> colDesc;

    @FXML private TextArea taCommentaire;
    @FXML private Label msgLabel;

    private final ServiceService serviceService = new ServiceService();
    private final ReponseService reponseService = new ReponseService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateDemande"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadData();
    }

    @FXML
    public void refresh() {
        loadData();
    }

    @FXML
    private void onTraiter() {
        handleDecision("TRAITEE");
    }

    @FXML
    private void onRefuse() {
        handleDecision("REFUSEE");
    }

    private void handleDecision(String decisionStatut) {
        msgLabel.setText("");

        if (Session.getCurrentUser() == null) {
            msgLabel.setText("Session expirée.");
            return;
        }

        Service selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("Sélectionne une demande.");
            return;
        }

        long rhId = Session.getCurrentUser().getId();
        long employeId = selected.getEmployeeId();
        long serviceId = selected.getId();
        String commentaire = taCommentaire.getText();

        try {
            if (reponseService.hasReponseForService(serviceId)) {
                msgLabel.setText("Cette demande a déjà une réponse.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("Erreur DB: " + e.getMessage());
            return;
        }

        Connection cnx = MyConnection.getInstance().getCnx();
        try {
            cnx.setAutoCommit(false);

            try (PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE demande_service SET statut=? WHERE id=?")) {
                ps.setString(1, decisionStatut);
                ps.setLong(2, serviceId);
                ps.executeUpdate();
            }

            Reponse rep = Reponse.forService(
                    decisionStatut,
                    commentaire,
                    rhId,
                    employeId,
                    serviceId
            );

            try (PreparedStatement ps = cnx.prepareStatement(
                    "INSERT INTO reponse (decision, commentaire, rh_id, employe_id, conge_tt_id, demande_service_id) VALUES (?,?,?,?,?,?)")) {

                ps.setString(1, rep.getDecision());

                if (rep.getCommentaire() != null && !rep.getCommentaire().isBlank())
                    ps.setString(2, rep.getCommentaire());
                else
                    ps.setNull(2, Types.LONGVARCHAR);

                ps.setLong(3, rep.getRhId());
                ps.setObject(4, rep.getEmployeId());
                ps.setObject(5, null);
                ps.setObject(6, rep.getDemandeServiceId());

                ps.executeUpdate();
            }

            cnx.commit();
            cnx.setAutoCommit(true);

            taCommentaire.clear();
            msgLabel.setStyle("-fx-text-fill:#047857; -fx-font-weight:900;");
            msgLabel.setText("Décision enregistrée ✅");

            loadData();

        } catch (Exception e) {
            e.printStackTrace();
            try { cnx.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { cnx.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }

            msgLabel.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            List<Service> list = serviceService.findPending(); // ✅ needs method in ServiceService
            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setStyle("-fx-text-fill:#6b7280; -fx-font-weight:900;");
            msgLabel.setText(list.size() + " demande(s) en attente");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
            msgLabel.setText("Erreur DB: " + e.getMessage());
        }
    }
}
