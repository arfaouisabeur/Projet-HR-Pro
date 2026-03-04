package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Tache;
import edu.RhPro.entities.User;
import edu.RhPro.services.TacheService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class MesTachesController {

    @FXML private TableView<Tache> table;
    @FXML private TableColumn<Tache, Integer> colId;
    @FXML private TableColumn<Tache, String> colTitre;
    @FXML private TableColumn<Tache, String> colStatut;
    @FXML private TableColumn<Tache, Integer> colProjet;
    @FXML private TableColumn<Tache, String> colDesc;

    @FXML private Label msgLabel;

    private final TacheService tacheService = new TacheService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colProjet.setCellValueFactory(new PropertyValueFactory<>("projetId"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
    }



    private void setStatus(String s) {
        Tache sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { msgLabel.setText("⚠️ Sélectionne une tâche."); return; }

        try {
            sel.setStatut(s);
            tacheService.updateTache(sel);

            msgLabel.setText("✅ Statut = " + s);
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur update statut.");
        }
    }

    @FXML public void setTodo() { setStatus("TODO"); }
    @FXML public void setDoing() { setStatus("DOING"); }
    @FXML public void setDone() { setStatus("DONE"); }
}
