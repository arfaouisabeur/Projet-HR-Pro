package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Evenement;
import edu.RhPro.entities.Participation;
import edu.RhPro.services.ParticipationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class EventParticipantsController {

    @FXML private Label titleLabel;
    @FXML private TableView<Participation> table;
    @FXML private TableColumn<Participation, String> colId, colEmployeId, colDate, colStatut;
    @FXML private Label msgLabel;

    private final ParticipationService service = new ParticipationService();
    private Evenement evenement;

    public void setEvenement(Evenement e) {
        this.evenement = e;
        titleLabel.setText("Participants - " + e.getTitre());
        refresh();
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colEmployeId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getEmployeId())));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateInscription() != null ? c.getValue().getDateInscription().toString() : ""));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
    }

    @FXML
    public void refresh() {
        if (evenement == null) return;
        try {
            List<Participation> list = service.findByEvenementId(evenement.getId());
            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setText("✅ " + list.size() + " participation(s).");
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur chargement.");
        }
    }

    @FXML
    public void accept() {
        Participation p = table.getSelectionModel().getSelectedItem();
        if (p == null) { msgLabel.setText("⚠️ Sélectionne une participation."); return; }

        try {
            service.updateStatus(p.getId(), "ACCEPTEE");
            msgLabel.setText("✅ Acceptée.");
            refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur.");
        }
    }

    @FXML
    public void refuse() {
        Participation p = table.getSelectionModel().getSelectedItem();
        if (p == null) { msgLabel.setText("⚠️ Sélectionne une participation."); return; }

        try {
            service.updateStatus(p.getId(), "REFUSEE");
            msgLabel.setText("✅ Refusée.");
            refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur.");
        }
    }
}
