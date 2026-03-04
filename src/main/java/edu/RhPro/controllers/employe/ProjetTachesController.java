package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Projet;
import edu.RhPro.entities.Tache;
import edu.RhPro.entities.User;
import edu.RhPro.services.ProjetService;
import edu.RhPro.services.TacheService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ProjetTachesController {

    @FXML private Label titleLabel;
    @FXML private TextField titreField;
    @FXML private TextField descField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField employeIdField;

    @FXML private TableView<Tache> table;
    @FXML private TableColumn<Tache, Integer> colId;
    @FXML private TableColumn<Tache, String> colTitre;
    @FXML private TableColumn<Tache, String> colStatut;
    @FXML private TableColumn<Tache, Integer> colEmploye;
    @FXML private TableColumn<Tache, String> colDesc;

    @FXML private Label msgLabel;

    private Projet projet;
    private final ProjetService projetService = new ProjetService();
    private final TacheService tacheService = new TacheService();

    public void setProjet(Projet p) {
        this.projet = p;
        titleLabel.setText("Tâches du projet #" + p.getId() + " — " + p.getTitre());
        refresh();
    }

    @FXML
    public void initialize() {
        statutCombo.setItems(FXCollections.observableArrayList("TODO", "DOING", "DONE"));

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeId"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    private boolean isResponsible() {
        try {
            User u = Session.getCurrentUser();
            Projet fresh = projetService.getProjetById(projet.getId());
            return fresh != null && fresh.getResponsableEmployeId() == (int) u.getId();
        } catch (SQLException e) {
            return false;
        }
    }

    @FXML
    public void add() {
        if (projet == null) return;

        if (!isResponsible()) {
            msgLabel.setText("⛔ Seul le responsable du projet peut créer des tâches.");
            return;
        }

        String titre = titreField.getText();
        String desc = descField.getText();
        String statut = statutCombo.getValue();

        if (titre == null || titre.isBlank() || statut == null) {
            msgLabel.setText("⚠️ Titre + statut obligatoires.");
            return;
        }

        int empId;
        try {
            empId = Integer.parseInt(employeIdField.getText().trim());
        } catch (Exception e) {
            msgLabel.setText("⚠️ Employe ID invalide.");
            return;
        }

        // ✅ NEW RULES (your new DB)
        // - RH fills everything
        // - start & end required
        // - level not null
        // If you want "smart default": start=today, end=today+7 days, level=1
        LocalDate dateDebut = LocalDate.now();
        LocalDate dateFin = LocalDate.now().plusDays(7);
        int level = 1; // default (you can later add UI field)

        try {
            Tache t = new Tache(
                    titre,
                    desc,
                    statut,
                    dateDebut,
                    dateFin,
                    level,
                    projet.getId(),
                    empId,
                    null // primeId
            );

            tacheService.addTache(t);

            titreField.clear();
            descField.clear();
            employeIdField.clear();
            statutCombo.getSelectionModel().clearSelection();

            refresh();
            msgLabel.setText("✅ Tâche ajoutée.");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur ajout tâche.");
        }
    }

    @FXML
    public void refresh() {
        if (projet == null) return;
        try {
            List<Tache> data = tacheService.findByProjetId(projet.getId());
            table.setItems(FXCollections.observableArrayList(data));
            msgLabel.setText("✅ " + data.size() + " tâche(s).");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur chargement tâches.");
        }
    }

    @FXML
    public void deleteSelected() {
        Tache sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { msgLabel.setText("⚠️ Sélectionne une tâche."); return; }

        if (!isResponsible()) {
            msgLabel.setText("⛔ Seul le responsable peut supprimer.");
            return;
        }

        try {
            tacheService.deleteTache(sel.getId());
            refresh();
            msgLabel.setText("✅ Supprimée.");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur suppression.");
        }
    }

    @FXML
    public void markDone() {
        Tache sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { msgLabel.setText("⚠️ Sélectionne une tâche."); return; }

        if (!isResponsible()) {
            msgLabel.setText("⛔ Seul le responsable peut forcer DONE.");
            return;
        }

        try {
            sel.setStatut("DONE");
            tacheService.updateTache(sel);
            refresh();
            msgLabel.setText("✅ Statut = DONE.");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur update statut.");
        }
    }
}