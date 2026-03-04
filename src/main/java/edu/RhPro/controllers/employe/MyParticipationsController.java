package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Participation;
import edu.RhPro.entities.User;
import edu.RhPro.services.ParticipationService;
import edu.RhPro.utils.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class MyParticipationsController {

    @FXML private TableView<Participation> table;
    @FXML private TableColumn<Participation, String> colId, colEventId, colDate, colStatut;
    @FXML private Label msgLabel;

    private final ParticipationService service = new ParticipationService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colEventId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getEvenementId())));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateInscription() != null ? c.getValue().getDateInscription().toString() : ""));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        refresh();
    }

    private long currentEmployeId() {
        User u = Session.getCurrentUser();
        return u == null ? 0 : u.getId();
    }

    @FXML
    public void refresh() {
        try {
            long empId = currentEmployeId();
            if (empId == 0) { msgLabel.setText("❌ Session invalide."); return; }

            // You need this method in ParticipationService:
            // findByEmployeId(long employeId)
            List<Participation> list = service.findByEmployeId(empId);

            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setText("✅ " + list.size() + " participation(s).");
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur chargement.");
        }
    }
}
