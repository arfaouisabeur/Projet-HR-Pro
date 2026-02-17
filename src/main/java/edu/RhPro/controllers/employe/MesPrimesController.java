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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MesPrimesController {

    @FXML private TableView<Prime> table;
    @FXML private TableColumn<Prime, BigDecimal> colMontant;
    @FXML private TableColumn<Prime, LocalDate> colDate;
    @FXML private TableColumn<Prime, String> colDesc;

    @FXML private Label msgLabel;

    // Summary labels
    @FXML private Label lblTotalPrimes;
    @FXML private Label lblNbPrimes;
    @FXML private Label lblLastPrime;

    private final PrimeService primeService = new PrimeService();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat moneyFmt = NumberFormat.getNumberInstance(Locale.FRANCE);

    @FXML
    public void initialize() {
        moneyFmt.setMinimumFractionDigits(2);
        moneyFmt.setMaximumFractionDigits(2);

        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateAttribution"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Format montant
        colMontant.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) setText(null);
                else setText(moneyFmt.format(value) + " DT");
            }
        });

        // Format date
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) setText("-");
                else setText(dateFmt.format(value));
            }
        });

        // Make description clean (truncate)
        colDesc.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String txt, boolean empty) {
                super.updateItem(txt, empty);
                if (empty || txt == null) {
                    setText(null);
                } else {
                    String t = txt.strip();
                    if (t.length() > 60) t = t.substring(0, 57) + "...";
                    setText(t);
                }
            }
        });

        // Double click to show full details
        table.setRowFactory(tv -> {
            TableRow<Prime> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    Prime p = row.getItem();
                    showPrimeDetails(p);
                }
            });
            return row;
        });

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

            updateSummary(list);

        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("Erreur DB: " + e.getMessage());
        }
    }

    private void updateSummary(List<Prime> list) {
        BigDecimal total = BigDecimal.ZERO;
        for (Prime p : list) {
            if (p.getMontant() != null) total = total.add(p.getMontant());
        }

        Prime last = list.stream()
                .filter(p -> p.getDateAttribution() != null)
                .max(Comparator.comparing(Prime::getDateAttribution))
                .orElse(null);

        lblTotalPrimes.setText(moneyFmt.format(total) + " DT");
        lblNbPrimes.setText(String.valueOf(list.size()));

        if (last == null) {
            lblLastPrime.setText("-");
        } else {
            String d = dateFmt.format(last.getDateAttribution());
            String m = last.getMontant() == null ? "-" : (moneyFmt.format(last.getMontant()) + " DT");
            lblLastPrime.setText(d + " • " + m);
        }
    }

    private void showPrimeDetails(Prime p) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la prime");
        alert.setHeaderText("Prime du " + (p.getDateAttribution() == null ? "-" : dateFmt.format(p.getDateAttribution())));

        String montant = p.getMontant() == null ? "-" : (moneyFmt.format(p.getMontant()) + " DT");
        String desc = p.getDescription() == null ? "-" : p.getDescription();

        alert.setContentText("Montant: " + montant + "\n\nDescription:\n" + desc);
        alert.showAndWait();
    }
}
