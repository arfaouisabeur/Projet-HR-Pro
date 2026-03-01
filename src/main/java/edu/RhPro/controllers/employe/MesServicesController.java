package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Reponse;
import edu.RhPro.entities.Service;
import edu.RhPro.services.ReponseService;
import edu.RhPro.services.ServiceService;
import edu.RhPro.utils.Session;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MesServicesController {

    @FXML private ComboBox<String> cbTypeService;
    @FXML private ComboBox<String> cbPriorite;
    @FXML private VBox             vboxPrioriteInfo;
    @FXML private Label            lblPrioriteInfo;
    @FXML private Label            lblPrioriteError;
    @FXML private TextArea         taDescription;
    @FXML private Label            lblMsg;
    @FXML private Label            lblTitreError;
    @FXML private Label            lblDescriptionError;
    @FXML private VBox             cardsContainer;
    @FXML private VBox             formCard;

    private final ServiceService serviceService = new ServiceService();
    private final ReponseService reponseService = new ReponseService();

    private Service serviceEnModification = null;
    private List<Service> allServices;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String FIELD_NORMAL = "-fx-background-radius:14; -fx-border-radius:14;" +
            "-fx-border-color:#e0d9f7; -fx-border-width:1.5; -fx-font-size:13px;";
    private final String FIELD_ERROR  = "-fx-background-radius:14; -fx-border-radius:14;" +
            "-fx-border-color:#dc2626; -fx-border-width:2; -fx-font-size:13px;";

    // ══════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════
    @FXML
    public void initialize() {

        // ── Types de services (comme types de congés) ──
        cbTypeService.setItems(FXCollections.observableArrayList(
                "💻 Matériel informatique",
                "🚗 Véhicule de service",
                "📦 Fournitures de bureau",
                "🏠 Télétravail",
                "📚 Formation professionnelle",
                "🔧 Maintenance / Réparation",
                "🌐 Accès logiciel / Licence",
                "✈ Mission / Déplacement",
                "💰 Remboursement de frais",
                "📋 Autre demande"
        ));

        // ── Priorités ──────────────────────────────
        cbPriorite.setItems(FXCollections.observableArrayList(
                "🟢 FAIBLE  — Délai : 7 jours",
                "🟡 NORMAL  — Délai : 3 jours",
                "🔴 URGENT  — Délai : 24 heures"
        ));
        cbPriorite.getSelectionModel().select("🟡 NORMAL  — Délai : 3 jours");

        // Listener priorité → affiche info dynamique
        cbPriorite.valueProperty().addListener((obs, o, n) -> {
            if (n == null) { vboxPrioriteInfo.setVisible(false); vboxPrioriteInfo.setManaged(false); return; }
            if (n.contains("URGENT")) {
                lblPrioriteInfo.setText("🔴 URGENT : Le RH doit répondre dans les 24 heures.");
                lblPrioriteInfo.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#dc2626;");
                vboxPrioriteInfo.setStyle("-fx-background-color:#fee2e2; -fx-background-radius:14; -fx-padding:10 14;" +
                        "-fx-border-color:#fca5a5; -fx-border-radius:14; -fx-border-width:1;");
            } else if (n.contains("FAIBLE")) {
                lblPrioriteInfo.setText("🟢 FAIBLE : Délai de réponse de 7 jours.");
                lblPrioriteInfo.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#059669;");
                vboxPrioriteInfo.setStyle("-fx-background-color:#d1fae5; -fx-background-radius:14; -fx-padding:10 14;" +
                        "-fx-border-color:#6ee7b7; -fx-border-radius:14; -fx-border-width:1;");
            } else {
                lblPrioriteInfo.setText("🟡 NORMAL : Délai de réponse de 3 jours.");
                lblPrioriteInfo.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#d97706;");
                vboxPrioriteInfo.setStyle("-fx-background-color:#fef3c7; -fx-background-radius:14; -fx-padding:10 14;" +
                        "-fx-border-color:#fde68a; -fx-border-radius:14; -fx-border-width:1;");
            }
            vboxPrioriteInfo.setVisible(true);
            vboxPrioriteInfo.setManaged(true);
            FadeTransition ft = new FadeTransition(Duration.millis(250), vboxPrioriteInfo);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        });

        // Validation live
        cbTypeService.valueProperty().addListener((obs, o, n) -> {
            if (n != null) hideError(cbTypeService, lblTitreError);
        });
        taDescription.textProperty().addListener((obs, o, n) -> {
            if (n != null && n.trim().length() >= 5 && n.length() <= 500)
                hideError(taDescription, lblDescriptionError);
        });

        refresh();
    }

    // ══════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════
    private boolean validateForm() {
        boolean ok = true;

        if (cbTypeService.getValue() == null) {
            showError(cbTypeService, lblTitreError, "Choisissez un type de service");
            ok = false;
        } else {
            hideError(cbTypeService, lblTitreError);
        }

        String desc = taDescription.getText();
        if (desc == null || desc.trim().length() < 5) {
            showError(taDescription, lblDescriptionError, "Description minimum 5 caractères");
            ok = false;
        } else if (desc.length() > 500) {
            showError(taDescription, lblDescriptionError, "Maximum 500 caractères");
            ok = false;
        } else {
            hideError(taDescription, lblDescriptionError);
        }

        return ok;
    }

    private void showError(Control f, Label lbl, String msg) {
        f.setStyle(FIELD_ERROR);
        lbl.setText("⚠ " + msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
        shake(f);
    }

    private void hideError(Control f, Label lbl) {
        f.setStyle(FIELD_NORMAL + "-fx-padding:4 8;");
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    // ══════════════════════════════════════════════
    //  SUBMIT
    // ══════════════════════════════════════════════
    @FXML
    private void onSubmit() {
        if (!validateForm()) {
            afficherMsgHeader("⚠ Corrigez les erreurs", true);
            return;
        }
        try {
            long empId = Session.getCurrentUser().getId();
            // Nettoyer le titre
            String titre = cbTypeService.getValue().replaceAll("^[\\p{So}\\p{Sm}\\p{Sk}\\p{Sc}\\s]+", "").trim();

            // Extraire la priorité (ex: "🔴 URGENT  — Délai : 24 heures" → "URGENT")
            String prioriteRaw = cbPriorite.getValue() != null ? cbPriorite.getValue() : "🟡 NORMAL";
            String priorite = prioriteRaw.contains("URGENT") ? "URGENT"
                    : prioriteRaw.contains("FAIBLE") ? "FAIBLE" : "NORMAL";

            if (serviceEnModification == null) {
                Service s = new Service(
                        titre,
                        taDescription.getText().trim(),
                        LocalDate.now(),
                        "EN_ATTENTE",
                        empId
                );
                s.setPriorite(priorite);
                serviceService.addEntity(s);
                afficherMsgHeader("✅ Demande envoyée avec succès !", false);
            } else {
                serviceEnModification.setTitre(titre);
                serviceEnModification.setDescription(taDescription.getText().trim());
                serviceEnModification.setPriorite(priorite);
                serviceService.updateEntity(serviceEnModification);
                afficherMsgHeader("✅ Demande modifiée !", false);
                serviceEnModification = null;
            }
            clearForm();
            refresh();
        } catch (SQLException e) {
            afficherMsgHeader("❌ Erreur base de données", true);
        }
    }

    // ══════════════════════════════════════════════
    //  REFRESH / LOAD
    // ══════════════════════════════════════════════
    @FXML
    public void refresh() {
        loadData();
    }

    private void loadData() {
        try {
            cardsContainer.getChildren().clear();
            long empId = Session.getCurrentUser().getId();
            allServices = serviceService.findByEmployeId(empId);

            int delay = 0;
            for (Service s : allServices) {
                Reponse rep = reponseService.getOneByServiceId(s.getId());
                VBox card = createCard(s, rep);
                cardsContainer.getChildren().add(card);
                animateCard(card, delay);
                delay += 70;
            }

            if (allServices.isEmpty()) {
                Label empty = new Label("✨  Aucune demande pour le moment");
                empty.setStyle("-fx-text-fill:#c4b5f4; -fx-font-size:14px; -fx-font-weight:700; -fx-padding:20;");
                cardsContainer.getChildren().add(empty);
            }

        } catch (SQLException e) {
            afficherMsgHeader("❌ Erreur chargement", true);
        }
    }

    // ══════════════════════════════════════════════
    //  CREATE CARD
    // ══════════════════════════════════════════════
    private VBox createCard(Service s, Reponse rep) {

        String statut = s.getStatut() != null ? s.getStatut().trim().toUpperCase() : "EN_ATTENTE";

        String topColor = switch (statut) {
            case "ACCEPTEE" -> "#059669";
            case "REFUSEE"  -> "#dc2626";
            default         -> "#6d2269";
        };

        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:20;" +
                        "-fx-border-color:" + topColor + " transparent transparent transparent;" +
                        "-fx-border-width:4 0 0 0;" +
                        "-fx-border-radius:20 20 0 0;" +
                        "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.09),16,0,0,4);");

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#fdfbff;" +
                        "-fx-background-radius:20;" +
                        "-fx-border-color:" + topColor + " transparent transparent transparent;" +
                        "-fx-border-width:4 0 0 0;" +
                        "-fx-border-radius:20 20 0 0;" +
                        "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.18),22,0,0,7);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:20;" +
                        "-fx-border-color:" + topColor + " transparent transparent transparent;" +
                        "-fx-border-width:4 0 0 0;" +
                        "-fx-border-radius:20 20 0 0;" +
                        "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.09),16,0,0,4);"));

        // ── TOP ROW ───────────────────────────────
        HBox topRow = new HBox(12);
        topRow.setPadding(new Insets(16, 18, 8, 18));
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Icône selon le type de service
        String icon = getIconForType(s.getTitre());
        Label titleLbl = new Label(icon + "  " + s.getTitre());
        titleLbl.setStyle("-fx-font-size:15px; -fx-font-weight:900; -fx-text-fill:#3d1a3b;");

        // Badge statut
        String sBg, sFg, sTxt;
        switch (statut) {
            case "ACCEPTEE" -> { sBg = "#d1fae5"; sFg = "#059669"; sTxt = "✅ ACCEPTÉE"; }
            case "REFUSEE"  -> { sBg = "#fee2e2"; sFg = "#dc2626"; sTxt = "❌ REFUSÉE";  }
            default         -> { sBg = "#ede9f6"; sFg = "#6d2269"; sTxt = "⏳ EN ATTENTE"; }
        }
        Label statutBadge = new Label(sTxt);
        statutBadge.setStyle("-fx-background-color:" + sBg + "; -fx-text-fill:" + sFg + ";" +
                "-fx-font-weight:800; -fx-font-size:11px; -fx-background-radius:20; -fx-padding:4 12;");

        // Badge priorité
        String prio = s.getPriorite() != null ? s.getPriorite() : "NORMAL";
        String pBg = switch (prio) { case "URGENT" -> "#fee2e2"; case "FAIBLE" -> "#d1fae5"; default -> "#f3f0fa"; };
        String pFg = switch (prio) { case "URGENT" -> "#dc2626"; case "FAIBLE" -> "#059669"; default -> "#6d2269"; };
        String pIc = switch (prio) { case "URGENT" -> "🔴"; case "FAIBLE" -> "🟢"; default -> "🟡"; };
        Label prioBadge = new Label(pIc + " " + prio);
        prioBadge.setStyle("-fx-background-color:" + pBg + "; -fx-text-fill:" + pFg + ";" +
                "-fx-font-weight:700; -fx-font-size:10px; -fx-background-radius:20; -fx-padding:3 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(titleLbl, spacer, prioBadge, statutBadge);

        // ── WORKFLOW BAR ──────────────────────────
        HBox workflowBar = new HBox(4);
        workflowBar.setPadding(new Insets(0, 18, 10, 18));
        workflowBar.setAlignment(Pos.CENTER_LEFT);

        String etape = s.getEtapeWorkflow() != null ? s.getEtapeWorkflow() : "SOUMISE";
        String[] etapes = {"SOUMISE", "EN_ANALYSE", "EN_TRAITEMENT", "RESOLUE"};
        boolean estRejetee = "REJETEE".equals(etape);
        java.util.List<String> etapesList = java.util.Arrays.asList(etapes);
        int idxCourant = etapesList.indexOf(etape);

        for (int i = 0; i < etapes.length; i++) {
            boolean done   = !estRejetee && i <= idxCourant;
            boolean active = etapes[i].equals(etape);

            Label step = new Label(etapes[i].replace("EN_", "").replace("_", " "));
            step.setStyle(
                    "-fx-background-color:" + (done ? "#6d2269" : "#ede9f6") + ";" +
                            "-fx-text-fill:" + (done ? "white" : "#b39cb0") + ";" +
                            "-fx-font-size:9px; -fx-font-weight:" + (active ? "900" : "600") + ";" +
                            "-fx-background-radius:14; -fx-padding:3 8;");

            if (active && !"RESOLUE".equals(etape) && !estRejetee) {
                FadeTransition pulse = new FadeTransition(Duration.millis(900), step);
                pulse.setFromValue(0.55); pulse.setToValue(1.0);
                pulse.setCycleCount(Animation.INDEFINITE); pulse.setAutoReverse(true);
                pulse.play();
            }
            workflowBar.getChildren().add(step);
            if (i < etapes.length - 1) {
                Label arrow = new Label("›");
                arrow.setStyle("-fx-text-fill:" + (done && i < idxCourant ? "#6d2269" : "#c4b5f4") + "; -fx-font-size:13px;");
                workflowBar.getChildren().add(arrow);
            }
        }
        if (estRejetee) {
            Label rejLabel = new Label("  ✕ REJETÉE");
            rejLabel.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                    "-fx-font-size:10px; -fx-font-weight:900; -fx-background-radius:14; -fx-padding:3 8;");
            workflowBar.getChildren().add(rejLabel);
        }

        // ── DATE + SLA ────────────────────────────
        HBox dateRow = new HBox(8);
        dateRow.setPadding(new Insets(0, 18, 8, 18));
        dateRow.setAlignment(Pos.CENTER_LEFT);

        Label dateVal = new Label("📅 " + (s.getDateDemande() != null ? s.getDateDemande().format(formatter) : "-"));
        dateVal.setStyle("-fx-font-size:12px; -fx-text-fill:#7c3a7a; -fx-font-weight:600;");
        dateRow.getChildren().add(dateVal);

        if (s.getDeadlineReponse() != null && !"RESOLUE".equals(etape) && !"REJETEE".equals(etape)) {
            long j = LocalDate.now().until(s.getDeadlineReponse(), java.time.temporal.ChronoUnit.DAYS);
            String slaText, slaColor;
            if (s.isSlaDepasse() || j < 0) {
                slaText = "  🔴 SLA dépassé"; slaColor = "#dc2626";
            } else if (j == 0) {
                slaText = "  🟡 Réponse aujourd'hui"; slaColor = "#d97706";
            } else {
                slaText = "  🟢 " + j + "j restants"; slaColor = "#059669";
            }
            Label slaLbl = new Label(slaText);
            slaLbl.setStyle("-fx-font-size:11px; -fx-text-fill:" + slaColor + "; -fx-font-weight:700;");
            dateRow.getChildren().add(slaLbl);
        }

        // ── SÉPARATEUR ────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#f0ebf9;");

        // ── DESCRIPTION ───────────────────────────
        VBox descBox = new VBox(4);
        descBox.setPadding(new Insets(10, 18, 8, 18));
        Label descTitle = new Label("DESCRIPTION");
        descTitle.setStyle("-fx-font-size:10px; -fx-text-fill:#b39cb0; -fx-font-weight:900;");
        Label descVal = new Label(s.getDescription());
        descVal.setWrapText(true);
        descVal.setStyle("-fx-font-size:13px; -fx-text-fill:#444; -fx-line-spacing:2;");
        descBox.getChildren().addAll(descTitle, descVal);

        // ── RÉPONSE RH ────────────────────────────
        HBox reponseRow = new HBox(8);
        reponseRow.setPadding(new Insets(0, 18, 10, 18));
        reponseRow.setAlignment(Pos.CENTER_LEFT);

        if (rep != null && rep.getDecision() != null && !rep.getDecision().isBlank()) {
            Label repLbl = new Label("💬 Réponse RH disponible — cliquer pour voir");
            repLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#6d2269; -fx-underline:true;" +
                    "-fx-cursor:hand; -fx-font-weight:700;");
            repLbl.setOnMouseClicked(e -> afficherPopupReponse(rep, s));
            reponseRow.getChildren().add(repLbl);
        }

        // ── ACTIONS ───────────────────────────────
        HBox actionsRow = new HBox(10);
        actionsRow.setPadding(new Insets(0, 14, 14, 14));
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if ("EN_ATTENTE".equals(s.getStatut())) {
            Button btnEdit = new Button("✏  Modifier");
            btnEdit.setStyle(
                    "-fx-background-color:#6d2269; -fx-text-fill:white; -fx-font-weight:700;" +
                            "-fx-background-radius:20; -fx-padding:7 16; -fx-cursor:hand; -fx-font-size:12px;");
            btnEdit.setOnAction(e -> {
                serviceEnModification = s;
                // Sélectionner le bon type dans le ComboBox
                String titre = s.getTitre();
                String found = cbTypeService.getItems().stream()
                        .filter(item -> item.contains(titre) || titre.contains(
                                item.replaceAll("^[\\p{So}\\p{Sm}\\p{Sk}\\p{Sc}\\s]+", "").trim()))
                        .findFirst()
                        .orElse(null);
                if (found != null) cbTypeService.setValue(found);
                else {
                    // Ajouter le titre tel quel si pas trouvé
                    if (!cbTypeService.getItems().contains(titre))
                        cbTypeService.getItems().add(titre);
                    cbTypeService.setValue(titre);
                }
                // Restaurer la priorité
                String prioEdit = s.getPriorite() != null ? s.getPriorite() : "NORMAL";
                cbPriorite.getItems().stream()
                        .filter(item -> item.contains(prioEdit))
                        .findFirst()
                        .ifPresent(cbPriorite::setValue);
                taDescription.setText(s.getDescription());
                afficherMsgHeader("✏  Mode modification activé", false);
                formCard.requestFocus();
            });

            Button btnDel = new Button("🗑  Supprimer");
            btnDel.setStyle(
                    "-fx-background-color:#fff0f0; -fx-text-fill:#dc2626; -fx-font-weight:700;" +
                            "-fx-background-radius:20; -fx-padding:7 16; -fx-cursor:hand; -fx-font-size:12px;" +
                            "-fx-border-color:#fca5a5; -fx-border-width:1; -fx-border-radius:20;");
            btnDel.setOnAction(e -> afficherPopupSuppression(s));

            actionsRow.getChildren().addAll(btnEdit, btnDel);
        }

        card.getChildren().addAll(topRow, workflowBar, dateRow, sep, descBox, reponseRow, actionsRow);
        return card;
    }

    // ══════════════════════════════════════════════
    //  ICÔNE SELON TYPE
    // ══════════════════════════════════════════════
    private String getIconForType(String titre) {
        if (titre == null) return "📋";
        String t = titre.toLowerCase();
        if (t.contains("matériel") || t.contains("informatique") || t.contains("ordinateur")) return "💻";
        if (t.contains("véhicule") || t.contains("voiture") || t.contains("transport"))      return "🚗";
        if (t.contains("fourniture") || t.contains("bureau"))                                 return "📦";
        if (t.contains("télétravail") || t.contains("remote"))                                return "🏠";
        if (t.contains("formation") || t.contains("cours"))                                   return "📚";
        if (t.contains("maintenance") || t.contains("réparation"))                            return "🔧";
        if (t.contains("logiciel") || t.contains("licence") || t.contains("accès"))           return "🌐";
        if (t.contains("mission") || t.contains("déplacement"))                               return "✈";
        if (t.contains("remboursement") || t.contains("frais"))                               return "💰";
        return "📋";
    }

    // ══════════════════════════════════════════════
    //  POPUP SUPPRESSION
    // ══════════════════════════════════════════════
    private void afficherPopupSuppression(Service s) {
        Stage popup = new Stage(StageStyle.TRANSPARENT);
        popup.initOwner(cardsContainer.getScene().getWindow());

        VBox overlay = new VBox();
        overlay.setStyle("-fx-background-color:rgba(30,10,30,0.60);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setPrefSize(580, 340);

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 40, 30, 40));
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color:white; -fx-background-radius:26;" +
                "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.35),35,0,0,12);");

        Label icone = new Label("🗑");
        icone.setStyle("-fx-font-size:46px;");
        Label titre = new Label("Supprimer cette demande ?");
        titre.setStyle("-fx-font-size:18px; -fx-font-weight:900; -fx-text-fill:#3d1a3b;");
        titre.setWrapText(true); titre.setAlignment(Pos.CENTER);

        VBox infoBox = new VBox(6);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setStyle("-fx-background-color:#f8f5ff; -fx-background-radius:14; -fx-padding:12;");
        Label infoTitre = new Label("\"" + s.getTitre() + "\"");
        infoTitre.setStyle("-fx-font-size:13px; -fx-text-fill:#6d2269; -fx-font-weight:700;");
        infoTitre.setWrapText(true); infoTitre.setAlignment(Pos.CENTER);
        infoBox.getChildren().add(infoTitre);

        Label warn = new Label("⚠  Cette action est irréversible.");
        warn.setStyle("-fx-font-size:12px; -fx-text-fill:#dc2626; -fx-font-weight:600;");

        HBox btnRow = new HBox(16);
        btnRow.setAlignment(Pos.CENTER);

        Button btnAnn = new Button("Annuler");
        btnAnn.setStyle("-fx-background-color:#f3f0fa; -fx-text-fill:#6d2269; -fx-font-weight:800;" +
                "-fx-background-radius:22; -fx-padding:12 24; -fx-cursor:hand; -fx-font-size:13px;" +
                "-fx-border-color:#ddd6fe; -fx-border-width:1.5; -fx-border-radius:22;");

        Button btnConf = new Button("🗑  Oui, supprimer");
        btnConf.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white; -fx-font-weight:800;" +
                "-fx-background-radius:22; -fx-padding:12 24; -fx-cursor:hand; -fx-font-size:13px;");

        btnAnn.setOnAction(e -> fermerPopup(card, popup));
        btnConf.setOnAction(e -> {
            try {
                serviceService.deleteEntity(s);
                popup.close(); refresh();
                afficherMsgHeader("✅ Demande supprimée", false);
            } catch (SQLException ex) {
                afficherMsgHeader("❌ Erreur suppression", true);
                popup.close();
            }
        });

        btnRow.getChildren().addAll(btnAnn, btnConf);
        card.getChildren().addAll(icone, titre, infoBox, warn, btnRow);
        overlay.getChildren().add(card);
        ouvrirPopup(popup, overlay, card, 580, 340);
    }

    // ══════════════════════════════════════════════
    //  POPUP RÉPONSE RH
    // ══════════════════════════════════════════════
    private void afficherPopupReponse(Reponse rep, Service s) {
        Stage popup = new Stage(StageStyle.TRANSPARENT);
        popup.initOwner(cardsContainer.getScene().getWindow());

        VBox overlay = new VBox();
        overlay.setStyle("-fx-background-color:rgba(30,10,30,0.60);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setPrefSize(600, 400);

        VBox card = new VBox(18);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(28, 34, 26, 34));
        card.setMaxWidth(480);
        card.setStyle("-fx-background-color:white; -fx-background-radius:26;" +
                "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.3),35,0,0,12);");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("💬");
        icon.setStyle("-fx-font-size:28px;");
        VBox titreBox = new VBox(3);
        Label titre = new Label("Réponse du RH");
        titre.setStyle("-fx-font-size:17px; -fx-font-weight:900; -fx-text-fill:#3d1a3b;");
        Label sous = new Label(s.getTitre());
        sous.setStyle("-fx-font-size:11px; -fx-text-fill:#9c5c9a; -fx-font-weight:600;");
        sous.setWrapText(true);
        titreBox.getChildren().addAll(titre, sous);

        String decision = rep.getDecision() != null ? rep.getDecision().toUpperCase() : "";
        String dBg = decision.contains("ACCEPT") ? "#d1fae5" : decision.contains("REFUS") ? "#fee2e2" : "#ede9f6";
        String dFg = decision.contains("ACCEPT") ? "#059669" : decision.contains("REFUS") ? "#dc2626" : "#6d2269";
        Label dBadge = new Label(rep.getDecision());
        dBadge.setStyle("-fx-background-color:" + dBg + "; -fx-text-fill:" + dFg + ";" +
                "-fx-font-weight:800; -fx-font-size:11px; -fx-background-radius:20; -fx-padding:4 12;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(icon, titreBox, spacer, dBadge);

        Separator sep = new Separator();

        VBox repBox = new VBox(8);
        repBox.setStyle("-fx-background-color:#f8f5ff; -fx-background-radius:16;" +
                "-fx-border-color:#ddd6fe; -fx-border-radius:16; -fx-border-width:1.5; -fx-padding:16;");
        String repTxt = rep.getCommentaire() != null && !rep.getCommentaire().isBlank()
                ? rep.getCommentaire() : "(Aucun commentaire ajouté)";
        Label repLbl = new Label(repTxt);
        repLbl.setWrapText(true);
        repLbl.setStyle("-fx-font-size:14px; -fx-text-fill:#3d1a3b; -fx-line-spacing:4;");
        repBox.getChildren().add(repLbl);

        Button btnClose = new Button("✓  Fermer");
        btnClose.setStyle("-fx-background-color:#6d2269; -fx-text-fill:white; -fx-font-weight:800;" +
                "-fx-background-radius:22; -fx-padding:11 30; -fx-cursor:hand; -fx-font-size:13px;");
        btnClose.setOnAction(e -> fermerPopup(card, popup));

        HBox btnRow = new HBox();
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getChildren().add(btnClose);

        card.getChildren().addAll(header, sep, repBox, btnRow);
        overlay.getChildren().add(card);
        ouvrirPopup(popup, overlay, card, 600, 400);
    }

    // ══════════════════════════════════════════════
    //  HELPERS POPUP
    // ══════════════════════════════════════════════
    private void ouvrirPopup(Stage popup, VBox overlay, VBox card, double w, double h) {
        Scene scene = new Scene(overlay);
        scene.setFill(null);
        popup.setScene(scene);
        card.setScaleX(0.7); card.setScaleY(0.7); card.setOpacity(0);
        popup.show();
        ScaleTransition sc = new ScaleTransition(Duration.millis(280), card);
        sc.setFromX(0.7); sc.setToX(1.0); sc.setFromY(0.7); sc.setToY(1.0);
        FadeTransition ft = new FadeTransition(Duration.millis(280), card);
        ft.setFromValue(0); ft.setToValue(1);
        new ParallelTransition(sc, ft).play();
        javafx.stage.Window owner = cardsContainer.getScene().getWindow();
        popup.setX(owner.getX() + (owner.getWidth()  - w) / 2);
        popup.setY(owner.getY() + (owner.getHeight() - h) / 2);
    }

    private void fermerPopup(VBox card, Stage popup) {
        ScaleTransition sc = new ScaleTransition(Duration.millis(200), card);
        sc.setToX(0.85); sc.setToY(0.85);
        FadeTransition ft = new FadeTransition(Duration.millis(200), card);
        ft.setFromValue(1); ft.setToValue(0);
        ParallelTransition pt = new ParallelTransition(sc, ft);
        pt.setOnFinished(e -> popup.close());
        pt.play();
    }

    // ══════════════════════════════════════════════
    //  ANIMATIONS
    // ══════════════════════════════════════════════
    private void animateCard(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(18);
        FadeTransition ft = new FadeTransition(Duration.millis(400), node);
        ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(delayMs));
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), node);
        tt.setFromY(18); tt.setToY(0); tt.setDelay(Duration.millis(delayMs));
        ft.play(); tt.play();
    }

    private void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setByX(6); tt.setCycleCount(4); tt.setAutoReverse(true);
        tt.play();
    }

    // ══════════════════════════════════════════════
    //  MESSAGE HEADER
    // ══════════════════════════════════════════════
    private void afficherMsgHeader(String texte, boolean error) {
        lblMsg.setText(texte);
        lblMsg.setStyle("-fx-font-size:13px; -fx-font-weight:800; -fx-text-fill:white;" +
                (error ? "-fx-background-color:#dc2626;" : "-fx-background-color:rgba(255,255,255,0.22);") +
                "-fx-background-radius:20; -fx-padding:6 16;");
        FadeTransition ft = new FadeTransition(Duration.millis(350), lblMsg);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ══════════════════════════════════════════════
    //  CLEAR FORM
    // ══════════════════════════════════════════════
    private void clearForm() {
        cbTypeService.setValue(null);
        taDescription.clear();
        serviceEnModification = null;
    }
}