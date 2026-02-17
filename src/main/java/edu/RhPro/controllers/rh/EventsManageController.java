package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Evenement;
import edu.RhPro.entities.User;
import edu.RhPro.services.EvenementService;
import edu.RhPro.utils.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventsManageController {

    @FXML private TableView<Evenement> table;
    @FXML private TableColumn<Evenement, String> colId, colTitre, colDebut, colFin, colLieu, colImage, colDesc;

    // Form fields
    @FXML private TextField titreField;
    @FXML private TextField lieuField;
    @FXML private TextField imageUrlField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private TextField heureDebutField;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField heureFinField;
    @FXML private TextArea descArea;

    // Error labels
    @FXML private Label titreError;
    @FXML private Label lieuError;
    @FXML private Label imageUrlError;
    @FXML private Label dateDebutError;
    @FXML private Label heureDebutError;
    @FXML private Label dateFinError;
    @FXML private Label heureFinError;
    @FXML private Label descError;
    @FXML private Label msgLabel;

    private final EvenementService service = new EvenementService();

    @FXML
    public void initialize() {
        setupTableColumns();

        if (table != null) {
            table.getSelectionModel().selectedItemProperty().addListener((obs, old, e) -> {
                if (e != null) {
                    clearErrors();
                    fillForm(e);
                }
            });
        }

        // Set default times
        if (heureDebutField != null && heureDebutField.getText().isBlank()) {
            heureDebutField.setText("09:00");
        }
        if (heureFinField != null && heureFinField.getText().isBlank()) {
            heureFinField.setText("17:00");
        }

        refresh();
    }

    private void setupTableColumns() {
        if (colId != null) colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        if (colTitre != null) colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        if (colDebut != null) colDebut.setCellValueFactory(c -> new SimpleStringProperty(formatDateTime(c.getValue().getDateDebut())));
        if (colFin != null) colFin.setCellValueFactory(c -> new SimpleStringProperty(formatDateTime(c.getValue().getDateFin())));
        if (colLieu != null) colLieu.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLieu() == null ? "" : c.getValue().getLieu()));
        if (colImage != null) colImage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getImageUrl() == null ? "" : c.getValue().getImageUrl()));
        if (colDesc != null) colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription() == null ? "" : c.getValue().getDescription()));
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private void fillForm(Evenement e) {
        titreField.setText(e.getTitre());
        lieuField.setText(e.getLieu());
        imageUrlField.setText(e.getImageUrl());
        descArea.setText(e.getDescription());

        if (e.getDateDebut() != null) {
            dateDebutPicker.setValue(e.getDateDebut().toLocalDate());
            heureDebutField.setText(e.getDateDebut().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (e.getDateFin() != null) {
            dateFinPicker.setValue(e.getDateFin().toLocalDate());
            heureFinField.setText(e.getDateFin().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    private long getCurrentUserId() {
        User u = Session.getCurrentUser();
        return u != null ? u.getId() : 0;
    }

    private LocalDateTime buildDateTime(LocalDate date, String time) {
        if (date == null || time == null || time.isBlank()) return null;
        try {
            return LocalDateTime.of(date, LocalTime.parse(time.trim()));
        } catch (Exception e) {
            return null;
        }
    }

    private void clearErrors() {
        // Clear all error messages
        titreError.setText("");
        lieuError.setText("");
        imageUrlError.setText("");
        dateDebutError.setText("");
        heureDebutError.setText("");
        dateFinError.setText("");
        heureFinError.setText("");
        descError.setText("");

        // Hide error labels
        titreError.setManaged(false);
        lieuError.setManaged(false);
        imageUrlError.setManaged(false);
        dateDebutError.setManaged(false);
        heureDebutError.setManaged(false);
        dateFinError.setManaged(false);
        heureFinError.setManaged(false);
        descError.setManaged(false);

        // Reset field styles
        resetFieldStyle(titreField);
        resetFieldStyle(lieuField);
        resetFieldStyle(imageUrlField);
        resetFieldStyle(dateDebutPicker);
        resetFieldStyle(heureDebutField);
        resetFieldStyle(dateFinPicker);
        resetFieldStyle(heureFinField);
        resetFieldStyle(descArea);
    }

    private void resetFieldStyle(Control field) {
        if (field != null) {
            field.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1;");
        }
    }

    private void setFieldError(Control field, Label errorLabel, String message) {
        if (field != null) {
            field.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2;");
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setManaged(true);
        }
    }

    private boolean validateAllFields() {
        clearErrors();
        List<String> errors = new ArrayList<>();
        boolean isValid = true;

        // Validate Titre
        String titre = titreField.getText();
        if (titre == null || titre.trim().isEmpty()) {
            setFieldError(titreField, titreError, "Le titre est obligatoire");
            isValid = false;
        }

        // Validate Lieu
        String lieu = lieuField.getText();
        if (lieu == null || lieu.trim().isEmpty()) {
            setFieldError(lieuField, lieuError, "Le lieu est obligatoire");
            isValid = false;
        }

        // Validate Image URL (just required, no format validation)
        String imageUrl = imageUrlField.getText();
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            setFieldError(imageUrlField, imageUrlError, "L'URL de l'image est obligatoire");
            isValid = false;
        }

        // Validate Description
        String description = descArea.getText();
        if (description == null || description.trim().isEmpty()) {
            setFieldError(descArea, descError, "La description est obligatoire");
            isValid = false;
        }

        // Validate Date Début
        LocalDate dateDebut = dateDebutPicker.getValue();
        if (dateDebut == null) {
            setFieldError(dateDebutPicker, dateDebutError, "La date de début est obligatoire");
            isValid = false;
        }

        // Validate Heure Début
        String heureDebut = heureDebutField.getText();
        if (heureDebut == null || heureDebut.trim().isEmpty()) {
            setFieldError(heureDebutField, heureDebutError, "L'heure de début est obligatoire");
            isValid = false;
        } else {
            try {
                LocalTime.parse(heureDebut.trim());
            } catch (Exception e) {
                setFieldError(heureDebutField, heureDebutError, "Format d'heure invalide (HH:MM)");
                isValid = false;
            }
        }

        // Validate Date Fin
        LocalDate dateFin = dateFinPicker.getValue();
        if (dateFin == null) {
            setFieldError(dateFinPicker, dateFinError, "La date de fin est obligatoire");
            isValid = false;
        }

        // Validate Heure Fin
        String heureFin = heureFinField.getText();
        if (heureFin == null || heureFin.trim().isEmpty()) {
            setFieldError(heureFinField, heureFinError, "L'heure de fin est obligatoire");
            isValid = false;
        } else {
            try {
                LocalTime.parse(heureFin.trim());
            } catch (Exception e) {
                setFieldError(heureFinField, heureFinError, "Format d'heure invalide (HH:MM)");
                isValid = false;
            }
        }

        // If basic validation passed, check date logic
        if (isValid) {
            LocalDateTime debut = buildDateTime(dateDebut, heureDebut);
            LocalDateTime fin = buildDateTime(dateFin, heureFin);

            if (debut != null && fin != null && (fin.isBefore(debut) || fin.isEqual(debut))) {
                setFieldError(dateFinPicker, dateFinError, "La date de fin doit être après la date de début");
                setFieldError(heureFinField, heureFinError, "");
                isValid = false;
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
        try {
            List<Evenement> list = service.getData();
            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setText("✅ " + list.size() + " événement(s) chargé(s)");
            msgLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: 600;");
            clearErrors();
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur lors du chargement");
            msgLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
        }
    }

    @FXML
    public void add() {
        if (!validateAllFields()) {
            return;
        }

        try {
            Evenement newEvent = new Evenement(
                    titreField.getText().trim(),
                    buildDateTime(dateDebutPicker.getValue(), heureDebutField.getText()),
                    buildDateTime(dateFinPicker.getValue(), heureFinField.getText()),
                    lieuField.getText().trim(),
                    descArea.getText().trim(),
                    imageUrlField.getText().trim(),
                    getCurrentUserId()
            );

            // Check for duplicates
            if (isDuplicateEvent(newEvent, null)) {
                showDuplicateWarning("ajouter", () -> {
                    try {
                        performAdd(newEvent);
                    } catch (SQLException ex) {
                        handleException(ex, "ajout");
                    }
                });
                return;
            }

            // Show confirmation dialog
            showConfirmationDialog(
                    "Confirmation d'ajout",
                    "Ajouter un nouvel événement",
                    "Êtes-vous sûr de vouloir ajouter l'événement \"" + newEvent.getTitre() + "\" ?",
                    () -> {
                        try {
                            performAdd(newEvent);
                        } catch (SQLException ex) {
                            handleException(ex, "ajout");
                        }
                    }
            );

        } catch (Exception ex) {
            handleException(ex, "ajout");
        }
    }

    private void performAdd(Evenement event) throws SQLException {
        service.addEntity(event);
        showSuccess("✅ Événement ajouté avec succès (ID=" + event.getId() + ")");
        refresh();
        clearForm();
    }

    @FXML
    public void update() {
        Evenement selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("⚠️ Veuillez sélectionner un événement à modifier");
            return;
        }

        if (!validateAllFields()) {
            return;
        }

        try {
            Evenement updatedEvent = new Evenement(
                    titreField.getText().trim(),
                    buildDateTime(dateDebutPicker.getValue(), heureDebutField.getText()),
                    buildDateTime(dateFinPicker.getValue(), heureFinField.getText()),
                    lieuField.getText().trim(),
                    descArea.getText().trim(),
                    imageUrlField.getText().trim(),
                    getCurrentUserId()
            );
            updatedEvent.setId(selected.getId());

            // Check for duplicates (excluding current event)
            if (isDuplicateEvent(updatedEvent, selected.getId())) {
                showDuplicateWarning("modifier", () -> {
                    try {
                        performUpdate(selected, updatedEvent);
                    } catch (SQLException ex) {
                        handleException(ex, "modification");
                    }
                });
                return;
            }

            // Show confirmation dialog
            showConfirmationDialog(
                    "Confirmation de modification",
                    "Modifier l'événement",
                    "Êtes-vous sûr de vouloir modifier l'événement \"" + selected.getTitre() + "\" ?",
                    () -> {
                        try {
                            performUpdate(selected, updatedEvent);
                        } catch (SQLException ex) {
                            handleException(ex, "modification");
                        }
                    }
            );

        } catch (Exception ex) {
            handleException(ex, "modification");
        }
    }

    private void performUpdate(Evenement selected, Evenement updatedEvent) throws SQLException {
        selected.setTitre(updatedEvent.getTitre());
        selected.setLieu(updatedEvent.getLieu());
        selected.setImageUrl(updatedEvent.getImageUrl());
        selected.setDescription(updatedEvent.getDescription());
        selected.setDateDebut(updatedEvent.getDateDebut());
        selected.setDateFin(updatedEvent.getDateFin());
        selected.setRhId(updatedEvent.getRhId());

        service.updateEntity(selected);
        showSuccess("✅ Événement modifié avec succès");
        refresh();
    }

    @FXML
    public void delete() {
        Evenement selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("⚠️ Veuillez sélectionner un événement à supprimer");
            return;
        }

        // Show confirmation dialog
        showConfirmationDialog(
                "Confirmation de suppression",
                "Supprimer l'événement",
                "Êtes-vous sûr de vouloir supprimer l'événement \"" + selected.getTitre() + "\" ?\n\nCette action est irréversible.",
                () -> {
                    try {
                        service.deleteEntity(selected);
                        showSuccess("✅ Événement supprimé");
                        refresh();
                        clearForm();
                    } catch (SQLException ex) {
                        handleException(ex, "suppression");
                    }
                }
        );
    }

    private boolean isDuplicateEvent(Evenement newEvent, Long excludeId) {
        try {
            List<Evenement> allEvents = service.getData();

            for (Evenement existing : allEvents) {
                // Skip if we're excluding this ID (for update)
                if (excludeId != null && existing.getId() == excludeId) continue;

                // Check for duplicates based on multiple criteria
                boolean sameTitre = existing.getTitre() != null &&
                        existing.getTitre().equalsIgnoreCase(newEvent.getTitre());
                boolean sameLieu = existing.getLieu() != null &&
                        existing.getLieu().equalsIgnoreCase(newEvent.getLieu());
                boolean sameDescription = existing.getDescription() != null &&
                        existing.getDescription().equalsIgnoreCase(newEvent.getDescription());
                boolean sameImage = existing.getImageUrl() != null &&
                        existing.getImageUrl().equals(newEvent.getImageUrl());
                boolean sameDate = existing.getDateDebut() != null &&
                        existing.getDateDebut().equals(newEvent.getDateDebut());

                // If multiple fields match, consider it a duplicate
                int matchCount = 0;
                if (sameTitre) matchCount++;
                if (sameLieu) matchCount++;
                if (sameDescription) matchCount++;
                if (sameImage) matchCount++;
                if (sameDate) matchCount++;

                if (matchCount >= 3) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void handleException(Exception ex, String action) {
        ex.printStackTrace();
        msgLabel.setText("❌ Erreur lors de la " + action);
        msgLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
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

    private void showConfirmationDialog(String title, String header, String content, Runnable onConfirm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-radius: 15; -fx-padding: 20;");

        // Style buttons
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

        if (okButton != null) {
            okButton.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 25; -fx-font-weight: 600; -fx-cursor: hand; -fx-border: none;");
        }
        if (cancelButton != null) {
            cancelButton.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 20; -fx-padding: 8 25; -fx-font-weight: 600; -fx-cursor: hand; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 20;");
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            onConfirm.run();
        }
    }

    private void showDuplicateWarning(String action, Runnable onConfirm) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention - Duplicat possible");
        alert.setHeaderText("Événement similaire détecté");
        alert.setContentText("Un événement avec des informations similaires existe déjà.\n\nVoulez-vous quand même " + action + " cet événement ?");

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-radius: 15; -fx-padding: 20;");

        // Add custom buttons
        ButtonType confirmButton = new ButtonType("Oui, " + action, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Non, annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(confirmButton, cancelButton);

        // Style buttons
        Button okBtn = (Button) dialogPane.lookupButton(confirmButton);
        Button cancelBtn = (Button) dialogPane.lookupButton(cancelButton);

        if (okBtn != null) {
            okBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 25; -fx-font-weight: 600; -fx-cursor: hand; -fx-border: none;");
        }
        if (cancelBtn != null) {
            cancelBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 20; -fx-padding: 8 25; -fx-font-weight: 600; -fx-cursor: hand; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 20;");
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == confirmButton) {
            onConfirm.run();
        }
    }

    @FXML
    public void clearForm() {
        table.getSelectionModel().clearSelection();
        titreField.clear();
        lieuField.clear();
        imageUrlField.clear();
        descArea.clear();
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        heureDebutField.setText("09:00");
        heureFinField.setText("17:00");
        clearErrors();
        msgLabel.setText("");
    }

    @FXML
    public void openActivites() {
        openSubWindow("/rh/EventActivitesManageView.fxml", "Activités");
    }

    @FXML
    public void openParticipations() {
        openSubWindow("/rh/EventParticipantsView.fxml", "Participants");
    }

    private void openSubWindow(String fxmlPath, String title) {
        Evenement selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("⚠️ Veuillez sélectionner un événement");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (title.equals("Activités")) {
                EventActivitesManageController ctrl = loader.getController();
                ctrl.setEvenement(selected);
            } else {
                EventParticipantsController ctrl = loader.getController();
                ctrl.setEvenement(selected);
            }

            Stage stage = new Stage();
            stage.setTitle(title + " - " + selected.getTitre());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Impossible d'ouvrir " + title);
        }
    }
}