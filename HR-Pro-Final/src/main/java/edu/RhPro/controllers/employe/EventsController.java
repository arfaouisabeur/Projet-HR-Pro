package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Evenement;
import edu.RhPro.entities.Participation;
import edu.RhPro.entities.Rating;
import edu.RhPro.entities.User;
import edu.RhPro.services.EvenementService;
import edu.RhPro.services.ParticipationService;
import edu.RhPro.services.RatingService;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class EventsController {

    @FXML private FlowPane cardsContainer;
    @FXML private Label msgLabel;

    private final EvenementService eventService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();
    private List<Evenement> allEvents;
    private Map<Long, Participation> userParticipationsMap = new HashMap<>();

    private final RatingService ratingService = new RatingService();
    private Map<Long, Double> averageRatings = new HashMap<>();
    private Map<Long, Integer> ratingCounts = new HashMap<>();
    private Map<Long, Rating> userRatingsMap = new HashMap<>();

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
                List<Participation> userParticipations = participationService.findByEmployeId(empId);
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
            case "EN_ATTENTE": return "En attente";
            case "ACCEPTE":    return "Accepté";
            case "REFUSE":     return "Refusé";
            default:           return statut;
        }
    }

    private String getStatusColor(String statut) {
        switch (statut) {
            case "EN_ATTENTE": return "#f59e0b";
            case "ACCEPTE":    return "#10b981";
            case "REFUSE":     return "#ef4444";
            default:           return "#6b7280";
        }
    }

    @FXML
    public void refresh() {
        try {
            loadUserParticipations();
            loadRatingData();
            allEvents = eventService.getData();
            displayWithRecommendations();

            long pendingCount = userParticipationsMap.values().stream()
                    .filter(p -> "EN_ATTENTE".equals(p.getStatut())).count();
            long acceptedCount = userParticipationsMap.values().stream()
                    .filter(p -> "ACCEPTE".equals(p.getStatut())).count();

            msgLabel.setText(String.format("✅ %d événement(s) • Vos inscriptions: %d en attente, %d acceptées",
                    allEvents.size(), pendingCount, acceptedCount));
            msgLabel.setStyle("-fx-text-fill: #059669;");
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur lors du chargement");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    // ==================== DISPLAY WITH SECTIONS ====================
    private void displayWithRecommendations() {
        // Clear the FlowPane and add a single VBox that will contain all sections
        cardsContainer.getChildren().clear();

        if (allEvents.isEmpty()) {
            // Empty state (same as before)
            VBox emptyState = new VBox(15);
            emptyState.setAlignment(Pos.CENTER);
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

        // Main container VBox (will hold sections)
        VBox mainContainer = new VBox(20);
        mainContainer.setMaxWidth(Double.MAX_VALUE);

        List<Evenement> recommended = getRecommendedEvents();

        // ----- SECTION : Recommandations -----
        if (!recommended.isEmpty()) {
            Label recHeader = new Label("⭐ Recommandé pour vous");
            recHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0f172a; -fx-padding: 5 0 0 0;");

            FlowPane recFlow = new FlowPane();
            recFlow.setHgap(20);
            recFlow.setVgap(20);
            recFlow.setPrefWrapLength(1000);
            recFlow.setAlignment(Pos.TOP_LEFT);

            for (Evenement ev : recommended) {
                recFlow.getChildren().add(createEventCard(ev));
            }

            mainContainer.getChildren().addAll(recHeader, recFlow);
            mainContainer.getChildren().add(new Separator());
        }

        // ----- SECTION : Tous les événements -----
        Label allHeader = new Label("📅 Tous les événements");
        allHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0f172a; -fx-padding: 5 0 0 0;");

        FlowPane allFlow = new FlowPane();
        allFlow.setHgap(20);
        allFlow.setVgap(20);
        allFlow.setPrefWrapLength(1000);
        allFlow.setAlignment(Pos.TOP_LEFT);

        Set<Long> recommendedIds = recommended.stream().map(Evenement::getId).collect(Collectors.toSet());
        for (Evenement ev : allEvents) {
            if (!recommendedIds.contains(ev.getId())) {
                allFlow.getChildren().add(createEventCard(ev));
            }
        }

        mainContainer.getChildren().addAll(allHeader, allFlow);

        // Add the main container to the FlowPane (it will be the only child)
        cardsContainer.getChildren().add(mainContainer);
    }

    // ==================== RECOMMENDATION LOGIC (unchanged) ====================
    private Set<String> extractKeywords(Evenement ev) {
        Set<String> keywords = new HashSet<>();
        String text = (ev.getTitre() + " " +
                (ev.getDescription() == null ? "" : ev.getDescription()) + " " +
                (ev.getLieu() == null ? "" : ev.getLieu())).toLowerCase();
        String[] words = text.split("\\W+");
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "le", "la", "les", "de", "du", "des", "un", "une", "et", "ou",
                "pour", "dans", "sur", "avec", "a", "au", "aux", "ce", "cet",
                "cette", "ces", "mon", "ton", "son", "notre", "votre", "leur",
                "je", "tu", "il", "elle", "nous", "vous", "ils", "elles"
        ));
        for (String w : words) {
            if (w.length() > 2 && !stopWords.contains(w)) {
                keywords.add(w);
            }
        }
        return keywords;
    }

    private double computeSimilarity(Set<String> userKeywords, Set<String> candidateKeywords) {
        if (userKeywords.isEmpty() || candidateKeywords.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(userKeywords);
        intersection.retainAll(candidateKeywords);
        Set<String> union = new HashSet<>(userKeywords);
        union.addAll(candidateKeywords);
        return (double) intersection.size() / union.size();
    }

    private Evenement findEventById(long id) {
        return allEvents.stream().filter(e -> e.getId() == id).findFirst().orElse(null);
    }

    private List<Evenement> getRecommendedEvents() {
        long empId = currentEmployeId();
        if (empId == 0 || userParticipationsMap.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> participatedIds = userParticipationsMap.keySet();
        List<Evenement> candidates = allEvents.stream()
                .filter(e -> !participatedIds.contains(e.getId()))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) return Collections.emptyList();

        Set<String> userKeywords = new HashSet<>();
        for (Long pid : participatedIds) {
            Evenement ev = findEventById(pid);
            if (ev != null) {
                userKeywords.addAll(extractKeywords(ev));
            }
        }

        Map<Evenement, Double> scores = new HashMap<>();
        for (Evenement candidate : candidates) {
            Set<String> candidateKeywords = extractKeywords(candidate);
            double score = computeSimilarity(userKeywords, candidateKeywords);
            if (score > 0) {
                scores.put(candidate, score);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Evenement, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ==================== CARD CREATION (with cancel button) ====================
    private VBox createEventCard(Evenement event) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #f1f5f9; -fx-border-width: 1;");
        card.setPadding(new Insets(0, 0, 16, 0));

        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(10);
        dropShadow.setOffsetX(0);
        dropShadow.setOffsetY(4);
        dropShadow.setColor(Color.color(0, 0, 0, 0.1));
        card.setEffect(dropShadow);

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

        // Title with indicator
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
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
        locationRow.setAlignment(Pos.CENTER_LEFT);
        Label locationIcon = new Label("📍");
        locationIcon.setStyle("-fx-font-size: 14px;");
        Label locationLabel = new Label(event.getLieu() != null ? event.getLieu() : "Lieu non spécifié");
        locationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        locationRow.getChildren().addAll(locationIcon, locationLabel);

        // Date
        HBox dateRow = new HBox(8);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("🕒");
        dateIcon.setStyle("-fx-font-size: 14px;");
        String dateText = formatDateTime(event.getDateDebut());
        if (event.getDateFin() != null) {
            dateText += " → " + event.getDateFin().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        Label dateLabel = new Label(dateText);
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        dateRow.getChildren().addAll(dateIcon, dateLabel);

        // Rating stars
        double avgRating = averageRatings.getOrDefault(event.getId(), 0.0);
        int ratingCount = ratingCounts.getOrDefault(event.getId(), 0);
        HBox ratingDisplay = createStarDisplay(avgRating, ratingCount);

        content.getChildren().addAll(titleRow, locationRow, dateRow, ratingDisplay);

        // Description preview
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            String desc = event.getDescription();
            if (desc.length() > 80) desc = desc.substring(0, 80) + "...";
            Label descLabel = new Label(desc);
            descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-padding: 5 0 0 0;");
            descLabel.setWrapText(true);
            content.getChildren().add(descLabel);
        }

        // Action buttons
        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 5, 0));

        Button detailsBtn = new Button("📋 Activités");
        detailsBtn.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #1e293b; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand;");
        detailsBtn.setOnAction(e -> openActivites(event));

        Button ratingBtn = new Button();
        if (userRatingsMap.containsKey(event.getId())) {
            ratingBtn.setText("⭐ Modifier");
            ratingBtn.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-border-color: #fcd34d; -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand;");
        } else {
            ratingBtn.setText("⭐ Évaluer");
            ratingBtn.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #1e293b; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand;");
        }
        ratingBtn.setOnAction(e -> openRatingPopup(event));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (userParticipation != null) {
            // User already has a participation
            String statusText = getStatusText(userParticipation.getStatut());
            Button statusBtn = new Button("📋 " + statusText);
            statusBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-background-radius: 20; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: 600; -fx-border-color: #cbd5e1; -fx-border-width: 1;");
            statusBtn.setDisable(true);

            if ("EN_ATTENTE".equals(userParticipation.getStatut())) {
                // Pending participation → show cancel button
                Button cancelBtn = new Button("❌ Annuler");
                cancelBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-background-radius: 20; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: 600; -fx-border-color: #fecaca; -fx-border-width: 1; -fx-cursor: hand;");
                cancelBtn.setOnAction(e -> annulerParticipation(event));
                actions.getChildren().addAll(detailsBtn, ratingBtn, spacer, cancelBtn, statusBtn);
            } else {
                actions.getChildren().addAll(detailsBtn, ratingBtn, spacer, statusBtn);
            }
        } else {
            // Not participating
            Button participateBtn = new Button("✅ Participer");
            participateBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand;");
            participateBtn.setOnAction(e -> participer(event));
            actions.getChildren().addAll(detailsBtn, ratingBtn, spacer, participateBtn);
        }

        content.getChildren().add(actions);
        card.getChildren().addAll(imageContainer, content);

        // Hover effects
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

    // ==================== CANCEL PARTICIPATION ====================
    private void annulerParticipation(Evenement event) {
        Participation p = userParticipationsMap.get(event.getId());
        if (p == null) {
            msgLabel.setText("❌ Aucune participation trouvée");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler la participation");
        confirm.setHeaderText("Voulez‑vous vraiment annuler votre participation ?");
        confirm.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                participationService.deleteEntity(p);
                refresh();
                msgLabel.setText("✅ Participation annulée");
                msgLabel.setStyle("-fx-text-fill: #059669;");
            } catch (SQLException e) {
                e.printStackTrace();
                msgLabel.setText("❌ Erreur lors de l'annulation");
                msgLabel.setStyle("-fx-text-fill: #dc2626;");
            }
        }
    }

    // ==================== OTHER METHODS (unchanged) ====================
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

            if (participationService.isAlreadyRegistered(empId, event.getId())) {
                msgLabel.setText("⚠️ Vous êtes déjà inscrit à cet événement");
                msgLabel.setStyle("-fx-text-fill: #f59e0b;");
                refresh();
                return;
            }

            // Check for duplicate participation
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
            if (existingEvent.getId() == event.getId()) continue;
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

            int matchCount = 0;
            if (sameTitre) matchCount++;
            if (sameLieu) matchCount++;
            if (sameDescription) matchCount++;
            if (sameImage) matchCount++;
            if (sameDate) matchCount++;

            if (matchCount >= 3) return true;
        }
        return false;
    }

    private void saveParticipation(Evenement event, long empId) throws Exception {
        Participation participation = new Participation(
                LocalDate.now(),
                "EN_ATTENTE",
                event.getId(),
                empId
        );
        participationService.addEntity(participation);
        refresh();
        msgLabel.setText("✅ Participation envoyée (en attente de validation)");
        msgLabel.setStyle("-fx-text-fill: #059669;");
    }

    private void showConfirmationDialog(String title, String header, String content, Runnable onConfirm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
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

    // Rating methods
    private void loadRatingData() {
        try {
            averageRatings = ratingService.getAllAverageRatings();
            ratingCounts = ratingService.getRatingCounts();
            long empId = currentEmployeId();
            if (empId != 0) {
                userRatingsMap.clear();
                List<Rating> allRatings = ratingService.getData();
                for (Rating rating : allRatings) {
                    if (rating.getEmployeId() == empId) {
                        userRatingsMap.put(rating.getEvenementId(), rating);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox createStarDisplay(double average, int count) {
        HBox starBox = new HBox(2);
        starBox.setAlignment(Pos.CENTER_LEFT);
        int fullStars = (int) Math.round(average);
        boolean hasHalfStar = average - fullStars > 0.3 && average - fullStars < 0.7;
        for (int i = 1; i <= 5; i++) {
            Label star = new Label();
            if (i <= fullStars) {
                star.setText("★");
                star.setStyle("-fx-font-size: 14px; -fx-text-fill: #fbbf24;");
            } else if (hasHalfStar && i == fullStars + 1) {
                star.setText("½");
                star.setStyle("-fx-font-size: 14px; -fx-text-fill: #fbbf24;");
            } else {
                star.setText("☆");
                star.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");
            }
            starBox.getChildren().add(star);
        }
        if (count > 0) {
            Label countLabel = new Label("(" + count + ")");
            countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-padding: 0 0 0 3;");
            starBox.getChildren().add(countLabel);
        }
        return starBox;
    }

    private void openRatingPopup(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/employe/RatingPopup.fxml"));
            Parent root = loader.load();
            RatingPopupController ctrl = loader.getController();
            ctrl.setEvenement(event);
            Rating existingRating = userRatingsMap.get(event.getId());
            if (existingRating != null) {
                ctrl.setExistingRating(existingRating);
            }
            ctrl.setOnRatingSubmitted(() -> refresh());
            Stage stage = new Stage();
            stage.setTitle("Évaluer - " + event.getTitre());
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Impossible d'ouvrir la fenêtre d'évaluation");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }
}