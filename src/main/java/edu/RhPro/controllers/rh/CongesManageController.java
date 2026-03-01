package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Conge;
import edu.RhPro.services.CongeService;
import edu.RhPro.services.CongeRegleService;
import edu.RhPro.services.CongeRegleService.*;
import edu.RhPro.services.ReponseService;
import edu.RhPro.services.SmsService;
import java.sql.ResultSet;
import edu.RhPro.tools.MyConnection;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.util.Duration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class CongesManageController {

    @FXML private TableView<Conge>              table;
    @FXML private TableColumn<Conge, Long>      colId;
    @FXML private TableColumn<Conge, Long>      colEmploye;
    @FXML private TableColumn<Conge, String>    colType;
    @FXML private TableColumn<Conge, LocalDate> colDebut;
    @FXML private TableColumn<Conge, LocalDate> colFin;
    @FXML private TableColumn<Conge, String>    colDesc;

    @FXML private ComboBox<String> cbCriteria;
    @FXML private TextField        tfSearch;
    @FXML private TextArea         taCommentaire;
    @FXML private Label            msgLabel;

    // Panneau droit
    @FXML private Label   lblSelectionInfo;
    @FXML private Button  btnAnalyse;
    @FXML private VBox    vboxCertifInfo;
    @FXML private Label   lblCertifStatus;
    @FXML private Button  btnVoirCertifPanel;
    @FXML private VBox    vboxResumeDuree;
    @FXML private Label   lblJoursCal;
    @FXML private Label   lblJoursOuv;
    @FXML private Label   lblRegleStatut;
    @FXML private Label   lblFeriesResume;

    private final CongeService   congeService   = new CongeService();
    private final ReponseService reponseService = new ReponseService();

    private ObservableList<Conge> masterData;
    private FilteredList<Conge>   filteredData;
    private Conge selectedConge;

    // ══════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════
    @FXML
    public void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeConge"));
        colDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        colDesc.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setTooltip(null); }
                else {
                    setText(item.length() > 35 ? item.substring(0, 35) + "…" : item);
                    Tooltip tip = new Tooltip(item);
                    tip.setWrapText(true); tip.setMaxWidth(350);
                    setTooltip(tip);
                }
            }
        });

        // ── Colonne Durée ──────────────────────────
        TableColumn<Conge, Void> colDuree = new TableColumn<>("Durée");
        colDuree.setPrefWidth(100);
        colDuree.setResizable(false);
        colDuree.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Conge c = getTableView().getItems().get(getIndex());
                if (c.getDateDebut() == null || c.getDateFin() == null) { setGraphic(null); return; }
                long jCal = c.getDateFin().toEpochDay() - c.getDateDebut().toEpochDay();
                int  jOuv = CongeRegleService.calculerJoursOuvrables(c.getDateDebut(), c.getDateFin());
                RegleConge regle = CongeRegleService.getRegle(c.getTypeConge());
                boolean depasse = regle != null && jCal > regle.maxJours;
                boolean sousMin = regle != null && jCal < regle.minJours;

                VBox box = new VBox(2);
                box.setAlignment(Pos.CENTER_LEFT);
                Label cal = new Label(jCal + " j cal.");
                cal.setStyle("-fx-font-size:11px; -fx-font-weight:800; -fx-text-fill:" +
                        (depasse || sousMin ? "#dc2626" : "#6d2269") + ";");
                Label ouv = new Label(jOuv + " j ouv.");
                ouv.setStyle("-fx-font-size:10px; -fx-text-fill:#9c5c9a;");
                box.getChildren().addAll(cal, ouv);
                if (depasse) {
                    Label warn = new Label("⚠ DÉPASSÉ");
                    warn.setStyle("-fx-font-size:9px; -fx-text-fill:#dc2626; -fx-font-weight:900;");
                    box.getChildren().add(warn);
                } else if (sousMin) {
                    Label warn = new Label("⚠ TROP COURT");
                    warn.setStyle("-fx-font-size:9px; -fx-text-fill:#d97706; -fx-font-weight:900;");
                    box.getChildren().add(warn);
                }
                setGraphic(box);
            }
        });
        table.getColumns().add(colDuree);

        // ── Colonne Fériés ─────────────────────────
        TableColumn<Conge, Void> colFeries = new TableColumn<>("Fériés");
        colFeries.setPrefWidth(65);
        colFeries.setResizable(false);
        colFeries.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Conge c = getTableView().getItems().get(getIndex());
                if (c.getDateDebut() == null || c.getDateFin() == null) { setGraphic(null); return; }
                List<Map.Entry<LocalDate, JourFerie>> feries =
                        CongeRegleService.getJeriesDansPeriode(c.getDateDebut(), c.getDateFin());
                if (feries.isEmpty()) { setText("—"); setStyle("-fx-text-fill:#ccc;"); setGraphic(null); return; }
                Label badge = new Label("🎉 " + feries.size());
                badge.setStyle("-fx-background-color:#fff7ed; -fx-text-fill:#ea580c;" +
                        "-fx-background-radius:14; -fx-padding:3 8;" +
                        "-fx-font-size:11px; -fx-font-weight:800;");
                StringBuilder sb = new StringBuilder("Jours fériés dans la période :\n\n");
                for (Map.Entry<LocalDate, JourFerie> e : feries)
                    sb.append(e.getValue().emoji).append("  ").append(e.getValue().nom)
                            .append("  (").append(e.getKey()).append(")\n");
                Tooltip tip = new Tooltip(sb.toString());
                tip.setStyle("-fx-font-size:13px;");
                badge.setTooltip(tip);
                setGraphic(badge);
            }
        });
        table.getColumns().add(colFeries);

        // ── Colonne Certificat OCR ─────────────────
        TableColumn<Conge, Void> colCertif = new TableColumn<>("Certificat");
        colCertif.setPrefWidth(140);
        colCertif.setResizable(false);
        colCertif.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Conge conge = getTableView().getItems().get(getIndex());
                if (!"Congé maladie".equals(conge.getTypeConge())) { setGraphic(null); return; }
                Label badge;
                if (conge.isOcrVerified()) {
                    badge = new Label("🔐 OCR ✅");
                    badge.setStyle("-fx-background-color:#d1fae5; -fx-text-fill:#059669;" +
                            "-fx-background-radius:15; -fx-padding:3 8; -fx-font-size:11px; -fx-font-weight:bold;");
                } else if (conge.getDocumentPath() != null) {
                    badge = new Label("⚠ Non reconnu");
                    badge.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                            "-fx-background-radius:15; -fx-padding:3 8; -fx-font-size:11px; -fx-font-weight:bold;");
                } else {
                    badge = new Label("❌ Pas de certif");
                    badge.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                            "-fx-background-radius:15; -fx-padding:3 8; -fx-font-size:11px; -fx-font-weight:bold;");
                    setGraphic(badge); return;
                }
                if (conge.getDocumentPath() != null) {
                    Button btnOuvrir = new Button("📎");
                    btnOuvrir.setStyle("-fx-background-color:#6d2269; -fx-text-fill:white;" +
                            "-fx-background-radius:12; -fx-padding:3 8; -fx-cursor:hand; -fx-font-size:11px;");
                    btnOuvrir.setOnAction(e -> ouvrirFichier(conge.getDocumentPath()));
                    HBox box = new HBox(6, badge, btnOuvrir);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                } else { setGraphic(badge); }
            }
        });
        table.getColumns().add(colCertif);

        cbCriteria.setItems(FXCollections.observableArrayList("ID", "Employé", "Type", "Date Début", "Date Fin"));
        cbCriteria.getSelectionModel().selectFirst();

        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> onSelectionChanged(selected));

        loadData();
        tfSearch.textProperty().addListener((obs, o, n) -> applyFilter());

        table.setRowFactory(tv -> {
            TableRow<Conge> row = new TableRow<>();
            row.hoverProperty().addListener((obs, wasHover, isHover) -> {
                if (!row.isEmpty()) row.setStyle(isHover ? "-fx-background-color:#f5f0fc;" : "");
            });
            row.itemProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    FadeTransition ft = new FadeTransition(Duration.millis(200), row);
                    ft.setFromValue(0.3); ft.setToValue(1.0); ft.play();
                }
            });
            return row;
        });

        FadeTransition ft = new FadeTransition(Duration.millis(500), table);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), table);
        tt.setFromX(-20); tt.setToX(0); tt.play();
    }

    // ══════════════════════════════════════════
    //  SÉLECTION → PANNEAU DROITE
    // ══════════════════════════════════════════
    private void onSelectionChanged(Conge conge) {
        selectedConge = conge;

        if (conge == null) {
            lblSelectionInfo.setText("Cliquez sur une ligne dans le tableau");
            btnAnalyse.setVisible(false);
            btnAnalyse.setManaged(false);
            vboxCertifInfo.setVisible(false);
            vboxCertifInfo.setManaged(false);
            vboxResumeDuree.setVisible(false);
            vboxResumeDuree.setManaged(false);
            return;
        }

        // Info principale
        String date1 = conge.getDateDebut() != null ? conge.getDateDebut().toString() : "?";
        String date2 = conge.getDateFin()   != null ? conge.getDateFin().toString()   : "?";
        lblSelectionInfo.setText(
                "ID " + conge.getId() + "  •  " + conge.getTypeConge() + "\n" +
                        "Employé #" + conge.getEmployeeId() + "\n" +
                        "Du " + date1 + " au " + date2
        );

        FadeTransition ft = new FadeTransition(Duration.millis(200), lblSelectionInfo);
        ft.setFromValue(0.2); ft.setToValue(1.0); ft.play();

        // Bouton analyse
        btnAnalyse.setVisible(true);
        btnAnalyse.setManaged(true);

        // Panneau certif
        if ("Congé maladie".equals(conge.getTypeConge())) {
            vboxCertifInfo.setVisible(true);
            vboxCertifInfo.setManaged(true);
            if (conge.isOcrVerified()) {
                lblCertifStatus.setText("✅ Certificat vérifié par OCR");
                vboxCertifInfo.setStyle("-fx-background-color:#f0fdf4; -fx-background-radius:14; -fx-padding:14;" +
                        "-fx-border-color:#86efac; -fx-border-radius:14; -fx-border-width:1;");
            } else if (conge.getDocumentPath() != null) {
                lblCertifStatus.setText("⚠ Document fourni, OCR non reconnu");
                vboxCertifInfo.setStyle("-fx-background-color:#fffbeb; -fx-background-radius:14; -fx-padding:14;" +
                        "-fx-border-color:#fcd34d; -fx-border-radius:14; -fx-border-width:1;");
            } else {
                lblCertifStatus.setText("❌ Aucun certificat fourni — requis pour congé maladie");
                vboxCertifInfo.setStyle("-fx-background-color:#fef2f2; -fx-background-radius:14; -fx-padding:14;" +
                        "-fx-border-color:#fca5a5; -fx-border-radius:14; -fx-border-width:1;");
            }
            boolean hasFichier = conge.getDocumentPath() != null;
            btnVoirCertifPanel.setVisible(hasFichier);
            btnVoirCertifPanel.setManaged(hasFichier);
            ScaleTransition st = new ScaleTransition(Duration.millis(200), vboxCertifInfo);
            st.setFromY(0.6); st.setToY(1.0); st.play();
        } else {
            vboxCertifInfo.setVisible(false);
            vboxCertifInfo.setManaged(false);
        }

        // Résumé durée/limite dans le panneau droit
        if (conge.getDateDebut() != null && conge.getDateFin() != null) {
            long jCal = CongeRegleService.calculerJoursCalendaires(conge.getDateDebut(), conge.getDateFin());
            int  jOuv = CongeRegleService.calculerJoursOuvrables(conge.getDateDebut(), conge.getDateFin());
            RegleConge regle = CongeRegleService.getRegle(conge.getTypeConge());
            List<Map.Entry<LocalDate, JourFerie>> feries =
                    CongeRegleService.getJeriesDansPeriode(conge.getDateDebut(), conge.getDateFin());

            lblJoursCal.setText("📅 " + jCal + " jours calendaires");
            lblJoursOuv.setText("💼 " + jOuv + " jours ouvrables");

            if (regle != null) {
                boolean ok = jCal >= regle.minJours && jCal <= regle.maxJours;
                if (ok) {
                    lblRegleStatut.setText("✅ Conforme — max " + regle.maxJours + " j / min " + regle.minJours + " j");
                    lblRegleStatut.setStyle("-fx-background-color:#d1fae5; -fx-text-fill:#059669;" +
                            "-fx-font-size:12px; -fx-font-weight:800; -fx-background-radius:10; -fx-padding:8 12;");
                } else if (jCal > regle.maxJours) {
                    lblRegleStatut.setText("❌ DÉPASSÉ — max autorisé : " + regle.maxJours + " jours");
                    lblRegleStatut.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                            "-fx-font-size:12px; -fx-font-weight:800; -fx-background-radius:10; -fx-padding:8 12;");
                } else {
                    lblRegleStatut.setText("⚠ Trop court — min requis : " + regle.minJours + " jours");
                    lblRegleStatut.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                            "-fx-font-size:12px; -fx-font-weight:800; -fx-background-radius:10; -fx-padding:8 12;");
                }
            } else {
                lblRegleStatut.setText("— Aucune règle définie pour ce type");
                lblRegleStatut.setStyle("-fx-text-fill:#9c5c9a; -fx-font-size:12px;");
            }

            if (!feries.isEmpty()) {
                lblFeriesResume.setText("🎉 " + feries.size() + " jour(s) férié(s) dans la période\n→ Cliquez « Voir règles » pour le détail");
                lblFeriesResume.setVisible(true);
                lblFeriesResume.setManaged(true);
            } else {
                lblFeriesResume.setVisible(false);
                lblFeriesResume.setManaged(false);
            }

            vboxResumeDuree.setVisible(true);
            vboxResumeDuree.setManaged(true);
            FadeTransition ftR = new FadeTransition(Duration.millis(300), vboxResumeDuree);
            ftR.setFromValue(0); ftR.setToValue(1); ftR.play();
        } else {
            vboxResumeDuree.setVisible(false);
            vboxResumeDuree.setManaged(false);
        }
    }

    // ══════════════════════════════════════════
    //  POPUP ANALYSE COMPLÈTE (règles + fériés)
    // ══════════════════════════════════════════
    @FXML
    private void onShowAnalyse() {
        if (selectedConge == null) return;
        afficherPopupAnalyse(selectedConge);
    }

    private void afficherPopupAnalyse(Conge conge) {
        Stage popup = new Stage();
        popup.setTitle("Analyse — " + conge.getTypeConge() + " #" + conge.getId());
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setWidth(700);
        popup.setHeight(620);

        RegleConge regle = CongeRegleService.getRegle(conge.getTypeConge());
        ResultatValidation resultat = CongeRegleService.valider(
                conge.getTypeConge(), conge.getDateDebut(), conge.getDateFin(),
                conge.getDocumentPath() != null);

        // ── HEADER ──────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:#6d2269; -fx-padding:20 28;");
        Label hIcon = new Label(regle != null ? regle.icone : "📋");
        hIcon.setStyle("-fx-font-size:30px;");
        VBox hText = new VBox(3);
        Label hTitle = new Label(conge.getTypeConge() + "  —  Demande #" + conge.getId());
        hTitle.setStyle("-fx-font-size:17px; -fx-font-weight:900; -fx-text-fill:white;");
        Label hSub = new Label("Employé #" + conge.getEmployeeId() +
                "   •   " + conge.getDateDebut() + " → " + conge.getDateFin());
        hSub.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(255,255,255,0.75);");
        hText.getChildren().addAll(hTitle, hSub);
        header.getChildren().addAll(hIcon, hText);

        // ── CONTENU ──────────────────────────────
        VBox content = new VBox(16);
        content.setPadding(new Insets(22, 28, 22, 28));
        content.setStyle("-fx-background-color:#f8f5ff;");

        // Section 1 : Durée
        VBox secDuree = creerSection("📊 Durée de la demande");
        HBox dureeRow = new HBox(12);
        dureeRow.setAlignment(Pos.CENTER_LEFT);
        Label lCal = creerBadge(resultat.joursCalendaires + " jours calendaires", "#ede9f6", "#6d2269");
        Label lOuv = creerBadge(resultat.joursOuvrables + " jours ouvrables effectifs", "#d1fae5", "#059669");
        dureeRow.getChildren().addAll(lCal, lOuv);
        secDuree.getChildren().add(dureeRow);
        content.getChildren().add(secDuree);

        // Section 2 : Règle légale
        if (regle != null) {
            VBox secRegle = creerSection("⚖ Règle légale applicable");
            Label descRegle = new Label(regle.description);
            descRegle.setWrapText(true);
            descRegle.setStyle("-fx-font-size:13px; -fx-text-fill:#444; -fx-line-spacing:3;");

            HBox limitRow = new HBox(12);
            limitRow.setAlignment(Pos.CENTER_LEFT);
            Label lMin = creerBadge("⬇ Minimum : " + regle.minJours + " jours", "#dbeafe", "#1d4ed8");
            Label lMax = creerBadge("⬆ Maximum : " + regle.maxJours + " jours", "#fef3c7", "#d97706");
            limitRow.getChildren().addAll(lMin, lMax);
            if (regle.documentObligatoire) {
                Label lDoc = creerBadge("📄 Certificat obligatoire", "#fee2e2", "#dc2626");
                limitRow.getChildren().add(lDoc);
            }
            secRegle.getChildren().addAll(descRegle, limitRow);

            // Statut conformité
            boolean conforme = resultat.erreurs.isEmpty();
            String statuStyle = conforme
                    ? "-fx-background-color:#d1fae5; -fx-text-fill:#059669;"
                    : "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;";
            Label statutLbl = new Label(conforme
                    ? "✅  Cette demande est conforme aux règles légales"
                    : "❌  " + String.join("\n❌  ", resultat.erreurs));
            statutLbl.setWrapText(true);
            statutLbl.setStyle(statuStyle + "-fx-font-size:13px; -fx-font-weight:800;" +
                    "-fx-background-radius:12; -fx-padding:12 16;");
            secRegle.getChildren().add(statutLbl);

            // Avertissements
            for (String warn : resultat.avertissements) {
                Label wLbl = new Label("⚠  " + warn);
                wLbl.setWrapText(true);
                wLbl.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                        "-fx-font-size:13px; -fx-font-weight:700;" +
                        "-fx-background-radius:12; -fx-padding:10 16;");
                secRegle.getChildren().add(wLbl);
            }

            content.getChildren().add(secRegle);
        }

        // Section 3 : Jours fériés
        VBox secFeries = creerSection("🎉 Jours fériés inclus dans la période");
        if (resultat.feriesDansPeriode.isEmpty()) {
            Label noFerie = new Label("✅  Aucun jour férié dans cette période");
            noFerie.setStyle("-fx-font-size:13px; -fx-text-fill:#059669; -fx-font-weight:700;");
            secFeries.getChildren().add(noFerie);
        } else {
            Label ferieInfo = new Label("Ces " + resultat.feriesDansPeriode.size() +
                    " jour(s) férié(s) sont inclus dans la période — ils ne comptent pas comme jours ouvrables.");
            ferieInfo.setWrapText(true);
            ferieInfo.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-padding:0 0 6 0;");
            secFeries.getChildren().add(ferieInfo);

            for (Map.Entry<LocalDate, JourFerie> entry : resultat.feriesDansPeriode) {
                HBox row = new HBox(14);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 16, 10, 16));
                boolean isIsl = "islamique".equals(entry.getValue().type);
                row.setStyle("-fx-background-color:" + (isIsl ? "#fffbeb" : "white") + ";" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + (isIsl ? "#fde68a" : "#e0d9f7") + ";" +
                        "-fx-border-radius:14; -fx-border-width:1.5;");
                Label eLbl = new Label(entry.getValue().emoji);
                eLbl.setStyle("-fx-font-size:22px; -fx-min-width:30;");
                VBox info = new VBox(2);
                Label nomLbl = new Label(entry.getValue().nom);
                nomLbl.setStyle("-fx-font-size:14px; -fx-font-weight:900; -fx-text-fill:#3d1a3b;");
                // Format jour de la semaine + date
                String jour = capitalize(entry.getKey().getDayOfWeek().toString().toLowerCase());
                Label dateLbl = new Label(jour + " " + entry.getKey());
                dateLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#9c5c9a;");
                info.getChildren().addAll(nomLbl, dateLbl);
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                Label typeLbl = new Label(isIsl ? "☪ Islamique" : "🏛 National");
                typeLbl.setStyle("-fx-background-color:" + (isIsl ? "#fef3c7" : "#dbeafe") + ";" +
                        "-fx-text-fill:" + (isIsl ? "#d97706" : "#1d4ed8") + ";" +
                        "-fx-background-radius:12; -fx-padding:4 12;" +
                        "-fx-font-size:11px; -fx-font-weight:800;");
                row.getChildren().addAll(eLbl, info, sp, typeLbl);
                secFeries.getChildren().add(row);
            }
        }
        content.getChildren().add(secFeries);

        // Bouton fermer
        Button btnFermer = new Button("✓  Fermer");
        btnFermer.setStyle("-fx-background-color:#6d2269; -fx-text-fill:white;" +
                "-fx-font-weight:900; -fx-background-radius:22;" +
                "-fx-padding:11 32; -fx-cursor:hand; -fx-font-size:13px;");
        btnFermer.setOnAction(e -> popup.close());
        HBox btnRow = new HBox(btnFermer);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(0, 28, 20, 28));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#f8f5ff; -fx-background:#f8f5ff;");

        VBox root = new VBox();
        root.getChildren().addAll(header, scroll, btnRow);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        popup.setScene(new Scene(root));
        popup.show();
    }

    // ── Popup Calendrier Jours Fériés ────────────
    @FXML
    private void onShowJoursFeries() {
        Stage popup = new Stage();
        popup.setTitle("Calendrier des jours fériés — Tunisie");
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setWidth(660);
        popup.setHeight(600);

        HBox header = new HBox(14);
        header.setStyle("-fx-background-color:#6d2269; -fx-padding:20 28;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label icH = new Label("🇹🇳");
        icH.setStyle("-fx-font-size:28px;");
        VBox hTxt = new VBox(2);
        Label h1 = new Label("Jours Fériés — Tunisie");
        h1.setStyle("-fx-font-size:18px; -fx-font-weight:900; -fx-text-fill:white;");
        Label h2 = new Label("Calendrier officiel national et islamique");
        h2.setStyle("-fx-font-size:11px; -fx-text-fill:rgba(255,255,255,0.72);");
        hTxt.getChildren().addAll(h1, h2);
        header.getChildren().addAll(icH, hTxt);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 24, 20, 24));
        content.setStyle("-fx-background-color:#f8f5ff;");

        int annee = LocalDate.now().getYear();
        for (int a = annee; a <= annee + 1; a++) {
            Label anneeLabel = new Label("━━━━  " + a + "  ━━━━");
            anneeLabel.setStyle("-fx-font-size:14px; -fx-font-weight:900; -fx-text-fill:#6d2269; -fx-padding:4 0 0 0;");
            content.getChildren().add(anneeLabel);

            Map<LocalDate, JourFerie> feries = CongeRegleService.getJoursFeries(a);
            for (Map.Entry<LocalDate, JourFerie> entry : feries.entrySet()) {
                HBox row = new HBox(14);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 16, 10, 16));
                boolean isIsl = "islamique".equals(entry.getValue().type);
                row.setStyle("-fx-background-color:" + (isIsl ? "#fffbeb" : "white") + ";" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + (isIsl ? "#fde68a" : "#e0d9f7") + ";" +
                        "-fx-border-radius:14; -fx-border-width:1.5;");
                Label eLbl = new Label(entry.getValue().emoji);
                eLbl.setStyle("-fx-font-size:22px; -fx-min-width:30;");
                VBox info = new VBox(2);
                Label nomLbl = new Label(entry.getValue().nom);
                nomLbl.setStyle("-fx-font-size:13px; -fx-font-weight:900; -fx-text-fill:#3d1a3b;");
                String jour = capitalize(entry.getKey().getDayOfWeek().toString().toLowerCase());
                Label dateLbl = new Label(jour + " " + entry.getKey());
                dateLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#9c5c9a;");
                info.getChildren().addAll(nomLbl, dateLbl);
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                Label typeLbl = new Label(isIsl ? "☪ Islamique" : "🏛 National");
                typeLbl.setStyle("-fx-background-color:" + (isIsl ? "#fef3c7" : "#dbeafe") + ";" +
                        "-fx-text-fill:" + (isIsl ? "#d97706" : "#1d4ed8") + ";" +
                        "-fx-background-radius:12; -fx-padding:3 10;" +
                        "-fx-font-size:10px; -fx-font-weight:800;");
                row.getChildren().addAll(eLbl, info, sp, typeLbl);
                content.getChildren().add(row);
            }
        }

        Label note = new Label("⚠  Les dates islamiques sont approximatives et peuvent varier selon l'observation de la lune.");
        note.setWrapText(true);
        note.setStyle("-fx-font-size:11px; -fx-text-fill:#9c5c9a; -fx-padding:8 24; -fx-font-style:italic;");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#f8f5ff; -fx-background:#f8f5ff;");
        VBox root = new VBox();
        root.getChildren().addAll(header, scroll, note);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        popup.setScene(new Scene(root));
        popup.show();
    }

    // ── Helpers UI ───────────────────────────────
    private VBox creerSection(String titre) {
        VBox sec = new VBox(12);
        sec.setStyle("-fx-background-color:white; -fx-background-radius:18;" +
                "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.08),12,0,0,3);" +
                "-fx-padding:18 20;");
        Label t = new Label(titre);
        t.setStyle("-fx-font-size:14px; -fx-font-weight:900; -fx-text-fill:#6d2269;");
        sec.getChildren().add(t);
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#ede9f6;");
        sec.getChildren().add(sep);
        return sec;
    }

    private Label creerBadge(String texte, String bg, String fg) {
        Label l = new Label(texte);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                "-fx-background-radius:20; -fx-padding:6 14;" +
                "-fx-font-size:12px; -fx-font-weight:800;");
        return l;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    // ══════════════════════════════════════════
    //  Certif
    // ══════════════════════════════════════════
    @FXML
    private void onOuvrirCertificat() {
        if (selectedConge != null && selectedConge.getDocumentPath() != null)
            ouvrirFichier(selectedConge.getDocumentPath());
    }

    private void ouvrirFichier(String path) {
        try {
            File certif = new File(path);
            if (certif.exists()) {
                java.awt.Desktop.getDesktop().open(certif);
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Fichier introuvable");
                alert.setContentText("Le fichier n'existe plus :\n" + path);
                alert.showAndWait();
            }
        } catch (Exception ex) { animerErreur("Erreur ouverture fichier ❌"); }
    }

    // ══════════════════════════════════════════
    //  DONNÉES
    // ══════════════════════════════════════════
    private void loadData() {
        try {
            List<Conge> list = congeService.findPending();
            masterData   = FXCollections.observableArrayList(list);
            filteredData = new FilteredList<>(masterData, p -> true);
            SortedList<Conge> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(table.comparatorProperty());
            table.setItems(sortedData);
            msgLabel.setText(list.size() + " demande(s) en attente");
        } catch (SQLException e) { msgLabel.setText("Erreur DB ❌"); }
    }

    // ══════════════════════════════════════════
    //  FILTRE
    // ══════════════════════════════════════════
    private void applyFilter() {
        String keyword  = tfSearch.getText();
        String criteria = cbCriteria.getValue();
        filteredData.setPredicate(conge -> {
            if (keyword == null || keyword.isEmpty()) return true;
            return switch (criteria) {
                case "ID"         -> String.valueOf(conge.getId()).contains(keyword);
                case "Employé"    -> String.valueOf(conge.getEmployeeId()).contains(keyword);
                case "Type"       -> conge.getTypeConge() != null &&
                        conge.getTypeConge().toLowerCase().contains(keyword.toLowerCase());
                case "Date Début" -> conge.getDateDebut() != null &&
                        conge.getDateDebut().toString().contains(keyword);
                case "Date Fin"   -> conge.getDateFin() != null &&
                        conge.getDateFin().toString().contains(keyword);
                default           -> true;
            };
        });
    }

    @FXML private void onReset() {
        tfSearch.clear();
        cbCriteria.getSelectionModel().selectFirst();
        filteredData.setPredicate(p -> true);
        table.refresh();
        lblSelectionInfo.setText("Cliquez sur une ligne dans le tableau");
        btnAnalyse.setVisible(false);
        btnAnalyse.setManaged(false);
        vboxCertifInfo.setVisible(false);
        vboxCertifInfo.setManaged(false);
        vboxResumeDuree.setVisible(false);
        vboxResumeDuree.setManaged(false);
        selectedConge = null;
    }

    @FXML private void onFilter() {
        String criteria = cbCriteria.getValue();
        if (criteria == null || masterData == null) return;
        FXCollections.sort(masterData, (c1, c2) -> switch (criteria) {
            case "ID"         -> Long.compare(c1.getId(), c2.getId());
            case "Employé"    -> Long.compare(c1.getEmployeeId(), c2.getEmployeeId());
            case "Type"       -> c1.getTypeConge().compareToIgnoreCase(c2.getTypeConge());
            case "Date Début" -> c1.getDateDebut().compareTo(c2.getDateDebut());
            case "Date Fin"   -> c1.getDateFin().compareTo(c2.getDateFin());
            default           -> 0;
        });
        table.refresh();
    }

    // ══════════════════════════════════════════
    //  DÉCISIONS
    // ══════════════════════════════════════════
    @FXML private void onAccept() { updateStatus("ACCEPTEE"); }
    @FXML private void onRefuse() { updateStatus("REFUSEE");  }

    private void updateStatus(String statut) {
        Conge selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { animerErreur("Sélectionne une demande."); return; }
        String commentaire = taCommentaire.getText();
        if (commentaire == null || commentaire.trim().isEmpty()) {
            animerErreur("Ajoute un commentaire avant de valider."); return;
        }

        // Vérification légale avant acceptation
        if ("ACCEPTEE".equals(statut) && selected.getDateDebut() != null && selected.getDateFin() != null) {
            ResultatValidation rv = CongeRegleService.valider(
                    selected.getTypeConge(), selected.getDateDebut(), selected.getDateFin(),
                    selected.getDocumentPath() != null);
            if (!rv.erreurs.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("⚠ Limite légale non respectée");
                alert.setHeaderText("Cette demande ne respecte pas les limites légales !");
                alert.setContentText(String.join("\n", rv.erreurs) +
                        "\n\nVoulez-vous quand même accepter cette demande ?");
                ButtonType btnOui = new ButtonType("Accepter quand même", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(btnNon, btnOui);
                java.util.Optional<ButtonType> result = alert.showAndWait();
                if (result.isEmpty() || result.get() == btnNon) return;
            }
        }

        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            PreparedStatement ps1 = cnx.prepareStatement("UPDATE conge_tt SET statut=? WHERE id=?");
            ps1.setString(1, statut); ps1.setLong(2, selected.getId()); ps1.executeUpdate();

            PreparedStatement check = cnx.prepareStatement("SELECT id FROM reponse WHERE conge_tt_id=?");
            check.setLong(1, selected.getId());
            boolean exists = check.executeQuery().next();

            if (exists) {
                PreparedStatement ps2 = cnx.prepareStatement(
                        "UPDATE reponse SET decision=?, commentaire=? WHERE conge_tt_id=?");
                ps2.setString(1, statut); ps2.setString(2, commentaire);
                ps2.setLong(3, selected.getId()); ps2.executeUpdate();
            } else {
                PreparedStatement ps3 = cnx.prepareStatement(
                        "INSERT INTO reponse(decision,commentaire,rh_id,employe_id,conge_tt_id) VALUES(?,?,?,?,?)");
                ps3.setString(1, statut); ps3.setString(2, commentaire);
                ps3.setLong(3, 1L); ps3.setLong(4, selected.getEmployeeId());
                ps3.setLong(5, selected.getId()); ps3.executeUpdate();
            }

            sendSmsToEmployee(selected, statut);
            animerSucces("ACCEPTEE".equals(statut) ? "✅ Congé accepté" : "❌ Congé refusé");
            taCommentaire.clear();
            loadData();

        } catch (SQLException e) { animerErreur("Erreur DB ❌"); e.printStackTrace(); }
    }

    @FXML
    private void onComment() {
        Conge selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { animerErreur("Sélectionne une demande."); return; }
        String commentaire = taCommentaire.getText();
        if (commentaire == null || commentaire.trim().isEmpty()) {
            animerErreur("Écris un commentaire avant de valider."); return;
        }
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            PreparedStatement check = cnx.prepareStatement("SELECT id FROM reponse WHERE conge_tt_id=?");
            check.setLong(1, selected.getId());
            boolean exists = check.executeQuery().next();
            if (exists) {
                PreparedStatement ps = cnx.prepareStatement(
                        "UPDATE reponse SET commentaire=? WHERE conge_tt_id=?");
                ps.setString(1, commentaire); ps.setLong(2, selected.getId()); ps.executeUpdate();
            } else {
                PreparedStatement ps = cnx.prepareStatement(
                        "INSERT INTO reponse(conge_tt_id,decision,commentaire,rh_id,employe_id) VALUES(?,?,?,?,?)");
                ps.setLong(1, selected.getId()); ps.setString(2, "-");
                ps.setString(3, commentaire); ps.setLong(4, 1L);
                ps.setLong(5, selected.getEmployeeId()); ps.executeUpdate();
            }
            animerSucces("💬 Commentaire enregistré ✅");
            taCommentaire.clear();
            loadData();
        } catch (SQLException e) { animerErreur("Erreur DB ❌"); e.printStackTrace(); }
    }

    // ══════════════════════════════════════════
    //  ANIMATIONS
    // ══════════════════════════════════════════
    private void animerSucces(String texte) {
        msgLabel.setText(texte);
        msgLabel.setStyle("-fx-font-size:13px; -fx-font-weight:800; -fx-text-fill:white;" +
                "-fx-background-color:#059669; -fx-background-radius:20; -fx-padding:6 16;");
        FadeTransition ft = new FadeTransition(Duration.millis(300), msgLabel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void animerErreur(String texte) {
        msgLabel.setText(texte);
        msgLabel.setStyle("-fx-font-size:13px; -fx-font-weight:800; -fx-text-fill:white;" +
                "-fx-background-color:#dc2626; -fx-background-radius:20; -fx-padding:6 16;");
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), msgLabel);
        shake.setFromX(0); shake.setToX(7);
        shake.setCycleCount(4); shake.setAutoReverse(true);
        shake.play();
    }

    // ══════════════════════════════════════════
    //  SMS
    // ══════════════════════════════════════════
    private void sendSmsToEmployee(Conge conge, String statut) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate debut = conge.getDateDebut();
            boolean isUrgentDate = debut != null &&
                    (debut.isEqual(today) || debut.isEqual(today.plusDays(1)) || debut.isEqual(today.plusDays(2)));
            boolean isUrgentType = conge.getTypeConge() != null &&
                    (conge.getTypeConge().toLowerCase().contains("maladie") ||
                            conge.getTypeConge().toLowerCase().contains("urgent"));
            if (!isUrgentDate && !isUrgentType) { System.out.println("SMS non envoyé."); return; }
            Connection cnx = MyConnection.getInstance().getCnx();
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT u.telephone FROM employe e JOIN users u ON e.user_id = u.id WHERE e.user_id = ?");
            ps.setLong(1, conge.getEmployeeId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String phone = rs.getString("telephone");
                if (phone != null && !phone.isEmpty()) {
                    if (!phone.startsWith("+")) phone = "+216" + phone;
                    SmsService.sendSms(phone, "ALERTE RH 🚨\nVotre congé (" + conge.getTypeConge() +
                            ") du " + conge.getDateDebut() + " a été " + statut + ".");
                }
            }
        } catch (Exception e) { System.out.println("Erreur SMS : " + e.getMessage()); }
    }
}