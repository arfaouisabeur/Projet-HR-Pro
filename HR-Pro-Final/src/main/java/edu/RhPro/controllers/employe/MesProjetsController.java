package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Projet;
import edu.RhPro.entities.User;
import edu.RhPro.services.ProjetService;
import edu.RhPro.utils.Session;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

public class MesProjetsController {

    @FXML private TableView<Projet>              table;
    @FXML private TableColumn<Projet, Integer>   colId;
    @FXML private TableColumn<Projet, String>    colTitre;
    @FXML private TableColumn<Projet, String>    colStatut;
    @FXML private TableColumn<Projet, LocalDate> colDebut;
    @FXML private TableColumn<Projet, LocalDate> colFin;
    @FXML private TableColumn<Projet, String>    colDesc;
    @FXML private TableColumn<Projet, Void>      colActions;

    @FXML private Label     msgLabel;
    @FXML private Label     pendingBadge;
    @FXML private TextField searchField;

    private final ProjetService projetService = new ProjetService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final String PURPLE     = "#6d2269";
    private static final String LAVENDER   = "#ddd6fe";
    private static final String LAV_BG     = "#f5f0fd";
    private static final String HDR_BG     = "#6d2269";  // table header bg
    private static final String ROW_EVEN   = "white";
    private static final String ROW_ODD    = "#faf7fe";
    private static final String ROW_SEL    = "#ede9fc";

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupColumns();
        setupTableStyle();
        setupSearch();
        refresh();
    }

    // ── Table header styling ─────────────────────────────────────────────────
    private void applyHeaderStyle() {
        Set<javafx.scene.Node> headers = table.lookupAll(".column-header");
        for (javafx.scene.Node h : headers) {
            h.setStyle("-fx-background-color:" + HDR_BG + ";-fx-border-color:rgba(255,255,255,0.12);-fx-border-width:0 1 0 0;");
        }
        Set<javafx.scene.Node> labels = table.lookupAll(".column-header .label");
        for (javafx.scene.Node l : labels) {
            l.setStyle("-fx-text-fill:white;-fx-font-weight:800;-fx-font-size:11.5px;-fx-padding:0 14;-fx-alignment:CENTER-LEFT;");
        }
        Set<javafx.scene.Node> bgs = table.lookupAll(".column-header-background");
        for (javafx.scene.Node b : bgs) {
            b.setStyle("-fx-background-color:" + HDR_BG + ";");
        }
        // filler corner
        Set<javafx.scene.Node> fillers = table.lookupAll(".filler");
        for (javafx.scene.Node f : fillers) {
            f.setStyle("-fx-background-color:" + HDR_BG + ";");
        }
    }

    // ── Column setup ─────────────────────────────────────────────────────────
    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setCellFactory(c -> new TableCell<Projet, Integer>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                Label l = new Label(String.valueOf(v));
                l.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#6b7280;");
                setGraphic(l); setText(null);
                setStyle("-fx-alignment:CENTER;-fx-padding:0 10;");
            }
        });

        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(c -> new TableCell<Projet, String>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                Label l = new Label(v);
                l.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#1a1a1a;");
                l.setWrapText(false);
                setGraphic(l); setText(null);
                setStyle("-fx-alignment:CENTER-LEFT;-fx-padding:0 14;");
            }
        });

        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(c -> new TableCell<Projet, String>() {
            @Override protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) { setGraphic(null); setText(null); return; }

                String fg, bg, dotColor, labelText;
                if ("DONE".equals(statut)) {
                    fg = "#065f46"; bg = "#d1fae5"; dotColor = "#059669"; labelText = "TERMINE";
                } else if ("DOING".equals(statut)) {
                    fg = PURPLE;    bg = LAVENDER;  dotColor = PURPLE;    labelText = "DOING";
                } else {
                    fg = "#b45309"; bg = "#fef3c7"; dotColor = "#d97706"; labelText = "A FAIRE";
                }

                HBox pill = new HBox(5);
                pill.setAlignment(Pos.CENTER);
                pill.setPadding(new Insets(3, 12, 3, 10));
                pill.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:20;");
                Circle dot = new Circle(4, Color.web(dotColor));
                Label lb  = new Label(labelText);
                lb.setStyle("-fx-text-fill:" + fg + ";-fx-font-size:10.5px;-fx-font-weight:800;");
                pill.getChildren().addAll(dot, lb);

                HBox wrap = new HBox(pill);
                wrap.setAlignment(Pos.CENTER_LEFT);
                wrap.setPadding(new Insets(0, 0, 0, 8));
                setGraphic(wrap); setText(null);
                setStyle("-fx-alignment:CENTER-LEFT;");
            }
        });

        colDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDebut.setCellFactory(c -> new TableCell<Projet, LocalDate>() {
            @Override protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                Label l = new Label(v.format(FMT));
                l.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;");
                setGraphic(l); setText(null);
                setStyle("-fx-alignment:CENTER-LEFT;-fx-padding:0 14;");
            }
        });

        colFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colFin.setCellFactory(c -> new TableCell<Projet, LocalDate>() {
            @Override protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                boolean overdue = v.isBefore(LocalDate.now());
                Label l = new Label(v.format(FMT));
                l.setStyle("-fx-font-size:12px;-fx-text-fill:" + (overdue ? "#ef4444" : "#374151") + ";"
                        + (overdue ? "-fx-font-weight:700;" : ""));
                setGraphic(l); setText(null);
                setStyle("-fx-alignment:CENTER-LEFT;-fx-padding:0 14;");
            }
        });

        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDesc.setCellFactory(c -> new TableCell<Projet, String>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                // Truncate if too long
                String display = v.length() > 55 ? v.substring(0, 52) + "..." : v;
                setText(display);
                setTooltip(new Tooltip(v));
                setStyle("-fx-font-size:11.5px;-fx-text-fill:#6b7280;-fx-padding:0 14;");
            }
        });

        colActions.setCellFactory(c -> new TableCell<Projet, Void>() {
            private final Button btn = new Button("Taches");
            {
                String base  = "-fx-background-color:" + LAVENDER + ";-fx-text-fill:" + PURPLE + ";"
                        + "-fx-font-size:11px;-fx-font-weight:700;"
                        + "-fx-background-radius:20;-fx-padding:5 14;-fx-cursor:hand;";
                String hover = "-fx-background-color:" + PURPLE + ";-fx-text-fill:white;"
                        + "-fx-font-size:11px;-fx-font-weight:700;"
                        + "-fx-background-radius:20;-fx-padding:5 14;-fx-cursor:hand;"
                        + "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.3),6,0,0,2);";
                btn.setStyle(base);
                btn.setOnMouseEntered(e -> btn.setStyle(hover));
                btn.setOnMouseExited(e  -> btn.setStyle(base));
                btn.setOnAction(e -> openTasksForProjet(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
                setStyle("-fx-alignment:CENTER;");
            }
        });

        // Apply headers after layout pass
        table.widthProperty().addListener((o, ov, nv) -> Platform.runLater(this::applyHeaderStyle));
    }

    // ── Table style ───────────────────────────────────────────────────────────
    private void setupTableStyle() {
        table.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");
        table.setFixedCellSize(44);
        table.setPlaceholder(buildPlaceholder());

        table.setRowFactory(tv -> new TableRow<Projet>() {
            @Override protected void updateItem(Projet item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("-fx-background-color:transparent;");
                    return;
                }
                String bg     = isSelected() ? ROW_SEL
                        : (getIndex() % 2 == 0 ? ROW_EVEN : ROW_ODD);
                String border = isSelected()
                        ? "-fx-border-color:transparent transparent transparent " + PURPLE + ";-fx-border-width:0 0 0 3;"
                        : "-fx-border-color:transparent;";
                setStyle("-fx-background-color:" + bg + ";" + border
                        + "-fx-table-cell-border-color:#f0eafa;");
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) ->
                table.refresh()
        );
    }

    private VBox buildPlaceholder() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50));
        Label ic = new Label("📂");
        ic.setStyle("-fx-font-size:32px;-fx-opacity:0.35;");
        Label lb = new Label("Aucun projet trouve");
        lb.setStyle("-fx-font-size:13px;-fx-text-fill:#9ca3af;-fx-font-style:italic;");
        box.getChildren().addAll(ic, lb);
        return box;
    }

    // ── Search ────────────────────────────────────────────────────────────────
    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, nv) -> filterTable(nv));
    }

    private void filterTable(String q) {
        try {
            User u = Session.getCurrentUser();
            List<Projet> all = projetService.findByResponsableId((int) u.getId());
            if (q == null || q.isEmpty()) {
                table.setItems(FXCollections.observableArrayList(all));
                updateBadge(all); return;
            }
            List<Projet> filtered = all.stream()
                    .filter(p -> p.getTitre().toLowerCase().contains(q.toLowerCase()) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains(q.toLowerCase())) ||
                            p.getStatut().toLowerCase().contains(q.toLowerCase()))
                    .toList();
            table.setItems(FXCollections.observableArrayList(filtered));
            updateBadge(filtered);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────
    @FXML
    public void refresh() {
        try {
            User u = Session.getCurrentUser();
            List<Projet> data = projetService.findByResponsableId((int) u.getId());
            table.setItems(FXCollections.observableArrayList(data));
            updateBadge(data);
            msgLabel.setText(data.size() + " projet(s) trouve(s)");

            FadeTransition ft = new FadeTransition(Duration.millis(280), table);
            ft.setFromValue(0.5); ft.setToValue(1); ft.play();
            Platform.runLater(this::applyHeaderStyle);

        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("Erreur chargement.");
        }
    }

    private void updateBadge(List<Projet> data) {
        long doing = data.stream().filter(p -> "DOING".equals(p.getStatut())).count();
        if (pendingBadge != null)
            pendingBadge.setText(doing + " projet(s) en cours");
    }

    // ── Open tasks ────────────────────────────────────────────────────────────
    @FXML
    public void openTasks() {
        Projet selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("Selectionnez un projet d'abord.");
            msgLabel.setStyle("-fx-text-fill:#b45309;-fx-font-size:12px;-fx-font-weight:600;"
                    + "-fx-background-color:#fef3c7;-fx-padding:6 14;-fx-background-radius:20;");
            return;
        }
        openTasksForProjet(selected);
    }

    private void openTasksForProjet(Projet projet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/employe/ProjetTachesView.fxml"));
            Parent root = loader.load();
            ProjetTachesController ctrl = loader.getController();
            ctrl.setProjet(projet);
            Stage st = new Stage();
            st.setTitle("Taches - " + projet.getTitre());
            st.initModality(Modality.APPLICATION_MODAL);
            st.setMinWidth(900);
            st.setMinHeight(600);
            Scene scene = new Scene(root);
            st.setScene(scene);
            st.setWidth(javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() * 0.82);
            st.setHeight(javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() * 0.85);
            st.centerOnScreen();
            st.showAndWait();
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("Impossible d'ouvrir les taches.");
        }
    }
}
