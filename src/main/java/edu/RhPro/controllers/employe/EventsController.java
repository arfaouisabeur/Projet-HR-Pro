package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Evenement;
import edu.RhPro.entities.Participation;
import edu.RhPro.entities.User;
import edu.RhPro.services.EvenementService;
import edu.RhPro.services.ParticipationService;
import edu.RhPro.utils.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventsController {

    @FXML private TableView<Evenement> table;
    @FXML private TableColumn<Evenement, String> colId, colTitre, colDebut, colFin, colLieu;
    @FXML private Label msgLabel;

    private final EvenementService eventService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();

    @FXML
    public void initialize() {

        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colDebut.setCellValueFactory(c -> new SimpleStringProperty(formatDT(c.getValue().getDateDebut())));
        colFin.setCellValueFactory(c -> new SimpleStringProperty(formatDT(c.getValue().getDateFin())));
        colLieu.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLieu() == null ? "" : c.getValue().getLieu()));

        refresh();
    }

    private String formatDT(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dt.format(fmt);
    }

    private long currentEmployeId() {
        User u = Session.getCurrentUser();
        return u == null ? 0 : u.getId();
    }

    @FXML
    public void refresh() {
        try {
            List<Evenement> list = eventService.getData();
            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setText("✅ " + list.size() + " événement(s).");
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur chargement.");
        }
    }

    @FXML
    public void openActivites() {
        Evenement selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("⚠️ Sélectionne un événement."); return; }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/employe/EventActivitesView.fxml"));
            Parent root = loader.load();

            EventActivitesController ctrl = loader.getController();
            ctrl.setEvenement(selected);

            Stage stage = new Stage();
            stage.setTitle("Activités - " + selected.getTitre());
            stage.setScene(new Scene(root, 900, 600));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Impossible d'ouvrir les activités.");
        }
    }

    @FXML
    public void participer() {
        Evenement selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("⚠️ Sélectionne un événement."); return; }

        try {
            long empId = currentEmployeId();
            if (empId == 0) { msgLabel.setText("❌ Session employé invalide."); return; }

            // Insert participation (EN_ATTENTE)
            Participation p = new Participation(LocalDate.now(), "EN_ATTENTE", selected.getId(), empId);
            participationService.addEntity(p);

            msgLabel.setText("✅ Participation envoyée (EN_ATTENTE).");
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur participation (déjà inscrit ?).");
        }
    }
}
