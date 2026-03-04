package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Activite;
import edu.RhPro.entities.Evenement;
import edu.RhPro.services.ActiviteService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;

public class EventActivitesController {

    @FXML private Label titleLabel;
    @FXML private TableView<Activite> table;
    @FXML private TableColumn<Activite, String> colId, colTitre, colDesc;
    @FXML private Label msgLabel;

    private final ActiviteService service = new ActiviteService();
    private Evenement evenement;

    public void setEvenement(Evenement e) {
        this.evenement = e;
        titleLabel.setText("Activités - " + e.getTitre());
        refresh();
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription() == null ? "" : c.getValue().getDescription()));
    }

    @FXML
    public void refresh() {
        if (evenement == null) return;
        try {
            List<Activite> all = service.getData();
            List<Activite> list = all.stream()
                    .filter(a -> a.getEvenementId() == evenement.getId())
                    .collect(Collectors.toList());

            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setText("✅ " + list.size() + " activité(s).");
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur chargement.");
        }
    }
}
