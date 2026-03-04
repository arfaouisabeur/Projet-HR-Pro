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
import javafx.scene.control.*;
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
    @FXML private TableColumn<Projet, Void> colActions;

    @FXML private Label msgLabel;
    @FXML private TextField searchField;

    private final ProjetService projetService = new ProjetService();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupTableStyle();
        setupSearch();
        refresh();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Add status cell styling
        colStatut.setCellFactory(column -> new TableCell<Projet, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("DOING".equals(status)) {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    } else if ("DONE".equals(status)) {
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Add actions column
        colActions.setCellFactory(param -> new TableCell<Projet, Void>() {
            private final Button tasksBtn = new Button("Tâches");

            {
                tasksBtn.setStyle("-fx-background-color: #065f46; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 5 12; -fx-cursor: hand;");
                tasksBtn.setOnAction(event -> {
                    Projet projet = getTableView().getItems().get(getIndex());
                    openTasksForProjet(projet);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(tasksBtn);
                }
            }
        });
    }

    private void setupTableStyle() {
        table.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-border-radius: 18;");
        table.setPlaceholder(new Label("Aucun projet trouvé"));

        // Fix: Proper row styling with visible selection
        table.setRowFactory(tv -> new TableRow<Projet>() {
            @Override
            protected void updateItem(Projet item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                } else {
                    // Handle selection state FIRST
                    if (isSelected()) {
                        // Selected row - light blue background with BLACK text
                        setStyle("-fx-background-color: #e0f2fe; " +
                                "-fx-table-cell-border-color: transparent; " +
                                "-fx-text-background-color: black;");
                    }
                    // Then handle alternating colors for non-selected rows
                    else if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: #f9fafb; " +
                                "-fx-table-cell-border-color: transparent; " +
                                "-fx-text-background-color: black;");
                    } else {
                        setStyle("-fx-background-color: white; " +
                                "-fx-table-cell-border-color: transparent; " +
                                "-fx-text-background-color: black;");
                    }
                }
            }
        });

        // Also fix the column header style
        table.lookupAll(".column-header").forEach(header ->
                header.setStyle("-fx-background-color: #f3f4f6; -fx-border-color: #e5e7eb; -fx-padding: 10;")
        );
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, newVal) -> {
            filterTable(newVal);
        });
    }

    private void filterTable(String searchText) {
        try {
            User u = Session.getCurrentUser();
            List<Projet> allProjets = projetService.findByResponsableId((int) u.getId());

            if (searchText == null || searchText.isEmpty()) {
                table.setItems(FXCollections.observableArrayList(allProjets));
            } else {
                List<Projet> filtered = allProjets.stream()
                        .filter(p -> p.getTitre().toLowerCase().contains(searchText.toLowerCase()) ||
                                p.getDescription().toLowerCase().contains(searchText.toLowerCase()) ||
                                p.getStatut().toLowerCase().contains(searchText.toLowerCase()))
                        .toList();
                table.setItems(FXCollections.observableArrayList(filtered));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void refresh() {
        try {
            User u = Session.getCurrentUser();
            List<Projet> data = projetService.findByResponsableId((int) u.getId());
            table.setItems(FXCollections.observableArrayList(data));
            msgLabel.setText(data.size() + " projet(s) trouvé(s)");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("Erreur chargement projets.");
        }
    }

    @FXML
    public void openTasks() {
        Projet selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélection requise", "Veuillez sélectionner un projet");
            return;
        }
        openTasksForProjet(selected);
    }

    private void openTasksForProjet(Projet projet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/employe/ProjetTachesView.fxml"));
            Parent root = loader.load();

            ProjetTachesController ctrl = loader.getController();
            ctrl.setProjet(projet);

            Stage st = new Stage();
            st.setTitle("Tâches - " + projet.getTitre());
            st.initModality(Modality.APPLICATION_MODAL);
            st.setScene(new Scene(root));
            st.showAndWait();
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("Impossible d'ouvrir la gestion des tâches.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}