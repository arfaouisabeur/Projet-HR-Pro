package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Tache;
import edu.RhPro.entities.User;
import edu.RhPro.services.TacheService;
import edu.RhPro.services.ProjetService;
import edu.RhPro.utils.Session;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class MesTachesController {

    @FXML private VBox mainContainer;
    @FXML private ChatbotController chatbotController;
    @FXML private Label msgLabel;
    @FXML private TextField searchField;

    // Kanban columns
    @FXML private VBox todoColumn;
    @FXML private VBox doingColumn;
    @FXML private VBox doneColumn;

    // Hidden FXML counter labels (kept for compatibility)
    @FXML private Label todoCount;
    @FXML private Label doingCount;
    @FXML private Label doneCount;

    // Chatbot button in header (injected from FXML)
    @FXML private Button chatbotBtn;

    // Visible header badges (built in code)
    private final Label todoBadge  = new Label("0");
    private final Label doingBadge = new Label("0");
    private final Label doneBadge  = new Label("0");

    private final TacheService  tacheService  = new TacheService();
    private final ProjetService projetService = new ProjetService();
    private ObservableList<Tache> allTasks;

    // ── Palette brand ─────────────────────────────────────────────────────────
    private static final String C_PURPLE   = "#6d2269";
    private static final String C_LAVENDER = "#ddd6fe";
    private static final String C_LAV_BG   = "#f5f0fd";
    private static final String C_TODO_FG  = "#b45309";
    private static final String C_TODO_BG  = "#fef3c7";
    private static final String C_DONE_FG  = "#065f46";
    private static final String C_DONE_BG  = "#d1fae5";

    private String getProjectFg(int id) {
        String[] c = {"#6d2269","#7c3aed","#0369a1","#065f46","#92400e","#9d174d","#1e40af"};
        return c[Math.abs(id) % c.length];
    }
    private String getProjectBg(int id) {
        String[] c = {"#f5f0fd","#ede9fc","#e0f2fe","#d1fae5","#fef3c7","#fce7f3","#dbeafe"};
        return c[Math.abs(id) % c.length];
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupColumnHeaders();
        setupSearch();
        setupChatbotButton();
        refresh();
    }

    private void setupChatbotButton() {
        if (chatbotBtn == null) return;
        String base =
                "-fx-background-color:#6d2269;-fx-text-fill:white;" +
                        "-fx-background-radius:25;-fx-padding:10 20;" +
                        "-fx-font-weight:700;-fx-font-size:12px;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.35),10,0,0,3);";
        String hover =
                "-fx-background-color:#8b2e87;-fx-text-fill:white;" +
                        "-fx-background-radius:25;-fx-padding:10 20;" +
                        "-fx-font-weight:700;-fx-font-size:12px;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.55),14,0,0,5);";
        chatbotBtn.setStyle(base);
        chatbotBtn.setOnMouseEntered(e -> chatbotBtn.setStyle(hover));
        chatbotBtn.setOnMouseExited(e  -> chatbotBtn.setStyle(base));
        chatbotBtn.setOnMousePressed(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(80), chatbotBtn);
            s.setToX(0.94); s.setToY(0.94); s.play();
        });
        chatbotBtn.setOnMouseReleased(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(80), chatbotBtn);
            s.setToX(1.0); s.setToY(1.0); s.play();
        });
    }

    private void setupColumnHeaders() {
        todoColumn.getChildren().add(0,  buildHeader("A FAIRE",  todoBadge,  C_TODO_FG, C_TODO_BG));
        doingColumn.getChildren().add(0, buildHeader("EN COURS", doingBadge, C_PURPLE,  C_LAVENDER));
        doneColumn.getChildren().add(0,  buildHeader("TERMINE",  doneBadge,  C_DONE_FG, C_DONE_BG));
    }

    private HBox buildHeader(String title, Label badge, String fg, String bg) {
        Circle dot = new Circle(5);
        dot.setFill(Color.web(fg));

        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size:13px;-fx-font-weight:800;-fx-text-fill:#1a1a1a;");

        badge.setText("0");
        badge.setStyle(
                "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                        "-fx-font-weight:800;-fx-font-size:11px;" +
                        "-fx-padding:2 10;-fx-background-radius:20;"
        );
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, dot, lbl, spacer, badge);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));
        return header;
    }

    // ── Chatbot popup ─────────────────────────────────────────────────────────
    @FXML
    public void openChatbot() {
        openChatbotPopup(chatbotBtn);
    }

    private void openChatbotPopup(Node anchor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/employe/ChatView.fxml"));
            VBox chat = (VBox) loader.load();
            chat.setPrefWidth(360);
            chat.setPrefHeight(500);
            chat.setStyle(
                    "-fx-background-color:white;" +
                            "-fx-background-radius:16;" +
                            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.25),24,0,0,8);"
            );

            Popup popup = new Popup();
            popup.setAutoHide(true);
            popup.getContent().add(chat);

            javafx.geometry.Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
            popup.show(anchor.getScene().getWindow(), b.getMinX() - 330, b.getMinY() - 520);

            // Fade + slide in
            FadeTransition ft = new FadeTransition(Duration.millis(200), chat);
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), chat);
            tt.setFromY(14); tt.setToY(0);
            new ParallelTransition(ft, tt).play();

            // Sync tasks
            ChatbotController cc = loader.getController();
            if (cc != null && allTasks != null) cc.updateTasks(allTasks);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    private void setupSearch() {
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

    // ── Refresh ───────────────────────────────────────────────────────────────
    @FXML
    public void refresh() {
        try {
            User u = Session.getCurrentUser();
            List<Tache> data = tacheService.findByEmployeId((int) u.getId());
            allTasks = FXCollections.observableArrayList(data);
            renderKanban(allTasks);
            msgLabel.setText(data.size() + " tache(s) assignee(s)");
            if (chatbotController != null) chatbotController.updateTasks(data);
        } catch (SQLException e) {
            e.printStackTrace();
            showMsg("Erreur chargement taches", "#ef4444");
        }
    }

    // ── Kanban ────────────────────────────────────────────────────────────────
    private void renderKanban(ObservableList<Tache> tasks) {
        clearColumn(todoColumn);
        clearColumn(doingColumn);
        clearColumn(doneColumn);

        List<Tache> todo  = tasks.stream().filter(t -> "TODO".equals(t.getStatut())).collect(Collectors.toList());
        List<Tache> doing = tasks.stream().filter(t -> "DOING".equals(t.getStatut())).collect(Collectors.toList());
        List<Tache> done  = tasks.stream().filter(t -> "DONE".equals(t.getStatut())).collect(Collectors.toList());

        updateBadge(todoBadge,  todo.size(),  C_TODO_FG, C_TODO_BG);
        updateBadge(doingBadge, doing.size(), C_PURPLE,  C_LAVENDER);
        updateBadge(doneBadge,  done.size(),  C_DONE_FG, C_DONE_BG);

        if (todoCount  != null) todoCount.setText(String.valueOf(todo.size()));
        if (doingCount != null) doingCount.setText(String.valueOf(doing.size()));
        if (doneCount  != null) doneCount.setText(String.valueOf(done.size()));

        addCardsStaggered(todoColumn,  todo);
        addCardsStaggered(doingColumn, doing);
        addCardsStaggered(doneColumn,  done);

        if (todo.isEmpty())  addEmptyState(todoColumn,  "📋", "Aucune tache a faire");
        if (doing.isEmpty()) addEmptyState(doingColumn, "⚡", "Aucune tache en cours");
        if (done.isEmpty())  addEmptyState(doneColumn,  "✔",  "Aucune tache terminee");
    }

    private void clearColumn(VBox col) {
        if (col.getChildren().size() > 1)
            col.getChildren().subList(1, col.getChildren().size()).clear();
    }

    private void updateBadge(Label badge, int count, String fg, String bg) {
        badge.setText(String.valueOf(count));
        badge.setStyle(
                "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                        "-fx-font-weight:800;-fx-font-size:11px;" +
                        "-fx-padding:2 10;-fx-background-radius:20;"
        );
    }

    private void addCardsStaggered(VBox col, List<Tache> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            VBox card = createCard(tasks.get(i));
            card.setOpacity(0);
            card.setTranslateY(12);
            col.getChildren().add(card);

            FadeTransition ft = new FadeTransition(Duration.millis(230), card);
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(230), card);
            tt.setFromY(12); tt.setToY(0);
            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.setDelay(Duration.millis(i * 55));
            pt.play();
        }
    }

    // ── Card ──────────────────────────────────────────────────────────────────
    private VBox createCard(Tache task) {
        String statut = task.getStatut() != null ? task.getStatut() : "TODO";

        String accentFg, accentBg, topColor;
        if ("DOING".equals(statut)) {
            accentFg = C_PURPLE;  accentBg = C_LAVENDER; topColor = C_PURPLE;
        } else if ("DONE".equals(statut)) {
            accentFg = C_DONE_FG; accentBg = C_DONE_BG;  topColor = "#059669";
        } else {
            accentFg = C_TODO_FG; accentBg = C_TODO_BG;  topColor = "#d97706";
        }

        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        String baseStyle  = buildCardStyle(accentFg, false);
        String hoverStyle = buildCardStyle(accentFg, true);
        card.setStyle(baseStyle);

        // Top accent bar
        HBox topBar = new HBox();
        topBar.setPrefHeight(3);
        topBar.setMaxWidth(Double.MAX_VALUE);
        topBar.setStyle("-fx-background-color:" + topColor + ";-fx-background-radius:10 10 0 0;");

        // Body
        VBox body = new VBox(7);
        body.setPadding(new Insets(11, 13, 11, 13));

        // Row 1: project badge + id
        HBox row1 = new HBox(6);
        row1.setAlignment(Pos.CENTER_LEFT);
        try {
            String pName = projetService.getProjetById(task.getProjetId()).getTitre();
            Label pBadge = new Label(pName);
            pBadge.setStyle(
                    "-fx-background-color:" + getProjectBg(task.getProjetId()) + ";" +
                            "-fx-text-fill:" + getProjectFg(task.getProjetId()) + ";" +
                            "-fx-font-size:10px;-fx-font-weight:700;" +
                            "-fx-padding:2 9;-fx-background-radius:20;"
            );
            row1.getChildren().add(pBadge);
        } catch (SQLException e) { e.printStackTrace(); }
        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        Label idLbl = new Label("#" + task.getId());
        idLbl.setStyle("-fx-text-fill:#9ca3af;-fx-font-size:10.5px;-fx-font-weight:600;");
        row1.getChildren().addAll(sp1, idLbl);
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
        sep.setPadding(new Insets(2, 0, 2, 0));
        sep.setStyle("-fx-opacity:0.35;");
        body.getChildren().add(sep);

        // Footer: status pill + action btn
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getChildren().add(buildStatusPill(statut, accentFg, accentBg));
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        footer.getChildren().add(sp2);

        if ("TODO".equals(statut)) {
            Button btn = buildBtn("Demarrer", C_PURPLE, C_LAVENDER);
            btn.setOnAction(e -> updateStatus(task, "DOING"));
            footer.getChildren().add(btn);
        } else if ("DOING".equals(statut)) {
            Button btn = buildBtn("Terminer", "#059669", "#d1fae5");
            btn.setOnAction(e -> updateStatus(task, "DONE"));
            footer.getChildren().add(btn);
        }

        body.getChildren().add(footer);
        card.getChildren().addAll(topBar, body);

        card.setOnMouseEntered(e -> {
            card.setStyle(hoverStyle);
            ScaleTransition s = new ScaleTransition(Duration.millis(120), card);
            s.setToX(1.013); s.setToY(1.013); s.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(baseStyle);
            ScaleTransition s = new ScaleTransition(Duration.millis(120), card);
            s.setToX(1.0); s.setToY(1.0); s.play();
        });
        return card;
    }

    private String buildCardStyle(String accentFg, boolean hover) {
        String bg     = hover ? C_LAV_BG : "white";
        String shadow = hover
                ? "dropshadow(gaussian,rgba(109,34,105,0.18),14,0,0,4)"
                : "dropshadow(gaussian,rgba(109,34,105,0.07),7,0,0,2)";
        return "-fx-background-color:" + bg + ";" +
                "-fx-background-radius:10;" +
                "-fx-border-color:transparent transparent transparent " + accentFg + ";" +
                "-fx-border-width:0 0 0 3;-fx-border-radius:10 0 0 10;" +
                "-fx-effect:" + shadow + ";" +
                (hover ? "-fx-cursor:hand;" : "");
    }

    private Label buildStatusPill(String statut, String fg, String bg) {
        String text;
        if ("DOING".equals(statut))     text = "EN COURS";
        else if ("DONE".equals(statut)) text = "TERMINE";
        else                            text = "A FAIRE";
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                        "-fx-font-size:9px;-fx-font-weight:800;" +
                        "-fx-padding:2 8;-fx-background-radius:20;"
        );
        return lbl;
    }

    private Button buildBtn(String text, String fg, String bg) {
        Button btn = new Button(text);
        String base  = "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:700;" +
                "-fx-padding:5 14;-fx-background-radius:20;-fx-cursor:hand;";
        String hover = "-fx-background-color:" + fg + ";-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-font-weight:700;" +
                "-fx-padding:5 14;-fx-background-radius:20;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.14),5,0,0,2);";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnMousePressed(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(70), btn);
            s.setToX(0.93); s.setToY(0.93); s.play();
        });
        btn.setOnMouseReleased(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(70), btn);
            s.setToX(1.0); s.setToY(1.0); s.play();
        });
        return btn;
    }

    private void addEmptyState(VBox col, String icon, String msg) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(22));
        box.setStyle(
                "-fx-background-color:" + C_LAV_BG + ";-fx-background-radius:10;" +
                        "-fx-border-color:" + C_LAVENDER + ";-fx-border-style:dashed;" +
                        "-fx-border-radius:10;-fx-border-width:1.5;"
        );
        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size:20px;-fx-opacity:0.45;");
        Label lb = new Label(msg);
        lb.setStyle("-fx-font-size:11.5px;-fx-text-fill:#9ca3af;-fx-font-style:italic;");
        box.getChildren().addAll(ic, lb);
        col.getChildren().add(box);
    }

    private void updateStatus(Tache task, String newStatus) {
        try {
            task.setStatut(newStatus);
            tacheService.updateTache(task);
            refresh();
            String color = "DONE".equals(newStatus) ? "#059669" : C_PURPLE;
            showMsg("Tache #" + task.getId() + " -> " + newStatus, color);
        } catch (SQLException e) {
            e.printStackTrace();
            showMsg("Erreur mise a jour", "#ef4444");
        }
    }

    private void showMsg(String text, String color) {
        msgLabel.setText(text);
        msgLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-weight:600;");
    }
}
