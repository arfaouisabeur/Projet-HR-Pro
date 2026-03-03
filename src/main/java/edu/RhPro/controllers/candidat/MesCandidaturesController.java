package edu.RhPro.controllers.candidat;

import edu.RhPro.entities.Candidature;
import edu.RhPro.services.CandidatureService;
import edu.RhPro.services.CvSkillsService;
import edu.RhPro.utils.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MesCandidaturesController {

    @FXML private TableView<Candidature> candTable;
    @FXML private TableColumn<Candidature, Integer> colId;
    @FXML private TableColumn<Candidature, LocalDate> colDate;
    @FXML private TableColumn<Candidature, String> colStatut;
    @FXML private TableColumn<Candidature, Long> colOffreId;
    @FXML private Label msgLabel;

    private final CandidatureService service = new CandidatureService();
    private final ObservableList<Candidature> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCandidature"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colOffreId.setCellValueFactory(new PropertyValueFactory<>("offreEmploiId"));

        candTable.setItems(data);
        refresh();
    }

    @FXML
    public void refresh() {
        try {
            long candidatId = Session.getCurrentUser().getId();
            List<Candidature> list = service.findByCandidatId(candidatId);
            data.setAll(list);
            msgLabel.setText("Total: " + list.size());
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void onExtractCvSkills() {
        try {
            // ✅ 1) récupérer la candidature sélectionnée
            Candidature selected = candTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Sélection", "Choisissez une candidature.");
                return;
            }

            // ✅ 2) récupérer le chemin du CV (il faut que Candidature ait getCvPath())
            String path = selected.getCvPath(); // <-- si ton getter est différent, dis-moi son nom
            if (path == null || path.isBlank()) {
                showAlert(Alert.AlertType.INFORMATION, "CV", "Aucun CV.");
                return;
            }

            File pdf = new File(path);
            if (!pdf.exists()) {
                showAlert(Alert.AlertType.ERROR, "CV", "Fichier introuvable :\n" + path);
                return;
            }

            // ✅ 3) extraction + détection compétences
            CvSkillsService svc = new CvSkillsService();
            String text = svc.extractTextFromPdf(pdf);

            List<String> skillsList = svc.loadSkills();
            List<String> found = svc.extractSkills(text, skillsList);

            if (found == null || found.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Compétences", "Aucune compétence détectée.");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Compétences détectées",
                        "✅ Compétences trouvées :\n- " + String.join("\n- ", found));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}