package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Projet;
import edu.RhPro.entities.Tache;
import edu.RhPro.entities.User;
import edu.RhPro.services.ProjetService;
import edu.RhPro.services.TacheService;
import edu.RhPro.services.UserService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import java.time.LocalDateTime;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.geometry.Pos;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.geometry.Insets;
public class ProjetsManageController {

    @FXML private TableView<Projet> table;
    @FXML private TableColumn<Projet, Integer> colId;
    @FXML private TableColumn<Projet, String> colTitre;
    @FXML private TableColumn<Projet, String> colStatut;
    @FXML private TableColumn<Projet, Integer> colResp;
    @FXML private TableColumn<Projet, LocalDate> colDebut;
    @FXML private TableColumn<Projet, LocalDate> colFin;
    @FXML private TableColumn<Projet, String> colDesc;

    // Form fields
    @FXML private TextField titreField;
    @FXML private TextField descField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private ComboBox<User> responsableCombo;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;

    // Error labels
    @FXML private Label titreErrorLabel;
    @FXML private Label descErrorLabel;
    @FXML private Label statutErrorLabel;
    @FXML private Label responsableErrorLabel;
    @FXML private Label dateDebutErrorLabel;
    @FXML private Label dateFinErrorLabel;
    @FXML private Label msgLabel;

    @FXML private ToggleButton statsToggleButton;
    @FXML private VBox statsPanel;
    @FXML private PieChart projectProgressChart;
    @FXML private BarChart<String, Number> employeeTaskChart;

    private final ProjetService projetService = new ProjetService();
    private final UserService userService = new UserService();

    private final TacheService tacheService = new TacheService();
    private List<User> allEmployees;
    private List<Projet> allProjects;

    // Style constants
    private final String normalFieldStyle = "-fx-border-color: #ececf5; -fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;";
    private final String errorFieldStyle = "-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 14; -fx-background-radius: 14;";

    @FXML
    public void initialize() {
        // Setup statut combo box
        statutCombo.setItems(FXCollections.observableArrayList("DOING", "DONE"));

        // Setup responsable combo box
        setupResponsableComboBox();

        // Load employees
        loadEmployees();

        // Setup table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colResp.setCellValueFactory(new PropertyValueFactory<>("responsableEmployeId"));
        colDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Table selection listener
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) fillForm(selected);

            if (statsPanel != null && statsPanel.isVisible()) {
                loadStats();
            }
        });

        // Add real-time validation listeners
        addValidationListeners();

        statsToggleButton.setOnAction(e -> {
            boolean show = statsToggleButton.isSelected();
            statsPanel.setVisible(show);
            statsPanel.setManaged(show);
            if (show) {
                loadStats();
            }
        });

        // Initial load
        refresh();
    }

    private void loadStats() {
        try {
            // Determine which project to show (selected or global)
            Projet selected = table.getSelectionModel().getSelectedItem();
            Map<String, Integer> statusData;
            if (selected != null) {
                statusData = tacheService.getTaskStatusCountByProject(selected.getId());
            } else {
                statusData = tacheService.getGlobalTaskStatus();
            }

            // --- Update Pie Chart (Project Progress) ---
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            statusData.forEach((status, count) ->
                    pieData.add(new PieChart.Data(status + " (" + count + ")", count))
            );
            projectProgressChart.setData(pieData);
            projectProgressChart.setTitle(selected != null ?
                    "Projet : " + selected.getTitre() : "Tous les projets");

            // --- Update Bar Chart (Tasks per Employee) ---
            Map<Integer, Integer> empTaskCounts = tacheService.getTaskCountPerEmployee();

            // Prepare data series
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Tâches assignées");

            // Convert employee IDs to names using allEmployees list (already loaded)
            for (Map.Entry<Integer, Integer> entry : empTaskCounts.entrySet()) {
                int empId = entry.getKey();
                int count = entry.getValue();
                // Find employee name from the allEmployees list
                String name = allEmployees.stream()
                        .filter(u -> u.getId() == empId)
                        .map(u -> u.getNom() + " " + u.getPrenom())
                        .findFirst()
                        .orElse("ID " + empId);
                series.getData().add(new XYChart.Data<>(name, count));
            }

            // Clear old data and add the new series
            employeeTaskChart.getData().clear();
            employeeTaskChart.getData().add(series);

            // Optionally, set axis labels (they are already defined in FXML)
            CategoryAxis xAxis = (CategoryAxis) employeeTaskChart.getXAxis();
            xAxis.setLabel("Employé");
            NumberAxis yAxis = (NumberAxis) employeeTaskChart.getYAxis();
            yAxis.setLabel("Nombre de tâches");

        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur chargement statistiques.");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private void setupResponsableComboBox() {
        responsableCombo.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                if (user == null) return "";
                return user.getNom() + " " + user.getPrenom() + " (ID: " + user.getId() + ")";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });

        responsableCombo.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getNom() + " " + user.getPrenom() + " (ID: " + user.getId() + ")");
                }
            }
        });
    }

    private void loadEmployees() {
        try {
            allEmployees = userService.getData().stream()
                    .filter(user -> "EMPLOYE".equalsIgnoreCase(user.getRole()))
                    .collect(Collectors.toList());
            responsableCombo.setItems(FXCollections.observableArrayList(allEmployees));
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur chargement employés.");
        }
    }

    private void addValidationListeners() {
        // Titre validation
        titreField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && newVal.length() >= 5) {
                hideError(titreField, titreErrorLabel);
            }
        });

        // Description validation - no numbers allowed
        descField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                // Check if contains numbers
                if (newVal.matches(".*\\d.*")) {
                    showError(descField, descErrorLabel, "La description ne doit pas contenir de chiffres");
                } else {
                    hideError(descField, descErrorLabel);
                }
            }
        });

        // Statut validation
        statutCombo.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                hideError(statutCombo, statutErrorLabel);
            }
        });

        // Responsable validation
        responsableCombo.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                hideError(responsableCombo, responsableErrorLabel);
            }
        });

        // Date début validation
        dateDebutPicker.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                hideError(dateDebutPicker, dateDebutErrorLabel);
                validateDateOrder();
            }
        });

        // Date fin validation
        dateFinPicker.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                hideError(dateFinPicker, dateFinErrorLabel);
                validateDateOrder();
            }
        });
    }

    private void validateDateOrder() {
        LocalDate debut = dateDebutPicker.getValue();
        LocalDate fin = dateFinPicker.getValue();

        if (debut != null && fin != null) {
            if (fin.isBefore(debut) || fin.isEqual(debut)) {
                showError(dateFinPicker, dateFinErrorLabel, "La date fin doit être après la date début");
            } else {
                hideError(dateFinPicker, dateFinErrorLabel);
            }
        }
    }

    private boolean isDuplicateProject(String titre, Integer excludeId) {
        if (allProjects == null) return false;

        for (Projet project : allProjects) {
            if (excludeId != null && project.getId() == excludeId) continue;

            // Check if title matches (case insensitive)
            if (project.getTitre().equalsIgnoreCase(titre.trim())) {
                return true;
            }
        }
        return false;
    }

    private void showError(Control field, Label errorLabel, String message) {
        field.setStyle(errorFieldStyle);
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError(Control field, Label errorLabel) {
        field.setStyle(normalFieldStyle);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private boolean validateForm(boolean isUpdate) {
        boolean isValid = true;

        // Validate Titre
        String titre = titreField.getText();
        if (titre == null || titre.trim().length() < 5) {
            showError(titreField, titreErrorLabel, "Le titre doit contenir au moins 5 caractères");
            isValid = false;
        } else {
            hideError(titreField, titreErrorLabel);
        }

        // Validate Description - no numbers allowed
        String description = descField.getText();
        if (description != null && description.matches(".*\\d.*")) {
            showError(descField, descErrorLabel, "La description ne doit pas contenir de chiffres");
            isValid = false;
        } else {
            hideError(descField, descErrorLabel);
        }

        // Validate Statut
        if (statutCombo.getValue() == null) {
            showError(statutCombo, statutErrorLabel, "Veuillez sélectionner un statut");
            isValid = false;
        } else {
            hideError(statutCombo, statutErrorLabel);
        }

        // Validate Responsable
        if (responsableCombo.getValue() == null) {
            showError(responsableCombo, responsableErrorLabel, "Veuillez sélectionner un responsable");
            isValid = false;
        } else {
            hideError(responsableCombo, responsableErrorLabel);
        }

        // Validate Date Début
        if (dateDebutPicker.getValue() == null) {
            showError(dateDebutPicker, dateDebutErrorLabel, "Veuillez sélectionner une date de début");
            isValid = false;
        } else {
            hideError(dateDebutPicker, dateDebutErrorLabel);
        }

        // Validate Date Fin
        LocalDate debut = dateDebutPicker.getValue();
        LocalDate fin = dateFinPicker.getValue();

        if (fin == null) {
            showError(dateFinPicker, dateFinErrorLabel, "Veuillez sélectionner une date de fin");
            isValid = false;
        } else if (debut != null && (fin.isBefore(debut) || fin.isEqual(debut))) {
            showError(dateFinPicker, dateFinErrorLabel, "La date fin doit être après la date début");
            isValid = false;
        } else {
            hideError(dateFinPicker, dateFinErrorLabel);
        }

        // Check for duplicates
        if (isValid && titre != null) {
            Projet selected = table.getSelectionModel().getSelectedItem();
            Integer excludeId = isUpdate && selected != null ? selected.getId() : null;

            if (isDuplicateProject(titre, excludeId)) {
                showError(titreField, titreErrorLabel, "Un projet avec ce titre existe déjà");
                isValid = false;
            }
        }

        return isValid;
    }

    private void fillForm(Projet p) {
        titreField.setText(p.getTitre());
        descField.setText(p.getDescription());
        statutCombo.setValue(p.getStatut());

        // Find and select responsible employee
        try {
            User responsible = userService.getData().stream()
                    .filter(u -> u.getId() == p.getResponsableEmployeId())
                    .findFirst()
                    .orElse(null);
            responsableCombo.setValue(responsible);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        dateDebutPicker.setValue(p.getDateDebut());
        dateFinPicker.setValue(p.getDateFin());

        // Clear all errors
        clearAllErrors();
    }

    private void clearAllErrors() {
        hideError(titreField, titreErrorLabel);
        hideError(descField, descErrorLabel);
        hideError(statutCombo, statutErrorLabel);
        hideError(responsableCombo, responsableErrorLabel);
        hideError(dateDebutPicker, dateDebutErrorLabel);
        hideError(dateFinPicker, dateFinErrorLabel);
    }

    @FXML
    public void clearForm() {
        titreField.clear();
        descField.clear();
        statutCombo.setValue(null);
        responsableCombo.setValue(null);
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        table.getSelectionModel().clearSelection();
        msgLabel.setText("");
        clearAllErrors();
    }

    @FXML
    public void refresh() {
        try {
            allProjects = projetService.getAllProjets();
            table.setItems(FXCollections.observableArrayList(allProjects));
            msgLabel.setText("✅ " + allProjects.size() + " projet(s).");
            msgLabel.setStyle("-fx-text-fill: #059669;");
            if (statsPanel != null && statsPanel.isVisible()) {
                loadStats();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur chargement projets.");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    public void addProjet() {
        if (!validateForm(false)) {
            msgLabel.setText("⚠️ Veuillez corriger les erreurs");
            msgLabel.setStyle("-fx-text-fill: #f59e0b;");
            return;
        }

        try {
            Projet p = new Projet(
                    titreField.getText().trim(),
                    descField.getText(),
                    statutCombo.getValue(),
                    (int) Session.getCurrentUser().getId(),
                    responsableCombo.getValue().getId(),
                    dateDebutPicker.getValue(),
                    dateFinPicker.getValue()
            );

            projetService.addProjet(p);
            refresh();
            clearForm();
            msgLabel.setText("✅ Projet ajouté avec succès");
            showToast("✅ Projet ajouté avec succès", "success");
            msgLabel.setStyle("-fx-text-fill: #059669;");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur ajout projet.");
            showToast("❌ Erreur lors de l'ajout du projet", "error");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    public void updateProjet() {
        Projet selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("⚠️ Sélectionnez un projet à modifier");
            msgLabel.setStyle("-fx-text-fill: #f59e0b;");
            return;
        }

        if (!validateForm(true)) {
            msgLabel.setText("⚠️ Veuillez corriger les erreurs");
            msgLabel.setStyle("-fx-text-fill: #f59e0b;");
            return;
        }

        try {
            Projet p = new Projet(
                    selected.getId(),
                    titreField.getText().trim(),
                    descField.getText(),
                    statutCombo.getValue(),
                    (int) Session.getCurrentUser().getId(),
                    responsableCombo.getValue().getId(),
                    dateDebutPicker.getValue(),
                    dateFinPicker.getValue()
            );

            projetService.updateProjet(p);
            refresh();
            msgLabel.setText("✅ Projet modifié avec succès");
            msgLabel.setStyle("-fx-text-fill: #059669;");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur modification projet.");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    public void deleteSelected() {
        Projet selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("⚠️ Sélectionnez un projet à supprimer");
            msgLabel.setStyle("-fx-text-fill: #f59e0b;");
            return;
        }

        // Confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le projet");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer le projet \"" + selected.getTitre() + "\" ?");

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-radius: 15; -fx-padding: 20;");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

        if (okButton != null) {
            okButton.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 25; -fx-font-weight: 600; -fx-cursor: hand;");
        }
        if (cancelButton != null) {
            cancelButton.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 20; -fx-padding: 8 25; -fx-font-weight: 600; -fx-cursor: hand; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 20;");
        }

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                projetService.deleteProjet(selected.getId());
                refresh();
                clearForm();
                msgLabel.setText("✅ Projet supprimé avec succès");
                msgLabel.setStyle("-fx-text-fill: #059669;");
            } catch (SQLException e) {
                e.printStackTrace();
                msgLabel.setText("❌ Erreur suppression projet.");
                msgLabel.setStyle("-fx-text-fill: #dc2626;");
            }
        }
    }

    // generate report PDF
    @FXML
    public void generateReport() {
        Projet selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("⚠️ Sélectionnez un projet pour générer le rapport");
            msgLabel.setStyle("-fx-text-fill: #f59e0b;");
            return;
        }

        try {
            // Show loading
            msgLabel.setText("⏳ Génération du rapport PDF...");
            msgLabel.setStyle("-fx-text-fill: #3b82f6;");

            // Build the HTML template with project data
            String htmlTemplate = buildHtmlReport(selected);

            // Create JSON payload
            String jsonPayload = String.format("""
            {
                "html": "%s",
                "options": {
                    "format": "A4",
                    "landscape": true,
                    "margin": "0mm",
                    "printBackground": true,
                    "preferCSSPageSize": true
                }
            }
            """, escapeJson(htmlTemplate));

            // Send request in background thread
            new Thread(() -> {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://apdf.io/api/pdf/file/create"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer D7888x8HgO4iHkwt29SkAPohnc9HC0AzwySEbYA3031f96a7") // Replace with your token
                            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        // Parse JSON response
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(response.body());
                        String pdfUrl = root.get("file").asText();

                        Platform.runLater(() -> {
                            try {
                                // Open in browser
                                java.awt.Desktop.getDesktop().browse(new URI(pdfUrl));
                                msgLabel.setText("✅ Rapport généré! Ouverture dans le navigateur...");
                                msgLabel.setStyle("-fx-text-fill: #059669;");
                            } catch (Exception e) {
                                msgLabel.setText("✅ PDF disponible à l'URL: " + pdfUrl);
                                msgLabel.setStyle("-fx-text-fill: #059669;");
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            msgLabel.setText("❌ Erreur API: " + response.statusCode());
                            msgLabel.setStyle("-fx-text-fill: #dc2626;");
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        msgLabel.setText("❌ Erreur: " + e.getMessage());
                        msgLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur: " + e.getMessage());
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private String buildHtmlReport(Projet projet) {
        try {
            // Validate project
            if (projet == null) throw new IllegalArgumentException("Projet is null");

            // Get tasks for this project
            List<Tache> tasks = tacheService.findByProjetId(projet.getId());
            if (tasks == null) tasks = List.of(); // avoid null

            // Calculate statistics safely
            long totalTasks = tasks.size();
            long todoCount = tasks.stream().filter(t -> "TODO".equals(t.getStatut())).count();
            long doingCount = tasks.stream().filter(t -> "DOING".equals(t.getStatut())).count();
            long doneCount = tasks.stream().filter(t -> "DONE".equals(t.getStatut())).count();
            double completionRate = totalTasks > 0 ? (doneCount * 100.0 / totalTasks) : 0;

            // Get responsible name (safe)
            String responsableName = getResponsibleName(projet.getResponsableEmployeId());
            if (responsableName == null) responsableName = "Non défini";

            // Format dates safely
            String startDate = projet.getDateDebut() != null
                    ? projet.getDateDebut().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                    : "Non définie";
            String endDate = projet.getDateFin() != null
                    ? projet.getDateFin().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                    : "Non définie";

            // Description safe
            String description = projet.getDescription() != null ? projet.getDescription() : "-";

            // Unique employee count
            long uniqueEmployees = tasks.stream().map(Tache::getEmployeId).distinct().count();

            // Task rows
            String taskRows = buildTaskRows(tasks);

            // Generation timestamp


            String generationDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            // Build HTML with clean data
            return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Rapport Projet - %s</title>
            <style>
                @page { size: A4; margin: 2cm; }
                body { font-family: 'Helvetica', Arial, sans-serif; line-height: 1.5; color: #333; }
                .header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #1a5f7a; padding-bottom: 10px; }
                .header h1 { color: #1a5f7a; font-size: 28px; margin: 0; }
                .header p { color: #666; font-size: 14px; margin: 5px 0 0; }
                .section { margin-bottom: 25px; }
                .section-title { font-size: 18px; font-weight: 600; color: #1a5f7a; border-left: 5px solid #1a5f7a; padding-left: 10px; margin-bottom: 15px; }
                .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
                .info-item { background: #f8f9fa; padding: 12px; border-radius: 6px; }
                .info-label { font-size: 12px; color: #666; text-transform: uppercase; }
                .info-value { font-size: 16px; font-weight: 600; color: #222; margin-top: 4px; }
                .stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin-bottom: 10px; }
                .stat-card { background: #f0f7fa; padding: 15px; border-radius: 8px; text-align: center; border-bottom: 3px solid #1a5f7a; }
                .stat-number { font-size: 28px; font-weight: 700; color: #1a5f7a; line-height: 1.2; }
                .stat-label { font-size: 13px; color: #555; text-transform: uppercase; }
                .stat-detail { display: flex; justify-content: space-between; margin-top: 15px; background: #f8f9fa; padding: 10px; border-radius: 6px; }
                .detail-item { text-align: center; flex: 1; }
                .detail-label { font-size: 11px; color: #777; }
                .detail-value { font-size: 18px; font-weight: 600; }
                .detail-value.todo { color: #f59e0b; }
                .detail-value.doing { color: #3b82f6; }
                .detail-value.done { color: #10b981; }
                table { width: 100%%; border-collapse: collapse; font-size: 12px; }
                th { background: #1a5f7a; color: white; padding: 10px; text-align: left; }
                td { padding: 8px 10px; border-bottom: 1px solid #ddd; }
                .status-badge { display: inline-block; padding: 3px 8px; border-radius: 12px; font-size: 11px; font-weight: 600; }
                .status-todo { background: #fff3e0; color: #f59e0b; }
                .status-doing { background: #e3f2fd; color: #3b82f6; }
                .status-done { background: #e8f5e9; color: #10b981; }
                .footer { margin-top: 30px; text-align: right; font-size: 11px; color: #999; border-top: 1px solid #eee; padding-top: 10px; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>RAPPORT DE PROJET</h1>
                <p>Généré le %s</p>
            </div>

            <div class="section">
                <div class="section-title">📋 INFORMATIONS GÉNÉRALES</div>
                <div class="info-grid">
                    <div class="info-item"><div class="info-label">Titre</div><div class="info-value">%s</div></div>
                    <div class="info-item"><div class="info-label">Statut</div><div class="info-value">%s</div></div>
                    <div class="info-item"><div class="info-label">Responsable</div><div class="info-value">%s</div></div>
                    <div class="info-item"><div class="info-label">Période</div><div class="info-value">%s - %s</div></div>
                </div>
                <div style="margin-top: 15px; background: #f8f9fa; padding: 12px; border-radius: 6px;">
                    <div class="info-label">Description</div>
                    <div style="font-size: 14px; color: #444;">%s</div>
                </div>
            </div>

            <div class="section">
                <div class="section-title">📊 STATISTIQUES</div>
                <div class="stats-grid">
                    <div class="stat-card"><div class="stat-number">%d</div><div class="stat-label">Total tâches</div></div>
                    <div class="stat-card"><div class="stat-number">%.1f%%</div><div class="stat-label">Taux complétion</div></div>
                    <div class="stat-card"><div class="stat-number">%d</div><div class="stat-label">Employés</div></div>
                </div>
                <div class="stat-detail">
                    <div class="detail-item"><div class="detail-label">À faire</div><div class="detail-value todo">%d</div></div>
                    <div class="detail-item"><div class="detail-label">En cours</div><div class="detail-value doing">%d</div></div>
                    <div class="detail-item"><div class="detail-label">Terminées</div><div class="detail-value done">%d</div></div>
                </div>
            </div>

            <div class="section">
                <div class="section-title">📋 LISTE DES TÂCHES</div>
                <table>
                    <thead><tr><th>Titre</th><th>Statut</th><th>Assigné à</th><th>Description</th></tr></thead>
                    <tbody>%s</tbody>
                </table>
            </div>

            <div class="footer">Rapport généré par RH Pro</div>
        </body>
        </html>
        """,
                    projet.getTitre(),
                    generationDate,
                    projet.getTitre(),
                    projet.getStatut(),
                    responsableName,
                    startDate, endDate,
                    description,
                    totalTasks,
                    completionRate,
                    uniqueEmployees,
                    todoCount,
                    doingCount,
                    doneCount,
                    taskRows
            );
        } catch (Exception e) {
            e.printStackTrace(); // This will print the exact error in your terminal
            // Return a diagnostic HTML
            return "<html><body><h3>Erreur lors de la génération du rapport</h3><p>"
                    + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + "</p></body></html>";
        }
    }
    private String buildTaskRows(List<Tache> tasks) {
        if (tasks == null) return "";
        StringBuilder rows = new StringBuilder();
        for (Tache task : tasks) {
            if (task == null) continue;
            String status = task.getStatut() != null ? task.getStatut() : "INCONNU";
            String statusClass = switch (status) {
                case "TODO" -> "status-todo";
                case "DOING" -> "status-doing";
                case "DONE" -> "status-done";
                default -> "";
            };
            rows.append(String.format("""
        <tr>
            <td>%s</td>
            <td><span class="status-badge %s">%s</span></td>
            <td>%s</td>
            <td>%s</td>
        </tr>
        """,
                    escapeHtml(task.getTitre()),
                    statusClass,
                    escapeHtml(status),
                    escapeHtml(getEmployeeName(task.getEmployeId())),
                    escapeHtml(task.getDescription() != null ? task.getDescription() : "-")
            ));
        }
        return rows.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Add a simple HTML escape for safety (optional)

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Helper methods you already have
    private String getResponsibleName(int responsableId) {
        try {
            return userService.getData().stream()
                    .filter(u -> u.getId() == responsableId)
                    .map(u -> u.getNom() + " " + u.getPrenom())
                    .findFirst()
                    .orElse("Inconnu");
        } catch (Exception e) {
            return "Inconnu";
        }
    }

    private String getEmployeeName(int employeeId) {
        if (employeeId == 0) return "Non assigné";
        try {
            return userService.getData().stream()
                    .filter(u -> u.getId() == employeeId)
                    .map(u -> u.getNom() + " " + u.getPrenom())
                    .findFirst()
                    .orElse("Inconnu");
        } catch (Exception e) {
            return "Inconnu";
        }
    }



    /**
     * Shows a toast notification (temporary popup message)
     */
    private void showToast(String message, String type) {
        // Create a popup
        Popup popup = new Popup();

        // Create toast content
        Label toastLabel = new Label(message);
        toastLabel.setStyle(getToastStyle(type));
        toastLabel.setPadding(new Insets(12, 24, 12, 24));
        toastLabel.setMaxWidth(400);
        toastLabel.setWrapText(true);
        toastLabel.setAlignment(Pos.CENTER);

        // Add to popup
        popup.getContent().add(toastLabel);

        // Position at bottom-right of the window
        Scene scene = table.getScene();
        Window window = scene.getWindow();

        popup.show(window,
                window.getX() + window.getWidth() - toastLabel.getWidth() - 40,
                window.getY() + window.getHeight() - toastLabel.getHeight() - 60
        );

        // Auto-hide after 3 seconds
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> popup.hide());
        delay.play();
    }

    /**
     * Returns CSS style for toast based on type
     */
    private String getToastStyle(String type) {
        String baseStyle = "-fx-background-radius: 25; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);";

        switch (type) {
            case "success":
                return baseStyle + " -fx-background-color: #10b981;";
            case "error":
                return baseStyle + " -fx-background-color: #ef4444;";
            case "warning":
                return baseStyle + " -fx-background-color: #f59e0b;";
            case "info":
                return baseStyle + " -fx-background-color: #3b82f6;";
            default:
                return baseStyle + " -fx-background-color: #6b7280;";
        }
    }


    /**
     * Shows a custom confirmation dialog
     */
    private boolean showConfirmDialog(String title, String message) {
        // Create dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.initOwner(table.getScene().getWindow());

        // Set dialog style
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 25;");
        dialogPane.getStylesheets().add(getClass().getResource("/styles/dialog.css").toExternalForm());

        // Add content
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);

        // Icon
        Label iconLabel = new Label("⚠️");
        iconLabel.setStyle("-fx-font-size: 48px;");

        // Message
        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-weight: 500;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(300);
        msgLabel.setAlignment(Pos.CENTER);

        content.getChildren().addAll(iconLabel, msgLabel);
        dialogPane.setContent(content);

        // Add buttons with custom style
        ButtonType confirmType = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialogPane.getButtonTypes().addAll(confirmType, cancelType);

        // Style buttons
        Button confirmButton = (Button) dialogPane.lookupButton(confirmType);
        Button cancelButton = (Button) dialogPane.lookupButton(cancelType);

        if (confirmButton != null) {
            confirmButton.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 10 25; -fx-font-weight: bold; -fx-cursor: hand;");
        }
        if (cancelButton != null) {
            cancelButton.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-background-radius: 25; -fx-padding: 10 25; -fx-font-weight: bold; -fx-cursor: hand;");
        }

        // Show dialog and return result
        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == confirmType;
    }
}