package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Tache;
import edu.RhPro.entities.TacheV2;
import edu.RhPro.services.TacheV2Service;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.awt.Desktop;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TachePrimePickerController {

    private static final String CHATBASE_IFRAME_URL =
            "https://www.chatbase.co/chatbot-iframe/B4HmaVFkyrAprVXBeURfp";

    @FXML private Label lblHeader;
    @FXML private TextField tfSearch;
    @FXML private Label lblTotal;
    @FXML private TextArea taPreviewDesc;

    @FXML private TableView<TachePayRow> table;
    @FXML private TableColumn<TachePayRow, Boolean> colSelect;
    @FXML private TableColumn<TachePayRow, String> colTitre;
    @FXML private TableColumn<TachePayRow, LocalDate> colDebut;
    @FXML private TableColumn<TachePayRow, LocalDate> colFin;
    @FXML private TableColumn<TachePayRow, Integer> colLevel;
    @FXML private TableColumn<TachePayRow, String> colAmount;

    @FXML private Button btnValider;
    @FXML private Button btnAnnuler;

    private final TacheV2Service tacheService = new TacheV2Service();
    private final ObservableList<TachePayRow> rows = FXCollections.observableArrayList();
    private Stage stage;
    private int employeId;

    public interface PickCallback {
        void onPicked(BigDecimal total, String description, List<Tache> selectedTaches);
    }
    private PickCallback callback;

    public void setStage(Stage stage) { this.stage = stage; }
    public void setOnPicked(PickCallback cb) { this.callback = cb; }

    public void initForEmploye(int employeId) {
        this.employeId = employeId;
        if (lblHeader != null) lblHeader.setText("Tâches de l'employé ID: " + employeId);
        loadTaches();
    }

    @FXML
    public void initialize() {
        // Select checkbox
        colSelect.setCellValueFactory(data -> data.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);

        // Basic columns
        colTitre.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getTache().getTitre()))
        );
        colDebut.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getTache().getDateDebut())
        );
        colFin.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getTache().getDateFin())
        );
        colLevel.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getTache().getLevel()).asObject()
        );

        colAmount.setCellValueFactory(data -> data.getValue().amountTextProperty());
        colAmount.setCellFactory(TextFieldTableCell.forTableColumn());
        colAmount.setOnEditCommit(e -> {
            e.getRowValue().setAmountText(e.getNewValue());

            BigDecimal typed = parseMoneyOrNull(e.getNewValue());
            if (typed != null && typed.signum() > 0) {
                e.getRowValue().setSelected(true);
            }

            // 3) recalc
            recalcTotalAndPreview();
        });

        // Table setup
        table.setEditable(true);
        table.setItems(rows);

        // Search filter
        tfSearch.textProperty().addListener((obs, o, n) -> applySearchFilter());

        // Initial calc
        recalcTotalAndPreview();
    }

    private void loadTaches() {
        try {
            List<TacheV2> taches = tacheService.findDoneByEmployeId(employeId);

            rows.clear();
            for (TacheV2 t : taches) {
                TachePayRow r = new TachePayRow(t);
                r.selectedProperty().addListener((a,b,c) -> recalcTotalAndPreview());
                r.amountTextProperty().addListener((a,b,c) -> recalcTotalAndPreview());
                rows.add(r);
            }

            applySearchFilter();
            recalcTotalAndPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applySearchFilter() {
        String q = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase();

        if (q.isEmpty()) {
            table.setItems(rows);
            recalcTotalAndPreview();
            return;
        }

        ObservableList<TachePayRow> filtered = FXCollections.observableArrayList();
        for (TachePayRow r : rows) {
            String titre = safe(r.getTache().getTitre()).toLowerCase();
            String desc = safe(r.getTache().getDescription()).toLowerCase();
            if (titre.contains(q) || desc.contains(q)) filtered.add(r);
        }
        table.setItems(filtered);
        recalcTotalAndPreview();
    }

    private void recalcTotalAndPreview() {
        BigDecimal total = computeTotal();
        lblTotal.setText(total.toPlainString() + " $");
        taPreviewDesc.setText(buildDescriptionFromSelected());
    }

    private BigDecimal computeTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (TachePayRow r : rows) {
            if (!r.isSelected()) continue;

            BigDecimal typed = parseMoneyOrNull(r.getAmountText());
            if (typed != null && typed.signum() > 0) total = total.add(typed);
            else total = total.add(defaultAmountByLevel(r.getTache().getLevel()));
        }
        return total;
    }

    private String buildDescriptionFromSelected() {
        List<String> titles = new ArrayList<>();
        for (TachePayRow r : rows) {
            if (!r.isSelected()) continue;

            String titre = safe(r.getTache().getTitre()).trim();
            if (titre.isEmpty()) titre = "Tâche #" + r.getTache().getId();
            titles.add(titre);
        }
        if (titles.isEmpty()) return "";
        return "Prime pour les tâches : " + String.join(", ", titles);
    }

    @FXML
    public void onValider() {
        BigDecimal total = computeTotal();
        String desc = buildDescriptionFromSelected();

        List<Tache> selected = new ArrayList<>();
        for (TachePayRow r : rows) if (r.isSelected()) selected.add(r.getTache());

        if (callback != null) callback.onPicked(total, desc, selected);

        if (stage == null) stage = (Stage) btnValider.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void onAnnuler() {
        if (stage == null) stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void onHelpAi() {
        String context = buildAiContextText();
        copyToClipboard(context);

        try {
            Desktop.getDesktop().browse(new URI(CHATBASE_IFRAME_URL));
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Cannot open browser.\nOpen manually:\n" + CHATBASE_IFRAME_URL + "\n\n" + e.getMessage()
            ).showAndWait();
        }
    }

    private String buildAiContextText() {
        List<TacheV2> selected = new ArrayList<>();
        for (TachePayRow r : rows) {
            if (r.isSelected()) selected.add(r.getTache());
        }

        StringBuilder sb = new StringBuilder();
        if (selected.isEmpty()) {
            sb.append("Aucune tâche sélectionnée.\n");
            sb.append("Demande-moi de sélectionner des tâches.\n");
            return sb.toString();
        }

        sb.append("Tâches sélectionnées:\n");
        int i = 1;
        for (TacheV2 t : selected) {
            sb.append(i++).append(") ");
            sb.append("Titre=").append(safe(t.getTitre()));
            sb.append(" | Début=").append(t.getDateDebut());
            sb.append(" | Fin=").append(t.getDateFin());
            sb.append(" | Level=").append(t.getLevel());
            String d = safe(t.getDescription()).replace("\n", " ").trim();
            if (!d.isEmpty()) sb.append(" | Desc=").append(d);
            sb.append("\n");
        }

        return sb.toString();
    }

    private void copyToClipboard(String text) {
        try {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BigDecimal defaultAmountByLevel(int level) {
        return switch (level) {
            case 1 -> new BigDecimal("10");
            case 2 -> new BigDecimal("20");
            case 3 -> new BigDecimal("30");
            case 4 -> new BigDecimal("50");
            case 5 -> new BigDecimal("80");
            default -> new BigDecimal("15");
        };
    }

    private BigDecimal parseMoneyOrNull(String s) {
        if (s == null) return null;
        String v = s.trim().replace(",", ".");
        if (v.isEmpty()) return null;
        try { return new BigDecimal(v); }
        catch (Exception e) { return null; }
    }

    private String safe(String s) { return s == null ? "" : s; }

    public static class TachePayRow {
        private final TacheV2 tache;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private final StringProperty amountText = new SimpleStringProperty("");

        public TachePayRow(TacheV2 t) { this.tache = t; }

        public TacheV2 getTache() { return tache; }

        public boolean isSelected() { return selected.get(); }
        public BooleanProperty selectedProperty() { return selected; }
        public void setSelected(boolean v) { selected.set(v); }

        public String getAmountText() { return amountText.get(); }
        public StringProperty amountTextProperty() { return amountText; }
        public void setAmountText(String v) { amountText.set(v == null ? "" : v); }
    }
}