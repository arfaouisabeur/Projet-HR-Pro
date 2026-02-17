package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Reponse;
import edu.RhPro.entities.Service;
import edu.RhPro.services.ReponseService;
import edu.RhPro.services.ServiceService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ServicesManageController {

    @FXML private TableView<Service> table;
    @FXML private TableColumn<Service, Long> colId;
    @FXML private TableColumn<Service, Long> colEmploye;
    @FXML private TableColumn<Service, String> colTitre;
    @FXML private TableColumn<Service, LocalDate> colDate;
    @FXML private TableColumn<Service, String> colDesc;

    @FXML private TextArea taCommentaire;
    @FXML private Label msgLabel;

    @FXML private ComboBox<String> cbCriteria;
    @FXML private TextField tfSearch;

    private final ServiceService serviceService = new ServiceService();
    private final ReponseService reponseService = new ReponseService();

    private FilteredList<Service> filteredData;

    @FXML
    public void initialize() {

        colId.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().getId()).asObject());
        colEmploye.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().getEmployeeId()).asObject());
        colTitre.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getTitre()));
        colDate.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getDateDemande()));
        colDesc.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDescription()));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        cbCriteria.setItems(FXCollections.observableArrayList("ID", "Titre", "Date"));
        cbCriteria.getSelectionModel().selectFirst();

        loadData();

        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        cbCriteria.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    private void applyFilter() {
        if (filteredData == null) return;

        String searchText = tfSearch.getText().toLowerCase();
        String criterion = cbCriteria.getValue();

        filteredData.setPredicate(service -> {
            if (searchText == null || searchText.isEmpty()) return true;

            switch (criterion) {
                case "ID":
                    return String.valueOf(service.getId()).contains(searchText);
                case "Titre":
                    return service.getTitre() != null && service.getTitre().toLowerCase().contains(searchText);
                case "Date":
                    return service.getDateDemande() != null && service.getDateDemande().toString().contains(searchText);
                default:
                    return true;
            }
        });
    }

    @FXML
    public void refresh() {
        tfSearch.clear();
        loadData();
    }

    private void loadData() {
        try {
            List<Service> list = serviceService.findPending(); // récupère les demandes EN_ATTENTE

            filteredData = new FilteredList<>(FXCollections.observableArrayList(list), p -> true);
            SortedList<Service> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(table.comparatorProperty());
            table.setItems(sortedData);

            msgLabel.setStyle("-fx-text-fill:#6d2269; -fx-font-weight:900;");
            msgLabel.setText(list.size() + " demande(s) en attente");

        } catch (SQLException e) {
            msgLabel.setStyle("-fx-text-fill:red;");
            msgLabel.setText("Erreur DB");
            e.printStackTrace();
        }
    }

    /*** TRAITER DEMANDE ***/
    @FXML
    private void onTraiter() {
        handleDecision("ACCEPTEE");
    }

    @FXML
    private void onRefuse() {
        handleDecision("REFUSEE");
    }

    private void handleDecision(String decisionStatut) {
        Service selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setStyle("-fx-text-fill:red;");
            msgLabel.setText("Sélectionne une demande.");
            return;
        }

        try {
            // 1️⃣ Mettre à jour le statut de la demande
            selected.setStatut(decisionStatut);
            serviceService.updateEntity(selected);

            // 2️⃣ Ajouter la réponse RH via la méthode statique forService
            long rhId = Session.getCurrentUser().getId();
            Long employeId = selected.getEmployeeId();
            Long demandeServiceId = selected.getId();

            Reponse rep = Reponse.forService(
                    decisionStatut,
                    taCommentaire.getText() != null ? taCommentaire.getText().trim() : null,
                    rhId,
                    employeId,
                    demandeServiceId
            );

            reponseService.addEntity(rep);

            msgLabel.setStyle("-fx-text-fill:#059669;");
            msgLabel.setText("Demande " + decisionStatut.toLowerCase() + " avec succès ✅");

            taCommentaire.clear();
            loadData();

        } catch (SQLException e) {
            msgLabel.setStyle("-fx-text-fill:red;");
            msgLabel.setText("Erreur lors du traitement de la demande ❌");
            e.printStackTrace();
        }
    }

}
