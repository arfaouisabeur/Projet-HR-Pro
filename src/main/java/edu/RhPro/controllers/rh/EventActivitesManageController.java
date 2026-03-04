package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Activite;
import edu.RhPro.entities.Evenement;
import edu.RhPro.services.ActiviteService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;

public class EventActivitesManageController {

    @FXML private Label titleLabel;
    @FXML private TableView<Activite> table;
    @FXML private TableColumn<Activite, String> colId, colTitre, colDesc;

    @FXML private TextField titreField;
    @FXML private TextArea descArea;
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

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, a) -> {
            if (a != null) {
                titreField.setText(a.getTitre());
                descArea.setText(a.getDescription());
            }
        });
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

    @FXML
    public void add() {
        if (evenement == null) return;
        try {
            Activite a = new Activite(titreField.getText(), descArea.getText(), evenement.getId());
            service.addEntity(a);
            msgLabel.setText("✅ Ajout OK.");
            refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur ajout.");
        }
    }

    @FXML
    public void update() {
        Activite selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("⚠️ Sélectionne une activité."); return; }

        try {
            selected.setTitre(titreField.getText());
            selected.setDescription(descArea.getText());
            service.updateEntity(selected);
            msgLabel.setText("✅ Modif OK.");
            refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur modification.");
        }
    }

    @FXML
    public void delete() {
        Activite selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("⚠️ Sélectionne une activité."); return; }

        try {
            service.deleteEntity(selected);
            msgLabel.setText("✅ Supprimée.");
            refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur suppression.");
        }
    }
}
