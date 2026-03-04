package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Projet;
import edu.RhPro.entities.Tache;
import edu.RhPro.entities.User;
import edu.RhPro.services.EmailService;
import edu.RhPro.services.ProjetService;
import edu.RhPro.services.TacheService;
import edu.RhPro.services.UserService;
import edu.RhPro.utils.Session;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ProjetTachesController {

    @FXML private Label      titleLabel;
    @FXML private Label      formTitleLabel;
    @FXML private TextField  titreField;
    @FXML private TextField  descField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private ComboBox<User>   employeCombo;

    @FXML private VBox todoColumn;
    @FXML private VBox doingColumn;
    @FXML private VBox doneColumn;

    @FXML private Label todoCount;
    @FXML private Label doingCount;
    @FXML private Label doneCount;

    @FXML private Label     msgLabel;
    @FXML private Label     titreErrorLabel;
    @FXML private Label     statutErrorLabel;
    @FXML private Label     employeErrorLabel;
    @FXML private TextField searchField;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Projet projet;
    private final ProjetService projetService = new ProjetService();
    private final TacheService  tacheService  = new TacheService();
    private final UserService   userService   = new UserService();
    private final EmailService  emailService  = new EmailService();

    private List<User>            allEmployees;
    private ObservableList<Tache> allTasks;
    private Tache  currentEditingTask    = null;
    private VBox   currentlySelectedCard = null;

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final String PURPLE     = "#6d2269";
    private static final String LAVENDER   = "#ddd6fe";
    private static final String LAV_BG     = "#f5f0fd";
    private static final String LAV_MID    = "#ede9fc";

    // Field styles
    private final String fieldNormal  = "-fx-border-color:#ddd6fe;-fx-border-width:1.5;" +
            "-fx-border-radius:10;-fx-background-radius:10;-fx-background-color:#fafafa;";
    private final String fieldError   = "-fx-border-color:#ef4444;-fx-border-width:2;" +
            "-fx-border-radius:10;-fx-background-radius:10;";
    private final String cardSelected = "-fx-background-color:" + LAV_BG + ";" +
            "-fx-background-radius:10;" +
            "-fx-border-color:" + PURPLE + ";-fx-border-width:2;" +
            "-fx-border-radius:10;" +
            "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.22),14,0,0,4);" +
            "-fx-cursor:hand;";

    // ─────────────────────────────────────────────────────────────────────────
    public void setProjet(Projet p) {
        this.projet = p;
        titleLabel.setText("📋  " + p.getTitre() + "  —  Gestion des tâches");
        refresh();
    }

    @FXML
    public void initialize() {
        statutCombo.setItems(FXCollections.observableArrayList("TODO", "DOING", "DONE"));
        setupEmployeCombo();
        loadEmployees();
        setupSearch();
        addValidationListeners();
        if (cancelButton != null) { cancelButton.setVisible(false); cancelButton.setManaged(false); }
        if (saveButton   != null) saveButton.setText("Ajouter");
        setupButtonHovers();
    }

    // ── Button hover effects ──────────────────────────────────────────────────
    private void setupButtonHovers() {
        if (saveButton != null) {
            String base  = "-fx-background-color:" + PURPLE + ";-fx-text-fill:white;" +
                    "-fx-background-radius:20;-fx-padding:9 24;" +
                    "-fx-font-weight:700;-fx-font-size:12px;-fx-cursor:hand;" +
                    "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.35),10,0,0,3);";
            String hover = "-fx-background-color:#8b2e87;-fx-text-fill:white;" +
                    "-fx-background-radius:20;-fx-padding:9 24;" +
                    "-fx-font-weight:700;-fx-font-size:12px;-fx-cursor:hand;" +
                    "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.55),14,0,0,5);";
            saveButton.setStyle(base);
            saveButton.setOnMouseEntered(e -> saveButton.setStyle(hover));
            saveButton.setOnMouseExited(e  -> saveButton.setStyle(base));
            saveButton.setOnMousePressed(e -> {
                ScaleTransition s = new ScaleTransition(Duration.millis(80), saveButton);
                s.setToX(0.94); s.setToY(0.94); s.play();
            });
            saveButton.setOnMouseReleased(e -> {
                ScaleTransition s = new ScaleTransition(Duration.millis(80), saveButton);
                s.setToX(1.0); s.setToY(1.0); s.play();
            });
        }
    }

    // ── Employe ComboBox ──────────────────────────────────────────────────────
    private void setupEmployeCombo() {
        employeCombo.setConverter(new StringConverter<User>() {
            @Override public String toString(User u)       { return u == null ? "" : u.getNom() + " " + u.getPrenom(); }
            @Override public User fromString(String s)     { return null; }
        });
        employeCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getNom() + " " + u.getPrenom());
            }
        });
    }

    private void loadEmployees() {
        try {
            allEmployees = userService.getData().stream()
                    .filter(u -> "EMPLOYE".equalsIgnoreCase(u.getRole()))
                    .collect(Collectors.toList());
            employeCombo.setItems(FXCollections.observableArrayList(allEmployees));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    private void setupSearch() {
        if (searchField != null)
            searchField.textProperty().addListener((obs, old, nv) -> filterTasks(nv));
    }

    private void filterTasks(String q) {
        if (allTasks == null) return;
        if (q == null || q.isEmpty()) { renderKanban(allTasks); return; }
        List<Tache> filtered = allTasks.stream()
                .filter(t -> t.getTitre().toLowerCase().contains(q.toLowerCase()) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(q.toLowerCase())))
                .collect(Collectors.toList());
        renderKanban(FXCollections.observableArrayList(filtered));
    }

    // ── Kanban render ─────────────────────────────────────────────────────────
    private void renderKanban(ObservableList<Tache> tasks) {
        todoColumn.getChildren().clear();
        doingColumn.getChildren().clear();
        doneColumn.getChildren().clear();

        List<Tache> todo  = tasks.stream().filter(t -> "TODO".equals(t.getStatut())).collect(Collectors.toList());
        List<Tache> doing = tasks.stream().filter(t -> "DOING".equals(t.getStatut())).collect(Collectors.toList());
        List<Tache> done  = tasks.stream().filter(t -> "DONE".equals(t.getStatut())).collect(Collectors.toList());

        updateCounter(todoCount,  todo.size());
        updateCounter(doingCount, doing.size());
        updateCounter(doneCount,  done.size());

        addStaggered(todoColumn,  todo);
        addStaggered(doingColumn, doing);
        addStaggered(doneColumn,  done);

        if (todo.isEmpty())  addEmptyState(todoColumn,  "Aucune tâche à faire");
        if (doing.isEmpty()) addEmptyState(doingColumn, "Aucune tâche en cours");
        if (done.isEmpty())  addEmptyState(doneColumn,  "Aucune tâche terminée");
    }

    private void updateCounter(Label lbl, int count) {
        if (lbl != null) lbl.setText(String.valueOf(count));
    }

    private void addStaggered(VBox col, List<Tache> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            VBox card = createCard(tasks.get(i));
            card.setOpacity(0);
            card.setTranslateY(10);
            col.getChildren().add(card);

            FadeTransition ft = new FadeTransition(Duration.millis(220), card);
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(220), card);
            tt.setFromY(10); tt.setToY(0);
            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.setDelay(Duration.millis(i * 50));
            pt.play();
        }
    }

    // ── Card builder ──────────────────────────────────────────────────────────
    private VBox createCard(Tache task) {
        String statut = task.getStatut() != null ? task.getStatut() : "TODO";

        String accentFg, accentBg, topColor;
        if ("DOING".equals(statut)) {
            accentFg = PURPLE;    accentBg = LAVENDER; topColor = PURPLE;
        } else if ("DONE".equals(statut)) {
            accentFg = "#065f46"; accentBg = "#d1fae5"; topColor = "#059669";
        } else {
            accentFg = "#b45309"; accentBg = "#fef3c7"; topColor = "#d97706";
        }

        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setUserData(task);

        String baseStyle =
                "-fx-background-color:white;-fx-background-radius:10;" +
                        "-fx-border-color:transparent transparent transparent " + accentFg + ";" +
                        "-fx-border-width:0 0 0 3;-fx-border-radius:10 0 0 10;" +
                        "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.07),7,0,0,2);";
        String hoverStyle =
                "-fx-background-color:" + LAV_BG + ";-fx-background-radius:10;" +
                        "-fx-border-color:transparent transparent transparent " + accentFg + ";" +
                        "-fx-border-width:0 0 0 3;-fx-border-radius:10 0 0 10;" +
                        "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.18),14,0,0,4);" +
                        "-fx-cursor:hand;";
        card.setStyle(baseStyle);

        // Top accent bar
        HBox topBar = new HBox();
        topBar.setPrefHeight(3);
        topBar.setMaxWidth(Double.MAX_VALUE);
        topBar.setStyle("-fx-background-color:" + topColor + ";-fx-background-radius:10 10 0 0;");

        // Body
        VBox body = new VBox(7);
        body.setPadding(new Insets(10, 12, 10, 12));

        // Row 1: employee badge + id
        HBox row1 = new HBox(6);
        row1.setAlignment(Pos.CENTER_LEFT);

        // Employee badge
        String empName = "Non assigné";
        try {
            empName = userService.getData().stream()
                    .filter(u -> u.getId() == task.getEmployeId())
                    .map(u -> u.getNom() + " " + u.getPrenom())
                    .findFirst().orElse("Inconnu");
        } catch (SQLException e) { e.printStackTrace(); }

        Circle empDot = new Circle(4, Color.web(accentFg));
        Label empLbl = new Label(empName);
        empLbl.setStyle(
                "-fx-background-color:" + accentBg + ";-fx-text-fill:" + accentFg + ";" +
                        "-fx-font-size:10px;-fx-font-weight:700;" +
                        "-fx-padding:2 9;-fx-background-radius:20;"
        );
        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        Label idLbl = new Label("#" + task.getId());
        idLbl.setStyle("-fx-text-fill:#9ca3af;-fx-font-size:10px;-fx-font-weight:600;");
        row1.getChildren().addAll(empLbl, sp1, idLbl);
        body.getChildren().add(row1);

        // Title
        Label title = new Label(task.getTitre());
        title.setFont(Font.font("System", FontWeight.BOLD, 13));
        title.setTextFill(Color.web("#1a1a1a"));
        title.setWrapText(true);
        body.getChildren().add(title);

        // Description
        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            Label desc = new Label(task.getDescription());
            desc.setFont(Font.font("System", 11.5));
            desc.setTextFill(Color.web("#6b7280"));
            desc.setWrapText(true);
            body.getChildren().add(desc);
        }

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-opacity:0.35;");
        sep.setPadding(new Insets(2, 0, 2, 0));
        body.getChildren().add(sep);

        // Footer: status pill + action buttons
        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER_LEFT);

        // Status pill
        Label pill = new Label();
        if ("DOING".equals(statut))     pill.setText("EN COURS");
        else if ("DONE".equals(statut)) pill.setText("TERMINE");
        else                            pill.setText("A FAIRE");
        pill.setStyle(
                "-fx-background-color:" + accentBg + ";-fx-text-fill:" + accentFg + ";" +
                        "-fx-font-size:9px;-fx-font-weight:800;" +
                        "-fx-padding:2 8;-fx-background-radius:20;"
        );
        footer.getChildren().add(pill);

        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        footer.getChildren().add(sp2);

        // Edit button
        Button editBtn = buildBtn("✎  Modifier", "#374151", "#f3f4f6");
        editBtn.setOnAction(e -> {
            deselectCurrent();
            card.setStyle(cardSelected);
            currentlySelectedCard = card;
            editTask(task);
        });
        footer.getChildren().add(editBtn);

        // Status advance button
        if ("TODO".equals(statut)) {
            Button startBtn = buildBtn("▶  Démarrer", PURPLE, LAVENDER);
            startBtn.setOnAction(e -> updateTaskStatus(task, "DOING"));
            footer.getChildren().add(startBtn);
        } else if ("DOING".equals(statut)) {
            Button doneBtn = buildBtn("✔  Terminer", "#059669", "#d1fae5");
            doneBtn.setOnAction(e -> updateTaskStatus(task, "DONE"));
            footer.getChildren().add(doneBtn);
        }

        body.getChildren().add(footer);
        card.getChildren().addAll(topBar, body);

        // Click to select
        card.setOnMouseClicked(e -> {
            deselectCurrent();
            card.setStyle(cardSelected);
            currentlySelectedCard = card;
            if (e.getClickCount() == 2) editTask(task);
        });

        // Hover (only when not selected)
        card.setOnMouseEntered(e -> {
            if (card != currentlySelectedCard) {
                card.setStyle(hoverStyle);
                ScaleTransition s = new ScaleTransition(Duration.millis(120), card);
                s.setToX(1.012); s.setToY(1.012); s.play();
            }
        });
        card.setOnMouseExited(e -> {
            if (card != currentlySelectedCard) {
                card.setStyle(baseStyle);
                ScaleTransition s = new ScaleTransition(Duration.millis(120), card);
                s.setToX(1.0); s.setToY(1.0); s.play();
            }
        });

        return card;
    }

    private Button buildBtn(String text, String fg, String bg) {
        Button btn = new Button(text);
        String base  = "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:10.5px;-fx-font-weight:700;" +
                "-fx-background-radius:20;-fx-padding:4 12;-fx-cursor:hand;";
        String hover = "-fx-background-color:" + fg + ";-fx-text-fill:white;" +
                "-fx-font-size:10.5px;-fx-font-weight:700;" +
                "-fx-background-radius:20;-fx-padding:4 12;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnMousePressed(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(70), btn);
            s.setToX(0.92); s.setToY(0.92); s.play();
        });
        btn.setOnMouseReleased(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(70), btn);
            s.setToX(1.0); s.setToY(1.0); s.play();
        });
        return btn;
    }

    private void addEmptyState(VBox col, String msg) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28));
        box.setStyle(
                "-fx-background-color:" + LAV_BG + ";-fx-background-radius:10;" +
                        "-fx-border-color:" + LAVENDER + ";-fx-border-style:dashed;" +
                        "-fx-border-radius:10;-fx-border-width:1.5;"
        );
        Label ic = new Label("📋");
        ic.setStyle("-fx-font-size:20px;-fx-opacity:0.4;");
        Label lb = new Label(msg);
        lb.setStyle("-fx-font-size:11.5px;-fx-text-fill:#9ca3af;-fx-font-style:italic;");
        box.getChildren().addAll(ic, lb);
        col.getChildren().add(box);
    }

    private void deselectCurrent() {
        if (currentlySelectedCard != null) {
            Tache t = (Tache) currentlySelectedCard.getUserData();
            String st = t != null ? t.getStatut() : "TODO";
            String fg = "DOING".equals(st) ? PURPLE : ("DONE".equals(st) ? "#065f46" : "#b45309");
            currentlySelectedCard.setStyle(
                    "-fx-background-color:white;-fx-background-radius:10;" +
                            "-fx-border-color:transparent transparent transparent " + fg + ";" +
                            "-fx-border-width:0 0 0 3;-fx-border-radius:10 0 0 10;" +
                            "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.07),7,0,0,2);"
            );
        }
    }

    // ── Edit task ─────────────────────────────────────────────────────────────
    private void editTask(Tache task) {
        if (!isResponsible()) { showMsg("Seul le responsable peut modifier.", "#ef4444"); return; }
        currentEditingTask = task;
        titreField.setText(task.getTitre());
        descField.setText(task.getDescription() != null ? task.getDescription() : "");
        statutCombo.setValue(task.getStatut());
        try {
            User u = userService.getData().stream()
                    .filter(x -> x.getId() == task.getEmployeId()).findFirst().orElse(null);
            employeCombo.setValue(u);
        } catch (SQLException e) { e.printStackTrace(); }
        if (saveButton   != null) saveButton.setText("Modifier");
        if (cancelButton != null) { cancelButton.setVisible(true); cancelButton.setManaged(true); }
        if (formTitleLabel != null) formTitleLabel.setText("Modifier la tâche #" + task.getId());
        showMsg("Mode édition — tâche #" + task.getId(), PURPLE);
    }

    @FXML
    public void cancelEdit() {
        clearForm();
        currentEditingTask = null;
        deselectCurrent();
        currentlySelectedCard = null;
        if (saveButton   != null) saveButton.setText("Ajouter");
        if (cancelButton != null) { cancelButton.setVisible(false); cancelButton.setManaged(false); }
        if (formTitleLabel != null) formTitleLabel.setText("Nouvelle tâche");
        showMsg("Édition annulée", "#6b7280");
    }

    // ── Validation ────────────────────────────────────────────────────────────
    private void addValidationListeners() {
        titreField.textProperty().addListener((o, ov, nv) -> {
            if (nv != null && !nv.trim().isEmpty()) hideError(titreField, titreErrorLabel);
        });
        statutCombo.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) hideError(statutCombo, statutErrorLabel);
        });
        employeCombo.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) hideError(employeCombo, employeErrorLabel);
        });
    }

    private void showError(Control c, Label l, String msg) {
        c.setStyle(fieldError + "-fx-font-size:12px;-fx-padding:9 14;");
        l.setText(msg); l.setVisible(true); l.setManaged(true);
    }
    private void hideError(Control c, Label l) {
        c.setStyle(fieldNormal + "-fx-font-size:12px;-fx-padding:9 14;");
        l.setVisible(false); l.setManaged(false);
    }

    private boolean validateForm() {
        boolean ok = true;
        if (titreField.getText() == null || titreField.getText().trim().isEmpty()) {
            showError(titreField, titreErrorLabel, "Le titre est requis"); ok = false;
        } else hideError(titreField, titreErrorLabel);
        if (statutCombo.getValue() == null) {
            showError(statutCombo, statutErrorLabel, "Le statut est requis"); ok = false;
        } else hideError(statutCombo, statutErrorLabel);
        if (employeCombo.getValue() == null) {
            showError(employeCombo, employeErrorLabel, "L'employe est requis"); ok = false;
        } else hideError(employeCombo, employeErrorLabel);
        return ok;
    }

    private void clearForm() {
        titreField.clear(); descField.clear();
        statutCombo.setValue(null); employeCombo.setValue(null);
        hideError(titreField, titreErrorLabel);
        hideError(statutCombo, statutErrorLabel);
        hideError(employeCombo, employeErrorLabel);
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    @FXML
    public void saveTask() {
        if (projet == null) return;
        if (!isResponsible()) { showMsg("Seul le responsable peut gerer les taches.", "#ef4444"); return; }
        if (!validateForm())  { showMsg("Veuillez corriger les erreurs.", "#ef4444"); return; }

        try {
            User emp = employeCombo.getValue();
            if (currentEditingTask == null) {
                Tache t = new Tache(titreField.getText().trim(), descField.getText(),
                        statutCombo.getValue(), projet.getId(), emp.getId(), null);
                tacheService.addTache(t);
                sendEmail(emp, t, projet);
                showMsg("Tache ajoutee avec succes !", "#059669");
            } else {
                boolean empChanged = currentEditingTask.getEmployeId() != emp.getId();
                currentEditingTask.setTitre(titreField.getText().trim());
                currentEditingTask.setDescription(descField.getText());
                currentEditingTask.setStatut(statutCombo.getValue());
                currentEditingTask.setEmployeId(emp.getId());
                tacheService.updateFullTache(currentEditingTask);
                if (empChanged) sendEmail(emp, currentEditingTask, projet);
                showMsg("Tache modifiee avec succes !", "#059669");
            }
            currentEditingTask = null;
            deselectCurrent(); currentlySelectedCard = null;
            if (saveButton   != null) saveButton.setText("Ajouter");
            if (cancelButton != null) { cancelButton.setVisible(false); cancelButton.setManaged(false); }
            if (formTitleLabel != null) formTitleLabel.setText("Nouvelle tache");
            clearForm();
            refresh();
        } catch (SQLException e) {
            e.printStackTrace();
            showMsg("Erreur sauvegarde.", "#ef4444");
        }
    }

    private void sendEmail(User emp, Tache t, Projet p) {
        if (emp == null || emp.getEmail() == null || emp.getEmail().isEmpty()) return;
        emailService.sendTaskAssignedEmail(emp.getEmail(),
                emp.getPrenom() + " " + emp.getNom(), t.getTitre(), p.getTitre());
    }

    // ── Refresh ───────────────────────────────────────────────────────────────
    @FXML
    public void refresh() {
        if (projet == null) return;
        try {
            List<Tache> data = tacheService.findByProjetId(projet.getId());
            allTasks = FXCollections.observableArrayList(data);
            renderKanban(allTasks);
            showMsg(data.size() + " tache(s)", "#6b7280");
            currentlySelectedCard = null;
        } catch (SQLException e) {
            e.printStackTrace();
            showMsg("Erreur chargement.", "#ef4444");
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    @FXML
    public void deleteSelected() {
        if (currentlySelectedCard == null) { showMsg("Selectionnez une tache.", "#b45309"); return; }
        Tache t = (Tache) currentlySelectedCard.getUserData();
        if (t == null) return;
        if (!isResponsible()) { showMsg("Seul le responsable peut supprimer.", "#ef4444"); return; }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer la tache \"" + t.getTitre() + "\" ?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                tacheService.deleteTache(t.getId());
                if (currentEditingTask != null && currentEditingTask.getId() == t.getId()) cancelEdit();
                currentlySelectedCard = null;
                refresh();
                showMsg("Tache supprimee.", "#059669");
            } catch (SQLException e) {
                e.printStackTrace();
                showMsg("Erreur suppression.", "#ef4444");
            }
        }
    }

    @FXML
    public void markDone() {
        if (currentlySelectedCard == null) { showMsg("Selectionnez une tache.", "#b45309"); return; }
        Tache t = (Tache) currentlySelectedCard.getUserData();
        if (t != null) updateTaskStatus(t, "DONE");
    }

    private void updateTaskStatus(Tache task, String newStatus) {
        if (!isResponsible()) { showMsg("Seul le responsable peut changer le statut.", "#ef4444"); return; }
        try {
            task.setStatut(newStatus);
            tacheService.updateTache(task);
            refresh();
            showMsg("Tache -> " + newStatus, "DONE".equals(newStatus) ? "#059669" : PURPLE);
        } catch (SQLException e) {
            e.printStackTrace();
            showMsg("Erreur mise a jour.", "#ef4444");
        }
    }

    private boolean isResponsible() {
        try {
            User u = Session.getCurrentUser();
            Projet fresh = projetService.getProjetById(projet.getId());
            return fresh != null && fresh.getResponsableEmployeId() == (int) u.getId();
        } catch (SQLException e) { return false; }
    }

    private void showMsg(String text, String color) {
        if (msgLabel != null) {
            msgLabel.setText(text);
            msgLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12px;-fx-font-weight:600;");
        }
    }
}
