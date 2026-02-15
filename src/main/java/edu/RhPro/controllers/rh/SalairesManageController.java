package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Salaire;
import edu.RhPro.services.SalaireService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SalairesManageController {

    @FXML private TextField tfEmployeId;
    @FXML private ComboBox<Integer> cbMois;
    @FXML private TextField tfAnnee;
    @FXML private TextField tfMontant;
    @FXML private DatePicker dpPaiement;
    @FXML private ComboBox<String> cbStatut;
    @FXML private Label msgLabel;

    @FXML private TableView<Salaire> table;
    @FXML private TableColumn<Salaire, Long> colId;
    @FXML private TableColumn<Salaire, Long> colEmploye;
    @FXML private TableColumn<Salaire, Integer> colMois;
    @FXML private TableColumn<Salaire, Integer> colAnnee;
    @FXML private TableColumn<Salaire, BigDecimal> colMontant;
    @FXML private TableColumn<Salaire, LocalDate> colDate;
    @FXML private TableColumn<Salaire, String> colStatut;

    private final SalaireService salaireService = new SalaireService();

    @FXML
    public void initialize() {
        cbMois.setItems(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));
        cbStatut.setItems(FXCollections.observableArrayList("EN_ATTENTE", "PAYE"));

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeId"));
        colMois.setCellValueFactory(new PropertyValueFactory<>("mois"));
        colAnnee.setCellValueFactory(new PropertyValueFactory<>("annee"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("datePaiement"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

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
            Integer mois = cbMois.getValue();
            int annee = Integer.parseInt(tfAnnee.getText().trim());
            BigDecimal montant = new BigDecimal(tfMontant.getText().trim());
            LocalDate paiement = dpPaiement.getValue();
            String statut = cbStatut.getValue();

            if (mois == null || statut == null) {
                msgLabel.setText("Choisis mois + statut.");
                return;
            }

            long rhId = Session.getCurrentUser().getId();
            Salaire s = new Salaire(mois, annee, montant, paiement, statut, rhId, employeId);

            salaireService.addEntity(s);

            tfEmployeId.clear();
            cbMois.setValue(null);
            tfAnnee.clear();
            tfMontant.clear();
            dpPaiement.setValue(null);
            cbStatut.setValue(null);

            msgLabel.setStyle("-fx-text-fill:#047857; -fx-font-weight:900;");
            msgLabel.setText("Salaire ajouté ✅");
            loadData();

        } catch (NumberFormatException e) {
            msgLabel.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
            msgLabel.setText("Vérifie Employé ID / Année / Montant.");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
            msgLabel.setText("Erreur DB: " + e.getMessage());
        }
    }

    @FXML
    public void onDelete() {
        Salaire selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("Sélectionne un salaire.");
            return;
        }
        try {
            salaireService.deleteEntity(selected);
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
            List<Salaire> list = salaireService.getData();
            table.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
