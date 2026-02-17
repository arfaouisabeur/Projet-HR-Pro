package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Evenement;
import edu.RhPro.entities.Participation;
import edu.RhPro.entities.User;
import edu.RhPro.services.EvenementService;
import edu.RhPro.services.ParticipationService;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

public class EventsController {

    @FXML private FlowPane cardsContainer;
    @FXML private Label msgLabel;

    private final EvenementService eventService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();
    private List<Evenement> allEvents;
    private Map<Long, Participation> userParticipationsMap = new HashMap<>(); // Map eventId -> Participation

    @FXML
    public void initialize() {
        refresh();
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm");
        return dt.format(fmt);
    }

    private long currentEmployeId() {
        User u = Session.getCurrentUser();
        return u == null ? 0 : u.getId();
    }

    private void loadUserParticipations() {
        try {
            long empId = currentEmployeId();
            userParticipationsMap.clear();

            if (empId != 0) {
                // Get all participations for current user using findByEmployeId
                List<Participation> userParticipations = participationService.findByEmployeId(empId);

                // Create map of eventId -> Participation for easy lookup
                for (Participation p : userParticipations) {
                    userParticipationsMap.put(p.getEvenementId(), p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Participation getUserParticipation(long eventId) {
        return userParticipationsMap.get(eventId);
    }

    private String getStatusText(String statut) {
        switch (statut) {
            case "EN_ATTENTE":
                return "En attente";
            case "ACCEPTE":
                return "Accepté";
            case "REFUSE":
                return "Refusé";
            default:
                return statut;
        }
    }

    private String getStatusColor(String statut) {
        switch (statut) {
            case "EN_ATTENTE":
                return "#f59e0b"; // Orange
            case "ACCEPTE":
                return "#10b981"; // Green
            case "REFUSE":
                return "#ef4444"; // Red
            default:
                return "#6b7280"; // Gray
        }
    }

    private void displayEvents(List<Evenement> events) {
        cardsContainer.getChildren().clear();

        if (events.isEmpty()) {
            VBox emptyState = new VBox(15);
            emptyState.setAlignment(javafx.geometry.Pos.CENTER);
            emptyState.setPrefWidth(800);
            emptyState.setPadding(new Insets(50, 0, 50, 0));

            Label iconLabel = new Label("📭");
            iconLabel.setStyle("-fx-font-size: 64px;");

            Label titleLabel = new Label("Aucun événement disponible");
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");

            emptyState.getChildren().addAll(iconLabel, titleLabel);
            cardsContainer.getChildren().add(emptyState);
            return;
        }

        for (Evenement event : events) {
            VBox card = createEventCard(event);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createEventCard(Evenement event) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #f1f5f9; -fx-border-width: 1;");
        card.setPadding(new Insets(0, 0, 16, 0));

        // Drop shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(10);
        dropShadow.setOffsetX(0);
        dropShadow.setOffsetY(4);
        dropShadow.setColor(Color.color(0, 0, 0, 0.1));
        card.setEffect(dropShadow);

        // Get user participation for this event
        Participation userParticipation = getUserParticipation(event.getId());

        // Image section
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(160);
        imageContainer.setStyle("-fx-background-radius: 20 20 0 0; -fx-background-color: linear-gradient(to bottom right, #1e293b, #0f172a);");

        try {
            String imageUrl = event.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Image image = new Image(imageUrl, 320, 160, false, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(320);
                imageView.setFitHeight(160);
                imageView.setPreserveRatio(false);
                imageView.setStyle("-fx-background-radius: 20 20 0 0;");
                imageContainer.getChildren().add(imageView);
            } else {
                Label eventIcon = new Label("📅");
                eventIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");
                imageContainer.getChildren().add(eventIcon);
            }
        } catch (Exception e) {
            Label eventIcon = new Label("📅");
            eventIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");
            imageContainer.getChildren().add(eventIcon);
        }

        // Content section
        VBox content = new VBox(10);
        content.setPadding(new Insets(0, 16, 0, 16));

        // Title with participation indicator
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label titleLabel = new Label(event.getTitre());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        if (userParticipation != null) {
            Label checkLabel = new Label("✓");
            checkLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #10b981;");
            titleRow.getChildren().addAll(titleLabel, checkLabel);
        } else {
            titleRow.getChildren().add(titleLabel);
        }

        // Location
        HBox locationRow = new HBox(8);
        locationRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label locationIcon = new Label("📍");
        locationIcon.setStyle("-fx-font-size: 14px;");

        Label locationLabel = new Label(event.getLieu() != null ? event.getLieu() : "Lieu non spécifié");
        locationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        locationRow.getChildren().addAll(locationIcon, locationLabel);

        // Date
        HBox dateRow = new HBox(8);
        dateRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label dateIcon = new Label("🕒");
        dateIcon.setStyle("-fx-font-size: 14px;");

        String dateText = formatDateTime(event.getDateDebut());
        if (event.getDateFin() != null) {
            dateText += " → " + event.getDateFin().format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        Label dateLabel = new Label(dateText);
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        dateRow.getChildren().addAll(dateIcon, dateLabel);

        // Add title, location, date to content
        content.getChildren().addAll(titleRow, locationRow, dateRow);

        // Description preview
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            String desc = event.getDescription();
            if (desc.length() > 80) {
                desc = desc.substring(0, 80) + "...";
            }
            Label descLabel = new Label(desc);
            descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-padding: 5 0 0 0;");
            descLabel.setWrapText(true);
            content.getChildren().add(descLabel);
        }

        // Action buttons
        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 5, 0));

        Button detailsBtn = new Button("📋 Voir activités");
        detailsBtn.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #1e293b; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-background-radius: 25; -fx-border-radius: 25; -fx-padding: 8 16; -fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand;");
        detailsBtn.setOnAction(e -> openActivites(event));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Show different button based on participation status
        if (userParticipation != null) {
            // Already participated - show status button (disabled)
            String statusText = getStatusText(userParticipation.getStatut());
            Button statusBtn = new Button("📋 " + statusText);
            statusBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-background-radius: 25; -fx-padding: 8 16; -fx-font-size: 12px; -fx-font-weight: 600; -fx-border-color: #cbd5e1; -fx-border-width: 1;");
            statusBtn.setDisable(true);
            actions.getChildren().addAll(detailsBtn, spacer, statusBtn);
        } else {
            // Not participated - show participate button
            Button participateBtn = new Button("✅ Participer");
            participateBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 8 16; -fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand;");
            participateBtn.setOnAction(e -> participer(event));
            actions.getChildren().addAll(detailsBtn, spacer, participateBtn);
        }

        content.getChildren().add(actions);

        card.getChildren().addAll(imageContainer, content);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-scale-x: 1.02; -fx-scale-y: 1.02;");
            DropShadow hoverShadow = new DropShadow();
            hoverShadow.setRadius(15);
            hoverShadow.setOffsetX(0);
            hoverShadow.setOffsetY(8);
            hoverShadow.setColor(Color.color(0, 0, 0, 0.15));
            card.setEffect(hoverShadow);
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #f1f5f9; -fx-border-width: 1; -fx-scale-x: 1; -fx-scale-y: 1;");
            card.setEffect(dropShadow);
        });

        return card;
    }

    @FXML
    public void refresh() {
        try {
            // Load user participations first
            loadUserParticipations();

            // Load events
            allEvents = eventService.getData();
            displayEvents(allEvents);

            // Count participations by status
            long pendingCount = userParticipationsMap.values().stream()
                    .filter(p -> "EN_ATTENTE".equals(p.getStatut()))
                    .count();
            long acceptedCount = userParticipationsMap.values().stream()
                    .filter(p -> "ACCEPTE".equals(p.getStatut()))
                    .count();

            msgLabel.setText(String.format("✅ %d événement(s) • Vos inscriptions: %d en attente, %d acceptées",
                    allEvents.size(), pendingCount, acceptedCount));
            msgLabel.setStyle("-fx-text-fill: #059669;");
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur lors du chargement");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private void openActivites(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/employe/EventActivitesView.fxml"));
            Parent root = loader.load();

            EventActivitesController ctrl = loader.getController();
            ctrl.setEvenement(event);

            Stage stage = new Stage();
            stage.setTitle("Activités - " + event.getTitre());
            stage.setScene(new Scene(root, 900, 600));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Impossible d'ouvrir les activités");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private void participer(Evenement event) {
        try {
            long empId = currentEmployeId();
            if (empId == 0) {
                msgLabel.setText("❌ Session employé invalide");
                msgLabel.setStyle("-fx-text-fill: #dc2626;");
                return;
            }

            // Check if already registered using isAlreadyRegistered method
            if (participationService.isAlreadyRegistered(empId, event.getId())) {
                msgLabel.setText("⚠️ Vous êtes déjà inscrit à cet événement");
                msgLabel.setStyle("-fx-text-fill: #f59e0b;");
                // Refresh to update UI
                refresh();
                return;
            }

            // Check for duplicate participation (same event, same date, same location, same description, same image)
            if (isDuplicateParticipation(event)) {
                showAlert("Participation en double",
                        "Vous avez déjà une participation similaire",
                        "Un événement avec les mêmes informations existe déjà. Voulez-vous quand même participer ?",
                        Alert.AlertType.CONFIRMATION,
                        () -> {
                            try {
                                saveParticipation(event, empId);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                return;
            }

            // Show confirmation dialog before participating
            showConfirmationDialog(
                    "Confirmation de participation",
                    "Participer à l'événement",
                    "Êtes-vous sûr de vouloir participer à \"" + event.getTitre() + "\" ?",
                    () -> {
                        try {
                            saveParticipation(event, empId);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            msgLabel.setText("❌ Erreur lors de la participation");
                            msgLabel.setStyle("-fx-text-fill: #dc2626;");
                        }
                    }
            );

        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur lors de la participation");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private boolean isDuplicateParticipation(Evenement event) {
        if (allEvents == null) return false;

        for (Evenement existingEvent : allEvents) {
            // Skip if it's the same event
            if (existingEvent.getId() == event.getId()) continue;

            // Check for duplicates based on multiple criteria
            boolean sameTitre = existingEvent.getTitre() != null &&
                    existingEvent.getTitre().equalsIgnoreCase(event.getTitre());
            boolean sameLieu = existingEvent.getLieu() != null &&
                    existingEvent.getLieu().equalsIgnoreCase(event.getLieu());
            boolean sameDescription = existingEvent.getDescription() != null &&
                    existingEvent.getDescription().equalsIgnoreCase(event.getDescription());
            boolean sameImage = existingEvent.getImageUrl() != null &&
                    existingEvent.getImageUrl().equals(event.getImageUrl());
            boolean sameDate = existingEvent.getDateDebut() != null &&
                    existingEvent.getDateDebut().equals(event.getDateDebut());

            // If multiple fields match, consider it a duplicate
            int matchCount = 0;
            if (sameTitre) matchCount++;
            if (sameLieu) matchCount++;
            if (sameDescription) matchCount++;
            if (sameImage) matchCount++;
            if (sameDate) matchCount++;

            if (matchCount >= 3) { // If at least 3 fields match, it's likely a duplicate
                return true;
            }
        }
        return false;
    }

    private void saveParticipation(Evenement event, long empId) throws Exception {
        // Create new participation
        Participation participation = new Participation(
                LocalDate.now(),
                "EN_ATTENTE",
                event.getId(),
                empId
        );

        // Save to database
        participationService.addEntity(participation);

        // Refresh participations and display
        refresh();

        msgLabel.setText("✅ Participation envoyée (en attente de validation)");
        msgLabel.setStyle("-fx-text-fill: #059669;");
    }

    private void showConfirmationDialog(String title, String header, String content, Runnable onConfirm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-radius: 15;");
        dialogPane.getButtonTypes().stream()
                .map(dialogPane::lookupButton)
                .forEach(button -> button.setStyle("-fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: 600; -fx-cursor: hand;"));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            onConfirm.run();
        }
    }

    private void showAlert(String title, String header, String content, Alert.AlertType type, Runnable onConfirm) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-radius: 15;");

        if (type == Alert.AlertType.CONFIRMATION) {
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                onConfirm.run();
            }
        } else {
            alert.showAndWait();
        }
    }
}