package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Salaire;
import edu.RhPro.services.SalaireService;
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

public class SalairesManageController {

    // Form
    @FXML private TextField tfEmployeId;
    @FXML private ComboBox<Integer> cbMois;
    @FXML private TextField tfAnnee;
    @FXML private TextField tfMontant;
    @FXML private DatePicker dpPaiement;
    @FXML private ComboBox<String> cbStatut;
    @FXML private Label msgLabel;

    // Summary
    @FXML private Label lblSalEnAttente;
    @FXML private Label lblSalPaye;
    @FXML private Label lblSalPaidThisMonthAmount;

    // Filters
    @FXML private TextField tfFilterEmploye;
    @FXML private ComboBox<Integer> cbFilterMois;
    @FXML private TextField tfFilterAnnee;
    @FXML private ComboBox<String> cbFilterStatut;
    @FXML private DatePicker dpFilterFrom;
    @FXML private DatePicker dpFilterTo;

    // Table
    @FXML private TableView<Salaire> table;
    @FXML private TableColumn<Salaire, Long> colId;
    @FXML private TableColumn<Salaire, Long> colEmploye;
    @FXML private TableColumn<Salaire, Integer> colMois;
    @FXML private TableColumn<Salaire, Integer> colAnnee;
    @FXML private TableColumn<Salaire, BigDecimal> colMontant;
    @FXML private TableColumn<Salaire, LocalDate> colDate;
    @FXML private TableColumn<Salaire, String> colStatut;

    private final SalaireService salaireService = new SalaireService();

    private final ObservableList<Salaire> masterData = FXCollections.observableArrayList();
    private FilteredList<Salaire> filteredData;

    private static final NumberFormat MONEY_FMT;
    static {
        MONEY_FMT = NumberFormat.getNumberInstance(Locale.FRANCE);
        MONEY_FMT.setMinimumFractionDigits(2);
        MONEY_FMT.setMaximumFractionDigits(2);
    }

    @FXML
    public void initialize() {
        cbMois.setItems(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));

        // Status workflow
        cbStatut.setItems(FXCollections.observableArrayList("EN_ATTENTE", "VALIDE", "PAYE", "ANNULE"));
        if (cbStatut.getValue() == null) cbStatut.setValue("EN_ATTENTE");

        // Filters
        cbFilterMois.setItems(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));
        cbFilterStatut.setItems(FXCollections.observableArrayList("", "EN_ATTENTE", "VALIDE", "PAYE", "ANNULE"));
        cbFilterStatut.setValue("");

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeId"));
        colMois.setCellValueFactory(new PropertyValueFactory<>("mois"));
        colAnnee.setCellValueFactory(new PropertyValueFactory<>("annee"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("datePaiement"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Pretty amount
        colMontant.setCellFactory(c -> new TableCell<Salaire, BigDecimal>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : MONEY_FMT.format(item) + " $");
            }
        });

        // Double click => edit
        table.setRowFactory(tv -> {
            TableRow<Salaire> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit(row.getItem());
                }
            });
            return row;
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Aucun salaire."));

        // Filter pipeline
        filteredData = new FilteredList<>(masterData, s -> true);
        SortedList<Salaire> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        // Listeners
        tfFilterEmploye.textProperty().addListener((o,a,b)->applyFilters());
        cbFilterMois.valueProperty().addListener((o,a,b)->applyFilters());
        tfFilterAnnee.textProperty().addListener((o,a,b)->applyFilters());
        cbFilterStatut.valueProperty().addListener((o,a,b)->applyFilters());
        dpFilterFrom.valueProperty().addListener((o,a,b)->applyFilters());
        dpFilterTo.valueProperty().addListener((o,a,b)->applyFilters());

        // UX: if payment date chosen, auto set PAYE
        dpPaiement.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) cbStatut.setValue("PAYE");
        });

        loadData();
    }

    @FXML
    public void refresh() { loadData(); }

    @FXML
    public void clearFilters() {
        tfFilterEmploye.clear();
        cbFilterMois.setValue(null);
        tfFilterAnnee.clear();
        cbFilterStatut.setValue("");
        dpFilterFrom.setValue(null);
        dpFilterTo.setValue(null);
        applyFilters();
        setMsgInfo("Filtres effacés.");
    }

    @FXML
    public void onAdd() {
        clearMsg();

        if (Session.getCurrentUser() == null) { setMsgError("Session expirée."); return; }

        Long employeId = parseLong(tfEmployeId.getText(), "Employé ID");
        Integer mois = cbMois.getValue();
        Integer annee = parseInt(tfAnnee.getText(), "Année");
        BigDecimal montant = parseBigDecimal(tfMontant.getText(), "Montant");
        LocalDate paiement = dpPaiement.getValue();
        String statut = cbStatut.getValue();

        if (employeId == null || annee == null || montant == null) return;
        if (mois == null) { setMsgError("Choisis le mois."); return; }
        if (annee < 1900 || annee > 2100) { setMsgError("Année invalide."); return; }
        if (montant.signum() <= 0) { setMsgError("Le montant doit être positif."); return; }

        // If payment date set -> PAYE
        if (paiement != null) statut = "PAYE";
        if (statut == null || statut.isBlank()) statut = "EN_ATTENTE";

        // Duplicate rule: employeId + mois + annee
        Salaire existing = findDuplicate(employeId, mois, annee);
        if (existing != null) {
            boolean updateInstead = confirmAction(
                    "Doublon détecté",
                    "Salaire existe déjà pour cet employé (mois/année).",
                    "Voulez-vous METTRE À JOUR l'enregistrement existant ?\n" +
                            "ID: " + existing.getId() + "\nAncien montant: " + existing.getMontant()
            );
            if (!updateInstead) {
                setMsgError("Ajout annulé (doublon).");
                return;
            }

            try {
                existing.setMontant(montant);
                existing.setDatePaiement(paiement);
                existing.setStatut(statut);
                salaireService.updateEntity(existing);

                setMsgSuccess("Salaire mis à jour ✅");
                loadData();
                clearForm();
                return;
            } catch (SQLException e) {
                e.printStackTrace();
                setMsgError("Erreur DB: " + e.getMessage());
                return;
            }
        }

        if (!confirmAction("Confirmation", "Ajouter un salaire ?",
                "Employé: " + employeId +
                        "\nPériode: " + mois + "/" + annee +
                        "\nMontant: " + montant +
                        "\nStatut: " + statut +
                        (paiement != null ? "\nPaiement: " + paiement : ""))) return;

        try {
            long rhId = Session.getCurrentUser().getId();
            Salaire s = new Salaire(mois, annee, montant, paiement, statut, rhId, employeId);
            salaireService.addEntity(s);

            setMsgSuccess("Salaire ajouté ✅");
            loadData();
            clearForm();

        } catch (SQLException e) {
            e.printStackTrace();
            setMsgError("Erreur DB: " + e.getMessage());
        }
    }

    @FXML
    public void onDelete() {
        clearMsg();
        Salaire selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { setMsgError("Sélectionne un salaire."); return; }

        if (!confirmAction("Confirmation", "Supprimer ce salaire ?",
                "ID: " + selected.getId() +
                        "\nEmployé: " + selected.getEmployeId() +
                        "\nPériode: " + selected.getMois() + "/" + selected.getAnnee() +
                        "\nMontant: " + selected.getMontant())) return;

        try {
            salaireService.deleteEntity(selected);
            setMsgSuccess("Supprimé ✅");
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
            setMsgError("Erreur DB: " + e.getMessage());
        }
    }

    // Workflow buttons
    @FXML
    public void onValidateSelected() {
        updateSelectedStatus("VALIDE", false);
    }

    @FXML
    public void onMarkPaidSelected() {
        updateSelectedStatus("PAYE", true);
    }

    @FXML
    public void onCancelSelected() {
        updateSelectedStatus("ANNULE", false);
    }

    private void updateSelectedStatus(String newStatus, boolean setPaymentDateToday) {
        clearMsg();
        Salaire selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { setMsgError("Sélectionne un salaire."); return; }

        String content = "ID: " + selected.getId() +
                "\nEmployé: " + selected.getEmployeId() +
                "\nPériode: " + selected.getMois() + "/" + selected.getAnnee() +
                "\nNouveau statut: " + newStatus +
                (setPaymentDateToday ? "\nDate paiement: Aujourd'hui" : "");

        if (!confirmAction("Confirmation", "Appliquer le workflow ?", content)) return;

        try {
            selected.setStatut(newStatus);
            if (setPaymentDateToday) selected.setDatePaiement(LocalDate.now());
            salaireService.updateEntity(selected);

            setMsgSuccess("Statut mis à jour ✅");
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
            setMsgError("Erreur DB: " + e.getMessage());
        }
    }

    private void onEdit(Salaire s) {
        if (s == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier un salaire");
        dialog.setHeaderText("ID: " + s.getId() + " | Employé: " + s.getEmployeId());

        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType save = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cancel, save);

        TextField tfEmp = new TextField(String.valueOf(s.getEmployeId()));
        ComboBox<Integer> cbM = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));
        cbM.setValue(s.getMois());
        TextField tfY = new TextField(String.valueOf(s.getAnnee()));
        TextField tfMont = new TextField(s.getMontant() == null ? "" : s.getMontant().toString());
        DatePicker dp = new DatePicker(s.getDatePaiement());
        ComboBox<String> cbS = new ComboBox<>(FXCollections.observableArrayList("EN_ATTENTE", "VALIDE", "PAYE", "ANNULE"));
        cbS.setValue(s.getStatut());

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.addRow(0, new Label("Employé ID"), tfEmp);
        gp.addRow(1, new Label("Mois"), cbM);
        gp.addRow(2, new Label("Année"), tfY);
        gp.addRow(3, new Label("Montant"), tfMont);
        gp.addRow(4, new Label("Paiement"), dp);
        gp.addRow(5, new Label("Statut"), cbS);
        dialog.getDialogPane().setContent(gp);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != save) return;

        Long newEmp = parseLong(tfEmp.getText(), "Employé ID");
        Integer newMois = cbM.getValue();
        Integer newYear = parseInt(tfY.getText(), "Année");
        BigDecimal newMont = parseBigDecimal(tfMont.getText(), "Montant");
        LocalDate newPay = dp.getValue();
        String newStat = cbS.getValue();

        if (newEmp == null || newYear == null || newMont == null) return;
        if (newMois == null) { setMsgError("Choisis le mois."); return; }
        if (newMont.signum() <= 0) { setMsgError("Montant invalide."); return; }
        if (newPay != null) newStat = "PAYE";
        if (newStat == null || newStat.isBlank()) newStat = "EN_ATTENTE";

        // if key changed, check duplicates
        Salaire dup = findDuplicate(newEmp, newMois, newYear);
        if (dup != null && dup.getId() != s.getId()) {
            setMsgError("Doublon: salaire existe déjà pour ce mois/année.");
            return;
        }

        if (!confirmAction("Confirmation", "Enregistrer les modifications ?",
                "Employé: " + newEmp + "\nPériode: " + newMois + "/" + newYear +
                        "\nMontant: " + newMont + "\nStatut: " + newStat)) return;

        try {
            s.setEmployeId(newEmp);
            s.setMois(newMois);
            s.setAnnee(newYear);
            s.setMontant(newMont);
            s.setDatePaiement(newPay);
            s.setStatut(newStat);

            salaireService.updateEntity(s);

            setMsgSuccess("Salaire modifié ✅");
            loadData();

        } catch (SQLException e) {
            e.printStackTrace();
            setMsgError("Erreur DB: " + e.getMessage());
        }
    }

    private Salaire findDuplicate(long employeId, int mois, int annee) {
        for (Salaire s : masterData) {
            if (s == null) continue;
            if (s.getEmployeId() == employeId && s.getMois() == mois && s.getAnnee() == annee) {
                return s;
            }
        }
        return null;
    }

    private void clearForm() {
        tfEmployeId.clear();
        cbMois.setValue(null);
        tfAnnee.clear();
        tfMontant.clear();
        dpPaiement.setValue(null);
        cbStatut.setValue("EN_ATTENTE");
    }

    private void loadData() {
        try {
            List<Salaire> list = salaireService.getData();
            masterData.setAll(list);
            applyFilters();
        } catch (SQLException e) {
            e.printStackTrace();
            setMsgError("Erreur DB: " + e.getMessage());
        }
    }

    private void applyFilters() {
        filteredData.setPredicate(s -> {
            if (s == null) return false;

            // Employe ID
            String empText = tfFilterEmploye.getText() == null ? "" : tfFilterEmploye.getText().trim();
            if (!empText.isEmpty()) {
                try {
                    long emp = Long.parseLong(empText);
                    if (s.getEmployeId() != emp) return false;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }

            // Mois
            Integer fm = cbFilterMois.getValue();
            if (fm != null && s.getMois() != fm) return false;

            // Année
            String yText = tfFilterAnnee.getText() == null ? "" : tfFilterAnnee.getText().trim();
            if (!yText.isEmpty()) {
                try {
                    int yy = Integer.parseInt(yText);
                    if (s.getAnnee() != yy) return false;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }

            // Statut
            String fs = cbFilterStatut.getValue();
            if (fs != null && !fs.isBlank()) {
                if (s.getStatut() == null || !s.getStatut().equals(fs)) return false;
            }

            // Date range on datePaiement (if null, it won't match when range set)
            LocalDate from = dpFilterFrom.getValue();
            LocalDate to = dpFilterTo.getValue();
            LocalDate d = s.getDatePaiement();
            if (from != null && (d == null || d.isBefore(from))) return false;
            if (to != null && (d == null || d.isAfter(to))) return false;

            return true;
        });

        updateSummary();
    }

    private void updateSummary() {
        int enAttente = 0;
        int paye = 0;
        BigDecimal paidThisMonth = BigDecimal.ZERO;

        LocalDate now = LocalDate.now();
        int m = now.getMonthValue();
        int y = now.getYear();

        for (Salaire s : filteredData) {
            if (s == null) continue;
            String st = s.getStatut() == null ? "" : s.getStatut();

            if (st.equals("EN_ATTENTE")) enAttente++;
            if (st.equals("PAYE")) paye++;

            if (st.equals("PAYE") && s.getDatePaiement() != null &&
                    s.getDatePaiement().getMonthValue() == m &&
                    s.getDatePaiement().getYear() == y &&
                    s.getMontant() != null) {
                paidThisMonth = paidThisMonth.add(s.getMontant());
            }
        }

        lblSalEnAttente.setText(String.valueOf(enAttente));
        lblSalPaye.setText(String.valueOf(paye));
        lblSalPaidThisMonthAmount.setText(MONEY_FMT.format(paidThisMonth) + " $");
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

    private Integer parseInt(String value, String fieldName) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) { setMsgError(fieldName + " est obligatoire."); return null; }
        try {
            return Integer.parseInt(v);
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
