package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Conge;
import edu.RhPro.entities.Reponse;
import edu.RhPro.services.CongeService;
import edu.RhPro.services.ReponseService;
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

import edu.RhPro.tools.MyConnection;

public class CongesManageController {

    @FXML private TableView<Conge> table;
    @FXML private TableColumn<Conge, Long> colId;
    @FXML private TableColumn<Conge, Long> colEmploye;
    @FXML private TableColumn<Conge, String> colType;
    @FXML private TableColumn<Conge, LocalDate> colDebut;
    @FXML private TableColumn<Conge, LocalDate> colFin;
    @FXML private TableColumn<Conge, String> colDesc;

    @FXML private TextArea taCommentaire;
    @FXML private Label msgLabel;

    private final CongeService congeService = new CongeService();
    private final ReponseService reponseService = new ReponseService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeConge"));
        colDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadData();
    }

    @FXML
    public void refresh() {
        loadData();
    }

    @FXML
    private void onAccept() {
        handleDecision("ACCEPTEE");
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

        Conge selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("Sélectionne une demande.");
            return;
        }

        long rhId = Session.getCurrentUser().getId();
        long employeId = selected.getEmployeeId();
        long congeId = selected.getId();
        String commentaire = taCommentaire.getText();

        // Prevent double response
        try {
            if (reponseService.hasReponseForConge(congeId)) {
                msgLabel.setText("Cette demande a déjà une réponse.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("Erreur DB: " + e.getMessage());
            return;
        }

        // ✅ Transaction: update status + insert response
        Connection cnx = MyConnection.getInstance().getCnx();
        try {
            cnx.setAutoCommit(false);

            try (PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE conge_tt SET statut=? WHERE id=?")) {
                ps.setString(1, decisionStatut);
                ps.setLong(2, congeId);
                ps.executeUpdate();
            }

            Reponse rep = Reponse.forConge(
                    decisionStatut,
                    commentaire,
                    rhId,
                    employeId,
                    congeId
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
                ps.setObject(5, rep.getCongeTtId());
                ps.setObject(6, null);

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
            List<Conge> list = congeService.findPending(); // ✅ needs method in CongeService
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
