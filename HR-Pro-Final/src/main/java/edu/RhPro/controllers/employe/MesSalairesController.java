package edu.RhPro.controllers.employe;

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

public class MesSalairesController {

    @FXML private TableView<Salaire> table;
    @FXML private TableColumn<Salaire, Long> colId;
    @FXML private TableColumn<Salaire, Integer> colMois;
    @FXML private TableColumn<Salaire, Integer> colAnnee;
    @FXML private TableColumn<Salaire, BigDecimal> colMontant;
    @FXML private TableColumn<Salaire, LocalDate> colDate;
    @FXML private TableColumn<Salaire, String> colStatut;
    @FXML private Label msgLabel;

    private final SalaireService salaireService = new SalaireService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
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

    private void loadData() {
        try {
            if (Session.getCurrentUser() == null) return;

            long empId = Session.getCurrentUser().getId();
            List<Salaire> list = salaireService.findByEmployeId(empId);

            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setText(list.size() + " salaire(s)");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("Erreur DB: " + e.getMessage());
        }
    }
}
