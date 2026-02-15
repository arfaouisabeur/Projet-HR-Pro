package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Candidature;
import edu.RhPro.entities.offreEmploi;
import edu.RhPro.services.CandidatureService;
import edu.RhPro.services.OffreEmploiService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;


public class OffresController {

    @FXML private TableView<offreEmploi> offreTable;
    @FXML private TableColumn<offreEmploi, Integer> colId;
    @FXML private TableColumn<offreEmploi, String> colTitre;
    @FXML private TableColumn<offreEmploi, String> colLoc;
    @FXML private TableColumn<offreEmploi, String> colType;
    @FXML private TableColumn<offreEmploi, LocalDate> colPub;
    @FXML private TableColumn<offreEmploi, LocalDate> colExp;
    @FXML private TableColumn<offreEmploi, String> colStatut;

    @FXML private Label detailTitre;
    @FXML private Label detailInfo;
    @FXML private TextArea detailDesc;
    @FXML private Label msgLabel;

    private final OffreEmploiService offreService = new OffreEmploiService();
    private final CandidatureService candidatureService = new CandidatureService();

    private final ObservableList<offreEmploi> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colLoc.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeContrat"));
        colPub.setCellValueFactory(new PropertyValueFactory<>("datePublication"));
        colExp.setCellValueFactory(new PropertyValueFactory<>("dateExpiration"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        offreTable.setItems(data);

        offreTable.getSelectionModel().selectedItemProperty().addListener((obs, old, o) -> {
            if (o != null) showDetails(o);
        });

        loadOffres();
    }

    private void loadOffres() {
        try {
            List<offreEmploi> list = offreService.findActives();
            data.setAll(list);
            msgLabel.setText("");
            if (!data.isEmpty()) {
                offreTable.getSelectionModel().select(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("Erreur chargement offres: " + e.getMessage());
        }
    }

    private void showDetails(offreEmploi o) {
        detailTitre.setText(o.getTitre());
        detailInfo.setText(o.getLocalisation() + " • " + o.getTypeContrat()
                + " • Exp: " + o.getDateExpiration());
        detailDesc.setText(o.getDescription() == null ? "" : o.getDescription());
    }

    @FXML
    public void onApply() {
        try {
            if (Session.getCurrentUser() == null) {
                showAlert(Alert.AlertType.WARNING, "Connexion", "Veuillez vous connecter.");
                return;
            }

            offreEmploi selected = offreTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Offre", "Choisissez une offre.");
                return;
            }

            long candidatId = Session.getCurrentUser().getId();

            Candidature c = new Candidature(
                    LocalDate.now(),
                    "ENVOYEE",
                    null,
                    candidatId,
                    selected.getId()
            );

            candidatureService.add(c);

            // ✅ Popup de confirmation
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Candidature envoyée ✅");

            // (optionnel) petit message en bas aussi
            msgLabel.setStyle("-fx-text-fill:#16a34a;");
            msgLabel.setText("✅ Candidature envoyée !");

        } catch (IllegalArgumentException e) {
            // Exemple: "Vous avez déjà postulé..."
            showAlert(Alert.AlertType.WARNING, "Attention", e.getMessage());
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            msgLabel.setText(e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'envoyer la candidature.");
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            msgLabel.setText("Erreur: " + e.getMessage());
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
