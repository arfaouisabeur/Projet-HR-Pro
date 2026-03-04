package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Projet;
import edu.RhPro.entities.User;
import edu.RhPro.services.ProjetService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MesProjetsController {

    @FXML private TableView<Projet> table;
    @FXML private TableColumn<Projet, Integer> colId;
    @FXML private TableColumn<Projet, String> colTitre;
    @FXML private TableColumn<Projet, String> colStatut;
    @FXML private TableColumn<Projet, LocalDate> colDebut;
    @FXML private TableColumn<Projet, LocalDate> colFin;
    @FXML private TableColumn<Projet, String> colDesc;

    @FXML private Label msgLabel;

    private final ProjetService projetService = new ProjetService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        refresh();
    }

    @FXML
    public void refresh() {
        try {
            User u = Session.getCurrentUser();
            List<Projet> data = projetService.findByResponsableId((int) u.getId());
            table.setItems(FXCollections.observableArrayList(data));
            msgLabel.setText("✅ " + data.size() + " projet(s).");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur chargement projets.");
        }
    }

    @FXML
    public void openTasks() {
        Projet selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("⚠️ Sélectionne un projet.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/employe/ProjetTachesView.fxml"));
            Parent root = loader.load();

            ProjetTachesController ctrl = loader.getController();
            ctrl.setProjet(selected);

            Stage st = new Stage();
            st.setTitle("Tâches du projet #" + selected.getId());
            st.initModality(Modality.APPLICATION_MODAL);
            st.setScene(new Scene(root, 1100, 650));
            st.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("❌ Impossible d’ouvrir la gestion des tâches.");
        }
    }
}
