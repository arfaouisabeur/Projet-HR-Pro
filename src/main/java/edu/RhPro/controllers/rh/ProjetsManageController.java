package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Projet;
import edu.RhPro.entities.User;
import edu.RhPro.services.ProjetService;
import edu.RhPro.services.UserService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private final ProjetService projetService = new ProjetService();
    private final UserService userService = new UserService();
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
        });

        // Add real-time validation listeners
        addValidationListeners();

        // Initial load
        refresh();
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
            msgLabel.setStyle("-fx-text-fill: #059669;");
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur ajout projet.");
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
}