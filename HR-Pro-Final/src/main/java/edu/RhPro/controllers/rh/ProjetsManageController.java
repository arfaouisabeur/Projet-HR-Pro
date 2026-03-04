package edu.RhPro.controllers.rh;



import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import edu.RhPro.entities.Projet;
import edu.RhPro.entities.Tache;
import edu.RhPro.entities.User;
import edu.RhPro.services.EmailService;
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
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.time.LocalDateTime;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import javafx.scene.control.ProgressBar;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.geometry.Pos;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.geometry.Insets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalTime;




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

    // ── KPI labels ──
    @FXML private Label statsTitleLabel;
    @FXML private Label kpiTotalLabel;
    @FXML private Label kpiTotalSub;
    @FXML private Label kpiTodoLabel;
    @FXML private Label kpiTodoPct;
    @FXML private Label kpiDoingLabel;
    @FXML private Label kpiDoingPct;
    @FXML private Label kpiDoneLabel;
    @FXML private Label kpiDonePct;

    // ── Progress bars ──
    @FXML private ProgressBar doneProgressBar;
    @FXML private ProgressBar doingProgressBar;
    @FXML private ProgressBar todoProgressBar;
    @FXML private Label donePctLabel;
    @FXML private Label doingPctLabel;
    @FXML private Label todoPctLabel;


    // gestions taches
    @FXML private TextField taskTitreField;
    @FXML private TextField taskDescField;
    @FXML private ComboBox<String> taskStatutCombo;
    @FXML private ComboBox<User> taskEmployeCombo;
    @FXML private Button cancelTaskButton;

    @FXML private Label taskTitreErrorLabel;
    @FXML private Label taskStatutErrorLabel;
    @FXML private Label taskEmployeErrorLabel;
    @FXML private Label taskMsgLabel;

    @FXML private VBox todoColumn;
    @FXML private VBox doingColumn;
    @FXML private VBox doneColumn;
    @FXML private Label todoCount;
    @FXML private Label doingCount;
    @FXML private Label doneCount;

    private ObservableList<Tache> allTasks;
    private Tache currentEditingTask = null; // null = add mode
    private VBox currentlySelectedCard = null;


    private List<User> allUsers;
    private final String selectedCardStyle = "-fx-background-color: #e0f2fe; " +
            "-fx-background-radius: 16; " +
            "-fx-padding: 15; " +
            "-fx-border-color: #3b82f6; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 16;";

    private final ProjetService projetService = new ProjetService();
    private final UserService userService = new UserService();
    private final EmailService emailService = new EmailService();
    private final TacheService tacheService = new TacheService();
    private List<User> allEmployees;
    private List<Projet> allProjects;
    private ScheduledExecutorService deadlineScheduler;

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


        try {
            allUsers = userService.getData();  // gets all users (any role)
        } catch (SQLException e) {
            e.printStackTrace();
            allUsers = new ArrayList<>();
        }

        // gestion taches
        // Setup task statut combo
        taskStatutCombo.setItems(FXCollections.observableArrayList("TODO", "DOING", "DONE"));

// Setup task employee combo
        setupTaskEmployeComboBox();
        loadEmployeesForTasks(); // separate method to load employees into task combo

// Add validation listeners for task fields
        addTaskValidationListeners();

// Listen to project table selection to load tasks (add this to the existing listener)
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                fillForm(selected);
                loadTasksForSelectedProject();  // <-- new call
            } else {
                clearTaskColumns();
            }
            if (statsPanel != null && statsPanel.isVisible()) {
                loadStats();
            }
        });

        // Setup table columns
        // Setup table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(column -> new TableCell<Projet, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) { setText(null); setGraphic(null); setStyle(""); return; }
                String bg, fg, label;
                switch (statut.toUpperCase()) {
                    case "DONE"  -> { bg = "#d1fae5"; fg = "#065f46"; label = "✔  DONE";  }
                    case "DOING" -> { bg = "#ddd6fe"; fg = "#6d2269"; label = "⚡ DOING"; }
                    default      -> { bg = "#fef3c7"; fg = "#b45309"; label = statut;      }
                }
                Label badge = new Label(label);
                badge.setStyle(
                        "-fx-background-color:" + bg + ";" +
                                "-fx-text-fill:" + fg + ";" +
                                "-fx-font-weight:700;" +
                                "-fx-font-size:11px;" +
                                "-fx-padding:3 10 3 10;" +
                                "-fx-background-radius:20;"
                );
                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment:CENTER-LEFT;");
            }
        });
        colResp.setCellValueFactory(new PropertyValueFactory<>("responsableEmployeId"));   // <-- ADD THIS
        colResp.setCellFactory(column -> new TableCell<Projet, Integer>() {
            @Override
            protected void updateItem(Integer respId, boolean empty) {
                super.updateItem(respId, empty);
                if (empty || respId == null) {
                    setText(null);
                } else {
                    String name = allUsers.stream()
                            .filter(u -> u.getId() == respId)
                            .map(u -> u.getNom() + " " + u.getPrenom())
                            .findFirst()
                            .orElse("ID: " + respId);
                    setText(name);
                }
            }
        });
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
        startDeadlineScheduler();
    }

    // ═══════════════════════════════════════════════════════════════
    //  SCHEDULER — vérifie chaque jour les deadlines des projets
    // ═══════════════════════════════════════════════════════════════
    private void startDeadlineScheduler() {
        deadlineScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "deadline-scheduler");
            t.setDaemon(true);
            return t;
        });
        deadlineScheduler.scheduleAtFixedRate(
                this::checkAndSendDeadlineEmails,
                0,
                24 * 3600,
                TimeUnit.SECONDS
        );
        System.out.println("⏰ Scheduler deadline démarré — vérification quotidienne active");
    }

    private void checkAndSendDeadlineEmails() {
        try {
            List<Projet> projets = projetService.getAllProjets();
            java.time.LocalDate today = java.time.LocalDate.now();
            int sent = 0;
            for (Projet projet : projets) {
                if ("DONE".equalsIgnoreCase(projet.getStatut())) continue;
                if (projet.getDateFin() == null) continue;
                java.time.LocalDate dateFin = projet.getDateFin();
                List<User> users = userService.getData();
                String emailResp = users.stream()
                        .filter(u -> u.getId() == projet.getResponsableEmployeId())
                        .map(User::getEmail)
                        .filter(e -> e != null && !e.isBlank())
                        .findFirst().orElse(null);
                if (emailResp == null) continue;
                String nomResp = users.stream()
                        .filter(u -> u.getId() == projet.getResponsableEmployeId())
                        .map(u -> u.getPrenom() + " " + u.getNom())
                        .findFirst().orElse("Responsable");
                if (dateFin.isEqual(today)) {
                    emailService.sendDeadlineEmail(emailResp, nomResp, projet.getTitre(), dateFin, "today");
                    System.out.println("📧 Email 'aujourd'hui' → " + projet.getTitre());
                    sent++;
                } else if (today.plusDays(3).isEqual(dateFin)) {
                    emailService.sendDeadlineEmail(emailResp, nomResp, projet.getTitre(), dateFin, "3days");
                    System.out.println("📧 Email '3 jours' → " + projet.getTitre());
                    sent++;
                } else if (dateFin.isBefore(today)) {
                    emailService.sendDeadlineEmail(emailResp, nomResp, projet.getTitre(), dateFin, "overdue");
                    System.out.println("📧 Email 'dépassé' → " + projet.getTitre());
                    sent++;
                }
            }
            System.out.println("✅ Vérification deadline terminée — " + sent + " email(s) envoyé(s)");
        } catch (Exception e) {
            System.err.println("❌ Erreur deadline scheduler : " + e.getMessage());
        }
    }

    private void loadStats() {
        try {
            Projet selected = table.getSelectionModel().getSelectedItem();

            // ── Normalise status data to UPPERCASE keys ──────────────────
            Map<String, Integer> rawData;
            if (selected != null) {
                rawData = tacheService.getTaskStatusCountByProject(selected.getId());
            } else {
                rawData = tacheService.getGlobalTaskStatus();
            }
            Map<String, Integer> statusData = new java.util.HashMap<>();
            rawData.forEach((k, v) -> statusData.merge(
                    k == null ? "INCONNU" : k.toUpperCase(), v, Integer::sum));

            // ── Counts ───────────────────────────────────────────────────
            int todo  = statusData.getOrDefault("TODO",  0);
            int doing = statusData.getOrDefault("DOING", 0);
            int done  = statusData.getOrDefault("DONE",  0);
            int total = todo + doing + done;

            // ── Title label ──────────────────────────────────────────────
            if (statsTitleLabel != null) {
                statsTitleLabel.setText(selected != null
                        ? "Tableau de bord — " + selected.getTitre()
                        : "Tableau de bord — Tous les projets");
            }

            // ── KPI labels ───────────────────────────────────────────────
            if (kpiTotalLabel != null) kpiTotalLabel.setText(String.valueOf(total));
            if (kpiTotalSub   != null) kpiTotalSub.setText(total == 0
                    ? "aucune tâche"
                    : "dans " + (selected != null ? "ce projet" : "tous les projets"));
            if (kpiTodoLabel  != null) kpiTodoLabel.setText(String.valueOf(todo));
            if (kpiTodoPct    != null) kpiTodoPct.setText(total == 0 ? "—" : pct(todo, total) + "% du total");
            if (kpiDoingLabel != null) kpiDoingLabel.setText(String.valueOf(doing));
            if (kpiDoingPct   != null) kpiDoingPct.setText(total == 0 ? "—" : pct(doing, total) + "% du total");
            if (kpiDoneLabel  != null) kpiDoneLabel.setText(String.valueOf(done));
            if (kpiDonePct    != null) kpiDonePct.setText(total == 0 ? "—" : pct(done, total) + "% du total");

            // ── Progress bars ────────────────────────────────────────────
            double donePct  = total > 0 ? (double) done  / total : 0;
            double doingPct = total > 0 ? (double) doing / total : 0;
            double todoPct  = total > 0 ? (double) todo  / total : 0;
            if (doneProgressBar  != null) doneProgressBar.setProgress(donePct);
            if (doingProgressBar != null) doingProgressBar.setProgress(doingPct);
            if (todoProgressBar  != null) todoProgressBar.setProgress(todoPct);
            if (donePctLabel  != null) donePctLabel.setText(total == 0 ? "0%" : Math.round(donePct  * 100) + "%");
            if (doingPctLabel != null) doingPctLabel.setText(total == 0 ? "0%" : Math.round(doingPct * 100) + "%");
            if (todoPctLabel  != null) todoPctLabel.setText(total == 0 ? "0%" : Math.round(todoPct  * 100) + "%");

            // ── Pie Chart ────────────────────────────────────────────────
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            if (total == 0) {
                pieData.add(new PieChart.Data("Aucune tâche", 1));
            } else {
                if (todo  > 0) pieData.add(new PieChart.Data("TODO ("  + todo  + ")", todo));
                if (doing > 0) pieData.add(new PieChart.Data("DOING (" + doing + ")", doing));
                if (done  > 0) pieData.add(new PieChart.Data("DONE ("  + done  + ")", done));
            }
            projectProgressChart.setData(pieData);
            projectProgressChart.setTitle(null);

            Platform.runLater(() -> {
                for (PieChart.Data d : projectProgressChart.getData()) {
                    String name = d.getName();
                    String color;
                    if (name.startsWith("DONE"))       color = "#5A2A80";
                    else if (name.startsWith("DOING")) color = "#7B3FAF";
                    else if (name.startsWith("TODO"))  color = "#C4A8DC";
                    else                                color = "#EDE0F5";
                    if (d.getNode() != null)
                        d.getNode().setStyle("-fx-pie-color:" + color + ";");
                }
            });

            // ── Bar Chart ────────────────────────────────────────────────
            Map<Integer, Integer> empTaskCounts = tacheService.getTaskCountPerEmployee();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Tâches assignées");
            for (Map.Entry<Integer, Integer> entry : empTaskCounts.entrySet()) {
                int empId = entry.getKey();
                int count = entry.getValue();
                String name = allEmployees.stream()
                        .filter(u -> u.getId() == empId)
                        .map(u -> u.getNom() + " " + u.getPrenom())
                        .findFirst()
                        .orElse("ID " + empId);
                series.getData().add(new XYChart.Data<>(name, count));
            }
            employeeTaskChart.getData().clear();
            employeeTaskChart.getData().add(series);
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

    private int pct(int part, int total) {
        if (total == 0) return 0;
        return (int) Math.round((double) part / total * 100);
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

            // ── Email : statut DONE OU date de fin atteinte/dépassée ──
            boolean isNowDone     = "DONE".equalsIgnoreCase(p.getStatut());
            boolean dateAtteinte  = p.getDateFin() != null
                    && !p.getDateFin().isAfter(LocalDate.now());

            if (isNowDone || dateAtteinte) {
                System.out.println("🔍 Recherche responsable ID: " + p.getResponsableEmployeId());

                // Chercher dans TOUS les users (pas seulement les employés)
                User responsable = null;
                try {
                    responsable = userService.getData().stream()
                            .filter(u -> u.getId() == p.getResponsableEmployeId())
                            .findFirst().orElse(null);
                } catch (Exception ex) {
                    System.err.println("❌ Erreur récupération responsable: " + ex.getMessage());
                }

                System.out.println("🔍 Responsable: " + (responsable != null ? responsable.getEmail() : "NULL"));

                if (responsable != null
                        && responsable.getEmail() != null
                        && !responsable.getEmail().isBlank()) {

                    String cas = isNowDone ? "today"
                            : p.getDateFin().isBefore(LocalDate.now()) ? "overdue" : "today";

                    emailService.sendDeadlineEmail(
                            responsable.getEmail(),
                            responsable.getPrenom() + " " + responsable.getNom(),
                            p.getTitre(),
                            p.getDateFin(),
                            cas
                    );
                    System.out.println("📧 Email envoyé à " + responsable.getEmail() + " [" + cas + "]");
                } else {
                    System.out.println("⚠️ Responsable introuvable ou sans email");
                }
            }
            // ─────────────────────────────────────────────────────────

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
            msgLabel.setText("⏳ Génération du rapport...");
            msgLabel.setStyle("-fx-text-fill: #3b82f6;");

            String htmlContent = buildHtmlReport(selected);

            // Save HTML to temp file
            java.io.File tempFile = java.io.File.createTempFile("rapport_projet_", ".html");
            tempFile.deleteOnExit();
            java.nio.file.Files.writeString(tempFile.toPath(), htmlContent, java.nio.charset.StandardCharsets.UTF_8);

            // Open in default browser — user prints to PDF with Ctrl+P
            java.awt.Desktop.getDesktop().browse(tempFile.toURI());

            msgLabel.setText("✅ Rapport ouvert — utilisez Ctrl+P dans le navigateur pour sauvegarder en PDF");
            msgLabel.setStyle("-fx-text-fill: #059669;");

        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur: " + e.getMessage());
            msgLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private String buildHtmlReport(Projet projet) {
        try {
            if (projet == null) throw new IllegalArgumentException("Projet is null");

            List<Tache> tasks = tacheService.findByProjetId(projet.getId());
            if (tasks == null) tasks = List.of();

            long totalTasks   = tasks.size();
            long todoCount    = tasks.stream().filter(t -> "TODO".equals(t.getStatut())).count();
            long doingCount   = tasks.stream().filter(t -> "DOING".equals(t.getStatut())).count();
            long doneCount    = tasks.stream().filter(t -> "DONE".equals(t.getStatut())).count();
            double completionRate = totalTasks > 0 ? (doneCount * 100.0 / totalTasks) : 0;

            String responsableName = getResponsibleName(projet.getResponsableEmployeId());
            if (responsableName == null) responsableName = "Non défini";

            String startDate = projet.getDateDebut() != null
                    ? projet.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
            String endDate = projet.getDateFin() != null
                    ? projet.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
            String description  = projet.getDescription() != null ? projet.getDescription() : "—";
            String generationDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String taskRows     = buildTaskRows(tasks);

            // ── Statut banner ─────────────────────────────────────────────
            boolean isDone   = "DONE".equalsIgnoreCase(projet.getStatut());
            String bannerBg  = isDone ? "#d1fae5" : "#ddd6fe";
            String bannerFg  = isDone ? "#065f46" : "#6d2269";
            String statutLabel = isDone ? "✔  PROJET TERMINÉ" : "⚡  PROJET EN COURS";

            // ── Logo base64 (embedded so the HTML is self-contained) ──────
            String logoBase64 = getLogoBase64();
            String logoTag = logoBase64.isEmpty()
                    ? "<span style='color:white;font-size:26px;font-weight:900;letter-spacing:2px;'>RH PRO</span>"
                    : "<img src='data:image/png;base64," + logoBase64 + "' style='height:48px;object-fit:contain;'/>";

            return String.format("""
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<title>Rapport Projet — RH Pro</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&display=swap');

  *, *::before, *::after { margin:0; padding:0; box-sizing:border-box; }

  body {
    font-family: 'Inter', 'Segoe UI', Arial, sans-serif;
    background: #f3f0fa;
    color: #1a1a1a;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  /* ── CARD ────────────────────────────────────────────── */
  .page {
    width: 800px;
    margin: 32px auto 48px;
    background: #ffffff;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 8px 40px rgba(109,34,105,0.13), 0 2px 8px rgba(0,0,0,0.06);
    border: 1.5px solid #e4d9f5;
  }

  /* ── HEADER ──────────────────────────────────────────── */
  .header {
    background: #6d2269;
    padding: 26px 36px 22px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    position: relative;
  }
  .header::after {
    content: '';
    position: absolute;
    bottom: 0; left: 0; right: 0;
    height: 3px;
    background: linear-gradient(90deg, #ddd6fe 0%%, #6d2269 50%%, #ddd6fe 100%%);
  }
  .brand { display: flex; align-items: center; gap: 14px; }
  .brand-text { color: white; }
  .brand-name { font-size: 26px; font-weight: 900; letter-spacing: 1.5px; color: #fff; }
  .brand-sub  { font-size: 10.5px; color: rgba(221,214,254,0.85); margin-top: 2px; }
  .header-right { text-align: right; }
  .meta-num  { font-size: 15px; font-weight: 800; color: #ffffff; letter-spacing: 0.5px; }
  .meta-date { font-size: 10px; color: rgba(221,214,254,0.8); margin-top: 3px; }

  /* ── STATUS BANNER ───────────────────────────────────── */
  .status-banner {
    background: %s;
    color: %s;
    text-align: center;
    padding: 11px 0;
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 3px;
    text-transform: uppercase;
    border-bottom: 1.5px solid rgba(109,34,105,0.12);
  }

  /* ── SECTION HEADER ──────────────────────────────────── */
  .section-hd {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 9px 36px;
    margin-top: 22px;
    background: #f5f0fd;
    border-left: 4px solid #6d2269;
    font-size: 10px;
    font-weight: 800;
    letter-spacing: 1.5px;
    color: #6d2269;
    text-transform: uppercase;
  }

  /* ── INFO TABLE ──────────────────────────────────────── */
  .info-wrap { padding: 0 36px; }
  .info-row {
    display: flex;
    align-items: stretch;
    border-bottom: 1px solid #f0eafa;
  }
  .info-row:last-child { border-bottom: none; }
  .info-key {
    width: 190px;
    flex-shrink: 0;
    padding: 10px 0;
    font-size: 11.5px;
    font-weight: 700;
    color: #6d2269;
  }
  .info-val {
    padding: 10px 0;
    font-size: 12.5px;
    color: #1a1a1a;
    font-weight: 500;
  }
  .info-row:nth-child(even) { background: #faf7fe; margin: 0 -36px; padding: 0 36px; }

  /* ── DESCRIPTION BOX ─────────────────────────────────── */
  .desc-box {
    margin: 4px 36px 0;
    padding: 14px 18px;
    background: #faf7fe;
    border-radius: 10px;
    font-size: 12.5px;
    color: #374151;
    line-height: 1.7;
    border: 1.5px solid #ddd6fe;
  }

  /* ── TASK TABLE ──────────────────────────────────────── */
  .tasks-wrap { padding: 4px 36px 0; }
  .tasks-table { width: 100%%; border-collapse: collapse; font-size: 11.5px; }
  .tasks-table thead tr { background: #6d2269; }
  .tasks-table th {
    color: #ffffff;
    padding: 10px 14px;
    text-align: left;
    font-weight: 700;
    font-size: 10px;
    letter-spacing: 0.8px;
    text-transform: uppercase;
  }
  .tasks-table td {
    padding: 9px 14px;
    border-bottom: 1px solid #f0eafa;
    color: #374151;
    vertical-align: middle;
  }
  .tasks-table tbody tr:nth-child(even) td { background: #faf7fe; }
  .tasks-table tbody tr:hover td { background: #f0eafa; }

  /* ── BADGES ──────────────────────────────────────────── */
  .badge {
    display: inline-block;
    padding: 3px 11px;
    border-radius: 20px;
    font-size: 9.5px;
    font-weight: 800;
    letter-spacing: 0.6px;
    text-transform: uppercase;
  }
  .badge-todo  { background: #fef3c7; color: #b45309; border: 1px solid #fcd34d; }
  .badge-doing { background: #dbeafe; color: #1d4ed8; border: 1px solid #93c5fd; }
  .badge-done  { background: #d1fae5; color: #065f46; border: 1px solid #6ee7b7; }

  .empty-tasks {
    text-align: center; padding: 22px;
    color: #9ca3af; font-style: italic; font-size: 12px;
  }

  /* ── RECAP ───────────────────────────────────────────── */
  .recap-section { margin-top: 22px; }
  .recap-hd {
    background: #6d2269;
    color: white;
    padding: 9px 36px;
    font-size: 10px;
    font-weight: 800;
    letter-spacing: 1.5px;
    text-transform: uppercase;
  }
  .recap-grid { display: flex; }
  .recap-cell { flex: 1; text-align: center; padding: 22px 10px; position: relative; }
  .recap-cell + .recap-cell::before {
    content: '';
    position: absolute;
    left: 0; top: 20%%; height: 60%%;
    width: 1px;
    background: rgba(255,255,255,0.2);
  }
  .recap-label {
    font-size: 9px; font-weight: 800;
    letter-spacing: 1.5px; text-transform: uppercase;
    color: rgba(255,255,255,0.8);
  }
  .recap-value { font-size: 32px; font-weight: 900; color: white; margin-top: 6px; }
  .recap-sub   { font-size: 9px; color: rgba(255,255,255,0.6); margin-top: 3px; }
  .cell-todo  { background: #d97706; }
  .cell-doing { background: #6d2269; }
  .cell-done  { background: #059669; }

  /* ── PROGRESS BAR ────────────────────────────────────── */
  .progress-section { padding: 18px 36px 4px; }
  .progress-row { margin-bottom: 10px; }
  .progress-label { font-size: 10.5px; font-weight: 600; color: #6d2269; margin-bottom: 4px; display: flex; justify-content: space-between; }
  .progress-track { height: 7px; background: #f0eafa; border-radius: 10px; overflow: hidden; }
  .progress-fill  { height: 100%%; border-radius: 10px; }
  .fill-done  { background: #059669; }
  .fill-doing { background: #6d2269; }
  .fill-todo  { background: #ddd6fe; }

  /* ── FOOTER ──────────────────────────────────────────── */
  .footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 13px 36px;
    margin-top: 22px;
    border-top: 1.5px solid #f0eafa;
    font-size: 9.5px;
    color: #9ca3af;
    font-style: italic;
  }
  .footer-badge {
    background: #ddd6fe;
    color: #6d2269;
    font-weight: 700;
    font-style: normal;
    padding: 3px 12px;
    border-radius: 20px;
    font-size: 9px;
    letter-spacing: 0.5px;
  }

  @media print {
    body { background: white; }
    .page { box-shadow: none; margin: 0; width: 100%%; border: none; border-radius: 0; }
  }
</style>
</head>
<body>
<div class="page">

  <!-- HEADER -->
  <div class="header">
    <div class="brand">
      %s
      <div class="brand-text">
        <div class="brand-name">RH PRO</div>
        <div class="brand-sub">Système de gestion des ressources humaines</div>
      </div>
    </div>
    <div class="header-right">
      <div class="meta-num">PROJET #%d</div>
      <div class="meta-date">Généré le %s</div>
    </div>
  </div>

  <!-- STATUS BANNER -->
  <div class="status-banner">%s</div>

  <!-- INFORMATIONS -->
  <div class="section-hd">Informations du projet</div>
  <div class="info-wrap">
    <div class="info-row"><div class="info-key">Titre du projet</div><div class="info-val">%s</div></div>
    <div class="info-row"><div class="info-key">Responsable</div><div class="info-val">%s</div></div>
    <div class="info-row"><div class="info-key">Date de début</div><div class="info-val">%s</div></div>
    <div class="info-row"><div class="info-key">Date de fin</div><div class="info-val">%s</div></div>
    <div class="info-row"><div class="info-key">Total tâches</div><div class="info-val">%d tâche(s)</div></div>
    <div class="info-row"><div class="info-key">Taux de complétion</div><div class="info-val">%.1f%%</div></div>
  </div>

  <!-- PROGRESS BARS -->
  <div class="progress-section">
    <div class="progress-row">
      <div class="progress-label"><span>Terminées</span><span>%d / %d</span></div>
      <div class="progress-track"><div class="progress-fill fill-done" style="width:%.1f%%"></div></div>
    </div>
    <div class="progress-row">
      <div class="progress-label"><span>En cours</span><span>%d / %d</span></div>
      <div class="progress-track"><div class="progress-fill fill-doing" style="width:%.1f%%"></div></div>
    </div>
    <div class="progress-row">
      <div class="progress-label"><span>À faire</span><span>%d / %d</span></div>
      <div class="progress-track"><div class="progress-fill fill-todo" style="width:%.1f%%"></div></div>
    </div>
  </div>

  <!-- DESCRIPTION -->
  <div class="section-hd">Description du projet</div>
  <div class="desc-box">%s</div>

  <!-- TÂCHES -->
  <div class="section-hd">Liste des tâches</div>
  <div class="tasks-wrap">
    <table class="tasks-table">
      <thead>
        <tr>
          <th style="width:28%%">Titre</th>
          <th style="width:13%%">Statut</th>
          <th style="width:22%%">Assigné à</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        %s
      </tbody>
    </table>
  </div>

  <!-- RÉCAP -->
  <div class="recap-section">
    <div class="recap-hd">Récapitulatif</div>
    <div class="recap-grid">
      <div class="recap-cell cell-todo">
        <div class="recap-label">À faire</div>
        <div class="recap-value">%d</div>
        <div class="recap-sub">tâche(s)</div>
      </div>
      <div class="recap-cell cell-doing">
        <div class="recap-label">En cours</div>
        <div class="recap-value">%d</div>
        <div class="recap-sub">tâche(s)</div>
      </div>
      <div class="recap-cell cell-done">
        <div class="recap-label">Terminées</div>
        <div class="recap-value">%d</div>
        <div class="recap-sub">tâche(s)</div>
      </div>
    </div>
  </div>

  <!-- FOOTER -->
  <div class="footer">
    <span>Document généré automatiquement par RH Pro — %s</span>
    <span class="footer-badge">Projet #%d &nbsp;•&nbsp; Confidentiel</span>
  </div>

</div>
</body>
</html>
""",
                    bannerBg, bannerFg,
                    logoTag,
                    projet.getId(), generationDate,
                    statutLabel,
                    projet.getTitre(), responsableName,
                    startDate, endDate,
                    totalTasks, completionRate,
                    // progress bars: done
                    doneCount, totalTasks, totalTasks > 0 ? (doneCount * 100.0 / totalTasks) : 0,
                    // doing
                    doingCount, totalTasks, totalTasks > 0 ? (doingCount * 100.0 / totalTasks) : 0,
                    // todo
                    todoCount, totalTasks, totalTasks > 0 ? (todoCount * 100.0 / totalTasks) : 0,
                    description,
                    taskRows.isEmpty()
                            ? "<tr><td colspan='4' class='empty-tasks'>Aucune tâche pour ce projet</td></tr>"
                            : taskRows,
                    todoCount, doingCount, doneCount,
                    generationDate, projet.getId()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return "<html><body><h3>Erreur: " + e.getMessage() + "</h3></body></html>";
        }
    }

    /**
     * Loads the RH Pro logo as a Base64 string for embedding in HTML reports.
     * Looks for the logo in the classpath resources.
     */
    private String getLogoBase64() {
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/images/logo.png");
            if (is == null) return "";
            byte[] bytes = is.readAllBytes();
            is.close();
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return "";
        }
    }

    private String buildTaskRows(List<Tache> tasks) {
        if (tasks == null) return "";
        StringBuilder rows = new StringBuilder();
        for (Tache task : tasks) {
            if (task == null) continue;
            String status = task.getStatut() != null ? task.getStatut() : "INCONNU";
            String badgeClass = switch (status) {
                case "TODO"  -> "badge badge-todo";
                case "DOING" -> "badge badge-doing";
                case "DONE"  -> "badge badge-done";
                default      -> "badge";
            };
            rows.append(String.format("""
        <tr>
            <td>%s</td>
            <td><span class="%s">%s</span></td>
            <td>%s</td>
            <td>%s</td>
        </tr>
        """,
                    escapeHtml(task.getTitre()),
                    badgeClass,
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



    // ==================== TASK MANAGEMENT METHODS ====================

    private void setupTaskEmployeComboBox() {
        taskEmployeCombo.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                if (user == null) return "";
                return user.getNom() + " " + user.getPrenom();
            }
            @Override
            public User fromString(String string) { return null; }
        });

        taskEmployeCombo.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) setText(null);
                else setText(user.getNom() + " " + user.getPrenom());
            }
        });
    }

    private void loadEmployeesForTasks() {
        try {
            List<User> employees = userService.getData().stream()
                    .filter(u -> "EMPLOYE".equalsIgnoreCase(u.getRole()))
                    .collect(Collectors.toList());
            taskEmployeCombo.setItems(FXCollections.observableArrayList(employees));
        } catch (SQLException e) {
            e.printStackTrace();
            taskMsgLabel.setText("❌ Erreur chargement employés.");
        }
    }

    private void addTaskValidationListeners() {
        taskTitreField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty())
                hideTaskError(taskTitreField, taskTitreErrorLabel);
        });
        taskStatutCombo.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null)
                hideTaskError(taskStatutCombo, taskStatutErrorLabel);
        });
        taskEmployeCombo.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null)
                hideTaskError(taskEmployeCombo, taskEmployeErrorLabel);
        });
    }

    private void hideTaskError(Control field, Label errorLabel) {
        field.setStyle(normalFieldStyle);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showTaskError(Control field, Label errorLabel, String msg) {
        field.setStyle(errorFieldStyle);
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private boolean validateTaskForm() {
        boolean ok = true;
        if (taskTitreField.getText() == null || taskTitreField.getText().trim().isEmpty()) {
            showTaskError(taskTitreField, taskTitreErrorLabel, "Le titre est requis");
            ok = false;
        } else {
            hideTaskError(taskTitreField, taskTitreErrorLabel);
        }
        if (taskStatutCombo.getValue() == null) {
            showTaskError(taskStatutCombo, taskStatutErrorLabel, "Statut requis");
            ok = false;
        } else {
            hideTaskError(taskStatutCombo, taskStatutErrorLabel);
        }
        if (taskEmployeCombo.getValue() == null) {
            showTaskError(taskEmployeCombo, taskEmployeErrorLabel, "Employé requis");
            ok = false;
        } else {
            hideTaskError(taskEmployeCombo, taskEmployeErrorLabel);
        }
        return ok;
    }

    private void clearTaskForm() {
        taskTitreField.clear();
        taskDescField.clear();
        taskStatutCombo.setValue(null);
        taskEmployeCombo.setValue(null);
        hideTaskError(taskTitreField, taskTitreErrorLabel);
        hideTaskError(taskStatutCombo, taskStatutErrorLabel);
        hideTaskError(taskEmployeCombo, taskEmployeErrorLabel);
    }

    private void loadTasksForSelectedProject() {
        Projet selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            clearTaskColumns();
            return;
        }
        try {
            List<Tache> tasks = tacheService.findByProjetId(selected.getId());
            allTasks = FXCollections.observableArrayList(tasks);
            renderKanban(allTasks);
            taskMsgLabel.setText(allTasks.size() + " tâche(s)");
        } catch (SQLException e) {
            e.printStackTrace();
            taskMsgLabel.setText("❌ Erreur chargement tâches.");
        }
    }

    private void clearTaskColumns() {
        todoColumn.getChildren().clear();
        doingColumn.getChildren().clear();
        doneColumn.getChildren().clear();
        todoCount.setText("0");
        doingCount.setText("0");
        doneCount.setText("0");
        currentlySelectedCard = null;
    }

    private void renderKanban(ObservableList<Tache> tasks) {
        todoColumn.getChildren().clear();
        doingColumn.getChildren().clear();
        doneColumn.getChildren().clear();

        List<Tache> todoTasks = tasks.stream().filter(t -> "TODO".equals(t.getStatut())).collect(Collectors.toList());
        List<Tache> doingTasks = tasks.stream().filter(t -> "DOING".equals(t.getStatut())).collect(Collectors.toList());
        List<Tache> doneTasks = tasks.stream().filter(t -> "DONE".equals(t.getStatut())).collect(Collectors.toList());

        todoCount.setText(todoTasks.size() + " tâche(s)");
        doingCount.setText(doingTasks.size() + " tâche(s)");
        doneCount.setText(doneTasks.size() + " tâche(s)");

        todoTasks.forEach(task -> todoColumn.getChildren().add(createTaskCard(task)));
        doingTasks.forEach(task -> doingColumn.getChildren().add(createTaskCard(task)));
        doneTasks.forEach(task -> doneColumn.getChildren().add(createTaskCard(task)));

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
        card.setUserData(task);

        card.setOnMouseClicked(e -> {
            if (currentlySelectedCard != null) {
                resetCardStyle(currentlySelectedCard);
            }
            card.setStyle(selectedCardStyle);
            currentlySelectedCard = card;
            if (e.getClickCount() == 2) {
                editTask(task);
            }
        });

        // Employee badge
        String employeeName = getEmployeeName(task.getEmployeId());
        Label employeeBadge = new Label("👤 " + employeeName);
        employeeBadge.setStyle("-fx-background-color: #f3f4f6; " +
                "-fx-text-fill: #4b5563; " +
                "-fx-background-radius: 12; " +
                "-fx-padding: 4 12; " +
                "-fx-font-size: 12px;");
        card.getChildren().add(employeeBadge);

        // Title
        Label titleLabel = new Label(task.getTitre());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #111827;");
        titleLabel.setWrapText(true);
        card.getChildren().add(titleLabel);

        // Description
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            Label descLabel = new Label(task.getDescription());
            descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");
            descLabel.setWrapText(true);
            card.getChildren().add(descLabel);
        }

        // Footer with actions
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label idLabel = new Label("#" + task.getId());
        idLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(5);

        // Edit button
        Button editBtn = createActionButton("✎", "#3b82f6", "Modifier");
        editBtn.setOnAction(e -> {
            editTask(task);
            if (currentlySelectedCard != null) resetCardStyle(currentlySelectedCard);
            card.setStyle(selectedCardStyle);
            currentlySelectedCard = card;
        });
        actions.getChildren().add(editBtn);

        // Status change buttons (only if not DONE)
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
        emptyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #9ca3af;");
        emptyBox.getChildren().add(emptyLabel);
        column.getChildren().add(emptyBox);
    }

    private void editTask(Tache task) {
        currentEditingTask = task;
        taskTitreField.setText(task.getTitre());
        taskDescField.setText(task.getDescription());
        taskStatutCombo.setValue(task.getStatut());
        User assigned = taskEmployeCombo.getItems().stream()
                .filter(u -> u.getId() == task.getEmployeId())
                .findFirst().orElse(null);
        taskEmployeCombo.setValue(assigned);
        cancelTaskButton.setVisible(true);
        cancelTaskButton.setManaged(true);
        taskMsgLabel.setText("Édition de la tâche #" + task.getId());
    }

    @FXML
    public void cancelTaskEdit() {
        currentEditingTask = null;
        clearTaskForm();
        cancelTaskButton.setVisible(false);
        cancelTaskButton.setManaged(false);
        taskMsgLabel.setText("Édition annulée.");
    }

    @FXML
    public void saveTask() {
        Projet selectedProject = table.getSelectionModel().getSelectedItem();
        if (selectedProject == null) {
            taskMsgLabel.setText("⚠️ Aucun projet sélectionné.");
            return;
        }

        if (!validateTaskForm()) {
            taskMsgLabel.setText("⚠️ Veuillez corriger les erreurs.");
            return;
        }

        try {
            User assignedEmployee = taskEmployeCombo.getValue();
            if (currentEditingTask == null) {
                // Add mode
                Tache t = new Tache(
                        taskTitreField.getText().trim(),
                        taskDescField.getText(),
                        taskStatutCombo.getValue(),
                        selectedProject.getId(),
                        assignedEmployee.getId(),
                        null
                );
                tacheService.addTache(t);
                sendTaskAssignedEmail(assignedEmployee, t, selectedProject);
                taskMsgLabel.setText("✅ Tâche ajoutée.");
            } else {
                // Update mode
                boolean employeeChanged = currentEditingTask.getEmployeId() != assignedEmployee.getId();
                currentEditingTask.setTitre(taskTitreField.getText().trim());
                currentEditingTask.setDescription(taskDescField.getText());
                currentEditingTask.setStatut(taskStatutCombo.getValue());
                currentEditingTask.setEmployeId(assignedEmployee.getId());
                tacheService.updateFullTache(currentEditingTask);
                if (employeeChanged) {
                    sendTaskAssignedEmail(assignedEmployee, currentEditingTask, selectedProject);
                }
                taskMsgLabel.setText("✅ Tâche modifiée.");
            }

            // Reset to add mode
            currentEditingTask = null;
            cancelTaskButton.setVisible(false);
            cancelTaskButton.setManaged(false);
            clearTaskForm();
            loadTasksForSelectedProject();

        } catch (SQLException e) {
            e.printStackTrace();
            taskMsgLabel.setText("❌ Erreur sauvegarde tâche.");
        }
    }

    private void sendTaskAssignedEmail(User employee, Tache task, Projet project) {
        if (employee == null || employee.getEmail() == null || employee.getEmail().isBlank()) return;
        emailService.sendTaskAssignedEmail(
                employee.getEmail(),
                employee.getPrenom() + " " + employee.getNom(),
                task.getTitre(),
                project.getTitre()
        );
    }

    @FXML
    public void deleteSelectedTask() {
        if (currentlySelectedCard == null) {
            taskMsgLabel.setText("⚠️ Sélectionnez une tâche.");
            return;
        }
        Tache selectedTask = (Tache) currentlySelectedCard.getUserData();
        if (selectedTask == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la tâche");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette tâche ?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                tacheService.deleteTache(selectedTask.getId());
                if (currentEditingTask != null && currentEditingTask.getId() == selectedTask.getId()) {
                    cancelTaskEdit();
                }
                loadTasksForSelectedProject();
                taskMsgLabel.setText("✅ Tâche supprimée.");
            } catch (SQLException e) {
                e.printStackTrace();
                taskMsgLabel.setText("❌ Erreur suppression.");
            }
        }
    }

    private void updateTaskStatus(Tache task, String newStatus) {
        try {
            task.setStatut(newStatus);
            tacheService.updateTache(task);
            loadTasksForSelectedProject();
            taskMsgLabel.setText("Statut mis à jour.");
        } catch (SQLException e) {
            e.printStackTrace();
            taskMsgLabel.setText("❌ Erreur mise à jour.");
        }
    }


    private void showPdfInWebView(String pdfUrl) {
        Stage stage = new Stage();
        stage.setTitle("Rapport PDF");

        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        try {
            // Encode the PDF URL to use as a query parameter
            String encodedUrl = URLEncoder.encode(pdfUrl, StandardCharsets.UTF_8);
            // Use Google Docs Viewer to display the PDF
            String viewerUrl = "https://docs.google.com/viewer?url=" + encodedUrl + "&embedded=true";
            webEngine.load(viewerUrl);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: try loading the PDF directly (may still be blank, but worth a try)
            webEngine.load(pdfUrl);
        }

        Scene scene = new Scene(webView, 900, 700);
        stage.setScene(scene);
        stage.show();
    }
}