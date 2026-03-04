package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Evenement;
import edu.RhPro.entities.User;
import edu.RhPro.services.EvenementService;
import edu.RhPro.utils.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventsManageController {

    @FXML private TableView<Evenement> table;
    @FXML private TableColumn<Evenement, String> colId, colTitre, colDebut, colFin, colLieu, colDesc;

    @FXML private TextField titreField;
    @FXML private TextField lieuField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private TextField heureDebutField;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField heureFinField;
    @FXML private TextArea descArea;

    @FXML private Label msgLabel;

    private final EvenementService service = new EvenementService();

    @FXML
    public void initialize() {

        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colDebut.setCellValueFactory(c -> new SimpleStringProperty(formatDT(c.getValue().getDateDebut())));
        colFin.setCellValueFactory(c -> new SimpleStringProperty(formatDT(c.getValue().getDateFin())));
        colLieu.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLieu() == null ? "" : c.getValue().getLieu()));
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription() == null ? "" : c.getValue().getDescription()));

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, e) -> {
            if (e != null) fillForm(e);
        });

        // defaults
        if (heureDebutField != null && (heureDebutField.getText() == null || heureDebutField.getText().isBlank()))
            heureDebutField.setText("09:00");
        if (heureFinField != null && (heureFinField.getText() == null || heureFinField.getText().isBlank()))
            heureFinField.setText("12:00");

        refresh();
    }

    private String formatDT(LocalDateTime dt) {
        if (dt == null) return "";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dt.format(fmt);
    }

    private void fillForm(Evenement e) {
        titreField.setText(e.getTitre());
        lieuField.setText(e.getLieu());
        descArea.setText(e.getDescription());

        if (e.getDateDebut() != null) {
            dateDebutPicker.setValue(e.getDateDebut().toLocalDate());
            heureDebutField.setText(e.getDateDebut().toLocalTime().toString().substring(0,5));
        }
        if (e.getDateFin() != null) {
            dateFinPicker.setValue(e.getDateFin().toLocalDate());
            heureFinField.setText(e.getDateFin().toLocalTime().toString().substring(0,5));
        }
    }

    private long currentRhId() {
        User u = Session.getCurrentUser();
        if (u == null) return 0;
        return u.getId(); // ✅ RH ID auto from session (like you asked)
    }

    private LocalDateTime buildDT(LocalDate d, String hhmm) {
        if (d == null) return null;
        if (hhmm == null || hhmm.isBlank()) hhmm = "00:00";
        LocalTime t = LocalTime.parse(hhmm.trim());
        return LocalDateTime.of(d, t);
    }

    @FXML
    public void refresh() {
        try {
            List<Evenement> list = service.getData();
            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setText("✅ " + list.size() + " événement(s).");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur chargement.");
        }
    }

    @FXML
    public void add() {
        try {
            String titre = titreField.getText() == null ? "" : titreField.getText().trim();
            if (titre.isEmpty()) { msgLabel.setText("⚠️ Titre obligatoire."); return; }

            LocalDateTime debut = buildDT(dateDebutPicker.getValue(), heureDebutField.getText());
            LocalDateTime fin = buildDT(dateFinPicker.getValue(), heureFinField.getText());
            if (debut == null || fin == null) { msgLabel.setText("⚠️ Dates obligatoires."); return; }
            if (fin.isBefore(debut)) { msgLabel.setText("⚠️ Date fin < date début."); return; }

            Evenement e = new Evenement(
                    titre,
                    debut,
                    fin,
                    lieuField.getText(),
                    descArea.getText(),
                    currentRhId()
            );

            service.addEntity(e);
            msgLabel.setText("✅ Ajout OK (ID=" + e.getId() + ")");
            refresh();
            clearForm();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur ajout.");
        }
    }

    @FXML
    public void update() {
        Evenement selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("⚠️ Sélectionne un événement."); return; }

        try {
            String titre = titreField.getText() == null ? "" : titreField.getText().trim();
            if (titre.isEmpty()) { msgLabel.setText("⚠️ Titre obligatoire."); return; }

            LocalDateTime debut = buildDT(dateDebutPicker.getValue(), heureDebutField.getText());
            LocalDateTime fin = buildDT(dateFinPicker.getValue(), heureFinField.getText());
            if (debut == null || fin == null) { msgLabel.setText("⚠️ Dates obligatoires."); return; }
            if (fin.isBefore(debut)) { msgLabel.setText("⚠️ Date fin < date début."); return; }

            selected.setTitre(titre);
            selected.setLieu(lieuField.getText());
            selected.setDescription(descArea.getText());
            selected.setDateDebut(debut);
            selected.setDateFin(fin);
            selected.setRhId(currentRhId()); // ✅ keep auto

            service.updateEntity(selected);
            msgLabel.setText("✅ Update OK.");
            refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur update.");
        }
    }

    @FXML
    public void delete() {
        Evenement selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("⚠️ Sélectionne un événement."); return; }

        try {
            service.deleteEntity(selected);
            msgLabel.setText("✅ Supprimé.");
            refresh();
            clearForm();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur suppression.");
        }
    }

    // ✅ this is what your FXML calls
    @FXML
    public void clearForm() {
        table.getSelectionModel().clearSelection();
        titreField.clear();
        lieuField.clear();
        descArea.clear();
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        heureDebutField.setText("09:00");
        heureFinField.setText("12:00");
        msgLabel.setText("");
    }

    // ✅ Opens your existing controllers/pages
    @FXML
    public void openActivites() {
        Evenement selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("⚠️ Sélectionne un événement."); return; }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/rh/EventActivitesManageView.fxml"));
            Parent root = loader.load();

            EventActivitesManageController ctrl = loader.getController();
            ctrl.setEvenement(selected);

            Stage stage = new Stage();
            stage.setTitle("Activités - " + selected.getTitre());
            stage.setScene(new Scene(root, 980, 650));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Impossible d'ouvrir Activités.");
        }
    }

    @FXML
    public void openParticipations() {
        Evenement selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("⚠️ Sélectionne un événement."); return; }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/rh/EventParticipantsView.fxml"));
            Parent root = loader.load();

            EventParticipantsController ctrl = loader.getController();
            ctrl.setEvenement(selected);

            Stage stage = new Stage();
            stage.setTitle("Participants - " + selected.getTitre());
            stage.setScene(new Scene(root, 900, 600));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Impossible d'ouvrir Participants.");
        }
    }
}
