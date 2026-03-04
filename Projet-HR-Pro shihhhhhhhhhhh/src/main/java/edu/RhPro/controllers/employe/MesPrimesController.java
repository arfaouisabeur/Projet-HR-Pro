package edu.RhPro.controllers.employe;

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

public class MesPrimesController {

    @FXML private TableView<Prime> table;
    @FXML private TableColumn<Prime, Long> colId;
    @FXML private TableColumn<Prime, BigDecimal> colMontant;
    @FXML private TableColumn<Prime, LocalDate> colDate;
    @FXML private TableColumn<Prime, String> colDesc;
    @FXML private Label msgLabel;

    private final PrimeService primeService = new PrimeService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
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

    private void loadData() {
        try {
            if (Session.getCurrentUser() == null) return;

            long empId = Session.getCurrentUser().getId();
            List<Prime> list = primeService.findByEmployeId(empId);

            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setText(list.size() + " prime(s)");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("Erreur DB: " + e.getMessage());
        }
    }
}
