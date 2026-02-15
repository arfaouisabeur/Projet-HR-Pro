package edu.RhPro.controllers.candidat;

import edu.RhPro.services.CandidatureAdminRow;
import edu.RhPro.services.CandidatureService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

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
    @FXML
    private ComboBox<String> filterStatutBox;

    private final CandidatureService service = new CandidatureService();
    private final ObservableList<CandidatureAdminRow> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCandidature"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // custom string via getters:
        colCandidat.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCandidatFullName()));
        colEmail.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCandidatEmail()));
        colOffre.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getOffreTitre()));
        colLoc.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getOffreLocalisation()));
        colType.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getOffreTypeContrat()));

        candTable.setItems(data);

        statusBox.setItems(FXCollections.observableArrayList("ENVOYEE", "EN_COURS", "ACCEPTEE", "REFUSEE"));
        statusBox.getSelectionModel().select("EN_COURS");

        refresh();
        filterStatutBox.setItems(FXCollections.observableArrayList(
                "TOUS",
                "ENVOYEE",
                "EN_COURS",
                "ACCEPTEE",
                "REFUSEE"
        ));
        filterStatutBox.getSelectionModel().select("TOUS");

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
                msgLabel.setStyle("-fx-text-fill:#b91c1c;");
                msgLabel.setText("Sélectionne une candidature.");
                return;
            }

            String newStatus = statusBox.getValue();
            if (newStatus == null || newStatus.isBlank()) {
                msgLabel.setStyle("-fx-text-fill:#b91c1c;");
                msgLabel.setText("Choisis un statut.");
                return;
            }

            service.updateStatus(selected.getId(), newStatus);
            msgLabel.setStyle("-fx-text-fill:#16a34a;");
            msgLabel.setText("✅ Statut mis à jour.");
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }
    @FXML
    public void onFilter() {
        try {
            String selectedStatut = filterStatutBox.getValue();

            List<CandidatureAdminRow> list = service.findAllForAdmin();

            if (!"TOUS".equals(selectedStatut)) {
                list = list.stream()
                        .filter(c -> selectedStatut.equalsIgnoreCase(c.getStatut()))
                        .toList();
            }

            data.setAll(list);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
