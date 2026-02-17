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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MesSalairesController {

    @FXML private TableView<Salaire> table;
    @FXML private TableColumn<Salaire, Integer> colMois;
    @FXML private TableColumn<Salaire, Integer> colAnnee;
    @FXML private TableColumn<Salaire, BigDecimal> colMontant;
    @FXML private TableColumn<Salaire, LocalDate> colDate;
    @FXML private TableColumn<Salaire, String> colStatut;

    @FXML private Label msgLabel;

    // Summary labels
    @FXML private Label lblTotalPaye;
    @FXML private Label lblEnAttente;
    @FXML private Label lblDernierPaiement;

    private final SalaireService salaireService = new SalaireService();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat moneyFmt = NumberFormat.getNumberInstance(Locale.FRANCE);

    @FXML
    public void initialize() {
        moneyFmt.setMinimumFractionDigits(2);
        moneyFmt.setMaximumFractionDigits(2);

        colMois.setCellValueFactory(new PropertyValueFactory<>("mois"));
        colAnnee.setCellValueFactory(new PropertyValueFactory<>("annee"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("datePaiement"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Format montant
        colMontant.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(moneyFmt.format(value) + " DT");
                }
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

        // Status badge (label)
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(statut);
                badge.setStyle(badgeStyle(statut));
                setGraphic(badge);
                setText(null);
            }
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
            List<Salaire> list = salaireService.findByEmployeId(empId);

            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setText(list.size() + " salaire(s)");

            updateSummary(list);

        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("Erreur DB: " + e.getMessage());
        }
    }

    private void updateSummary(List<Salaire> list) {
        BigDecimal totalPaye = BigDecimal.ZERO;
        int pending = 0;

        Salaire lastPaid = list.stream()
                .filter(s -> s.getStatut() != null && s.getStatut().equalsIgnoreCase("PAYE"))
                .filter(s -> s.getDatePaiement() != null)
                .max(Comparator.comparing(Salaire::getDatePaiement))
                .orElse(null);

        for (Salaire s : list) {
            String st = s.getStatut() == null ? "" : s.getStatut().toUpperCase();
            if ("PAYE".equals(st)) {
                if (s.getMontant() != null) totalPaye = totalPaye.add(s.getMontant());
            } else if ("EN_ATTENTE".equals(st) || "ATTENTE".equals(st)) {
                pending++;
            }
        }

        lblTotalPaye.setText(moneyFmt.format(totalPaye) + " DT");
        lblEnAttente.setText(String.valueOf(pending));

        if (lastPaid == null) {
            lblDernierPaiement.setText("-");
        } else {
            String d = dateFmt.format(lastPaid.getDatePaiement());
            String m = lastPaid.getMontant() == null ? "-" : (moneyFmt.format(lastPaid.getMontant()) + " DT");
            lblDernierPaiement.setText(d + " • " + m);
        }
    }

    private String badgeStyle(String statut) {
        String st = statut.toUpperCase();

        if (st.contains("PAYE")) {
            return "-fx-background-color:#dcfce7; -fx-text-fill:#166534; -fx-font-weight:900;" +
                    "-fx-padding:4 10; -fx-background-radius:999;";
        }
        if (st.contains("EN_ATTENTE") || st.contains("ATTENTE")) {
            return "-fx-background-color:#ffedd5; -fx-text-fill:#9a3412; -fx-font-weight:900;" +
                    "-fx-padding:4 10; -fx-background-radius:999;";
        }
        if (st.contains("ANNULE")) {
            return "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b; -fx-font-weight:900;" +
                    "-fx-padding:4 10; -fx-background-radius:999;";
        }
        return "-fx-background-color:#f3f4f6; -fx-text-fill:#374151; -fx-font-weight:900;" +
                "-fx-padding:4 10; -fx-background-radius:999;";
    }
}
