package edu.RhPro.controllers.employe;

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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ProjetTachesController {

    @FXML private Label titleLabel;
    @FXML private TextField titreField;
    @FXML private TextField descField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private ComboBox<User> employeCombo;

    // Kanban columns
    @FXML private VBox todoColumn;
    @FXML private VBox doingColumn;
    @FXML private VBox doneColumn;

    // Column counters
    @FXML private Label todoCount;
    @FXML private Label doingCount;
    @FXML private Label doneCount;

    @FXML private Label msgLabel;
    @FXML private Label titreErrorLabel;
    @FXML private Label statutErrorLabel;
    @FXML private Label employeErrorLabel;
    @FXML private TextField searchField;

    // Form mode
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Projet projet;
    private final ProjetService projetService = new ProjetService();
    private final TacheService tacheService = new TacheService();
    private final UserService userService = new UserService();
    private List<User> allEmployees;
    private ObservableList<Tache> allTasks;

    private Tache currentEditingTask = null; // null means add mode, not null means update mode
    private VBox currentlySelectedCard = null; // Track selected card for visual feedback

    // Style constants
    private final String normalFieldStyle = "-fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;";
    private final String errorFieldStyle = "-fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 14; -fx-background-radius: 14;";
    private final String selectedCardStyle = "-fx-background-color: #e0f2fe; " +
            "-fx-background-radius: 16; " +
            "-fx-padding: 15; " +
            "-fx-border-color: #3b82f6; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 16;";

    public void setProjet(Projet p) {
        this.projet = p;
        titleLabel.setText("📋 " + p.getTitre() + " - Gestion des tâches");
        refresh();
    }

    @FXML
    public void initialize() {
        // Setup statut combo
        statutCombo.setItems(FXCollections.observableArrayList("TODO", "DOING", "DONE"));

        // Setup employe combo
        setupEmployeComboBox();
        loadEmployees();

        // Setup search
        setupSearch();

        // Add validation listeners
        addValidationListeners();

        // Initially hide cancel button
        if (cancelButton != null) {
            cancelButton.setVisible(false);
            cancelButton.setManaged(false);
        }

        if (saveButton != null) {
            saveButton.setText("Ajouter");
        }
    }

    private void setupEmployeComboBox() {
        employeCombo.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                if (user == null) return "";
                return user.getNom() + " " + user.getPrenom();
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });

        employeCombo.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getNom() + " " + user.getPrenom());
                }
            }
        });
    }

    private void loadEmployees() {
        try {
            allEmployees = userService.getData().stream()
                    .filter(user -> "EMPLOYE".equalsIgnoreCase(user.getRole()))
                    .collect(Collectors.toList());
            employeCombo.setItems(FXCollections.observableArrayList(allEmployees));
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur chargement employés");
        }
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, newVal) -> {
                filterTasks(newVal);
            });
        }
    }

    private void filterTasks(String searchText) {
        if (allTasks == null) return;

        if (searchText == null || searchText.isEmpty()) {
            renderKanban(allTasks);
        } else {
            List<Tache> filtered = allTasks.stream()
                    .filter(t -> t.getTitre().toLowerCase().contains(searchText.toLowerCase()) ||
                            (t.getDescription() != null && t.getDescription().toLowerCase().contains(searchText.toLowerCase())))
                    .collect(Collectors.toList());
            renderKanban(FXCollections.observableArrayList(filtered));
        }
    }

    private void renderKanban(ObservableList<Tache> tasks) {
        // Clear existing cards (keep the column headers from FXML)
        todoColumn.getChildren().clear();
        doingColumn.getChildren().clear();
        doneColumn.getChildren().clear();

        // Filter tasks by status
        List<Tache> todoTasks = tasks.stream()
                .filter(t -> "TODO".equals(t.getStatut()))
                .collect(Collectors.toList());
        List<Tache> doingTasks = tasks.stream()
                .filter(t -> "DOING".equals(t.getStatut()))
                .collect(Collectors.toList());
        List<Tache> doneTasks = tasks.stream()
                .filter(t -> "DONE".equals(t.getStatut()))
                .collect(Collectors.toList());

        // Update counters
        todoCount.setText(todoTasks.size() + " tâche(s)");
        doingCount.setText(doingTasks.size() + " tâche(s)");
        doneCount.setText(doneTasks.size() + " tâche(s)");

        // Add tasks to columns
        todoTasks.forEach(task -> todoColumn.getChildren().add(createTaskCard(task)));
        doingTasks.forEach(task -> doingColumn.getChildren().add(createTaskCard(task)));
        doneTasks.forEach(task -> doneColumn.getChildren().add(createTaskCard(task)));

        // Add empty state messages
        if (todoTasks.isEmpty()) addEmptyState(todoColumn, "Aucune tâche à faire");
        if (doingTasks.isEmpty()) addEmptyState(doingColumn, "Aucune tâche en cours");
        if (doneTasks.isEmpty()) addEmptyState(doneColumn, "Aucune tâche terminée");
    }

    private VBox createTaskCard(Tache task) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 16; " +
                "-fx-padding: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setUserData(task); // Store task in card for later reference

        // Click to select
        card.setOnMouseClicked(e -> {
            // Deselect previous card
            if (currentlySelectedCard != null) {
                resetCardStyle(currentlySelectedCard);
            }

            // Select this card
            card.setStyle(selectedCardStyle);
            currentlySelectedCard = card;

            // Double-click to edit
            if (e.getClickCount() == 2) {
                editTask(task);
            }
        });

        // Get employee name
        String employeeName = "Non assigné";
        try {
            employeeName = userService.getData().stream()
                    .filter(u -> u.getId() == task.getEmployeId())
                    .map(u -> u.getNom() + " " + u.getPrenom())
                    .findFirst()
                    .orElse("Inconnu");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Employee badge
        Label employeeBadge = new Label("👤 " + employeeName);
        employeeBadge.setStyle("-fx-background-color: #f3f4f6; " +
                "-fx-text-fill: #4b5563; " +
                "-fx-background-radius: 12; " +
                "-fx-padding: 4 12; " +
                "-fx-font-size: 12px;");
        card.getChildren().add(employeeBadge);

        // Task title
        Label titleLabel = new Label(task.getTitre());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        titleLabel.setTextFill(Color.web("#111827"));
        titleLabel.setWrapText(true);
        card.getChildren().add(titleLabel);

        // Description (if exists)
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            Label descLabel = new Label(task.getDescription());
            descLabel.setFont(Font.font("System", 13));
            descLabel.setTextFill(Color.web("#6b7280"));
            descLabel.setWrapText(true);
            card.getChildren().add(descLabel);
        }

        // Footer with actions
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label idLabel = new Label("#" + task.getId());
        idLabel.setFont(Font.font("System", 12));
        idLabel.setTextFill(Color.web("#9ca3af"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Action buttons
        HBox actions = new HBox(5);

        // Edit button
        Button editBtn = createActionButton("✎", "#3b82f6", "Modifier");
        editBtn.setOnAction(e -> {
            editTask(task);
            // Select this card
            if (currentlySelectedCard != null) {
                resetCardStyle(currentlySelectedCard);
            }
            card.setStyle(selectedCardStyle);
            currentlySelectedCard = card;
        });
        actions.getChildren().add(editBtn);

        // Status change buttons (only if not in DONE column)
        if (!"DONE".equals(task.getStatut())) {
            if ("TODO".equals(task.getStatut())) {
                Button startBtn = createActionButton("▶", "#f59e0b", "Commencer");
                startBtn.setOnAction(e -> updateTaskStatus(task, "DOING"));
                actions.getChildren().add(startBtn);
            } else if ("DOING".equals(task.getStatut())) {
                Button doneBtn = createActionButton("✓", "#10b981", "Terminer");
                doneBtn.setOnAction(e -> updateTaskStatus(task, "DONE"));
                actions.getChildren().add(doneBtn);
            }
        }

        footer.getChildren().addAll(idLabel, spacer, actions);
        card.getChildren().add(footer);

        return card;
    }

    private void resetCardStyle(VBox card) {
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 16; " +
                "-fx-padding: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
    }

    private Button createActionButton(String icon, String color, String tooltip) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 12; " +
                "-fx-padding: 5 10; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand;");

        Tooltip tp = new Tooltip(tooltip);
        tp.setStyle("-fx-font-size: 11px;");
        Tooltip.install(btn, tp);

        return btn;
    }

    private void addEmptyState(VBox column, String message) {
        VBox emptyBox = new VBox();
        emptyBox.setStyle("-fx-background-color: #f9fafb; " +
                "-fx-background-radius: 16; " +
                "-fx-padding: 30; " +
                "-fx-border-color: #e5e7eb; " +
                "-fx-border-style: dashed; " +
                "-fx-border-radius: 16;");
        emptyBox.setAlignment(Pos.CENTER);

        Label emptyLabel = new Label(message);
        emptyLabel.setFont(Font.font("System", 13));
        emptyLabel.setTextFill(Color.web("#9ca3af"));

        emptyBox.getChildren().add(emptyLabel);
        column.getChildren().add(emptyBox);
    }

    private void editTask(Tache task) {
        if (!isResponsible()) {
            showError("Seul le responsable peut modifier les tâches");
            return;
        }

        currentEditingTask = task;

        // Fill form with task data
        titreField.setText(task.getTitre());
        descField.setText(task.getDescription());
        statutCombo.setValue(task.getStatut());

        // Find and select employee
        try {
            User assignedUser = userService.getData().stream()
                    .filter(u -> u.getId() == task.getEmployeId())
                    .findFirst()
                    .orElse(null);
            employeCombo.setValue(assignedUser);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Change button text and show cancel
        if (saveButton != null) {
            saveButton.setText("Modifier");
        }
        if (cancelButton != null) {
            cancelButton.setVisible(true);
            cancelButton.setManaged(true);
        }

        msgLabel.setText("Mode édition - Modification de la tâche #" + task.getId());
    }

    @FXML
    public void cancelEdit() {
        clearForm();
        currentEditingTask = null;

        if (saveButton != null) {
            saveButton.setText("Ajouter");
        }
        if (cancelButton != null) {
            cancelButton.setVisible(false);
            cancelButton.setManaged(false);
        }

        msgLabel.setText("Édition annulée");
    }

    private void addValidationListeners() {
        titreField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                hideError(titreField, titreErrorLabel);
            }
        });

        statutCombo.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                hideError(statutCombo, statutErrorLabel);
            }
        });

        employeCombo.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                hideError(employeCombo, employeErrorLabel);
            }
        });
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

    private boolean validateForm() {
        boolean isValid = true;

        if (titreField.getText() == null || titreField.getText().trim().isEmpty()) {
            showError(titreField, titreErrorLabel, "Le titre est requis");
            isValid = false;
        } else {
            hideError(titreField, titreErrorLabel);
        }

        if (statutCombo.getValue() == null) {
            showError(statutCombo, statutErrorLabel, "Le statut est requis");
            isValid = false;
        } else {
            hideError(statutCombo, statutErrorLabel);
        }

        if (employeCombo.getValue() == null) {
            showError(employeCombo, employeErrorLabel, "L'employé est requis");
            isValid = false;
        } else {
            hideError(employeCombo, employeErrorLabel);
        }

        return isValid;
    }

    private void clearForm() {
        titreField.clear();
        descField.clear();
        statutCombo.setValue(null);
        employeCombo.setValue(null);
        hideError(titreField, titreErrorLabel);
        hideError(statutCombo, statutErrorLabel);
        hideError(employeCombo, employeErrorLabel);
    }

    @FXML
    public void saveTask() {
        if (projet == null) return;

        if (!isResponsible()) {
            showError("Seul le responsable du projet peut gérer les tâches.");
            return;
        }

        if (!validateForm()) {
            showError("Veuillez corriger les erreurs");
            return;
        }

        try {
            if (currentEditingTask == null) {
                // ADD mode
                Tache t = new Tache(
                        titreField.getText().trim(),
                        descField.getText(),
                        statutCombo.getValue(),
                        projet.getId(),
                        employeCombo.getValue().getId(),
                        null
                );
                tacheService.addTache(t);
                showSuccess("Tâche ajoutée avec succès");
            } else {
                // UPDATE mode
                currentEditingTask.setTitre(titreField.getText().trim());
                currentEditingTask.setDescription(descField.getText());
                currentEditingTask.setStatut(statutCombo.getValue());
                currentEditingTask.setEmployeId(employeCombo.getValue().getId());

                tacheService.updateFullTache(currentEditingTask);
                showSuccess("Tâche modifiée avec succès");
            }

            // Reset to add mode
            currentEditingTask = null;
            if (saveButton != null) saveButton.setText("Ajouter");
            if (cancelButton != null) {
                cancelButton.setVisible(false);
                cancelButton.setManaged(false);
            }

            clearForm();
            refresh();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur sauvegarde tâche.");
        }
    }

    @FXML
    public void refresh() {
        if (projet == null) return;
        try {
            List<Tache> data = tacheService.findByProjetId(projet.getId());
            allTasks = FXCollections.observableArrayList(data);
            renderKanban(allTasks);
            msgLabel.setText(data.size() + " tâche(s)");

            // Clear selection
            currentlySelectedCard = null;
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur chargement tâches.");
        }
    }

    @FXML
    public void deleteSelected() {
        if (currentlySelectedCard == null) {
            showError("Sélectionnez une tâche.");
            return;
        }

        Tache selectedTask = (Tache) currentlySelectedCard.getUserData();
        if (selectedTask == null) return;

        if (!isResponsible()) {
            showError("Seul le responsable peut supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la tâche");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette tâche ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                tacheService.deleteTache(selectedTask.getId());

                // If we were editing this task, cancel edit mode
                if (currentEditingTask != null && currentEditingTask.getId() == selectedTask.getId()) {
                    cancelEdit();
                }

                refresh();
                showSuccess("Tâche supprimée");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Erreur suppression.");
            }
        }
    }

    private void updateTaskStatus(Tache task, String newStatus) {
        if (!isResponsible()) {
            showError("Seul le responsable peut changer le statut.");
            return;
        }

        try {
            task.setStatut(newStatus);
            tacheService.updateTache(task);
            refresh();
            showSuccess("Tâche " + newStatus);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur mise à jour.");
        }
    }

    @FXML
    public void markDone() {
        if (currentlySelectedCard == null) {
            showError("Sélectionnez une tâche.");
            return;
        }

        Tache selectedTask = (Tache) currentlySelectedCard.getUserData();
        if (selectedTask != null) {
            updateTaskStatus(selectedTask, "DONE");
        }
    }

    private void showError(String message) {
        msgLabel.setText(message);
        msgLabel.setStyle("-fx-text-fill: #dc2626;");
    }

    private void showSuccess(String message) {
        msgLabel.setText(message);
        msgLabel.setStyle("-fx-text-fill: #10b981;");
    }
}