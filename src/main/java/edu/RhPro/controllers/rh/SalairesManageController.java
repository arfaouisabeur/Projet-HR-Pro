package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Salaire;
import edu.RhPro.entities.User;
import edu.RhPro.services.EmailServiceV2;
import edu.RhPro.services.SalaireService;
import edu.RhPro.services.UserService;
import edu.RhPro.utils.CurrencyContext;
import edu.RhPro.utils.Session;
import javafx.application.Platform;
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
import java.util.List;
import java.util.Optional;

public class SalairesManageController {

    // Form
    @FXML private TextField tfEmployeId;
    @FXML private ComboBox<Integer> cbMois;
    @FXML private TextField tfAnnee;
    @FXML private TextField tfMontant;      // ✅ USER types DISPLAY currency
    @FXML private Label lblAmountPreview;  // ✅ shows base TND preview (ONLY HERE)
    @FXML private DatePicker dpPaiement;
    @FXML private ComboBox<String> cbStatut;

    // Header badges
    @FXML private Label lblCurrencyBadge; // ✅ "Devise: CAD"
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
    @FXML private TableColumn<Salaire, BigDecimal> colMontant; // stored TND in DB
    @FXML private TableColumn<Salaire, LocalDate> colDate;
    @FXML private TableColumn<Salaire, String> colStatut;

    private final SalaireService salaireService = new SalaireService();

    private final ObservableList<Salaire> masterData = FXCollections.observableArrayList();
    private FilteredList<Salaire> filteredData;

    @FXML
    public void initialize() {
        System.out.println(new EmailServiceV2().debugConfiguration());

        // ✅ load currency once
        CurrencyContext.ensureLoaded();

        if (lblCurrencyBadge != null) {
            lblCurrencyBadge.setText("Devise: " + CurrencyContext.getDisplayCurrency());
        }
        if (lblAmountPreview != null) {
            lblAmountPreview.setText("Affichage: —");
        }

        // months
        cbMois.setItems(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));

        // Status workflow
        cbStatut.setItems(FXCollections.observableArrayList("EN_ATTENTE", "VALIDE", "PAYE", "ANNULE"));
        if (cbStatut.getValue() == null) cbStatut.setValue("EN_ATTENTE");

        // Filters
        cbFilterMois.setItems(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));
        cbFilterStatut.setItems(FXCollections.observableArrayList("", "EN_ATTENTE", "VALIDE", "PAYE", "ANNULE"));
        cbFilterStatut.setValue("");

        // columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeId"));
        colMois.setCellValueFactory(new PropertyValueFactory<>("mois"));
        colAnnee.setCellValueFactory(new PropertyValueFactory<>("annee"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant")); // ✅ stored TND
        colDate.setCellValueFactory(new PropertyValueFactory<>("datePaiement"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // ✅ Pretty amount: show ONLY display currency (no base / no TND)
        colMontant.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(CurrencyContext.formatDisplayOnly(item)); // converts from TND
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

        // filter listeners
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

        // ✅ LIVE preview under amount (ONLY HERE): show base TND in small line
        if (tfMontant != null) {
            tfMontant.textProperty().addListener((obs, oldV, newV) -> updateAmountPreview());
        }
        Platform.runLater(this::updateAmountPreview);

        loadData();
    }

    private void updateAmountPreview() {
        if (lblAmountPreview == null || tfMontant == null) return;

        String raw = tfMontant.getText() == null ? "" : tfMontant.getText().trim();
        if (raw.isEmpty()) {
            lblAmountPreview.setText("Affichage: —");
            return;
        }

        BigDecimal displayAmount = parseBigDecimalSilent(raw);
        if (displayAmount == null || displayAmount.signum() <= 0) {
            lblAmountPreview.setText("Affichage: —");
            return;
        }

        // user typed DISPLAY -> convert to TND (base stored)
        BigDecimal baseTnd = CurrencyContext.convertDisplayToTnd(displayAmount);

        // ✅ user sees only display amount + base line here (ONLY HERE)
        lblAmountPreview.setText(
                "Affichage: " + displayAmount.toPlainString() + " " + CurrencyContext.getDisplayCurrency()
                        + "  •  Base: " + baseTnd.toPlainString() + " TND"
        );
    }

    @FXML
    public void onSearchEmploye() {
        clearMsg();
        try {
            var url = getClass().getResource("/rh/EmployeSearch.fxml");
            if (url == null) {
                setMsgError("EmployeSearch.fxml introuvable (chemin: /rh/EmployeSearch.fxml).");
                return;
            }

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

        // ✅ user typed DISPLAY currency -> convert to TND for DB
        BigDecimal montantDisplay = parseBigDecimal(tfMontant.getText(), "Montant");
        if (montantDisplay == null) return;
        BigDecimal montantTnd = CurrencyContext.convertDisplayToTnd(montantDisplay);

        LocalDate paiement = dpPaiement.getValue();
        String statut = cbStatut.getValue();

        if (employeId == null || annee == null) return;
        if (mois == null) { setMsgError("Choisis le mois."); return; }
        if (annee < 1900 || annee > 2100) { setMsgError("Année invalide."); return; }
        if (montantDisplay.signum() <= 0) { setMsgError("Le montant doit être positif."); return; }

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
                            "ID: " + existing.getId() +
                            "\nAncien montant: " + CurrencyContext.formatDisplayOnly(existing.getMontant())
            );
            if (!updateInstead) {
                setMsgError("Ajout annulé (doublon).");
                return;
            }

            try {
                existing.setMontant(montantTnd); // ✅ store TND
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
                        "\nMontant: " + montantDisplay + " " + CurrencyContext.getDisplayCurrency() +
                        "\nStatut: " + statut +
                        (paiement != null ? "\nPaiement: " + paiement : ""))) return;

        try {
            long rhId = Session.getCurrentUser().getId();
            Salaire s = new Salaire(mois, annee, montantTnd, paiement, statut, rhId, employeId); // ✅ store TND
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
                        "\nMontant: " + CurrencyContext.formatDisplayOnly(selected.getMontant()))) return;

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
    @FXML public void onValidateSelected() { updateSelectedStatus("VALIDE", false); }
    @FXML public void onMarkPaidSelected() { updateSelectedStatus("PAYE", true); }
    @FXML public void onCancelSelected() { updateSelectedStatus("ANNULE", false); }
    private final UserService userService = new UserService();


    private void updateSelectedStatus(String newStatus, boolean setPaymentDateToday) {
        clearMsg();
        Salaire selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { setMsgError("Sélectionne un salaire."); return; }

        String content =
                "ID: " + selected.getId() +
                        "\nEmployé: " + selected.getEmployeId() +
                        "\nPériode: " + selected.getMois() + "/" + selected.getAnnee() +
                        "\nNouveau statut: " + newStatus +
                        (setPaymentDateToday ? "\nDate paiement: Aujourd'hui" : "");

        if (!confirmAction("Confirmation", "Appliquer le workflow ?", content)) return;

        try {
            selected.setStatut(newStatus);
            if (setPaymentDateToday) selected.setDatePaiement(LocalDate.now());
            if ("PAYE".equals(newStatus) && selected.getDatePaiement() == null) {
                selected.setDatePaiement(LocalDate.now());
            }

            salaireService.updateEntity(selected);

            // If not PAYE, finish here
            if (!"PAYE".equals(newStatus)) {
                setMsgSuccess("Statut mis à jour ✅");
                loadData();
                return;
            }

            // PAYE => send email async (no UI freeze)
            setMsgInfo("PAYE ✅ enregistré. Envoi email en cours...");

            new Thread(() -> {
                try {
                    String to = userService.findEmailById(selected.getEmployeId());

                    if (to == null || to.isBlank()) {
                        Platform.runLater(() ->
                                setMsgInfo("PAYE ✅ mais aucun email trouvé pour l'employé.")
                        );
                        Platform.runLater(this::loadData);
                        return;
                    }

                    // ---- Build salary email with tax/net ----
                    // Gross is display currency shown to employee
                    BigDecimal grossDisplay = CurrencyContext.convertFromTnd(selected.getMontant());
                    if (grossDisplay == null) grossDisplay = BigDecimal.ZERO;

                    String continent = getContinentSafe(); // hook below
                    BigDecimal taxRate = edu.RhPro.utils.TaxPolicy.taxRateForContinent(continent); // ex 0.20
                    BigDecimal taxAmount = grossDisplay.multiply(taxRate);
                    BigDecimal netDisplay = grossDisplay.subtract(taxAmount);

                    String subject = String.format(
                            "RHPro Payroll - Salary Payment Confirmation %02d/%d",
                            selected.getMois(),
                            selected.getAnnee()
                    );
                    String html = buildSalaryPaidHtml(
                            selected.getEmployeId(),
                            selected.getMois(),
                            selected.getAnnee(),
                            grossDisplay,
                            taxRate,
                            taxAmount,
                            netDisplay,
                            selected.getDatePaiement(),
                            continent
                    );

                    EmailServiceV2 emailService = new EmailServiceV2();

                    // Print diagnostics always (helps you know problem)
                    System.out.println(emailService.debugConfiguration());

                    if (!emailService.isConfigured()) {
                        Platform.runLater(() ->
                                setMsgError("PAYE ✅ mais EmailService non configuré. Vérifie ENV dans Run Config.")
                        );
                        Platform.runLater(this::loadData);
                        return;
                    }

                    int status = emailService.sendSalaryPaidEmail(to, subject, html);

                    Platform.runLater(() ->
                            setMsgSuccess("PAYE ✅ + Email envoyé à " + to + " (SendGrid " + status + ")")
                    );
                    Platform.runLater(this::loadData);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() ->
                            setMsgError("PAYE ✅ mais email échoué: " + ex.getMessage())
                    );
                    Platform.runLater(this::loadData);
                }
            }).start();

        } catch (SQLException e) {
            e.printStackTrace();
            setMsgError("Erreur DB: " + e.getMessage());
        }
    }

    private String getContinentSafe() {
        // If you already have a continent in CurrencyContext or another util, use it here.
        // For now: try ENV override, else default AFRICA (safe)
        String env = System.getenv("RH_CONTINENT");
        if (env != null && !env.isBlank()) return env.trim();
        return "AFRICA";
    }

    private String buildSalaryPaidHtml(
            long employeId,
            int mois,
            int annee,
            BigDecimal grossDisplay,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal netDisplay,
            LocalDate paymentDate,
            String continent
    ) {

        String curr = CurrencyContext.getDisplayCurrency();
        String taxPct = taxRate.multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros()
                .toPlainString() + "%";

        String gross = grossDisplay.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        String taxA  = taxAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        String net   = netDisplay.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();

        return """
<!DOCTYPE html>
<html>
<body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:30px 0;">
    <tr>
      <td align="center">
        <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;padding:30px;">
          
          <!-- HEADER -->
          <tr>
            <td style="text-align:center;padding-bottom:20px;">
              <h1 style="margin:0;color:#1f2937;font-size:22px;">RHPro Payroll Notification</h1>
              <p style="margin:5px 0 0 0;color:#6b7280;font-size:14px;">
                Salary Payment Confirmation
              </p>
            </td>
          </tr>

          <!-- INTRO -->
          <tr>
            <td style="padding-bottom:20px;color:#374151;font-size:15px;">
              Dear Employee,<br><br>
              We confirm that your salary for the period below has been successfully processed.
            </td>
          </tr>

          <!-- DETAILS TABLE -->
          <tr>
            <td>
              <table width="100%%" cellpadding="10" cellspacing="0" style="border-collapse:collapse;font-size:14px;">
                <tr style="background:#f9fafb;">
                  <td style="border:1px solid #e5e7eb;"><strong>Employee ID</strong></td>
                  <td style="border:1px solid #e5e7eb;">%d</td>
                </tr>
                <tr>
                  <td style="border:1px solid #e5e7eb;"><strong>Period</strong></td>
                  <td style="border:1px solid #e5e7eb;">%02d / %d</td>
                </tr>
                <tr style="background:#f9fafb;">
                  <td style="border:1px solid #e5e7eb;"><strong>Gross Salary</strong></td>
                  <td style="border:1px solid #e5e7eb;">%s %s</td>
                </tr>
                <tr>
                  <td style="border:1px solid #e5e7eb;"><strong>Tax (%s)</strong></td>
                  <td style="border:1px solid #e5e7eb;">%s %s</td>
                </tr>
                <tr style="background:#f3f4f6;">
                  <td style="border:1px solid #e5e7eb;"><strong>Net Salary</strong></td>
                  <td style="border:1px solid #e5e7eb;"><strong>%s %s</strong></td>
                </tr>
                <tr>
                  <td style="border:1px solid #e5e7eb;"><strong>Payment Date</strong></td>
                  <td style="border:1px solid #e5e7eb;">%s</td>
                </tr>
              </table>
            </td>
          </tr>

          <!-- FOOTER -->
          <tr>
            <td style="padding-top:25px;color:#6b7280;font-size:12px;text-align:center;">
              This is an automated message from RHPro Payroll System.<br>
              For any questions regarding this payment, please contact HR support.<br><br>
              RHPro | Payroll & Human Resources System<br>
              Montreal, QC, Canada
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""".formatted(
                employeId,
                mois,
                annee,
                gross, curr,
                taxPct,
                taxA, curr,
                net, curr,
                paymentDate
        );
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

        // ✅ show DISPLAY currency in edit dialog
        BigDecimal currentDisplay = CurrencyContext.convertFromTnd(s.getMontant());
        TextField tfMont = new TextField(currentDisplay == null ? "" : currentDisplay.toPlainString());

        DatePicker dp = new DatePicker(s.getDatePaiement());
        ComboBox<String> cbS = new ComboBox<>(FXCollections.observableArrayList("EN_ATTENTE", "VALIDE", "PAYE", "ANNULE"));
        cbS.setValue(s.getStatut());

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.addRow(0, new Label("Employé ID"), tfEmp);
        gp.addRow(1, new Label("Mois"), cbM);
        gp.addRow(2, new Label("Année"), tfY);
        gp.addRow(3, new Label("Montant (" + CurrencyContext.getDisplayCurrency() + ")"), tfMont);
        gp.addRow(4, new Label("Paiement"), dp);
        gp.addRow(5, new Label("Statut"), cbS);
        dialog.getDialogPane().setContent(gp);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != save) return;

        Long newEmp = parseLong(tfEmp.getText(), "Employé ID");
        Integer newMois = cbM.getValue();
        Integer newYear = parseInt(tfY.getText(), "Année");

        // ✅ parse DISPLAY -> convert to TND
        BigDecimal newMontDisplay = parseBigDecimal(tfMont.getText(), "Montant");
        if (newMontDisplay == null) return;
        BigDecimal newMontTnd = CurrencyContext.convertDisplayToTnd(newMontDisplay);

        LocalDate newPay = dp.getValue();
        String newStat = cbS.getValue();

        if (newEmp == null || newYear == null) return;
        if (newMois == null) { setMsgError("Choisis le mois."); return; }
        if (newMontDisplay.signum() <= 0) { setMsgError("Montant invalide."); return; }
        if (newPay != null) newStat = "PAYE";
        if (newStat == null || newStat.isBlank()) newStat = "EN_ATTENTE";

        // duplicates
        Salaire dup = findDuplicate(newEmp, newMois, newYear);
        if (dup != null && dup.getId() != s.getId()) {
            setMsgError("Doublon: salaire existe déjà pour ce mois/année.");
            return;
        }

        if (!confirmAction("Confirmation", "Enregistrer les modifications ?",
                "Employé: " + newEmp +
                        "\nPériode: " + newMois + "/" + newYear +
                        "\nMontant: " + newMontDisplay + " " + CurrencyContext.getDisplayCurrency() +
                        "\nStatut: " + newStat)) return;

        try {
            s.setEmployeId(newEmp);
            s.setMois(newMois);
            s.setAnnee(newYear);
            s.setMontant(newMontTnd); // ✅ store TND
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
            if (s.getEmployeId() == employeId && s.getMois() == mois && s.getAnnee() == annee) return s;
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
        updateAmountPreview();
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

            String empText = tfFilterEmploye.getText() == null ? "" : tfFilterEmploye.getText().trim();
            if (!empText.isEmpty()) {
                try {
                    long emp = Long.parseLong(empText);
                    if (s.getEmployeId() != emp) return false;
                } catch (NumberFormatException ignored) { return false; }
            }

            Integer fm = cbFilterMois.getValue();
            if (fm != null && s.getMois() != fm) return false;

            String yText = tfFilterAnnee.getText() == null ? "" : tfFilterAnnee.getText().trim();
            if (!yText.isEmpty()) {
                try {
                    int yy = Integer.parseInt(yText);
                    if (s.getAnnee() != yy) return false;
                } catch (NumberFormatException ignored) { return false; }
            }

            String fs = cbFilterStatut.getValue();
            if (fs != null && !fs.isBlank()) {
                if (s.getStatut() == null || !s.getStatut().equals(fs)) return false;
            }

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
        BigDecimal paidThisMonthTnd = BigDecimal.ZERO;

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
                paidThisMonthTnd = paidThisMonthTnd.add(s.getMontant()); // ✅ sum TND
            }
        }

        lblSalEnAttente.setText(String.valueOf(enAttente));
        lblSalPaye.setText(String.valueOf(paye));
        lblSalPaidThisMonthAmount.setText(CurrencyContext.formatDisplayOnly(paidThisMonthTnd));
    }

    // helpers
    private void clearMsg() {
        if (msgLabel == null) return;
        msgLabel.setStyle("-fx-text-fill:#6b7280; -fx-font-weight:900;");
        msgLabel.setText("");
    }
    private void setMsgInfo(String msg) {
        if (msgLabel == null) return;
        msgLabel.setStyle("-fx-text-fill:#6b7280; -fx-font-weight:900;");
        msgLabel.setText(msg);
    }
    private void setMsgSuccess(String msg) {
        if (msgLabel == null) return;
        msgLabel.setStyle("-fx-text-fill:#047857; -fx-font-weight:900;");
        msgLabel.setText(msg);
    }
    private void setMsgError(String msg) {
        if (msgLabel == null) return;
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

    // ✅ parse number user typed (DISPLAY currency)
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

    // silent preview parser (no msg error spam)
    private BigDecimal parseBigDecimalSilent(String value) {
        try {
            String v = value == null ? "" : value.trim().replace(",", ".");
            if (v.isEmpty()) return null;
            return new BigDecimal(v);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String safe(String s) { return s == null ? "" : s; }
}