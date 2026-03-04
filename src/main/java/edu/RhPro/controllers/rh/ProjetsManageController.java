package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Projet;
import edu.RhPro.entities.User;
import edu.RhPro.services.ProjetService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ProjetsManageController {

    @FXML private TableView<Projet> table;
    @FXML private TableColumn<Projet, Integer> colId;
    @FXML private TableColumn<Projet, String> colTitre;
    @FXML private TableColumn<Projet, String> colStatut;
    @FXML private TableColumn<Projet, Integer> colResp;
    @FXML private TableColumn<Projet, LocalDate> colDebut;
    @FXML private TableColumn<Projet, LocalDate> colFin;
    @FXML private TableColumn<Projet, String> colDesc;

    @FXML private TextField titreField;
    @FXML private TextField descField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField responsableIdField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;

    @FXML private Label msgLabel;

    private final ProjetService projetService = new ProjetService();

    @FXML
    public void initialize() {
        statutCombo.setItems(FXCollections.observableArrayList("DOING", "DONE"));

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colResp.setCellValueFactory(new PropertyValueFactory<>("responsableEmployeId"));
        colDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) fillForm(selected);
        });

        refresh();
    }

    private void fillForm(Projet p) {
        titreField.setText(p.getTitre());
        descField.setText(p.getDescription());
        statutCombo.setValue(p.getStatut());
        responsableIdField.setText(String.valueOf(p.getResponsableEmployeId()));
        dateDebutPicker.setValue(p.getDateDebut());
        dateFinPicker.setValue(p.getDateFin());
    }

    @FXML
    public void clearForm() {
        titreField.clear();
        descField.clear();
        statutCombo.getSelectionModel().clearSelection();
        responsableIdField.clear();
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        table.getSelectionModel().clearSelection();
        msgLabel.setText("");
    }

    @FXML
    public void refresh() {
        try {
            List<Projet> data = projetService.getAllProjets();
            table.setItems(FXCollections.observableArrayList(data));
            msgLabel.setText("✅ " + data.size() + " projet(s).");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur chargement projets.");
        }
    }

    @FXML
    public void addProjet() {
        String titre = titreField.getText();
        String desc = descField.getText();
        String statut = statutCombo.getValue();
        LocalDate dd = dateDebutPicker.getValue();
        LocalDate df = dateFinPicker.getValue();

        if (titre == null || titre.isBlank() || statut == null || dd == null || df == null) {
            msgLabel.setText("⚠️ Titre + statut + dates obligatoires.");
            return;
        }

        int respId;
        try {
            respId = Integer.parseInt(responsableIdField.getText().trim());
        } catch (Exception e) {
            msgLabel.setText("⚠️ Responsable Employe ID invalide.");
            return;
        }

        User rh = Session.getCurrentUser();
        int rhId = (int) rh.getId();

        try {
            Projet p = new Projet(titre, desc, statut, rhId, respId, dd, df);
            projetService.addProjet(p);
            refresh();
            clearForm();
            msgLabel.setText("✅ Projet ajouté.");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur ajout projet.");
        }
    }

    @FXML
    public void updateProjet() {
        Projet selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("⚠️ Sélectionne un projet.");
            return;
        }

        String titre = titreField.getText();
        String desc = descField.getText();
        String statut = statutCombo.getValue();
        LocalDate dd = dateDebutPicker.getValue();
        LocalDate df = dateFinPicker.getValue();

        if (titre == null || titre.isBlank() || statut == null || dd == null || df == null) {
            msgLabel.setText("⚠️ Titre + statut + dates obligatoires.");
            return;
        }

        int respId;
        try {
            respId = Integer.parseInt(responsableIdField.getText().trim());
        } catch (Exception e) {
            msgLabel.setText("⚠️ Responsable Employe ID invalide.");
            return;
        }

        User rh = Session.getCurrentUser();
        int rhId = (int) rh.getId();

        try {
            Projet p = new Projet(selected.getId(), titre, desc, statut, rhId, respId, dd, df);
            projetService.updateProjet(p);
            refresh();
            msgLabel.setText("✅ Projet modifié.");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur modification projet.");
        }
    }

    @FXML
    public void deleteSelected() {
        Projet selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("⚠️ Sélectionne un projet.");
            return;
        }

        try {
            projetService.deleteProjet(selected.getId());
            refresh();
            clearForm();
            msgLabel.setText("✅ Projet supprimé.");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur suppression projet.");
        }
    }
}
