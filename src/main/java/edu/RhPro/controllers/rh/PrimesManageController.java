package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Prime;
import edu.RhPro.services.PrimeService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PrimesManageController {

    // Form
    @FXML private TextField tfEmployeId;
    @FXML private TextField tfMontant;
    @FXML private DatePicker dpAttribution;
    @FXML private TextArea taDescription;
    @FXML private Label msgLabel;

    // Summary
    @FXML private Label lblPrimeTotalCount;
    @FXML private Label lblPrimeThisMonthCount;
    @FXML private Label lblPrimeThisMonthAmount;

    // Filters
    @FXML private TextField tfFilterEmploye;
    @FXML private DatePicker dpFilterFrom;
    @FXML private DatePicker dpFilterTo;

    // Table
    @FXML private TableView<Prime> table;
    @FXML private TableColumn<Prime, Long> colId;
    @FXML private TableColumn<Prime, Long> colEmploye;
    @FXML private TableColumn<Prime, BigDecimal> colMontant;
    @FXML private TableColumn<Prime, LocalDate> colDate;
    @FXML private TableColumn<Prime, String> colDesc;

    private final PrimeService primeService = new PrimeService();

    private final ObservableList<Prime> masterData = FXCollections.observableArrayList();
    private FilteredList<Prime> filteredData;

    private static final NumberFormat MONEY_FMT;
    static {
        MONEY_FMT = NumberFormat.getNumberInstance(Locale.FRANCE);
        MONEY_FMT.setMinimumFractionDigits(2);
        MONEY_FMT.setMaximumFractionDigits(2);
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeId"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateAttribution"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Pretty amount display
        colMontant.setCellFactory(c -> new TableCell<Prime, BigDecimal>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : MONEY_FMT.format(item) + " $");
            }
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Aucune prime."));

        // Double click => edit
        table.setRowFactory(tv -> {
            TableRow<Prime> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit(row.getItem());
                }
            });
            return row;
        });

        // Filtering
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Prime> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        tfFilterEmploye.textProperty().addListener((obs, o, n) -> applyFilters());
        dpFilterFrom.valueProperty().addListener((obs, o, n) -> applyFilters());
        dpFilterTo.valueProperty().addListener((obs, o, n) -> applyFilters());

        loadData();
    }

    @FXML
    public void refresh() {
        loadData();
    }

    @FXML
    public void clearFilters() {
        tfFilterEmploye.clear();
        dpFilterFrom.setValue(null);
        dpFilterTo.setValue(null);
        applyFilters();
        setMsgInfo("Filtres effacés.");
    }

    @FXML
    public void onAdd() {
        clearMsg();

        if (Session.getCurrentUser() == null) {
            setMsgError("Session expirée.");
            return;
        }

        Long employeId = parseLong(tfEmployeId.getText(), "Employé ID");
        BigDecimal montant = parseBigDecimal(tfMontant.getText(), "Montant");
        LocalDate date = dpAttribution.getValue();
        String desc = taDescription.getText() == null ? "" : taDescription.getText().trim();

        if (employeId == null || montant == null) return;
        if (montant.signum() <= 0) { setMsgError("Le montant doit être positif."); return; }
        if (date == null) { setMsgError("Choisis la date d’attribution."); return; }

        if (!confirmAction("Confirmation", "Ajouter une prime ?",
                "Employé: " + employeId + "\nMontant: " + montant + "\nDate: " + date)) return;

        try {
            long rhId = Session.getCurrentUser().getId();
            Prime p = new Prime(montant, date, desc, rhId, employeId);
            primeService.addEntity(p);

            tfEmployeId.clear();
            tfMontant.clear();
            dpAttribution.setValue(null);
            taDescription.clear();

            setMsgSuccess("Prime ajoutée ✅");
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
            setMsgError("Erreur DB: " + e.getMessage());
        }
    }

    @FXML
    public void onDelete() {
        clearMsg();
        Prime selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { setMsgError("Sélectionne une prime."); return; }

        if (!confirmAction("Confirmation", "Supprimer cette prime ?",
                "ID: " + selected.getId() + "\nEmployé: " + selected.getEmployeId()
                        + "\nMontant: " + selected.getMontant() + "\nDate: " + selected.getDateAttribution())) return;

        try {
            primeService.deleteEntity(selected);
            setMsgSuccess("Supprimé ✅");
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
            setMsgError("Erreur DB: " + e.getMessage());
        }
    }

    private void onEdit(Prime prime) {
        if (prime == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier une prime");
        dialog.setHeaderText("ID: " + prime.getId());

        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType save = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cancel, save);

        TextField tfEmp = new TextField(String.valueOf(prime.getEmployeId()));
        TextField tfMont = new TextField(prime.getMontant() == null ? "" : prime.getMontant().toString());
        DatePicker dp = new DatePicker(prime.getDateAttribution());
        TextArea ta = new TextArea(prime.getDescription() == null ? "" : prime.getDescription());
        ta.setWrapText(true);
        ta.setPrefRowCount(4);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.addRow(0, new Label("Employé ID"), tfEmp);
        gp.addRow(1, new Label("Montant"), tfMont);
        gp.addRow(2, new Label("Date"), dp);
        gp.addRow(3, new Label("Description"), ta);
        dialog.getDialogPane().setContent(gp);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != save) return;

        Long newEmp = parseLong(tfEmp.getText(), "Employé ID");
        BigDecimal newMont = parseBigDecimal(tfMont.getText(), "Montant");
        LocalDate newDate = dp.getValue();
        String newDesc = ta.getText() == null ? "" : ta.getText().trim();

        if (newEmp == null || newMont == null) return;
        if (newMont.signum() <= 0) { setMsgError("Le montant doit être positif."); return; }
        if (newDate == null) { setMsgError("Choisis la date."); return; }

        if (!confirmAction("Confirmation", "Enregistrer les modifications ?",
                "Employé: " + newEmp + "\nMontant: " + newMont + "\nDate: " + newDate)) return;

        try {
            prime.setEmployeId(newEmp);
            prime.setMontant(newMont);
            prime.setDateAttribution(newDate);
            prime.setDescription(newDesc);

            // must exist in your service
            primeService.updateEntity(prime);

            setMsgSuccess("Prime modifiée ✅");
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
            setMsgError("Erreur DB: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            List<Prime> list = primeService.getData();
            masterData.setAll(list);
            applyFilters();
        } catch (SQLException e) {
            e.printStackTrace();
            setMsgError("Erreur DB: " + e.getMessage());
        }
    }

    private void applyFilters() {
        filteredData.setPredicate(p -> {
            if (p == null) return false;

            // employe filter
            String empText = tfFilterEmploye.getText() == null ? "" : tfFilterEmploye.getText().trim();
            if (!empText.isEmpty()) {
                try {
                    long emp = Long.parseLong(empText);
                    if (p.getEmployeId() != emp) return false;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }

            // date range
            LocalDate from = dpFilterFrom.getValue();
            LocalDate to = dpFilterTo.getValue();
            LocalDate d = p.getDateAttribution();
            if (from != null && (d == null || d.isBefore(from))) return false;
            if (to != null && (d == null || d.isAfter(to))) return false;

            return true;
        });

        updateSummary();
    }

    private void updateSummary() {
        int total = filteredData.size();
        lblPrimeTotalCount.setText(String.valueOf(total));

        LocalDate now = LocalDate.now();
        int m = now.getMonthValue();
        int y = now.getYear();

        int thisMonthCount = 0;
        BigDecimal thisMonthAmount = BigDecimal.ZERO;

        for (Prime p : filteredData) {
            if (p == null || p.getDateAttribution() == null) continue;
            if (p.getDateAttribution().getMonthValue() == m && p.getDateAttribution().getYear() == y) {
                thisMonthCount++;
                if (p.getMontant() != null) thisMonthAmount = thisMonthAmount.add(p.getMontant());
            }
        }

        lblPrimeThisMonthCount.setText(String.valueOf(thisMonthCount));
        lblPrimeThisMonthAmount.setText(MONEY_FMT.format(thisMonthAmount) + " $");
    }

    // ------- helpers -------
    private void clearMsg() {
        msgLabel.setStyle("-fx-text-fill:#6b7280; -fx-font-weight:900;");
        msgLabel.setText("");
    }
    private void setMsgInfo(String msg) {
        msgLabel.setStyle("-fx-text-fill:#6b7280; -fx-font-weight:900;");
        msgLabel.setText(msg);
    }
    private void setMsgSuccess(String msg) {
        msgLabel.setStyle("-fx-text-fill:#047857; -fx-font-weight:900;");
        msgLabel.setText(msg);
    }
    private void setMsgError(String msg) {
        msgLabel.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
        msgLabel.setText(msg);
    }

    private boolean confirmAction(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType ok = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, ok);

        Optional<ButtonType> res = alert.showAndWait();
        return res.isPresent() && res.get() == ok;
    }

    private Long parseLong(String value, String fieldName) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) { setMsgError(fieldName + " est obligatoire."); return null; }
        try {
            long n = Long.parseLong(v);
            if (n <= 0) { setMsgError(fieldName + " invalide."); return null; }
            return n;
        } catch (NumberFormatException e) {
            setMsgError("Vérifie " + fieldName + " (nombre).");
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value, String fieldName) {
        String v = value == null ? "" : value.trim().replace(",", ".");
        if (v.isEmpty()) { setMsgError(fieldName + " est obligatoire."); return null; }
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            setMsgError("Vérifie " + fieldName + " (format).");
            return null;
        }
    }
}
