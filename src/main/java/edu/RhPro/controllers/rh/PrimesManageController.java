package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Prime;
import edu.RhPro.entities.Tache;
import edu.RhPro.entities.User;
import edu.RhPro.services.PrimeService;
import edu.RhPro.services.TacheService;
import edu.RhPro.utils.CurrencyContext;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrimesManageController {

    // Form
    @FXML private TextField tfEmployeId;
    @FXML private TextField tfMontant;       // ✅ user enters DISPLAY currency amount
    @FXML private TextArea taDescription;
    @FXML private Label msgLabel;

    // Header badges
    @FXML private Label lblCurrencyBadge;
    @FXML private Label lblAmountPreview;

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
    @FXML private TableColumn<Prime, BigDecimal> colMontant; // stored TND in DB
    @FXML private TableColumn<Prime, LocalDate> colDate;
    @FXML private TableColumn<Prime, String> colDesc;

    private final PrimeService primeService = new PrimeService();
    private final TacheService tacheService = new TacheService();

    private final ObservableList<Prime> masterData = FXCollections.observableArrayList();
    private FilteredList<Prime> filteredData;

    // picked tasks
    private List<Tache> pendingPickedTaches = new ArrayList<>();

    @FXML
    public void initialize() {

        // Load currency once
        CurrencyContext.ensureLoaded();
        lblCurrencyBadge.setText("Devise: " + CurrencyContext.getDisplayCurrency());

        // amount preview under input
        lblAmountPreview.setText("Affichage: —");

        // live preview when user types amount
        tfMontant.textProperty().addListener((obs, oldV, newV) -> updateAmountPreview());

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeId"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant")); // TND stored
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateAttribution"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // ✅ show ONLY display currency in table (no TND / no base text)
        colMontant.setCellFactory(c -> new TableCell<Prime, BigDecimal>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(CurrencyContext.formatDisplayOnly(item)); // convert from TND -> display
            }
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Aucune prime."));

        // double click => edit
        table.setRowFactory(tv -> {
            TableRow<Prime> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit(row.getItem());
                }
            });
            return row;
        });

        // filtering
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Prime> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        tfFilterEmploye.textProperty().addListener((obs, o, n) -> applyFilters());
        dpFilterFrom.valueProperty().addListener((obs, o, n) -> applyFilters());
        dpFilterTo.valueProperty().addListener((obs, o, n) -> applyFilters());

        loadData();
    }

    private void updateAmountPreview() {
        String v = tfMontant.getText() == null ? "" : tfMontant.getText().trim().replace(",", ".");
        if (v.isEmpty()) {
            lblAmountPreview.setText("Affichage: —");
            return;
        }
        try {
            BigDecimal displayAmount = new BigDecimal(v);
            if (displayAmount.signum() <= 0) {
                lblAmountPreview.setText("Affichage: —");
                return;
            }
            BigDecimal tnd = CurrencyContext.convertDisplayToTnd(displayAmount);
            // ✅ only here show base info
            lblAmountPreview.setText("Affichage: " + CurrencyContext.getDisplayCurrency()
                    + " → Base (TND): " + tnd.toPlainString());
        } catch (Exception e) {
            lblAmountPreview.setText("Affichage: —");
        }
    }

    @FXML
    public void refresh() { loadData(); }

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

        // ✅ user typed display amount (CAD/EUR/...)
        BigDecimal amountDisplay = parseBigDecimal(tfMontant.getText(), "Montant");
        if (employeId == null || amountDisplay == null) return;
        if (amountDisplay.signum() <= 0) { setMsgError("Le montant doit être positif."); return; }

        // ✅ convert silently to TND for DB
        BigDecimal montantTnd = CurrencyContext.convertDisplayToTnd(amountDisplay);

        String desc = taDescription.getText() == null ? "" : taDescription.getText().trim();
        LocalDate date = LocalDate.now();

        String details =
                "Employé: " + employeId +
                        "\nMontant: " + amountDisplay.toPlainString() + " " + CurrencyContext.getDisplayCurrency() +
                        "\nDate: " + date +
                        ((pendingPickedTaches != null && !pendingPickedTaches.isEmpty())
                                ? "\nTâches sélectionnées: " + pendingPickedTaches.size()
                                : "");

        if (!confirmAction("Confirmation", "Ajouter une prime ?", details)) return;

        try {
            long rhId = Session.getCurrentUser().getId();

            // store TND
            Prime p = new Prime(montantTnd, date, desc, rhId, employeId);

            long primeId = primeService.addEntityAndReturnId(p);

            // link tasks -> prime_id
            if (pendingPickedTaches != null && !pendingPickedTaches.isEmpty()) {
                List<Integer> ids = new ArrayList<>();
                for (Tache t : pendingPickedTaches) {
                    if (t != null) ids.add(t.getId());
                }
                tacheService.assignPrimeToTaches((int) primeId, ids);
            }

            tfEmployeId.clear();
            tfMontant.clear();
            taDescription.clear();
            pendingPickedTaches.clear();
            lblAmountPreview.setText("Affichage: —");

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
                "ID: " + selected.getId() +
                        "\nEmployé: " + selected.getEmployeId())) return;

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

        // ✅ show DISPLAY amount to user (convert from TND)
        BigDecimal displayAmount = CurrencyContext.convertFromTnd(prime.getMontant());
        TextField tfMont = new TextField(displayAmount == null ? "" : displayAmount.toPlainString());

        TextArea ta = new TextArea(prime.getDescription() == null ? "" : prime.getDescription());
        ta.setWrapText(true);
        ta.setPrefRowCount(4);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.addRow(0, new Label("Employé ID"), tfEmp);
        gp.addRow(1, new Label("Montant (" + CurrencyContext.getDisplayCurrency() + ")"), tfMont);
        gp.addRow(2, new Label("Description"), ta);
        dialog.getDialogPane().setContent(gp);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != save) return;

        Long newEmp = parseLong(tfEmp.getText(), "Employé ID");
        BigDecimal newDisplay = parseBigDecimal(tfMont.getText(), "Montant");
        if (newEmp == null || newDisplay == null) return;
        if (newDisplay.signum() <= 0) { setMsgError("Le montant doit être positif."); return; }

        // convert silently to TND
        BigDecimal newMontTnd = CurrencyContext.convertDisplayToTnd(newDisplay);

        String newDesc = ta.getText() == null ? "" : ta.getText().trim();

        if (!confirmAction("Confirmation", "Enregistrer les modifications ?",
                "Employé: " + newEmp +
                        "\nMontant: " + newDisplay.toPlainString() + " " + CurrencyContext.getDisplayCurrency())) return;

        try {
            prime.setEmployeId(newEmp);
            prime.setMontant(newMontTnd); // store TND
            prime.setDescription(newDesc);

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

            String empText = tfFilterEmploye.getText() == null ? "" : tfFilterEmploye.getText().trim();
            if (!empText.isEmpty()) {
                try {
                    long emp = Long.parseLong(empText);
                    if (p.getEmployeId() != emp) return false;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }

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
        BigDecimal thisMonthAmountTnd = BigDecimal.ZERO;

        for (Prime p : filteredData) {
            if (p == null || p.getDateAttribution() == null) continue;
            if (p.getDateAttribution().getMonthValue() == m && p.getDateAttribution().getYear() == y) {
                thisMonthCount++;
                if (p.getMontant() != null) thisMonthAmountTnd = thisMonthAmountTnd.add(p.getMontant());
            }
        }

        lblPrimeThisMonthCount.setText(String.valueOf(thisMonthCount));
        lblPrimeThisMonthAmount.setText(CurrencyContext.formatDisplayOnly(thisMonthAmountTnd));
    }

    // EMPLOYEE SEARCH POPUP
    @FXML
    public void onSearchEmploye() {
        clearMsg();
        try {
            var url = getClass().getResource("/rh/EmployeSearch.fxml");
            if (url == null) { setMsgError("EmployeSearch.fxml introuvable."); return; }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            EmployeSearchController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Recherche employé");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));

            ctrl.setOnUserSelected((User u) -> {
                if (u != null) {
                    tfEmployeId.setText(String.valueOf(u.getId()));
                    setMsgInfo("Employé sélectionné: " + safe(u.getPrenom()) + " " + safe(u.getNom()) + " (ID " + u.getId() + ")");
                }
                stage.close();
            });

            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            setMsgError("Impossible d'ouvrir la recherche employé.");
        }
    }

    // TASK PICKER POPUP
    @FXML
    public void onPickTaches() {
        clearMsg();

        Long emp = parseLong(tfEmployeId.getText(), "Employé ID");
        if (emp == null) return;

        try {
            var url = getClass().getResource("/rh/TachePrimePicker.fxml");
            if (url == null) { setMsgError("TachePrimePicker.fxml introuvable."); return; }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            TachePrimePickerController ctrl = loader.getController();

            Stage st = new Stage();
            st.setTitle("Choisir tâches (prime)");
            st.initModality(Modality.APPLICATION_MODAL);
            st.setResizable(false);
            st.setScene(new Scene(root));

            ctrl.setStage(st);
            ctrl.initForEmploye(emp.intValue()); // DONE + prime_id null

            ctrl.setOnPicked((totalFromPicker, description, selectedTaches) -> {
                // ✅ IMPORTANT: totalFromPicker is already the amount you want to put in tfMontant
                // Do NOT convertFromTnd here.
                if (totalFromPicker != null) tfMontant.setText(totalFromPicker.toPlainString());
                else tfMontant.setText("");

                taDescription.setText(description == null ? "" : description);

                pendingPickedTaches = (selectedTaches == null) ? new ArrayList<>() : selectedTaches;
                setMsgInfo("Tâches sélectionnées: " + pendingPickedTaches.size());

                updateAmountPreview();
            });

            st.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            setMsgError("Impossible d'ouvrir le picker de tâches.");
        }
    }

    // helpers
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

    private String safe(String s) { return s == null ? "" : s; }
}