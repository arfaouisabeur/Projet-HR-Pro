package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Tache;
import edu.RhPro.entities.User;
import edu.RhPro.services.TacheService;
import edu.RhPro.services.ProjetService;
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
import javafx.scene.text.Text;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class MesTachesController {

    @FXML private VBox mainContainer;
    @FXML private Label msgLabel;
    @FXML private TextField searchField;

    // Kanban columns
    @FXML private VBox todoColumn;
    @FXML private VBox doingColumn;
    @FXML private VBox doneColumn;

    // Column counters
    @FXML private Label todoCount;
    @FXML private Label doingCount;
    @FXML private Label doneCount;

    private final TacheService tacheService = new TacheService();
    private final ProjetService projetService = new ProjetService();
    private ObservableList<Tache> allTasks;

    // Colors for different task priorities/projects
    private final String[] colors = {
            "#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6",
            "#ec4899", "#06b6d4", "#f97316", "#6b7280"
    };

    @FXML
    public void initialize() {
        setupSearch();
        refresh();
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, newVal) -> {
            filterTasks(newVal);
        });
    }

    private void filterTasks(String searchText) {
        if (allTasks == null) return;

        if (searchText == null || searchText.isEmpty()) {
            renderKanban(allTasks);
        } else {
            List<Tache> filtered = allTasks.stream()
                    .filter(t -> t.getTitre().toLowerCase().contains(searchText.toLowerCase()) ||
                            t.getDescription().toLowerCase().contains(searchText.toLowerCase()))
                    .collect(Collectors.toList());
            renderKanban(FXCollections.observableArrayList(filtered));
        }
    }

    @FXML
    public void refresh() {
        try {
            User u = Session.getCurrentUser();
            List<Tache> data = tacheService.findByEmployeId((int) u.getId());
            allTasks = FXCollections.observableArrayList(data);
            renderKanban(allTasks);
            msgLabel.setText(data.size() + " tâche(s) assignée(s)");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur chargement tâches");
        }
    }

    private void renderKanban(ObservableList<Tache> tasks) {
        // Clear existing cards
        todoColumn.getChildren().clear();
        doingColumn.getChildren().clear();
        doneColumn.getChildren().clear();

        // Add column headers
        addColumnHeader(todoColumn, "À FAIRE", todoCount, "#6b7280");
        addColumnHeader(doingColumn, "EN COURS", doingCount, "#f59e0b");
        addColumnHeader(doneColumn, "TERMINÉ", doneCount, "#10b981");

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
        todoCount.setText(String.valueOf(todoTasks.size()));
        doingCount.setText(String.valueOf(doingTasks.size()));
        doneCount.setText(String.valueOf(doneTasks.size()));

        // Add tasks to columns
        todoTasks.forEach(task -> todoColumn.getChildren().add(createTaskCard(task)));
        doingTasks.forEach(task -> doingColumn.getChildren().add(createTaskCard(task)));
        doneTasks.forEach(task -> doneColumn.getChildren().add(createTaskCard(task)));

        // Add empty state messages
        if (todoTasks.isEmpty()) addEmptyState(todoColumn, "Aucune tâche à faire");
        if (doingTasks.isEmpty()) addEmptyState(doingColumn, "Aucune tâche en cours");
        if (doneTasks.isEmpty()) addEmptyState(doneColumn, "Aucune tâche terminée");
    }

    private void addColumnHeader(VBox column, String title, Label counter, String color) {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 15, 0));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#111827"));

        counter.setFont(Font.font("System", FontWeight.BOLD, 14));
        counter.setTextFill(Color.web(color));
        counter.setStyle("-fx-background-color: " + color + "20; -fx-background-radius: 20; -fx-padding: 5 12;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleLabel, spacer, counter);
        column.getChildren().add(header);
    }

    private VBox createTaskCard(Tache task) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 16; " +
                "-fx-padding: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        card.setMaxWidth(Double.MAX_VALUE);

        // Add hover effect
        card.setOnMouseEntered(e ->
                card.setStyle("-fx-background-color: #f9fafb; " +
                        "-fx-background-radius: 16; " +
                        "-fx-padding: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);" +
                        "-fx-cursor: hand;")
        );
        card.setOnMouseExited(e ->
                card.setStyle("-fx-background-color: white; " +
                        "-fx-background-radius: 16; " +
                        "-fx-padding: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);")
        );

        // Project badge
        try {
            String projectName = projetService.getProjetById(task.getProjetId()).getTitre();
            Label projectBadge = new Label(projectName);
            String color = colors[task.getProjetId() % colors.length];
            projectBadge.setStyle("-fx-background-color: " + color + "20; " +
                    "-fx-text-fill: " + color + "; " +
                    "-fx-background-radius: 12; " +
                    "-fx-padding: 4 12; " +
                    "-fx-font-size: 12px;");
            card.getChildren().add(projectBadge);
        } catch (SQLException e) {
            e.printStackTrace();
        }

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

        // Task ID and actions
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label idLabel = new Label("#" + task.getId());
        idLabel.setFont(Font.font("System", 12));
        idLabel.setTextFill(Color.web("#9ca3af"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status change buttons (only if not in DONE column)
        if (!"DONE".equals(task.getStatut())) {
            HBox actions = new HBox(5);

            if (!"DOING".equals(task.getStatut())) {
                Button startBtn = createActionButton("▶", "#f59e0b", "Commencer");
                startBtn.setOnAction(e -> updateTaskStatus(task, "DOING"));
                actions.getChildren().add(startBtn);
            }

            if (!"TODO".equals(task.getStatut())) {
                Button doneBtn = createActionButton("✓", "#10b981", "Terminer");
                doneBtn.setOnAction(e -> updateTaskStatus(task, "DONE"));
                actions.getChildren().add(doneBtn);
            }

            footer.getChildren().addAll(idLabel, spacer, actions);
        } else {
            // For done tasks, just show a completed badge
            Label completedBadge = new Label("✓ Terminé");
            completedBadge.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px;");
            footer.getChildren().addAll(idLabel, spacer, completedBadge);
        }

        card.getChildren().add(footer);

        return card;
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

    private void updateTaskStatus(Tache task, String newStatus) {
        try {
            task.setStatut(newStatus);
            tacheService.updateTache(task);
            refresh();
            showSuccess("Tâche " + task.getId() + " → " + newStatus);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur mise à jour");
        }
    }

    private void showError(String message) {
        msgLabel.setText(message);
        msgLabel.setStyle("-fx-text-fill: #ef4444;");
    }

    private void showSuccess(String message) {
        msgLabel.setText(message);
        msgLabel.setStyle("-fx-text-fill: #10b981;");
    }
}