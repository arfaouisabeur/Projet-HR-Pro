package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Reponse;
import edu.RhPro.entities.Service;
import edu.RhPro.services.ElevenLabsService;
import edu.RhPro.services.PdfService;
import edu.RhPro.services.ReponseService;
import edu.RhPro.services.ServiceService;
import edu.RhPro.tools.MyConnection;
import edu.RhPro.utils.Session;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ServicesManageController {

    // ── Table ────────────────────────────────────
    @FXML private TableView<Service>              table;
    @FXML private TableColumn<Service, Long>      colId;
    @FXML private TableColumn<Service, Long>      colEmploye;
    @FXML private TableColumn<Service, String>    colTitre;
    @FXML private TableColumn<Service, LocalDate> colDate;
    @FXML private TableColumn<Service, String>    colDesc;

    // ── Toolbar ──────────────────────────────────
    @FXML private ComboBox<String> cbCriteria;
    @FXML private TextField        tfSearch;
    @FXML private Label            msgLabel;

    // ── Panneau droit ─────────────────────────────
    @FXML private Label    lblSelectionInfo;
    @FXML private VBox     vboxEtapeInfo;
    @FXML private Label    lblEtapeStatus;
    @FXML private HBox     hboxWorkflowBar;
    @FXML private TextArea taCommentaire;

    private final ServiceService serviceService = new ServiceService();
    private final ReponseService reponseService = new ReponseService();

    private ObservableList<Service> masterData;
    private FilteredList<Service>   filteredData;
    private Service selectedService;

    // ══════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════
    @FXML
    public void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateDemande"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        colDesc.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setTooltip(null);
                } else {
                    setText(item.length() > 40 ? item.substring(0, 40) + "…" : item);
                    Tooltip tip = new Tooltip(item);
                    tip.setWrapText(true);
                    tip.setMaxWidth(350);
                    setTooltip(tip);
                }
            }
        });

        // ── Colonne Statut ────────────────────────
        TableColumn<Service, Void> colStatut = new TableColumn<>("Statut");
        colStatut.setPrefWidth(105);
        colStatut.setResizable(false);
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Service s = getTableView().getItems().get(getIndex());
                String statut = s.getStatut() != null ? s.getStatut() : "EN_ATTENTE";
                String bg = switch (statut) {
                    case "EN_ATTENTE" -> "#ede9f6";
                    case "ACCEPTEE"   -> "#d1fae5";
                    case "REFUSEE"    -> "#fee2e2";
                    default           -> "#ede9f6";
                };
                String fg = switch (statut) {
                    case "EN_ATTENTE" -> "#6d2269";
                    case "ACCEPTEE"   -> "#059669";
                    case "REFUSEE"    -> "#dc2626";
                    default           -> "#6d2269";
                };
                String txt = switch (statut) {
                    case "EN_ATTENTE" -> "⏳ En attente";
                    case "ACCEPTEE"   -> "✅ Acceptée";
                    case "REFUSEE"    -> "❌ Refusée";
                    default           -> statut;
                };
                Label badge = new Label(txt);
                badge.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                        "-fx-background-radius:14; -fx-padding:3 8; -fx-font-size:11px; -fx-font-weight:bold;");
                setGraphic(badge);
            }
        });
        table.getColumns().add(colStatut);

        // ── Colonne Priorité ──────────────────────
        TableColumn<Service, Void> colPriorite = new TableColumn<>("Priorité");
        colPriorite.setPrefWidth(88);
        colPriorite.setResizable(false);
        colPriorite.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Service s = getTableView().getItems().get(getIndex());
                String p = s.getPriorite() != null ? s.getPriorite() : "NORMAL";
                String bg = switch (p) { case "URGENT" -> "#fee2e2"; case "FAIBLE" -> "#d1fae5"; default -> "#ede9f6"; };
                String fg = switch (p) { case "URGENT" -> "#dc2626"; case "FAIBLE" -> "#059669"; default -> "#6d2269"; };
                String ic = switch (p) { case "URGENT" -> "🔴"; case "FAIBLE" -> "🟢"; default -> "🟡"; };
                Label badge = new Label(ic + " " + p);
                badge.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                        "-fx-background-radius:14; -fx-padding:3 8; -fx-font-size:11px; -fx-font-weight:bold;");
                setGraphic(badge);
            }
        });
        table.getColumns().add(colPriorite);

        // ── Colonne Étape Workflow ─────────────────
        TableColumn<Service, Void> colEtape = new TableColumn<>("Étape");
        colEtape.setPrefWidth(120);
        colEtape.setResizable(false);
        colEtape.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Service s = getTableView().getItems().get(getIndex());
                String e = s.getEtapeWorkflow() != null ? s.getEtapeWorkflow() : "SOUMISE";
                String bg = switch (e) {
                    case "SOUMISE"       -> "#ede9f6";
                    case "EN_ANALYSE"    -> "#fef3c7";
                    case "EN_TRAITEMENT" -> "#dbeafe";
                    case "RESOLUE"       -> "#d1fae5";
                    case "REJETEE"       -> "#fee2e2";
                    default              -> "#ede9f6";
                };
                String fg = switch (e) {
                    case "SOUMISE"       -> "#6d2269";
                    case "EN_ANALYSE"    -> "#d97706";
                    case "EN_TRAITEMENT" -> "#1d4ed8";
                    case "RESOLUE"       -> "#059669";
                    case "REJETEE"       -> "#dc2626";
                    default              -> "#6d2269";
                };
                Label badge = new Label(e.replace("_", " "));
                badge.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                        "-fx-background-radius:14; -fx-padding:3 8; -fx-font-size:11px; -fx-font-weight:bold;");
                setGraphic(badge);
            }
        });
        table.getColumns().add(colEtape);

        // ── Colonne SLA ───────────────────────────
        TableColumn<Service, Void> colSla = new TableColumn<>("SLA");
        colSla.setPrefWidth(100);
        colSla.setResizable(false);
        colSla.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Service s = getTableView().getItems().get(getIndex());
                if (s.getDeadlineReponse() == null) { setGraphic(null); return; }
                long j = LocalDate.now().until(s.getDeadlineReponse(), java.time.temporal.ChronoUnit.DAYS);
                Label badge;
                if (s.isSlaDepasse() || j < 0) {
                    badge = new Label("🔴 DÉPASSÉ");
                    badge.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                            "-fx-background-radius:14; -fx-padding:3 7; -fx-font-weight:bold; -fx-font-size:11px;");
                } else if (j == 0) {
                    badge = new Label("🟡 Auj.");
                    badge.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                            "-fx-background-radius:14; -fx-padding:3 7; -fx-font-weight:bold; -fx-font-size:11px;");
                } else {
                    badge = new Label("🟢 " + j + "j");
                    badge.setStyle("-fx-background-color:#d1fae5; -fx-text-fill:#059669;" +
                            "-fx-background-radius:14; -fx-padding:3 7; -fx-font-weight:bold; -fx-font-size:11px;");
                }
                setGraphic(badge);
            }
        });
        table.getColumns().add(colSla);

        // ── Colonne Récurrence ────────────────────
        TableColumn<Service, Void> colRec = new TableColumn<>("Récurrence");
        colRec.setPrefWidth(100);
        colRec.setResizable(false);
        colRec.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Service s = getTableView().getItems().get(getIndex());
                try {
                    int score = serviceService.getScoreRecurrence(s.getEmployeeId());
                    if (score >= 3) {
                        Label badge = new Label("⚠ " + score + "/mois");
                        badge.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                                "-fx-background-radius:14; -fx-padding:3 7; -fx-font-weight:bold; -fx-font-size:11px;");
                        setGraphic(badge);
                    } else {
                        setGraphic(null);
                    }
                } catch (SQLException ignored) { setGraphic(null); }
            }
        });
        table.getColumns().add(colRec);

        // ── Colonne PDF ───────────────────────────
        TableColumn<Service, Void> colPdf = new TableColumn<>("PDF");
        colPdf.setPrefWidth(55);
        colPdf.setResizable(false);
        colPdf.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Service s = getTableView().getItems().get(getIndex());
                if (s.getPdfPath() != null && !s.getPdfPath().isEmpty()) {
                    Button btn = new Button("📄");
                    btn.setStyle("-fx-background-color:#6d2269; -fx-text-fill:white;" +
                            "-fx-background-radius:10; -fx-padding:3 6; -fx-cursor:hand; -fx-font-size:11px;");
                    btn.setOnAction(e -> {
                        try {
                            java.io.File f = new java.io.File(s.getPdfPath());
                            if (f.exists()) java.awt.Desktop.getDesktop().open(f);
                            else animerErreur("❌ PDF introuvable");
                        } catch (Exception ex) { animerErreur("❌ Erreur ouverture PDF"); }
                    });
                    setGraphic(btn);
                } else { setGraphic(null); }
            }
        });
        table.getColumns().add(colPdf);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> {
            TableRow<Service> row = new TableRow<>();
            row.hoverProperty().addListener((obs, was, is) -> {
                if (!row.isEmpty()) row.setStyle(is ? "-fx-background-color:#f8f5ff;" : "");
            });
            return row;
        });

        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> onSelectionChanged(sel));

        cbCriteria.setItems(FXCollections.observableArrayList("ID", "Titre", "Date", "Statut"));
        cbCriteria.getSelectionModel().selectFirst();
        tfSearch.textProperty().addListener((obs, o, n) -> applyFilter());

        loadData();
    }

    // ══════════════════════════════════════════════
    //  SÉLECTION → PANNEAU DROIT
    // ══════════════════════════════════════════════
    private void onSelectionChanged(Service s) {
        selectedService = s;

        if (s == null) {
            lblSelectionInfo.setText("Cliquez sur une ligne dans le tableau");
            vboxEtapeInfo.setVisible(false);
            vboxEtapeInfo.setManaged(false);
            return;
        }

        String date = s.getDateDemande() != null ? s.getDateDemande().toString() : "-";
        lblSelectionInfo.setText(
                "#" + s.getId() + " — " + s.getTitre() + "\n" +
                        "👤 Employé : " + s.getEmployeeId() + "\n" +
                        "📅 " + date + "  |  " + (s.getStatut() != null ? s.getStatut() : "?")
        );

        String etape    = s.getEtapeWorkflow() != null ? s.getEtapeWorkflow() : "SOUMISE";
        String priorite = s.getPriorite()      != null ? s.getPriorite()      : "NORMAL";
        lblEtapeStatus.setText("Étape : " + etape.replace("_", " ") + "   |   Priorité : " + priorite);

        String[] colors = etapeColors(etape);
        vboxEtapeInfo.setStyle(
                "-fx-background-color:" + colors[0] + ";" +
                        "-fx-background-radius:14; -fx-padding:12;" +
                        "-fx-border-color:" + colors[1] + "; -fx-border-radius:14; -fx-border-width:1;");
        lblEtapeStatus.setStyle("-fx-font-size:12px; -fx-text-fill:" + colors[2] + "; -fx-font-weight:600;");

        buildWorkflowBar(etape);

        vboxEtapeInfo.setVisible(true);
        vboxEtapeInfo.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(250), vboxEtapeInfo);
        ft.setFromValue(0.2); ft.setToValue(1); ft.play();
    }

    private String[] etapeColors(String etape) {
        return switch (etape) {
            case "EN_ANALYSE"    -> new String[]{"#fefce8", "#fde68a", "#92400e"};
            case "EN_TRAITEMENT" -> new String[]{"#eff6ff", "#bfdbfe", "#1e40af"};
            case "RESOLUE"       -> new String[]{"#f0fdf4", "#86efac", "#166534"};
            case "REJETEE"       -> new String[]{"#fef2f2", "#fca5a5", "#991b1b"};
            default              -> new String[]{"#f5f3ff", "#c4b5f4", "#5b21b6"};
        };
    }

    private void buildWorkflowBar(String etapeCourante) {
        hboxWorkflowBar.getChildren().clear();
        String[] etapes = {"SOUMISE", "EN_ANALYSE", "EN_TRAITEMENT", "RESOLUE"};
        java.util.List<String> list = java.util.Arrays.asList(etapes);
        int idxCourant = list.indexOf(etapeCourante);
        boolean rejetee = "REJETEE".equals(etapeCourante);

        for (int i = 0; i < etapes.length; i++) {
            boolean done   = !rejetee && i <= idxCourant;
            boolean active = etapes[i].equals(etapeCourante);
            Label lbl = new Label(etapes[i].replace("_", " ").length() > 8
                    ? etapes[i].replace("EN_", "").replace("_", " ") : etapes[i].replace("_", " "));
            lbl.setStyle(
                    "-fx-background-color:" + (done ? "#6d2269" : "#ede9f6") + ";" +
                            "-fx-text-fill:" + (done ? "white" : "#b39cb0") + ";" +
                            "-fx-font-size:9px; -fx-font-weight:" + (active ? "900" : "600") + ";" +
                            "-fx-background-radius:12; -fx-padding:2 6;");
            hboxWorkflowBar.getChildren().add(lbl);
            if (i < etapes.length - 1) {
                Label arrow = new Label("›");
                arrow.setStyle("-fx-text-fill:#c4b5f4; -fx-font-size:11px;");
                hboxWorkflowBar.getChildren().add(arrow);
            }
        }
        if (rejetee) {
            Label rej = new Label("✕ REJETÉE");
            rej.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                    "-fx-font-size:9px; -fx-font-weight:900; -fx-background-radius:12; -fx-padding:2 6;");
            hboxWorkflowBar.getChildren().addAll(new Label(" "), rej);
        }
    }

    // ══════════════════════════════════════════════
    //  LOAD DATA
    // ══════════════════════════════════════════════
    private void loadData() {
        try {
            serviceService.verifierSlaDepasses();
            List<Service> list = serviceService.getData();

            masterData   = FXCollections.observableArrayList(list);
            filteredData = new FilteredList<>(masterData, p -> true);
            SortedList<Service> sorted = new SortedList<>(filteredData);
            sorted.comparatorProperty().bind(table.comparatorProperty());
            table.setItems(sorted);

            long pending = list.stream().filter(s -> "EN_ATTENTE".equals(s.getStatut())).count();
            msgLabel.setText(pending + " demande(s) en attente");
            msgLabel.setStyle("-fx-font-size:13px; -fx-font-weight:800; -fx-text-fill:white;");

        } catch (SQLException e) {
            animerErreur("❌ Erreur DB");
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════
    //  FILTRES
    // ══════════════════════════════════════════════
    private void applyFilter() {
        if (filteredData == null) return;
        String txt  = tfSearch.getText().toLowerCase();
        String crit = cbCriteria.getValue();
        filteredData.setPredicate(s -> {
            if (txt == null || txt.isEmpty()) return true;
            return switch (crit) {
                case "ID"     -> String.valueOf(s.getId()).contains(txt);
                case "Titre"  -> s.getTitre()       != null && s.getTitre().toLowerCase().contains(txt);
                case "Date"   -> s.getDateDemande()  != null && s.getDateDemande().toString().contains(txt);
                case "Statut" -> s.getStatut()       != null && s.getStatut().toLowerCase().contains(txt);
                default       -> true;
            };
        });
    }

    @FXML private void onFilter() { applyFilter(); }

    @FXML
    public void onReset() {
        tfSearch.clear();
        loadData();
    }

    // ══════════════════════════════════════════════
    //  DÉCISIONS RH
    // ══════════════════════════════════════════════
    @FXML private void onTraiter() { updateStatus("ACCEPTEE"); }
    @FXML private void onRefuse()  { updateStatus("REFUSEE");  }

    private void updateStatus(String statut) {
        Service sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { animerErreur("⚠ Sélectionnez une demande"); return; }

        try {
            sel.setStatut(statut);
            String etape = "ACCEPTEE".equals(statut) ? "RESOLUE" : "REJETEE";
            sel.setEtapeWorkflow(etape);
            sel.setDateDerniereEtape(LocalDate.now());
            serviceService.updateEntity(sel);

            if ("ACCEPTEE".equals(statut)) {
                try {
                    String pdfPath = PdfService.genererTicketPDF(sel);
                    Connection cnx = MyConnection.getInstance().getCnx();
                    PreparedStatement ps = cnx.prepareStatement(
                            "UPDATE demande_service SET pdf_path=? WHERE id=?");
                    ps.setString(1, pdfPath);
                    ps.setLong(2, sel.getId());
                    ps.executeUpdate();
                    java.awt.Desktop.getDesktop().open(new java.io.File(pdfPath));
                    animerSucces("✅ Acceptée — PDF généré 📄");
                } catch (Exception pdfEx) {
                    animerSucces("✅ Acceptée — PDF : " + pdfEx.getMessage());
                    pdfEx.printStackTrace();
                }
            } else {
                animerSucces("❌ Demande refusée avec succès");
            }

            String commentaire = taCommentaire.getText() != null ? taCommentaire.getText().trim() : null;
            long rhId = Session.getCurrentUser().getId();
            Reponse rep = Reponse.forService(statut, commentaire, rhId, sel.getEmployeeId(), sel.getId());
            reponseService.addEntity(rep);

            taCommentaire.clear();
            loadData();

        } catch (SQLException e) {
            animerErreur("❌ Erreur lors du traitement");
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════
    //  COMMENTAIRE SEUL
    // ══════════════════════════════════════════════
    @FXML
    private void onComment() {
        Service sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { animerErreur("⚠ Sélectionnez une demande"); return; }
        String commentaire = taCommentaire.getText();
        if (commentaire == null || commentaire.trim().isEmpty()) {
            animerErreur("⚠ Saisissez un commentaire"); return;
        }
        try {
            long rhId = Session.getCurrentUser().getId();
            Reponse rep = Reponse.forService("COMMENTAIRE", commentaire.trim(), rhId,
                    sel.getEmployeeId(), sel.getId());
            reponseService.addEntity(rep);
            taCommentaire.clear();
            animerSucces("💬 Commentaire enregistré !");
        } catch (SQLException e) {
            animerErreur("❌ Erreur enregistrement");
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════
    //  WORKFLOW — ÉTAPES
    // ══════════════════════════════════════════════
    @FXML private void onEnAnalyse()    { updateEtape("EN_ANALYSE"); }
    @FXML private void onEnTraitement() { updateEtape("EN_TRAITEMENT"); }

    private void updateEtape(String etape) {
        Service sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { animerErreur("⚠ Sélectionnez une demande"); return; }
        try {
            sel.setEtapeWorkflow(etape);
            sel.setDateDerniereEtape(LocalDate.now());
            serviceService.updateEntity(sel);
            animerSucces("✅ Étape → " + etape.replace("_", " "));
            loadData();
        } catch (SQLException e) {
            animerErreur("❌ Erreur mise à jour étape");
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════
    //  ANIMATIONS MESSAGE
    // ══════════════════════════════════════════════
    private void animerSucces(String texte) {
        msgLabel.setText(texte);
        msgLabel.setStyle("-fx-font-size:12px; -fx-font-weight:800; -fx-text-fill:white;");
        ScaleTransition sc = new ScaleTransition(Duration.millis(200), msgLabel);
        sc.setFromX(0.85); sc.setToX(1.0);
        sc.setFromY(0.85); sc.setToY(1.0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), msgLabel);
        ft.setFromValue(0.3); ft.setToValue(1.0);
        sc.play(); ft.play();
    }

    private void animerErreur(String texte) {
        msgLabel.setText(texte);
        msgLabel.setStyle("-fx-font-size:12px; -fx-font-weight:800; -fx-text-fill:#fee2e2;");
        FadeTransition ft = new FadeTransition(Duration.millis(300), msgLabel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ══════════════════════════════════════════════
    //  POPUP STATISTIQUES + AUDIO ElevenLabs
    // ══════════════════════════════════════════════
    @FXML
    private void onShowStats() {
        try {
            // ── 1. Données depuis la BDD ───────────────
            java.util.Map<String, Integer> statuts   = serviceService.getStatsByStatut();
            java.util.Map<String, Integer> mois      = serviceService.getStatsByMois();
            java.util.Map<String, Integer> priorites = serviceService.getStatsByPriorite();
            java.util.Map<String, Integer> sla       = serviceService.getStatsSla();
            double delaiMoyen  = serviceService.getDelaiMoyenTraitement();
            double tauxRes     = serviceService.getTauxResolution();
            int    nbUrgentes  = serviceService.getNbUrgentesEnAttente();
            long   slaDepasses = serviceService.getData().stream().filter(s -> s.isSlaDepasse()).count();
            int    total       = statuts.values().stream().mapToInt(i -> i).sum();

            // ── 2. Fenêtre popup ───────────────────────
            Stage popup = new Stage();
            popup.setTitle("Statistiques des Demandes de Service");
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setWidth(1100);
            popup.setHeight(750);

            // ── 3. Header ──────────────────────────────
            HBox header = new HBox(16);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setStyle("-fx-background-color:#6d2269; -fx-padding:18 30;");

            Label iconLbl = new Label("📊");
            iconLbl.setStyle("-fx-font-size:26px;");

            VBox headerText = new VBox(2);
            Label titleLbl = new Label("Statistiques des Services");
            titleLbl.setStyle("-fx-font-size:20px; -fx-font-weight:900; -fx-text-fill:white;");
            Label subLbl = new Label("Indicateurs de performance RH");
            subLbl.setStyle("-fx-font-size:11px; -fx-text-fill:rgba(255,255,255,0.72);");
            headerText.getChildren().addAll(titleLbl, subLbl);

            // ── 4. Bouton Audio ────────────────────────
            String resumeTexte = ElevenLabsService.construireResume(
                    total, delaiMoyen, tauxRes, slaDepasses, nbUrgentes, statuts, priorites);

            Button btnAudio = new Button("🔊  Lire le rapport");
            btnAudio.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.20);" +
                            "-fx-text-fill:white; -fx-font-weight:900;" +
                            "-fx-background-radius:22; -fx-padding:10 20;" +
                            "-fx-cursor:hand; -fx-font-size:13px;" +
                            "-fx-border-color:rgba(255,255,255,0.5);" +
                            "-fx-border-width:1.5; -fx-border-radius:22;");

            Label lblAudioStatus = new Label("Cliquez pour écouter le résumé vocal");
            lblAudioStatus.setStyle("-fx-font-size:10px; -fx-text-fill:rgba(255,255,255,0.65);");

            MediaPlayer[] playerRef = new MediaPlayer[1];

            btnAudio.setOnAction(ev -> {
                if (playerRef[0] != null) {
                    playerRef[0].stop();
                    playerRef[0] = null;
                    btnAudio.setText("🔊  Lire le rapport");
                    btnAudio.setStyle(
                            "-fx-background-color:rgba(255,255,255,0.20);" +
                                    "-fx-text-fill:white; -fx-font-weight:900;" +
                                    "-fx-background-radius:22; -fx-padding:10 20;" +
                                    "-fx-cursor:hand; -fx-font-size:13px;" +
                                    "-fx-border-color:rgba(255,255,255,0.5);" +
                                    "-fx-border-width:1.5; -fx-border-radius:22;");
                    lblAudioStatus.setText("Cliquez pour écouter le résumé vocal");
                    return;
                }

                btnAudio.setText("⏳  Génération...");
                btnAudio.setDisable(true);
                lblAudioStatus.setText("Connexion à ElevenLabs en cours...");

                Thread t = new Thread(() -> {
                    try {
                        File mp3 = ElevenLabsService.genererAudio(resumeTexte);
                        javafx.application.Platform.runLater(() -> {
                            try {
                                MediaPlayer player = ElevenLabsService.jouerAudio(mp3);
                                playerRef[0] = player;

                                player.setOnEndOfMedia(() -> {
                                    playerRef[0] = null;
                                    btnAudio.setText("🔊  Lire le rapport");
                                    btnAudio.setStyle(
                                            "-fx-background-color:rgba(255,255,255,0.20);" +
                                                    "-fx-text-fill:white; -fx-font-weight:900;" +
                                                    "-fx-background-radius:22; -fx-padding:10 20;" +
                                                    "-fx-cursor:hand; -fx-font-size:13px;" +
                                                    "-fx-border-color:rgba(255,255,255,0.5);" +
                                                    "-fx-border-width:1.5; -fx-border-radius:22;");
                                    lblAudioStatus.setText("Lecture terminée");
                                });

                                btnAudio.setText("⏹  Arrêter");
                                btnAudio.setStyle(
                                        "-fx-background-color:rgba(220,38,38,0.8);" +
                                                "-fx-text-fill:white; -fx-font-weight:900;" +
                                                "-fx-background-radius:22; -fx-padding:10 20;" +
                                                "-fx-cursor:hand; -fx-font-size:13px;");
                                btnAudio.setDisable(false);
                                lblAudioStatus.setText("▶ Lecture en cours...");

                            } catch (Exception ex) {
                                btnAudio.setText("🔊  Lire le rapport");
                                btnAudio.setDisable(false);
                                lblAudioStatus.setText("❌ Erreur : " + ex.getMessage());
                            }
                        });
                    } catch (Exception ex) {
                        javafx.application.Platform.runLater(() -> {
                            btnAudio.setText("🔊  Lire le rapport");
                            btnAudio.setDisable(false);
                            lblAudioStatus.setText("❌ " + ex.getMessage());
                        });
                    }
                });
                t.setDaemon(true);
                t.start();
            });

            popup.setOnHiding(e -> {
                if (playerRef[0] != null) playerRef[0].stop();
            });

            VBox audioBox = new VBox(8);
            audioBox.setAlignment(Pos.CENTER_RIGHT);
            audioBox.getChildren().addAll(btnAudio, lblAudioStatus);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(iconLbl, headerText, spacer, audioBox);

            // ── 5. KPI Cards ───────────────────────────
            HBox kpiRow = new HBox(14);
            kpiRow.setPadding(new Insets(0));
            kpiRow.getChildren().addAll(
                    creerKpiCard("Total demandes",  String.valueOf(total), "#6d2269"),
                    creerKpiCard("Délai moyen",     String.format("%.1f j", delaiMoyen), "#0369a1"),
                    creerKpiCard("Taux résolution", String.format("%.0f%%", tauxRes), "#059669"),
                    creerKpiCard("SLA dépassés",    String.valueOf(slaDepasses), slaDepasses > 0 ? "#dc2626" : "#059669"),
                    creerKpiCard("Urgentes",        String.valueOf(nbUrgentes), nbUrgentes > 0 ? "#ea580c" : "#6d2269")
            );

            // ── 6. Graphiques ──────────────────────────
            PieChart pieStatuts = new PieChart();
            pieStatuts.setTitle("Répartition par statut");
            pieStatuts.setLegendVisible(true);
            pieStatuts.setLabelsVisible(true);
            statuts.forEach((s, n) -> pieStatuts.getData().add(new PieChart.Data(s + " (" + n + ")", n)));

            CategoryAxis xMois = new CategoryAxis();
            NumberAxis   yMois = new NumberAxis();
            BarChart<String, Number> barMois = new BarChart<>(xMois, yMois);
            barMois.setTitle("Évolution mensuelle");
            barMois.setLegendVisible(false);
            XYChart.Series<String, Number> seriesMois = new XYChart.Series<>();
            mois.forEach((m, n) -> seriesMois.getData().add(new XYChart.Data<>(m, n)));
            barMois.getData().add(seriesMois);

            CategoryAxis xPrio = new CategoryAxis();
            NumberAxis   yPrio = new NumberAxis();
            BarChart<String, Number> barPrio = new BarChart<>(xPrio, yPrio);
            barPrio.setTitle("Par priorité");
            barPrio.setLegendVisible(false);
            XYChart.Series<String, Number> seriesPrio = new XYChart.Series<>();
            priorites.forEach((p, n) -> seriesPrio.getData().add(new XYChart.Data<>(p, n)));
            barPrio.getData().add(seriesPrio);

            PieChart pieSla = new PieChart();
            pieSla.setTitle("SLA respectés vs dépassés");
            pieSla.setLegendVisible(true);
            sla.forEach((s, n) -> pieSla.getData().add(new PieChart.Data(s + " (" + n + ")", n)));

            HBox chartsRow1 = new HBox(16);
            chartsRow1.getChildren().addAll(enveloperChart(pieStatuts), enveloperChart(barMois));
            HBox.setHgrow(chartsRow1.getChildren().get(0), Priority.ALWAYS);
            HBox.setHgrow(chartsRow1.getChildren().get(1), Priority.ALWAYS);

            HBox chartsRow2 = new HBox(16);
            chartsRow2.getChildren().addAll(enveloperChart(barPrio), enveloperChart(pieSla));
            HBox.setHgrow(chartsRow2.getChildren().get(0), Priority.ALWAYS);
            HBox.setHgrow(chartsRow2.getChildren().get(1), Priority.ALWAYS);

            // ── 7. Contenu scrollable ──────────────────
            VBox content = new VBox(18);
            content.setPadding(new Insets(22, 28, 22, 28));
            content.setStyle("-fx-background-color:#f3f0fa;");
            content.getChildren().addAll(kpiRow, chartsRow1, chartsRow2);

            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background-color:#f3f0fa; -fx-background:#f3f0fa;");

            VBox root = new VBox();
            root.getChildren().addAll(header, scroll);
            VBox.setVgrow(scroll, Priority.ALWAYS);

            popup.setScene(new Scene(root));
            popup.show();

        } catch (Exception e) {
            e.printStackTrace();
            animerErreur("❌ Erreur chargement statistiques");
        }
    }

    private VBox creerKpiCard(String titre, String valeur, String couleur) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:" + couleur + " transparent transparent transparent;"
                + "-fx-border-width:4 0 0 0; -fx-border-radius:16 16 0 0;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,4);"
                + "-fx-padding:14 18;");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label t = new Label(titre);
        t.setStyle("-fx-font-size:11px; -fx-text-fill:#9c5c9a; -fx-font-weight:700;");
        Label v = new Label(valeur);
        v.setStyle("-fx-font-size:30px; -fx-font-weight:900; -fx-text-fill:" + couleur + ";");
        card.getChildren().addAll(t, v);
        return card;
    }

    private VBox enveloperChart(javafx.scene.Node chart) {
        VBox box = new VBox(chart);
        box.setStyle("-fx-background-color:white; -fx-background-radius:18;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,4);"
                + "-fx-padding:14;");
        HBox.setHgrow(box, Priority.ALWAYS);
        VBox.setVgrow(chart, Priority.ALWAYS);
        return box;
    }
}