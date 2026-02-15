package edu.RhPro.controllers.rh;

import edu.RhPro.entities.offreEmploi;
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

import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class OffresManageController {

    @FXML private TableView<offreEmploi> offreTable;
    @FXML private TableColumn<offreEmploi, Integer> colId;
    @FXML private TableColumn<offreEmploi, String> colTitre;
    @FXML private TableColumn<offreEmploi, String> colLoc;
    @FXML private TableColumn<offreEmploi, String> colType;
    @FXML private TableColumn<offreEmploi, LocalDate> colPub;
    @FXML private TableColumn<offreEmploi, LocalDate> colExp;
    @FXML private TableColumn<offreEmploi, String> colStatut;

    @FXML private TextField titreField, locField, typeField;
    @FXML private DatePicker pubPicker, expPicker;
    @FXML private ComboBox<String> statutBox;
    @FXML private TextArea descArea;
    @FXML private Label msgLabel;

    private final OffreEmploiService service = new OffreEmploiService();
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

        statutBox.setItems(FXCollections.observableArrayList("ACTIVE", "FERMEE"));
        statutBox.getSelectionModel().select("ACTIVE");

        offreTable.getSelectionModel().selectedItemProperty().addListener((obs, old, o) -> {
            if (o != null) fillForm(o);
        });

        refresh();
    }

    private void fillForm(offreEmploi o) {
        titreField.setText(o.getTitre());
        locField.setText(o.getLocalisation());
        typeField.setText(o.getTypeContrat());
        pubPicker.setValue(o.getDatePublication());
        expPicker.setValue(o.getDateExpiration());
        statutBox.getSelectionModel().select(o.getStatut());
        descArea.setText(o.getDescription() == null ? "" : o.getDescription());
    }

    @FXML
    public void refresh() {
        try {
            List<offreEmploi> list = service.findAll(); // ✅ all offers
            data.setAll(list);
            msgLabel.setText("Total offres: " + list.size());
            msgLabel.setStyle("-fx-text-fill:#6b7280;");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void onAdd() {
        try {
            if (titreField.getText().isBlank() || locField.getText().isBlank() || typeField.getText().isBlank()) {
                msgLabel.setStyle("-fx-text-fill:#b91c1c;");
                msgLabel.setText("Titre, localisation et type contrat sont obligatoires.");
                return;
            }

            Integer rhId = Session.getCurrentUser().getId(); // ✅ required NOT NULL

            offreEmploi o = new offreEmploi(
                    titreField.getText().trim(),
                    descArea.getText().trim(),
                    locField.getText().trim(),
                    typeField.getText().trim(),
                    pubPicker.getValue() == null ? LocalDate.now() : pubPicker.getValue(),
                    expPicker.getValue() == null ? LocalDate.now().plusDays(30) : expPicker.getValue(),
                    statutBox.getValue() == null ? "ACTIVE" : statutBox.getValue(),
                    rhId
            );

            service.add(o);
            msgLabel.setStyle("-fx-text-fill:#16a34a;");
            msgLabel.setText("✅ Offre ajoutée.");
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void onUpdate() {
        try {
            offreEmploi selected = offreTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                msgLabel.setStyle("-fx-text-fill:#b91c1c;");
                msgLabel.setText("Sélectionne une offre à modifier.");
                return;
            }

            selected.setTitre(titreField.getText().trim());
            selected.setDescription(descArea.getText().trim());
            selected.setLocalisation(locField.getText().trim());
            selected.setTypeContrat(typeField.getText().trim());
            selected.setDatePublication(pubPicker.getValue());
            selected.setDateExpiration(expPicker.getValue());
            selected.setStatut(statutBox.getValue());
            selected.setRhId(Session.getCurrentUser().getId());

            service.update(selected);

            msgLabel.setStyle("-fx-text-fill:#16a34a;");
            msgLabel.setText("✅ Offre modifiée.");
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void onDelete() {
        try {
            offreEmploi selected = offreTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                msgLabel.setStyle("-fx-text-fill:#b91c1c;");
                msgLabel.setText("Sélectionne une offre à supprimer.");
                return;
            }
            service.delete(selected.getId());
            msgLabel.setStyle("-fx-text-fill:#16a34a;");
            msgLabel.setText("✅ Offre supprimée.");
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void onClose() {
        try {
            offreEmploi selected = offreTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                msgLabel.setStyle("-fx-text-fill:#b91c1c;");
                msgLabel.setText("Sélectionne une offre à fermer.");
                return;
            }
            service.fermer(selected.getId());
            msgLabel.setStyle("-fx-text-fill:#16a34a;");
            msgLabel.setText("✅ Offre fermée.");
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }
    @FXML
    private void onShowStats() {
        try {
            int actives = service.countByStatut("ACTIVE");
            int fermees = service.countByStatut("FERMEE");

            CategoryAxis xAxis = new CategoryAxis();
            xAxis.setLabel("Statut");

            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Nombre d'offres");

            BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
            chart.setTitle("Offres : Actives vs Fermées");

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Offres");
            series.getData().add(new XYChart.Data<>("ACTIVE", actives));
            series.getData().add(new XYChart.Data<>("FERMEE", fermees));

            chart.getData().setAll(series);

            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle("Statistiques Offres");
            popup.setScene(new Scene(chart, 520, 380));
            popup.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            msgLabel.setText("Erreur stats: " + e.getMessage());
        }
    }

}
