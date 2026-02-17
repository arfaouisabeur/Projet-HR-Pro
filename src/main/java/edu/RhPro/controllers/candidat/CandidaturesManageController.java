package edu.RhPro.controllers.rh;

import edu.RhPro.services.CandidatureAdminRow;
import edu.RhPro.services.CandidatureService;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.List;

public class CandidaturesManageController {

    @FXML private TableView<CandidatureAdminRow> candTable;
    @FXML private TableColumn<CandidatureAdminRow, Integer> colId;
    @FXML private TableColumn<CandidatureAdminRow, LocalDate> colDate;
    @FXML private TableColumn<CandidatureAdminRow, String> colStatut;
    @FXML private TableColumn<CandidatureAdminRow, String> colCandidat;
    @FXML private TableColumn<CandidatureAdminRow, String> colEmail;
    @FXML private TableColumn<CandidatureAdminRow, String> colOffre;
    @FXML private TableColumn<CandidatureAdminRow, String> colLoc;
    @FXML private TableColumn<CandidatureAdminRow, String> colType;

    @FXML private ComboBox<String> statusBox;
    @FXML private Label msgLabel;
    @FXML private ComboBox<String> filterStatutBox;

    private final CandidatureService service = new CandidatureService();
    private final ObservableList<CandidatureAdminRow> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCandidature"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colCandidat.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCandidatFullName()));
        colEmail.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCandidatEmail()));
        colOffre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOffreTitre()));
        colLoc.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOffreLocalisation()));
        colType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOffreTypeContrat()));

        candTable.setItems(data);

        statusBox.setItems(FXCollections.observableArrayList("ENVOYEE", "EN_COURS", "ACCEPTEE", "REFUSEE"));
        statusBox.getSelectionModel().select("EN_COURS");

        filterStatutBox.setItems(FXCollections.observableArrayList("TOUS", "ENVOYEE", "EN_COURS", "ACCEPTEE", "REFUSEE"));
        filterStatutBox.getSelectionModel().select("TOUS");

        refresh();
    }

    @FXML
    public void refresh() {
        try {
            List<CandidatureAdminRow> list = service.findAllForAdmin();
            data.setAll(list);
            msgLabel.setText("Total: " + list.size());
            msgLabel.setStyle("-fx-text-fill:#6b7280;");
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void applyStatus() {
        try {
            CandidatureAdminRow selected = candTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une candidature dans le tableau.");
                return;
            }

            String newStatus = statusBox.getValue();
            if (newStatus == null || newStatus.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Statut requis", "Veuillez choisir un statut.");
                return;
            }

            if (newStatus.equalsIgnoreCase(selected.getStatut())) {
                showAlert(Alert.AlertType.INFORMATION, "Aucun changement", "Cette candidature a déjà le statut : " + newStatus);
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText(null);
            confirm.setContentText("Changer le statut de la candidature ID " + selected.getId() + " vers : " + newStatus + " ?");
            ButtonType ok = new ButtonType("Oui", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(ok, cancel);

            if (confirm.showAndWait().orElse(cancel) != ok) {
                return;
            }

            service.updateStatus(selected.getId(), newStatus);

            // ✅ Message succès (Label)
            msgLabel.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:bold;");
            msgLabel.setText("✔ Statut modifié avec succès vers : " + newStatus);

            // ✅ Effacer le message après 3 secondes
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(event -> msgLabel.setText(""));
            pause.play();

            refresh();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le statut :\n" + e.getMessage());
        }
    }

    @FXML
    public void onFilter() {
        try {
            String selectedStatut = filterStatutBox.getValue();
            List<CandidatureAdminRow> list = service.findAllForAdmin();

            if (selectedStatut != null && !"TOUS".equals(selectedStatut)) {
                list = list.stream()
                        .filter(c -> selectedStatut.equalsIgnoreCase(c.getStatut()))
                        .toList();
            }

            data.setAll(list);

            msgLabel.setText("Total: " + list.size());
            msgLabel.setStyle("-fx-text-fill:#6b7280;");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de filtrer :\n" + e.getMessage());
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
