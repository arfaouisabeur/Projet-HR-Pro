package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Prime;
import edu.RhPro.services.PrimeService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PrimesManageController {

    @FXML private TextField tfEmployeId;
    @FXML private TextField tfMontant;
    @FXML private DatePicker dpAttribution;
    @FXML private TextArea taDescription;
    @FXML private Label msgLabel;

    @FXML private TableView<Prime> table;
    @FXML private TableColumn<Prime, Long> colId;
    @FXML private TableColumn<Prime, Long> colEmploye;
    @FXML private TableColumn<Prime, BigDecimal> colMontant;
    @FXML private TableColumn<Prime, LocalDate> colDate;
    @FXML private TableColumn<Prime, String> colDesc;

    private final PrimeService primeService = new PrimeService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeId"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateAttribution"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loadData();
    }

    @FXML
    public void refresh() {
        loadData();
    }

    @FXML
    public void onAdd() {
        msgLabel.setText("");

        if (Session.getCurrentUser() == null) {
            msgLabel.setText("Session expirée.");
            return;
        }

        try {
            long employeId = Long.parseLong(tfEmployeId.getText().trim());
            BigDecimal montant = new BigDecimal(tfMontant.getText().trim());
            LocalDate date = dpAttribution.getValue();
            String desc = taDescription.getText();

            if (date == null) {
                msgLabel.setText("Choisis la date.");
                return;
            }

            long rhId = Session.getCurrentUser().getId();
            Prime p = new Prime(montant, date, desc, rhId, employeId);

            primeService.addEntity(p);

            tfEmployeId.clear();
            tfMontant.clear();
            dpAttribution.setValue(null);
            taDescription.clear();

            msgLabel.setStyle("-fx-text-fill:#047857; -fx-font-weight:900;");
            msgLabel.setText("Prime ajoutée ✅");
            loadData();

        } catch (NumberFormatException e) {
            msgLabel.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
            msgLabel.setText("Vérifie Employé ID / Montant.");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
            msgLabel.setText("Erreur DB: " + e.getMessage());
        }
    }

    @FXML
    public void onDelete() {
        Prime selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("Sélectionne une prime.");
            return;
        }
        try {
            primeService.deleteEntity(selected);
            msgLabel.setStyle("-fx-text-fill:#047857; -fx-font-weight:900;");
            msgLabel.setText("Supprimé ✅");
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
            msgLabel.setText("Erreur DB: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            List<Prime> list = primeService.getData();
            table.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
