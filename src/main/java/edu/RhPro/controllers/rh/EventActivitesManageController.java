package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Activite;
import edu.RhPro.entities.Evenement;
import edu.RhPro.services.ActiviteService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.util.List;
import java.util.stream.Collectors;

public class EventActivitesManageController {

    @FXML private Label titleLabel;
    @FXML private TableView<Activite> table;
    @FXML private TableColumn<Activite, String> colId, colTitre, colDesc;

    @FXML private TextField titreField;
    @FXML private TextArea descArea;
    @FXML private Label msgLabel;

    // Error labels
    @FXML private Label titreError;
    @FXML private Label descError;

    private final ActiviteService service = new ActiviteService();
    private Evenement evenement;

    public void setEvenement(Evenement e) {
        this.evenement = e;
        titleLabel.setText("Activités - " + e.getTitre());
        refresh();
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription() == null ? "" : c.getValue().getDescription()));

        // Initialize error labels if they exist in FXML
        // If not, they will be null and we'll handle that
        clearErrors();

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, a) -> {
            if (a != null) {
                clearErrors();
                titreField.setText(a.getTitre());
                descArea.setText(a.getDescription());
            }
        });

        // Add real-time validation listeners
        setupValidationListeners();
    }

    private void setupValidationListeners() {
        // Real-time validation for titre field
        titreField.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty() && titreError != null) {
                titreError.setManaged(false);
                titreField.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1;");
            }
        });

        // Real-time validation for description field
        descArea.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty() && descError != null) {
                descError.setManaged(false);
                descArea.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1;");
            }
        });
    }

    private void clearErrors() {
        if (titreError != null) {
            titreError.setText("");
            titreError.setManaged(false);
        }
        if (descError != null) {
            descError.setText("");
            descError.setManaged(false);
        }

        // Reset field styles
        if (titreField != null) {
            titreField.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1;");
        }
        if (descArea != null) {
            descArea.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1;");
        }
    }

    private void setFieldError(TextField field, Label errorLabel, String message) {
        if (field != null) {
            field.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2;");
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setManaged(true);
        }
    }

    private void setFieldError(TextArea field, Label errorLabel, String message) {
        if (field != null) {
            field.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2;");
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setManaged(true);
        }
    }

    private boolean validateInputs() {
        clearErrors();
        boolean isValid = true;

        // Validate Titre
        String titre = titreField.getText();
        if (titre == null || titre.trim().isEmpty()) {
            setFieldError(titreField, titreError, "Le titre est obligatoire");
            isValid = false;
        } else if (titre.trim().length() < 3) {
            setFieldError(titreField, titreError, "Le titre doit contenir au moins 3 caractères");
            isValid = false;
        } else if (titre.trim().length() > 100) {
            setFieldError(titreField, titreError, "Le titre ne peut pas dépasser 100 caractères");
            isValid = false;
        }

        // Validate Description
        String description = descArea.getText();
        if (description == null || description.trim().isEmpty()) {
            setFieldError(descArea, descError, "La description est obligatoire");
            isValid = false;
        } else if (description.trim().length() < 5) {
            setFieldError(descArea, descError, "La description doit contenir au moins 5 caractères");
            isValid = false;
        }

        // Check for duplicate activity in the same event
        if (isValid && evenement != null) {
            try {
                List<Activite> all = service.getData();
                List<Activite> eventActivities = all.stream()
                        .filter(a -> a.getEvenementId() == evenement.getId())
                        .collect(Collectors.toList());

                Activite selected = table.getSelectionModel().getSelectedItem();
                String titreTrimmed = titre.trim();

                for (Activite a : eventActivities) {
                    // Skip if it's the same activity being updated
                    if (selected != null && a.getId() == selected.getId()) continue;

                    if (a.getTitre().equalsIgnoreCase(titreTrimmed)) {
                        setFieldError(titreField, titreError,
                                "Une activité avec ce titre existe déjà pour cet événement");
                        isValid = false;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!isValid) {
            msgLabel.setText("❌ Veuillez corriger les erreurs");
            msgLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
        }

        return isValid;
    }

    @FXML
    public void refresh() {
        if (evenement == null) return;
        try {
            List<Activite> all = service.getData();
            List<Activite> list = all.stream()
                    .filter(a -> a.getEvenementId() == evenement.getId())
                    .collect(Collectors.toList());

            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setText("✅ " + list.size() + " activité(s)");
            msgLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: 600;");
            clearErrors();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur chargement");
        }
    }

    @FXML
    public void add() {
        if (evenement == null) {
            showError("Événement non sélectionné");
            return;
        }

        if (!validateInputs()) {
            return;
        }

        try {
            String titre = titreField.getText().trim();
            String description = descArea.getText().trim();

            Activite a = new Activite(titre, description, evenement.getId());
            service.addEntity(a);

            showSuccess("✅ Activité ajoutée avec succès");
            refresh();
            clearForm();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur lors de l'ajout");
        }
    }

    @FXML
    public void update() {
        Activite selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("⚠️ Veuillez sélectionner une activité");
            return;
        }

        if (!validateInputs()) {
            return;
        }

        try {
            String titre = titreField.getText().trim();
            String description = descArea.getText().trim();

            selected.setTitre(titre);
            selected.setDescription(description);
            service.updateEntity(selected);

            showSuccess("✅ Activité modifiée avec succès");
            refresh();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur lors de la modification");
        }
    }

    @FXML
    public void delete() {
        Activite selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("⚠️ Veuillez sélectionner une activité");
            return;
        }

        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'activité");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + selected.getTitre() + "\" ?");

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
                service.deleteEntity(selected);
                showSuccess("✅ Activité supprimée");
                refresh();
                clearForm();
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Erreur lors de la suppression");
            }
        }
    }

    @FXML
    public void clearForm() {
        table.getSelectionModel().clearSelection();
        titreField.clear();
        descArea.clear();
        clearErrors();
        msgLabel.setText("");
    }

    private void showError(String message) {
        msgLabel.setText("❌ " + message);
        msgLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
    }

    private void showWarning(String message) {
        msgLabel.setText("⚠️ " + message);
        msgLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: 600;");
    }

    private void showSuccess(String message) {
        msgLabel.setText(message);
        msgLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: 600;");
    }
}