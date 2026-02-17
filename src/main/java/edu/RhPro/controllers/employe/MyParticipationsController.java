package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Evenement;
import edu.RhPro.entities.Participation;
import edu.RhPro.entities.User;
import edu.RhPro.services.EvenementService;
import edu.RhPro.services.ParticipationService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyParticipationsController {

    @FXML private VBox listContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label pendingCountLabel;
    @FXML private Label acceptedCountLabel;
    @FXML private Label rejectedCountLabel;
    @FXML private Label msgLabel;

    private final ParticipationService participationService = new ParticipationService();
    private final EvenementService evenementService = new EvenementService();
    private List<Participation> allParticipations;
    private FilteredList<Participation> filteredParticipations;
    private Map<Long, String> eventNamesMap = new HashMap<>(); // Cache for event names

    // Constants for status values from database
    private static final String STATUS_PENDING = "EN_ATTENTE";
    private static final String STATUS_ACCEPTED = "ACCEPTEE"; // Note the double E
    private static final String STATUS_REJECTED = "REFUSEE";  // If this is also with double E

    @FXML
    public void initialize() {
        setupFilters();
        setupSorting();
        loadEventNames(); // Load all event names first
        refresh();
    }

    private void loadEventNames() {
        try {
            List<Evenement> events = evenementService.getData();
            for (Evenement event : events) {
                eventNamesMap.put(event.getId(), event.getTitre());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getEventName(long eventId) {
        return eventNamesMap.getOrDefault(eventId, "Événement #" + eventId);
    }

    private void setupFilters() {
        // Status filter options
        statusFilter.setItems(FXCollections.observableArrayList(
                "Tous", "En attente", "Accepté", "Refusé"
        ));
        statusFilter.setValue("Tous");

        statusFilter.valueProperty().addListener((obs, old, newValue) -> {
            if (filteredParticipations != null) {
                applyFilters();
            }
        });

        // Search filter
        searchField.textProperty().addListener((obs, old, newValue) -> {
            if (filteredParticipations != null) {
                applyFilters();
            }
        });
    }

    private void applyFilters() {
        String selectedStatus = statusFilter.getValue();
        String searchText = searchField.getText();

        filteredParticipations.setPredicate(participation -> {
            // Status filter
            if (selectedStatus != null && !selectedStatus.equals("Tous")) {
                String status = participation.getStatut();
                switch (selectedStatus) {
                    case "En attente":
                        if (!STATUS_PENDING.equals(status)) return false;
                        break;
                    case "Accepté":
                        if (!STATUS_ACCEPTED.equals(status)) return false;
                        break;
                    case "Refusé":
                        if (!STATUS_REJECTED.equals(status)) return false;
                        break;
                }
            }

            // Search filter (by event name)
            if (searchText != null && !searchText.isEmpty()) {
                String lowerFilter = searchText.toLowerCase();
                String eventName = getEventName(participation.getEvenementId()).toLowerCase();
                String eventId = String.valueOf(participation.getEvenementId());

                return eventName.contains(lowerFilter) || eventId.contains(lowerFilter);
            }

            return true;
        });

        // Update display after filtering
        applySorting();
    }

    private void setupSorting() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Date (récente)", "Date (ancienne)", "Statut", "Nom événement", "ID événement"
        ));
        sortCombo.setValue("Date (récente)");

        sortCombo.valueProperty().addListener((obs, old, newValue) -> {
            if (filteredParticipations != null) {
                applySorting();
            }
        });
    }

    private void updateStats() {
        if (allParticipations == null) return;

        long pending = allParticipations.stream()
                .filter(p -> STATUS_PENDING.equals(p.getStatut()))
                .count();
        long accepted = allParticipations.stream()
                .filter(p -> STATUS_ACCEPTED.equals(p.getStatut()))
                .count();
        long rejected = allParticipations.stream()
                .filter(p -> STATUS_REJECTED.equals(p.getStatut()))
                .count();

        pendingCountLabel.setText(String.valueOf(pending));
        acceptedCountLabel.setText(String.valueOf(accepted));
        rejectedCountLabel.setText(String.valueOf(rejected));

        // Debug output to verify counts
        System.out.println("=== STATS UPDATE ===");
        System.out.println("Total participations: " + allParticipations.size());
        System.out.println("EN_ATTENTE: " + pending);
        System.out.println("ACCEPTEE: " + accepted);
        System.out.println("REFUSEE: " + rejected);

        // Print all statuses for debugging
        System.out.println("All statuses in data:");
        for (Participation p : allParticipations) {
            System.out.println("  Participation " + p.getId() + ": '" + p.getStatut() + "'");
        }
    }

    private String getStatusText(String statut) {
        if (statut == null) return "Inconnu";

        switch (statut) {
            case STATUS_PENDING:
                return "En attente";
            case STATUS_ACCEPTED:
                return "Accepté";
            case STATUS_REJECTED:
                return "Refusé";
            default:
                return statut; // This will show the actual DB value if unknown
        }
    }

    private String getStatusColor(String statut) {
        switch (statut) {
            case STATUS_PENDING:
                return "#f59e0b";
            case STATUS_ACCEPTED:
                return "#10b981";
            case STATUS_REJECTED:
                return "#ef4444";
            default:
                return "#6b7280";
        }
    }

    private String getStatusBgColor(String statut) {
        switch (statut) {
            case STATUS_PENDING:
                return "#fef3c7";
            case STATUS_ACCEPTED:
                return "#d1fae5";
            case STATUS_REJECTED:
                return "#fee2e2";
            default:
                return "#f3f4f6";
        }
    }

    private long currentEmployeId() {
        User u = Session.getCurrentUser();
        return u == null ? 0 : u.getId();
    }

    @FXML
    public void refresh() {
        try {
            long empId = currentEmployeId();
            if (empId == 0) {
                msgLabel.setText("❌ Session invalide.");
                msgLabel.setStyle("-fx-text-fill: #dc2626;");
                return;
            }

            // Get participations
            allParticipations = participationService.findByEmployeId(empId);

            // Debug: Print all participations to see statuses
            System.out.println("=== REFRESH ===");
            System.out.println("Loaded " + allParticipations.size() + " participations:");
            for (Participation p : allParticipations) {
                System.out.println("  Participation ID: " + p.getId() +
                        ", Event ID: " + p.getEvenementId() +
                        ", Status: '" + p.getStatut() + "'");
            }

            // Update stats
            updateStats();

            // Create filtered list
            filteredParticipations = new FilteredList<>(
                    FXCollections.observableArrayList(allParticipations),
                    p -> true
            );

            // Apply current filters
            applyFilters();

            msgLabel.setText("✅ " + allParticipations.size() + " participation(s) trouvée(s)");
            msgLabel.setStyle("-fx-text-fill: #059669;");

        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur lors du chargement");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private void applySorting() {
        if (filteredParticipations == null) return;

        SortedList<Participation> sortedList = new SortedList<>(filteredParticipations);

        String sortBy = sortCombo.getValue();
        if (sortBy != null) {
            Comparator<Participation> comparator = null;

            switch (sortBy) {
                case "Date (récente)":
                    comparator = (p1, p2) -> {
                        if (p1.getDateInscription() == null) return 1;
                        if (p2.getDateInscription() == null) return -1;
                        return p2.getDateInscription().compareTo(p1.getDateInscription());
                    };
                    break;
                case "Date (ancienne)":
                    comparator = (p1, p2) -> {
                        if (p1.getDateInscription() == null) return 1;
                        if (p2.getDateInscription() == null) return -1;
                        return p1.getDateInscription().compareTo(p2.getDateInscription());
                    };
                    break;
                case "Statut":
                    comparator = Comparator.comparing(p -> getStatusText(p.getStatut()));
                    break;
                case "Nom événement":
                    comparator = Comparator.comparing(p -> getEventName(p.getEvenementId()));
                    break;
                case "ID événement":
                    comparator = Comparator.comparingLong(Participation::getEvenementId);
                    break;
            }

            if (comparator != null) {
                sortedList.setComparator(comparator);
            }
        }

        displayParticipations(sortedList);
    }

    private void displayParticipations(List<Participation> participations) {
        listContainer.getChildren().clear();

        if (participations.isEmpty()) {
            showEmptyState();
            return;
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");

        for (Participation p : participations) {
            HBox item = createParticipationItem(p, dateFormatter);
            listContainer.getChildren().add(item);
        }
    }

    private HBox createParticipationItem(Participation p, DateTimeFormatter dateFormatter) {
        HBox item = new HBox(20);
        item.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #f1f5f9; -fx-border-width: 1;");
        item.setPadding(new Insets(16, 20, 16, 20));
        item.setPrefWidth(950);

        // ID Badge
        Label idBadge = new Label("#" + p.getId());
        idBadge.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 20; -fx-padding: 5 12; -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        // Event Name (not just ID)
        VBox eventBox = new VBox(5);
        Label eventLabel = new Label("Événement");
        eventLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");

        String eventName = getEventName(p.getEvenementId());
        Label eventValue = new Label(eventName);
        eventValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
        eventValue.setWrapText(true);
        eventValue.setMaxWidth(200);

        Label eventIdLabel = new Label("ID: " + p.getEvenementId());
        eventIdLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        eventBox.getChildren().addAll(eventLabel, eventValue, eventIdLabel);

        // Date
        VBox dateBox = new VBox(5);
        Label dateLabel = new Label("Date d'inscription");
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");

        String dateStr = p.getDateInscription() != null ?
                dateFormatter.format(p.getDateInscription()) : "Non spécifiée";
        Label dateValue = new Label(dateStr);
        dateValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #334155;");

        dateBox.getChildren().addAll(dateLabel, dateValue);

        // Status
        VBox statusBox = new VBox(5);
        Label statusLabel = new Label("Statut");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");

        String statusText = getStatusText(p.getStatut());
        String statusColor = getStatusColor(p.getStatut());
        String statusBgColor = getStatusBgColor(p.getStatut());

        Label statusValue = new Label(statusText);
        statusValue.setStyle("-fx-background-color: " + statusBgColor + "; -fx-text-fill: " + statusColor + "; -fx-background-radius: 20; -fx-padding: 6 16; -fx-font-size: 13px; -fx-font-weight: 600;");

        statusBox.getChildren().addAll(statusLabel, statusValue);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // View details button
        Button viewBtn = new Button("👁️ Voir détails");
        viewBtn.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #1e293b; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-background-radius: 25; -fx-border-radius: 25; -fx-padding: 8 20; -fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> showParticipationDetails(p));

        item.getChildren().addAll(idBadge, eventBox, dateBox, statusBox, spacer, viewBtn);

        // Hover effect
        item.setOnMouseEntered(e -> {
            item.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);");
        });

        item.setOnMouseExited(e -> {
            item.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #f1f5f9; -fx-border-width: 1;");
        });

        return item;
    }

    private void showEmptyState() {
        VBox emptyState = new VBox(15);
        emptyState.setAlignment(javafx.geometry.Pos.CENTER);
        emptyState.setPrefWidth(950);
        emptyState.setPadding(new Insets(50, 0, 50, 0));

        Label iconLabel = new Label("📭");
        iconLabel.setStyle("-fx-font-size: 64px;");

        Label titleLabel = new Label("Aucune participation");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");

        Label descLabel = new Label("Vous n'avez pas encore participé à des événements");
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        emptyState.getChildren().addAll(iconLabel, titleLabel, descLabel);
        listContainer.getChildren().add(emptyState);
    }

    private void showParticipationDetails(Participation p) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la participation");
        alert.setHeaderText("Participation #" + p.getId());

        String content = String.format(
                "Événement: %s\n" +
                        "ID Événement: %d\n" +
                        "Date d'inscription: %s\n" +
                        "Statut: %s (valeur DB: '%s')\n" +
                        "ID Participation: %d\n" +
                        "ID Employé: %d",
                getEventName(p.getEvenementId()),
                p.getEvenementId(),
                p.getDateInscription() != null ? p.getDateInscription().toString() : "Non spécifiée",
                getStatusText(p.getStatut()),
                p.getStatut(), // Show actual DB value
                p.getId(),
                p.getEmployeId()
        );

        alert.setContentText(content);
        alert.showAndWait();
    }
}