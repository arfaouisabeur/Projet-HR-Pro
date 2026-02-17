package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Activite;
import edu.RhPro.entities.Evenement;
import edu.RhPro.services.ActiviteService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;

import java.util.List;
import java.util.stream.Collectors;

public class EventActivitesController {

    @FXML private Label titleLabel;
    @FXML private Label statsLabel;
    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private ToggleButton gridViewToggle;
    @FXML private ToggleButton listViewToggle;
    @FXML private Label msgLabel;

    private final ActiviteService service = new ActiviteService();
    private Evenement evenement;
    private List<Activite> allActivities;
    private FilteredList<Activite> filteredActivities;

    public void setEvenement(Evenement e) {
        this.evenement = e;
        titleLabel.setText("Activités - " + e.getTitre());
        refresh();
    }

    @FXML
    public void initialize() {
        setupSearch();
        setupViewToggle();
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, newValue) -> {
            if (filteredActivities != null) {
                filteredActivities.setPredicate(activite -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    return activite.getTitre().toLowerCase().contains(lowerCaseFilter) ||
                            (activite.getDescription() != null &&
                                    activite.getDescription().toLowerCase().contains(lowerCaseFilter));
                });
                displayActivities(filteredActivities);
            }
        });
    }

    private void setupViewToggle() {
        ToggleGroup viewGroup = new ToggleGroup();
        gridViewToggle.setToggleGroup(viewGroup);
        listViewToggle.setToggleGroup(viewGroup);

        gridViewToggle.setSelected(true);

        viewGroup.selectedToggleProperty().addListener((obs, old, newValue) -> {
            if (filteredActivities != null) {
                displayActivities(filteredActivities);
            }
        });
    }

    @FXML
    public void refresh() {
        if (evenement == null) return;

        try {
            // Load all activities
            List<Activite> all = service.getData();

            // Filter for current event
            allActivities = all.stream()
                    .filter(a -> a.getEvenementId() == evenement.getId())
                    .collect(Collectors.toList());

            // Create filtered list
            filteredActivities = new FilteredList<>(
                    FXCollections.observableArrayList(allActivities),
                    p -> true
            );

            // Update stats
            statsLabel.setText(allActivities.size() + " activité" + (allActivities.size() > 1 ? "s" : ""));

            // Display activities
            displayActivities(filteredActivities);

            msgLabel.setText("✅ " + allActivities.size() + " activité(s) trouvée(s)");
            msgLabel.setStyle("-fx-text-fill: #059669;");

        } catch (Exception ex) {
            ex.printStackTrace();
            msgLabel.setText("❌ Erreur lors du chargement");
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private void displayActivities(List<Activite> activities) {
        cardsContainer.getChildren().clear();

        if (activities.isEmpty()) {
            showEmptyState();
            return;
        }

        boolean isGridView = gridViewToggle.isSelected();

        for (Activite activite : activities) {
            if (isGridView) {
                cardsContainer.getChildren().add(createGridCard(activite));
            } else {
                cardsContainer.getChildren().add(createListCard(activite));
            }
        }
    }

    private void showEmptyState() {
        VBox emptyState = new VBox(15);
        emptyState.setAlignment(javafx.geometry.Pos.CENTER);
        emptyState.setPrefWidth(800);
        emptyState.setPadding(new Insets(50, 0, 50, 0));

        Label iconLabel = new Label("📋");
        iconLabel.setStyle("-fx-font-size: 64px;");

        Label titleLabel = new Label("Aucune activité disponible");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");

        Label descLabel = new Label("Cet événement n'a pas encore d'activités programmées");
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        emptyState.getChildren().addAll(iconLabel, titleLabel, descLabel);
        cardsContainer.getChildren().add(emptyState);
    }

    private VBox createGridCard(Activite activite) {
        VBox card = new VBox(15);
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #f1f5f9; -fx-border-width: 1;");
        card.setPadding(new Insets(20, 20, 20, 20));

        // Drop shadow
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(10);
        dropShadow.setOffsetX(0);
        dropShadow.setOffsetY(4);
        dropShadow.setColor(Color.color(0, 0, 0, 0.1));
        card.setEffect(dropShadow);

        // Icon and color based on activity
        String[] colors = {"#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"};
        int colorIndex = (int)(activite.getId() % colors.length);
        String cardColor = colors[colorIndex];

        // Header with icon
        HBox header = new HBox(15);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(50, 50);
        iconContainer.setStyle("-fx-background-color: " + cardColor + "15; -fx-background-radius: 15;");

        Label iconLabel = new Label("📋");
        iconLabel.setStyle("-fx-font-size: 24px;");
        iconContainer.getChildren().add(iconLabel);

        VBox titleContainer = new VBox(5);
        Label titleLabel = new Label(activite.getTitre());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        titleLabel.setWrapText(true);

        Label idLabel = new Label("ID: " + activite.getId());
        idLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        titleContainer.getChildren().addAll(titleLabel, idLabel);
        HBox.setHgrow(titleContainer, Priority.ALWAYS);

        header.getChildren().addAll(iconContainer, titleContainer);

        // Separator
        Separator separator = new Separator();
        separator.setStyle("-fx-background: #e2e8f0;");

        // Description
        VBox descContainer = new VBox(8);

        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        String description = activite.getDescription();
        if (description == null || description.isEmpty()) {
            description = "Aucune description disponible";
        }

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-line-spacing: 2;");
        descLabel.setWrapText(true);

        descContainer.getChildren().addAll(descTitle, descLabel);

        card.getChildren().addAll(header, separator, descContainer);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: " + cardColor + "; -fx-border-width: 2; -fx-scale-x: 1.02; -fx-scale-y: 1.02;");
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

    private HBox createListCard(Activite activite) {
        HBox card = new HBox(20);
        card.setPrefWidth(950);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #f1f5f9; -fx-border-width: 1;");
        card.setPadding(new Insets(16, 20, 16, 20));

        // Drop shadow
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(8);
        dropShadow.setOffsetX(0);
        dropShadow.setOffsetY(2);
        dropShadow.setColor(Color.color(0, 0, 0, 0.05));
        card.setEffect(dropShadow);

        // Icon
        String[] colors = {"#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"};
        int colorIndex = (int)(activite.getId() % colors.length);
        String cardColor = colors[colorIndex];

        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(60, 60);
        iconContainer.setStyle("-fx-background-color: " + cardColor + "15; -fx-background-radius: 12;");

        Label iconLabel = new Label("📋");
        iconLabel.setStyle("-fx-font-size: 28px;");
        iconContainer.getChildren().add(iconLabel);

        // Content
        VBox content = new VBox(8);
        content.setPadding(new Insets(5, 0, 5, 0));
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLabel = new Label(activite.getTitre());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        String description = activite.getDescription();
        if (description == null || description.isEmpty()) {
            description = "Aucune description disponible";
        }

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        descLabel.setWrapText(true);

        // Metadata
        HBox metaData = new HBox(15);

        HBox idBox = new HBox(5);
        Label idIcon = new Label("🆔");
        idIcon.setStyle("-fx-font-size: 12px;");
        Label idValue = new Label("Activité #" + activite.getId());
        idValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        idBox.getChildren().addAll(idIcon, idValue);

        metaData.getChildren().addAll(idBox);

        content.getChildren().addAll(titleLabel, descLabel, metaData);

        card.getChildren().addAll(iconContainer, content);

        return card;
    }
}