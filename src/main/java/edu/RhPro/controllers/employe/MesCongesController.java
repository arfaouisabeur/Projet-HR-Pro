package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Conge;
import edu.RhPro.entities.Reponse;
import edu.RhPro.services.CongeService;
import edu.RhPro.services.CongeRegleService;
import edu.RhPro.services.CongeRegleService.*;
import edu.RhPro.services.OcrService;
import edu.RhPro.services.ReponseService;
import edu.RhPro.services.AIService;
import edu.RhPro.utils.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.util.Duration;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class MesCongesController {

    @FXML private ComboBox<String> cbType;
    @FXML private DatePicker dpDebut;
    @FXML private DatePicker dpFin;
    @FXML private TextArea taDescription;
    @FXML private Label lblMsg;

    @FXML private Label typeErrorLabel;
    @FXML private Label dateDebutErrorLabel;
    @FXML private Label dateFinErrorLabel;
    @FXML private Label descErrorLabel;

    @FXML private VBox cardContainer;
    @FXML private VBox formCard;

    @FXML private Button btnGenererIA;
    @FXML private ProgressIndicator aiSpinner;

    @FXML private VBox vboxOcr;
    @FXML private VBox vboxOcrResult;
    @FXML private Button btnUploadDoc;
    @FXML private Button btnAppliquerOcr;
    @FXML private Label lblFichierChoisi;
    @FXML private Label lblMedecin;
    @FXML private Label lblPeriodeOcr;
    @FXML private Label lblOcrStatut;
    @FXML private ProgressIndicator ocrSpinner;

    private final CongeService   congeService   = new CongeService();
    private final ReponseService reponseService = new ReponseService();

    private File fichierCertificat;
    private OcrService.OcrResult dernierResultatOcr;
    private Conge congeEnModification = null;
    private List<Conge> allConges;

    // Panneau de validation dynamique (créé programmatiquement)
    private VBox vboxValidation;

    private final String normalStyle =
            "-fx-border-color:#e0d9f7; -fx-border-width:1.5; -fx-border-radius:14; -fx-background-radius:14;";
    private final String errorStyle =
            "-fx-border-color:#dc2626; -fx-border-width:2; -fx-border-radius:14; -fx-background-radius:14;";

    // ══════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════
    @FXML
    public void initialize() {
        cbType.getItems().addAll(
                "Congé annuel", "Congé maladie", "Congé maternité",
                "Congé professionnel", "Congé sabbatique", "Autre"
        );

        addValidationListeners();

        cbType.valueProperty().addListener((obs, o, n) -> {
            boolean isMaladie = "Congé maladie".equals(n);
            vboxOcr.setVisible(isMaladie);
            vboxOcr.setManaged(isMaladie);
            if (isMaladie) {
                ScaleTransition st = new ScaleTransition(Duration.millis(250), vboxOcr);
                st.setFromY(0.5); st.setToY(1.0); st.play();
                FadeTransition ft = new FadeTransition(Duration.millis(250), vboxOcr);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            } else {
                fichierCertificat = null;
                lblFichierChoisi.setText("Aucun fichier sélectionné");
                vboxOcrResult.setVisible(false);
                vboxOcrResult.setManaged(false);
            }
            // Afficher la règle du type choisi
            mettreAJourPanneauRegle(n);
            mettreAJourValidation();
        });

        // Recalcul dès que les dates changent
        dpDebut.valueProperty().addListener((obs, o, n) -> mettreAJourValidation());
        dpFin.valueProperty().addListener((obs, o, n)  -> mettreAJourValidation());

        animerEntree(formCard, 0);
        refresh();
    }

    // ══════════════════════════════════════
    //  PANNEAU RÈGLE + VALIDATION EN TEMPS RÉEL
    // ══════════════════════════════════════

    /**
     * Crée (si nécessaire) et insère le panneau de validation
     * juste sous la zone OCR dans le formCard.
     */
    private void assurerPanneauValidation() {
        if (vboxValidation != null) return;

        vboxValidation = new VBox(10);
        vboxValidation.setVisible(false);
        vboxValidation.setManaged(false);
        vboxValidation.setStyle(
                "-fx-background-color:#f8f5ff;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:#c4b5f4;" +
                        "-fx-border-radius:16;" +
                        "-fx-border-width:1.5;" +
                        "-fx-padding:16;");

        // Insérer avant le bouton submit (dernier enfant de formCard)
        int insertIdx = formCard.getChildren().size() - 1;
        if (insertIdx < 0) insertIdx = 0;
        formCard.getChildren().add(insertIdx, vboxValidation);
    }

    /**
     * Affiche la règle légale pour le type sélectionné.
     */
    private void mettreAJourPanneauRegle(String type) {
        assurerPanneauValidation();
        vboxValidation.getChildren().clear();

        if (type == null) {
            vboxValidation.setVisible(false);
            vboxValidation.setManaged(false);
            return;
        }

        RegleConge regle = CongeRegleService.getRegle(type);
        if (regle == null) return;

        // Titre règle
        HBox titreRow = new HBox(8);
        titreRow.setAlignment(Pos.CENTER_LEFT);
        Label icone = new Label(regle.icone);
        icone.setStyle("-fx-font-size:20px;");
        Label titreLabel = new Label("Règle légale — " + type);
        titreLabel.setStyle("-fx-font-weight:900; -fx-text-fill:#6d2269; -fx-font-size:13px;");
        titreRow.getChildren().addAll(icone, titreLabel);

        // Description légale
        Label descLegale = new Label(regle.description);
        descLegale.setWrapText(true);
        descLegale.setStyle("-fx-font-size:12px; -fx-text-fill:#555; -fx-line-spacing:2;");

        // Badges min/max
        HBox badgesRow = new HBox(10);
        badgesRow.setAlignment(Pos.CENTER_LEFT);

        Label badgeMin = new Label("⬇ Min : " + regle.minJours + " jours");
        badgeMin.setStyle(
                "-fx-background-color:#dbeafe; -fx-text-fill:#1d4ed8;" +
                        "-fx-background-radius:20; -fx-padding:4 12;" +
                        "-fx-font-size:11px; -fx-font-weight:800;");

        Label badgeMax = new Label("⬆ Max : " + regle.maxJours + " jours");
        badgeMax.setStyle(
                "-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                        "-fx-background-radius:20; -fx-padding:4 12;" +
                        "-fx-font-size:11px; -fx-font-weight:800;");

        badgesRow.getChildren().addAll(badgeMin, badgeMax);

        if (regle.documentObligatoire) {
            Label badgeDoc = new Label("📄 Certificat obligatoire");
            badgeDoc.setStyle(
                    "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                            "-fx-background-radius:20; -fx-padding:4 12;" +
                            "-fx-font-size:11px; -fx-font-weight:800;");
            badgesRow.getChildren().add(badgeDoc);
        }

        vboxValidation.getChildren().addAll(titreRow, descLegale, badgesRow);
        vboxValidation.setVisible(true);
        vboxValidation.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(300), vboxValidation);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    /**
     * Met à jour la section de validation (durée, fériés, erreurs) en temps réel.
     */
    private void mettreAJourValidation() {
        assurerPanneauValidation();

        String type  = cbType.getValue();
        LocalDate debut = dpDebut.getValue();
        LocalDate fin   = dpFin.getValue();

        if (type == null || debut == null || fin == null || !fin.isAfter(debut)) {
            // Garder seulement la règle légale si type choisi
            List<Node> toKeep = new java.util.ArrayList<>();
            for (Node n : vboxValidation.getChildren()) {
                if (n.getUserData() != null && n.getUserData().equals("dynamic")) {
                    // supprimer
                } else {
                    toKeep.add(n);
                }
            }
            vboxValidation.getChildren().setAll(toKeep);
            return;
        }

        ResultatValidation resultat = CongeRegleService.valider(
                type, debut, fin,
                fichierCertificat != null);

        // Supprimer anciens blocs dynamiques
        vboxValidation.getChildren().removeIf(n ->
                n.getUserData() != null && n.getUserData().equals("dynamic"));

        Separator sep = new Separator();
        sep.setUserData("dynamic");
        vboxValidation.getChildren().add(sep);

        // ── Résumé durée ──────────────────────────
        HBox dureeRow = new HBox(12);
        dureeRow.setUserData("dynamic");
        dureeRow.setAlignment(Pos.CENTER_LEFT);

        Label lblCal = creerBadgeDuree(
                "📅 " + resultat.joursCalendaires + " jours calendaires",
                "#ede9f6", "#6d2269");
        Label lblOuv = creerBadgeDuree(
                "💼 " + resultat.joursOuvrables + " jours ouvrables",
                "#d1fae5", "#059669");
        dureeRow.getChildren().addAll(lblCal, lblOuv);
        vboxValidation.getChildren().add(dureeRow);

        // ── Jours fériés dans la période ──────────
        if (!resultat.feriesDansPeriode.isEmpty()) {
            VBox feriesBox = new VBox(6);
            feriesBox.setUserData("dynamic");
            feriesBox.setStyle(
                    "-fx-background-color:#fff7ed;" +
                            "-fx-background-radius:12;" +
                            "-fx-border-color:#fed7aa;" +
                            "-fx-border-radius:12;" +
                            "-fx-border-width:1.5;" +
                            "-fx-padding:12;");

            Label feriesTitre = new Label("🎉 Jours fériés dans votre période");
            feriesTitre.setStyle("-fx-font-weight:900; -fx-text-fill:#ea580c; -fx-font-size:12px;");
            feriesBox.getChildren().add(feriesTitre);

            for (Map.Entry<LocalDate, JourFerie> entry : resultat.feriesDansPeriode) {
                HBox ferieRow = new HBox(8);
                ferieRow.setAlignment(Pos.CENTER_LEFT);

                Label emojiLbl = new Label(entry.getValue().emoji);
                emojiLbl.setStyle("-fx-font-size:14px;");

                Label nomLbl = new Label(entry.getValue().nom);
                nomLbl.setStyle("-fx-font-weight:700; -fx-text-fill:#9a3412; -fx-font-size:12px;");

                Label dateLbl = new Label("• " + entry.getKey().toString());
                dateLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#c2410c;");

                String typeBadge = "islamique".equals(entry.getValue().type) ? "☪ Islamique" : "🏛 National";
                Label typeLbl = new Label(typeBadge);
                typeLbl.setStyle(
                        "-fx-background-color:" + ("islamique".equals(entry.getValue().type) ? "#fef3c7" : "#dbeafe") + ";" +
                                "-fx-text-fill:" + ("islamique".equals(entry.getValue().type) ? "#d97706" : "#1d4ed8") + ";" +
                                "-fx-background-radius:10; -fx-padding:2 8; -fx-font-size:10px; -fx-font-weight:700;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                ferieRow.getChildren().addAll(emojiLbl, nomLbl, dateLbl, spacer, typeLbl);
                feriesBox.getChildren().add(ferieRow);
            }

            vboxValidation.getChildren().add(feriesBox);
        }

        // ── Erreurs ───────────────────────────────
        if (!resultat.erreurs.isEmpty()) {
            for (String erreur : resultat.erreurs) {
                Label lbl = new Label("❌  " + erreur);
                lbl.setUserData("dynamic");
                lbl.setWrapText(true);
                lbl.setStyle(
                        "-fx-text-fill:#dc2626; -fx-font-size:12px; -fx-font-weight:700;" +
                                "-fx-background-color:#fee2e2; -fx-background-radius:10; -fx-padding:8 12;");
                vboxValidation.getChildren().add(lbl);
            }
        }

        // ── Avertissements ────────────────────────
        if (!resultat.avertissements.isEmpty()) {
            for (String warn : resultat.avertissements) {
                Label lbl = new Label("⚠  " + warn);
                lbl.setUserData("dynamic");
                lbl.setWrapText(true);
                lbl.setStyle(
                        "-fx-text-fill:#d97706; -fx-font-size:12px; -fx-font-weight:700;" +
                                "-fx-background-color:#fef3c7; -fx-background-radius:10; -fx-padding:8 12;");
                vboxValidation.getChildren().add(lbl);
            }
        }

        // ── Tout est OK ───────────────────────────
        if (resultat.valide && resultat.erreurs.isEmpty() && resultat.avertissements.isEmpty()) {
            Label ok = new Label("✅  Durée conforme aux règles légales");
            ok.setUserData("dynamic");
            ok.setStyle(
                    "-fx-text-fill:#059669; -fx-font-size:12px; -fx-font-weight:700;" +
                            "-fx-background-color:#d1fae5; -fx-background-radius:10; -fx-padding:8 12;");
            vboxValidation.getChildren().add(ok);
        }

        vboxValidation.setVisible(true);
        vboxValidation.setManaged(true);
    }

    private Label creerBadgeDuree(String texte, String bg, String fg) {
        Label l = new Label(texte);
        l.setUserData("dynamic");
        l.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-background-radius:20; -fx-padding:5 14;" +
                        "-fx-font-size:12px; -fx-font-weight:800;");
        return l;
    }

    // ══════════════════════════════════════
    //  ANIMATIONS ENTRÉE
    // ══════════════════════════════════════
    private void animerEntree(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(18);
        FadeTransition ft = new FadeTransition(Duration.millis(450), node);
        ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(delayMs));
        TranslateTransition tt = new TranslateTransition(Duration.millis(450), node);
        tt.setFromY(18); tt.setToY(0); tt.setDelay(Duration.millis(delayMs));
        ft.play(); tt.play();
    }

    private void animateCard(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(14);
        FadeTransition ft = new FadeTransition(Duration.millis(350), node);
        ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(delayMs));
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), node);
        tt.setFromY(14); tt.setToY(0); tt.setDelay(Duration.millis(delayMs));
        ft.play(); tt.play();
    }

    // ══════════════════════════════════════
    //  OCR
    // ══════════════════════════════════════
    @FXML
    private void onUploadCertificat() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir le certificat médical");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.bmp","*.gif"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf")
        );
        File file = fc.showOpenDialog(btnUploadDoc.getScene().getWindow());
        if (file == null) return;
        if (file.length() > 1_000_000) {
            afficherMsg("⚠ Image trop lourde — max 1MB recommandé", "#f59e0b");
            return;
        }
        fichierCertificat = file;
        lblFichierChoisi.setText("📄 " + file.getName());
        ocrSpinner.setVisible(true);
        ocrSpinner.setManaged(true);
        btnUploadDoc.setDisable(true);
        vboxOcrResult.setVisible(false);
        vboxOcrResult.setManaged(false);

        Thread t = new Thread(() -> {
            try {
                OcrService.OcrResult result = OcrService.analyserCertificat(fichierCertificat);
                dernierResultatOcr = result;
                Platform.runLater(() -> {
                    lblMedecin.setText(result.nomMedecin != null ? result.nomMedecin : "Non détecté");
                    lblMedecin.setStyle("-fx-font-size:13px; -fx-font-weight:bold; " +
                            (result.nomMedecin != null ? "-fx-text-fill:#059669;" : "-fx-text-fill:#f59e0b;"));
                    if (result.dateDebut != null && result.dateFin != null) {
                        lblPeriodeOcr.setText(result.dateDebut + "  →  " + result.dateFin
                                + "  (" + result.dureeJours + " jours)");
                        lblPeriodeOcr.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#059669;");
                        btnAppliquerOcr.setVisible(true);
                        btnAppliquerOcr.setManaged(true);
                    } else {
                        lblPeriodeOcr.setText("Non détecté");
                        lblPeriodeOcr.setStyle("-fx-font-size:13px; -fx-text-fill:#f59e0b;");
                        btnAppliquerOcr.setVisible(false);
                        btnAppliquerOcr.setManaged(false);
                    }
                    lblOcrStatut.setText(result.estValide ? "✅ Certificat reconnu" : "⚠ " + result.messageErreur);
                    lblOcrStatut.setStyle(result.estValide
                            ? "-fx-text-fill:#059669; -fx-font-size:13px; -fx-font-weight:bold;"
                            : "-fx-text-fill:#dc2626; -fx-font-size:13px;");
                    vboxOcrResult.setVisible(true);
                    vboxOcrResult.setManaged(true);
                    FadeTransition ft2 = new FadeTransition(Duration.millis(300), vboxOcrResult);
                    ft2.setFromValue(0); ft2.setToValue(1); ft2.play();
                    ocrSpinner.setVisible(false);
                    ocrSpinner.setManaged(false);
                    btnUploadDoc.setDisable(false);
                    mettreAJourValidation();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    afficherMsg("❌ Erreur OCR : " + e.getMessage(), "#dc2626");
                    ocrSpinner.setVisible(false);
                    ocrSpinner.setManaged(false);
                    btnUploadDoc.setDisable(false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onAppliquerDatesOcr() {
        if (dernierResultatOcr == null) return;
        if (dernierResultatOcr.dateDebut != null) dpDebut.setValue(dernierResultatOcr.dateDebut);
        if (dernierResultatOcr.dateFin   != null) dpFin.setValue(dernierResultatOcr.dateFin);
        afficherMsg("✅ Dates du certificat appliquées !", "#059669");
    }

    // ══════════════════════════════════════
    //  VALIDATION FORMULAIRE
    // ══════════════════════════════════════
    private void addValidationListeners() {
        cbType.valueProperty().addListener((obs, o, n) -> { if (n != null) hideError(cbType, typeErrorLabel); });
        dpDebut.valueProperty().addListener((obs, o, n) -> { if (n != null) { hideError(dpDebut, dateDebutErrorLabel); validateDateOrder(); } });
        dpFin.valueProperty().addListener((obs, o, n)   -> { if (n != null) { hideError(dpFin, dateFinErrorLabel); validateDateOrder(); } });
        taDescription.textProperty().addListener((obs, o, n) -> { if (n != null && n.trim().length() >= 5) hideError(taDescription, descErrorLabel); });
    }

    private void validateDateOrder() {
        LocalDate debut = dpDebut.getValue(), fin = dpFin.getValue();
        if (debut != null && fin != null && !fin.isAfter(debut))
            showError(dpFin, dateFinErrorLabel, "La date fin doit être après début");
        else
            hideError(dpFin, dateFinErrorLabel);
    }

    private void showError(Control field, Label label, String msg) {
        field.setStyle(errorStyle);
        label.setText(msg); label.setVisible(true); label.setManaged(true);
    }

    private void hideError(Control field, Label label) {
        field.setStyle(normalStyle);
        label.setVisible(false); label.setManaged(false);
    }

    private boolean isDuplicate(String type, LocalDate debut, LocalDate fin, Long excludeId) {
        if (allConges == null) return false;
        for (Conge c : allConges) {
            if (excludeId != null && c.getId() == excludeId) continue;
            if (c.getTypeConge().equals(type) && c.getDateDebut().equals(debut) && c.getDateFin().equals(fin)) return true;
        }
        return false;
    }

    private boolean validateForm() {
        boolean isValid = true;
        if (cbType.getValue() == null) { showError(cbType, typeErrorLabel, "Choisissez un type"); isValid = false; }
        else hideError(cbType, typeErrorLabel);
        if (dpDebut.getValue() == null) { showError(dpDebut, dateDebutErrorLabel, "Choisissez date début"); isValid = false; }
        else hideError(dpDebut, dateDebutErrorLabel);
        if (dpFin.getValue() == null || (dpDebut.getValue() != null && !dpFin.getValue().isAfter(dpDebut.getValue()))) {
            showError(dpFin, dateFinErrorLabel, "Date fin invalide"); isValid = false;
        } else hideError(dpFin, dateFinErrorLabel);
        if (taDescription.getText() == null || taDescription.getText().trim().length() < 5) {
            showError(taDescription, descErrorLabel, "Description minimum 5 caractères"); isValid = false;
        } else hideError(taDescription, descErrorLabel);
        if (isValid) {
            Long excludeId = congeEnModification != null ? congeEnModification.getId() : null;
            if (isDuplicate(cbType.getValue(), dpDebut.getValue(), dpFin.getValue(), excludeId)) {
                showError(cbType, typeErrorLabel, "Demande identique existe déjà"); isValid = false;
            }
        }

        // Validation légale (limites)
        if (isValid && cbType.getValue() != null && dpDebut.getValue() != null && dpFin.getValue() != null) {
            ResultatValidation rv = CongeRegleService.valider(
                    cbType.getValue(), dpDebut.getValue(), dpFin.getValue(),
                    fichierCertificat != null);
            if (!rv.erreurs.isEmpty()) {
                showError(dpFin, dateFinErrorLabel, rv.erreurs.get(0));
                isValid = false;
            }
        }

        return isValid;
    }

    // ══════════════════════════════════════
    //  SUBMIT
    // ══════════════════════════════════════
    @FXML
    private void onSubmit() {
        if (!validateForm()) { afficherMsg("⚠ Corrigez les erreurs", "#f59e0b"); return; }
        try {
            long empId = Session.getCurrentUser().getId();
            String desc = taDescription.getText();
            if (desc != null && desc.length() > 1000) desc = desc.substring(0, 1000);
            if (congeEnModification == null) {
                Conge c = new Conge(cbType.getValue(), dpDebut.getValue(), dpFin.getValue(), "EN_ATTENTE", desc, empId);
                if (fichierCertificat != null) {
                    c.setDocumentPath(fichierCertificat.getAbsolutePath());
                    c.setOcrVerified(dernierResultatOcr != null && dernierResultatOcr.estValide);
                }
                congeService.addEntity(c);
                afficherMsg("✅ Demande envoyée avec succès !", "#059669");
            } else {
                congeEnModification.setTypeConge(cbType.getValue());
                congeEnModification.setDateDebut(dpDebut.getValue());
                congeEnModification.setDateFin(dpFin.getValue());
                congeEnModification.setDescription(desc);
                congeService.updateEntity(congeEnModification);
                afficherMsg("✅ Demande modifiée !", "#059669");
                congeEnModification = null;
            }
            clearForm();
            refresh();
        } catch (SQLException e) {
            afficherMsg("❌ " + e.getMessage(), "#dc2626");
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════
    //  IA
    // ══════════════════════════════════════
    @FXML
    private void onGenererDescription() {
        String typeChoisi = cbType.getValue();
        if (typeChoisi == null || typeChoisi.isBlank()) { showError(cbType, typeErrorLabel, "Choisissez d'abord un type"); return; }
        btnGenererIA.setDisable(true);
        btnGenererIA.setText("⏳ Génération…");
        aiSpinner.setVisible(true);
        aiSpinner.setManaged(true);
        Thread thread = new Thread(() -> {
            try {
                AIService.AiResult result = AIService.genererDescriptionEtDuree(typeChoisi);
                Platform.runLater(() -> {
                    taDescription.setText(result.description);
                    if (result.dureeJoursSuggeree > 0 && dpDebut.getValue() != null) {
                        dpFin.setValue(dpDebut.getValue().plusDays(result.dureeJoursSuggeree));
                        afficherMsg("✅ Durée suggérée : " + result.dureeJoursSuggeree + " jours", "#059669");
                    } else {
                        afficherMsg("✅ Description générée !", "#059669");
                    }
                    resetBoutonIA();
                });
            } catch (Exception e) {
                Platform.runLater(() -> { afficherMsg("❌ Erreur IA : " + e.getMessage(), "#dc2626"); resetBoutonIA(); });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void resetBoutonIA() {
        btnGenererIA.setDisable(false);
        btnGenererIA.setText("✨ Générer avec IA");
        aiSpinner.setVisible(false);
        aiSpinner.setManaged(false);
    }

    // ══════════════════════════════════════
    //  MESSAGE HEADER
    // ══════════════════════════════════════
    private void afficherMsg(String texte, String couleur) {
        lblMsg.setText(texte);
        lblMsg.setStyle("-fx-font-size:13px; -fx-font-weight:800; -fx-text-fill:white;" +
                "-fx-background-color:" + couleur + ";" +
                "-fx-background-radius:20; -fx-padding:6 16;");
        FadeTransition ft = new FadeTransition(Duration.millis(300), lblMsg);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ══════════════════════════════════════
    //  REFRESH / LOAD
    // ══════════════════════════════════════
    @FXML public void refresh() { loadData(); }

    private void loadData() {
        try {
            cardContainer.getChildren().clear();
            long empId = Session.getCurrentUser().getId();
            allConges = congeService.findByEmployeId(empId);
            int delay = 0;
            for (Conge c : allConges) {
                Reponse rep = reponseService.getOneByCongeId(c.getId());
                String commentaire = (rep == null || rep.getCommentaire() == null) ? "" : rep.getCommentaire();
                VBox card = createCard(c, commentaire);
                cardContainer.getChildren().add(card);
                animateCard(card, delay);
                delay += 60;
            }
            if (allConges.isEmpty()) {
                Label empty = new Label("✨  Aucune demande pour le moment");
                empty.setStyle("-fx-text-fill:#c4b5f4; -fx-font-size:14px; -fx-font-weight:700; -fx-padding:20;");
                cardContainer.getChildren().add(empty);
            }
        } catch (SQLException e) {
            afficherMsg("❌ Erreur chargement", "#dc2626");
        }
    }

    // ══════════════════════════════════════
    //  CREATE CARD
    // ══════════════════════════════════════
    private VBox createCard(Conge c, String commentaire) {
        String statut = (c.getStatut() != null) ? c.getStatut().trim().toLowerCase() : "";
        String borderColor = statut.contains("accept") ? "#059669" : statut.contains("refus") ? "#dc2626" : "#6d2269";

        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:18;" +
                        "-fx-border-color:" + borderColor + " transparent transparent transparent;" +
                        "-fx-border-width:3 0 0 0;" +
                        "-fx-border-radius:18 18 0 0;" +
                        "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.09),14,0,0,3);");

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#fdfbff;" +
                        "-fx-background-radius:18;" +
                        "-fx-border-color:" + borderColor + " transparent transparent transparent;" +
                        "-fx-border-width:3 0 0 0;" +
                        "-fx-border-radius:18 18 0 0;" +
                        "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.18),18,0,0,6);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:18;" +
                        "-fx-border-color:" + borderColor + " transparent transparent transparent;" +
                        "-fx-border-width:3 0 0 0;" +
                        "-fx-border-radius:18 18 0 0;" +
                        "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.09),14,0,0,3);"));

        // ── TOP ROW ──────────────────────────────
        HBox topRow = new HBox(0);
        topRow.setPadding(new Insets(16, 18, 12, 18));
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setSpacing(14);

        String icone = switch (c.getTypeConge()) {
            case "Congé maladie"       -> "🏥";
            case "Congé annuel"        -> "🏖";
            case "Congé maternité"     -> "👶";
            case "Congé professionnel" -> "💼";
            case "Congé sabbatique"    -> "🌍";
            default                    -> "📋";
        };

        Label typeLabel = new Label(icone + "  " + c.getTypeConge());
        typeLabel.setStyle("-fx-font-size:15px; -fx-font-weight:900; -fx-text-fill:#3d1a3b;");

        Label statutBadge = new Label(c.getStatut());
        String badgeBg = statut.contains("accept") ? "#d1fae5" : statut.contains("refus") ? "#fee2e2" : "#ede9f6";
        String badgeFg = statut.contains("accept") ? "#059669" : statut.contains("refus") ? "#dc2626" : "#6d2269";
        statutBadge.setStyle(
                "-fx-background-color:" + badgeBg + ";" +
                        "-fx-text-fill:" + badgeFg + ";" +
                        "-fx-font-weight:800; -fx-font-size:11px;" +
                        "-fx-background-radius:20; -fx-padding:4 12;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        topRow.getChildren().addAll(typeLabel, spacer1, statutBadge);

        if ("Congé maladie".equals(c.getTypeConge())) {
            Label ocrBadge;
            if (c.isOcrVerified()) {
                ocrBadge = new Label("🔐 OCR ✅");
                ocrBadge.setStyle("-fx-background-color:#d1fae5; -fx-text-fill:#059669;" +
                        "-fx-background-radius:20; -fx-padding:4 10; -fx-font-size:11px; -fx-font-weight:bold;");
            } else if (c.getDocumentPath() != null) {
                ocrBadge = new Label("⚠ Certif");
                ocrBadge.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                        "-fx-background-radius:20; -fx-padding:4 10; -fx-font-size:11px; -fx-font-weight:bold;");
            } else {
                ocrBadge = new Label("❌ Sans certif");
                ocrBadge.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                        "-fx-background-radius:20; -fx-padding:4 10; -fx-font-size:11px; -fx-font-weight:bold;");
            }
            topRow.getChildren().add(ocrBadge);
        }

        // ── PÉRIODE + DURÉE + JOURS FÉRIÉS ────────
        VBox periodeSection = new VBox(6);
        periodeSection.setPadding(new Insets(0, 18, 10, 18));

        HBox periodeRow = new HBox(8);
        periodeRow.setAlignment(Pos.CENTER_LEFT);
        Label periodeIcon = new Label("📅");
        periodeIcon.setStyle("-fx-font-size:13px;");
        Label periodeLabel = new Label(c.getDateDebut() + "   →   " + c.getDateFin());
        periodeLabel.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#7c3a7a;");

        if (c.getDateDebut() != null && c.getDateFin() != null) {
            long joursCalendaires = c.getDateFin().toEpochDay() - c.getDateDebut().toEpochDay();
            int joursOuv = CongeRegleService.calculerJoursOuvrables(c.getDateDebut(), c.getDateFin());

            Label dureeLabel = new Label("(" + joursCalendaires + " j cal. / " + joursOuv + " j ouv.)");
            dureeLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#b39cb0;");
            periodeRow.getChildren().addAll(periodeIcon, periodeLabel, dureeLabel);

            // Jours fériés dans la période de cette carte
            java.util.List<Map.Entry<LocalDate, JourFerie>> feriesCard =
                    CongeRegleService.getJeriesDansPeriode(c.getDateDebut(), c.getDateFin());

            if (!feriesCard.isEmpty()) {
                HBox feriesRow = new HBox(6);
                feriesRow.setAlignment(Pos.CENTER_LEFT);
                Label feriesLbl = new Label("🎉 Fériés inclus :");
                feriesLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#ea580c; -fx-font-weight:700;");
                feriesRow.getChildren().add(feriesLbl);

                for (Map.Entry<LocalDate, JourFerie> entry : feriesCard) {
                    Label badge = new Label(entry.getValue().emoji + " " + entry.getValue().nom);
                    badge.setStyle(
                            "-fx-background-color:#fff7ed; -fx-text-fill:#c2410c;" +
                                    "-fx-background-radius:10; -fx-padding:2 8;" +
                                    "-fx-font-size:10px; -fx-font-weight:700;");
                    feriesRow.getChildren().add(badge);
                }
                periodeSection.getChildren().addAll(periodeRow, feriesRow);
            } else {
                periodeSection.getChildren().add(periodeRow);
            }
        } else {
            periodeRow.getChildren().addAll(periodeIcon, periodeLabel);
            periodeSection.getChildren().add(periodeRow);
        }

        // ── SEPARATEUR ───────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#f0ebf9;");

        // ── DESCRIPTION ──────────────────────────
        VBox descRow = new VBox(4);
        descRow.setPadding(new Insets(10, 18, 10, 18));
        Label descTitle = new Label("DESCRIPTION");
        descTitle.setStyle("-fx-font-size:10px; -fx-text-fill:#b39cb0; -fx-font-weight:900;");
        Label descValue = new Label(c.getDescription());
        descValue.setWrapText(true);
        descValue.setStyle("-fx-font-size:13px; -fx-text-fill:#444;");
        descRow.getChildren().addAll(descTitle, descValue);

        // ── RÉPONSE RH ───────────────────────────
        HBox reponseRow = new HBox(8);
        reponseRow.setPadding(new Insets(0, 18, 14, 18));
        reponseRow.setAlignment(Pos.CENTER_LEFT);
        if (commentaire != null && !commentaire.trim().isEmpty()) {
            Label repIcon = new Label("💬");
            repIcon.setStyle("-fx-font-size:13px;");
            Label repLabel = new Label("Réponse RH disponible");
            repLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#6d2269;" +
                    "-fx-underline:true; -fx-cursor:hand; -fx-font-weight:700;");
            repLabel.setOnMouseClicked(e -> afficherPopupCommentaire(commentaire, c));
            reponseRow.getChildren().addAll(repIcon, repLabel);
        }

        // ── ACTIONS ──────────────────────────────
        HBox actionsRow = new HBox(10);
        actionsRow.setPadding(new Insets(0, 14, 14, 14));
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if ("EN_ATTENTE".equals(c.getStatut())) {
            Button btnEdit = new Button("✏  Modifier");
            btnEdit.setStyle(
                    "-fx-background-color:#6d2269; -fx-text-fill:white;" +
                            "-fx-font-weight:700; -fx-background-radius:20;" +
                            "-fx-padding:7 16; -fx-cursor:hand; -fx-font-size:12px;");
            btnEdit.setOnAction(e -> {
                congeEnModification = c;
                cbType.setValue(c.getTypeConge());
                dpDebut.setValue(c.getDateDebut());
                dpFin.setValue(c.getDateFin());
                taDescription.setText(c.getDescription());
                afficherMsg("✏  Mode modification activé", "#6d2269");
            });

            Button btnDel = new Button("🗑  Supprimer");
            btnDel.setStyle(
                    "-fx-background-color:#fff0f0; -fx-text-fill:#dc2626;" +
                            "-fx-font-weight:700; -fx-background-radius:20;" +
                            "-fx-padding:7 16; -fx-cursor:hand; -fx-font-size:12px;" +
                            "-fx-border-color:#fca5a5; -fx-border-width:1; -fx-border-radius:20;");
            btnDel.setOnAction(e -> afficherPopupSuppression(c));
            actionsRow.getChildren().addAll(btnEdit, btnDel);
        }

        card.getChildren().addAll(topRow, periodeSection, sep, descRow, reponseRow, actionsRow);
        return card;
    }

    // ══════════════════════════════════════
    //  POPUP SUPPRESSION
    // ══════════════════════════════════════
    private void afficherPopupSuppression(Conge c) {
        Stage popup = new Stage(StageStyle.TRANSPARENT);
        popup.initOwner(cardContainer.getScene().getWindow());
        VBox overlay = new VBox();
        overlay.setStyle("-fx-background-color:rgba(30,10,30,0.55);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setPrefSize(520, 320);
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(32, 36, 28, 36));
        card.setMaxWidth(400);
        card.setStyle("-fx-background-color:white; -fx-background-radius:24;" +
                "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.3),30,0,0,10);");
        Label icone = new Label("🗑");
        icone.setStyle("-fx-font-size:42px;");
        icone.setAlignment(Pos.CENTER);
        Label titre = new Label("Supprimer cette demande ?");
        titre.setStyle("-fx-font-size:17px; -fx-font-weight:900; -fx-text-fill:#3d1a3b;");
        titre.setWrapText(true); titre.setAlignment(Pos.CENTER);
        Label sous = new Label(c.getTypeConge() + "  •  " + c.getDateDebut() + " → " + c.getDateFin());
        sous.setStyle("-fx-font-size:13px; -fx-text-fill:#9c5c9a; -fx-font-weight:600;");
        sous.setAlignment(Pos.CENTER);
        Label warn = new Label("Cette action est irréversible.");
        warn.setStyle("-fx-font-size:12px; -fx-text-fill:#dc2626;");
        warn.setAlignment(Pos.CENTER);
        HBox btnRow = new HBox(14);
        btnRow.setAlignment(Pos.CENTER);
        Button btnConfirm = new Button("🗑  Oui, supprimer");
        btnConfirm.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white;" +
                "-fx-font-weight:800; -fx-background-radius:22;" +
                "-fx-padding:11 22; -fx-cursor:hand; -fx-font-size:13px;");
        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color:#f3f0fa; -fx-text-fill:#6d2269;" +
                "-fx-font-weight:800; -fx-background-radius:22;" +
                "-fx-padding:11 22; -fx-cursor:hand; -fx-font-size:13px;" +
                "-fx-border-color:#ddd6fe; -fx-border-width:1.5; -fx-border-radius:22;");
        btnAnnuler.setOnAction(e -> fermerPopupAvecAnimation(overlay, popup));
        btnConfirm.setOnAction(e -> {
            try {
                congeService.deleteEntity(c);
                popup.close();
                refresh();
                afficherMsg("✅ Demande supprimée", "#dc2626");
            } catch (SQLException ex) {
                afficherMsg("❌ Erreur suppression", "#dc2626");
                popup.close();
            }
        });
        btnRow.getChildren().addAll(btnAnnuler, btnConfirm);
        card.getChildren().addAll(icone, titre, sous, warn, btnRow);
        overlay.getChildren().add(card);
        Scene scene = new Scene(overlay);
        scene.setFill(null);
        popup.setScene(scene);
        card.setScaleX(0.7); card.setScaleY(0.7); card.setOpacity(0);
        popup.show();
        new ParallelTransition(
                animScale(card, 0.7, 1.0), animFade(card, 0, 1)).play();
        javafx.stage.Window owner = cardContainer.getScene().getWindow();
        popup.setX(owner.getX() + (owner.getWidth()  - 520) / 2);
        popup.setY(owner.getY() + (owner.getHeight() - 320) / 2);
    }

    // ══════════════════════════════════════
    //  POPUP COMMENTAIRE
    // ══════════════════════════════════════
    private void afficherPopupCommentaire(String commentaire, Conge c) {
        Stage popup = new Stage(StageStyle.TRANSPARENT);
        popup.initOwner(cardContainer.getScene().getWindow());
        VBox overlay = new VBox();
        overlay.setStyle("-fx-background-color:rgba(30,10,30,0.55);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setPrefSize(560, 380);
        VBox card = new VBox(18);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(28, 32, 26, 32));
        card.setMaxWidth(460);
        card.setStyle("-fx-background-color:white; -fx-background-radius:24;" +
                "-fx-effect:dropshadow(gaussian,rgba(109,34,105,0.3),30,0,0,10);");
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label("💬");
        ic.setStyle("-fx-font-size:26px;");
        VBox titreBox = new VBox(3);
        Label titre = new Label("Commentaire du RH");
        titre.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:#3d1a3b;");
        Label sousTitre = new Label(c.getTypeConge() + "  •  " + c.getDateDebut() + " → " + c.getDateFin());
        sousTitre.setStyle("-fx-font-size:11px; -fx-text-fill:#9c5c9a; -fx-font-weight:600;");
        titreBox.getChildren().addAll(titre, sousTitre);
        String statut = (c.getStatut() != null) ? c.getStatut().trim().toLowerCase() : "";
        String badgeBg = statut.contains("accept") ? "#d1fae5" : statut.contains("refus") ? "#fee2e2" : "#ede9f6";
        String badgeFg = statut.contains("accept") ? "#059669" : statut.contains("refus") ? "#dc2626" : "#6d2269";
        Label statutBadge = new Label(c.getStatut());
        statutBadge.setStyle("-fx-background-color:" + badgeBg + "; -fx-text-fill:" + badgeFg + ";" +
                "-fx-font-weight:800; -fx-font-size:11px; -fx-background-radius:20; -fx-padding:4 12;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(ic, titreBox, sp, statutBadge);
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color:#f0ebf9;");
        VBox commentBox = new VBox(8);
        commentBox.setStyle("-fx-background-color:#f8f5ff; -fx-background-radius:14;" +
                "-fx-border-color:#ddd6fe; -fx-border-radius:14; -fx-border-width:1.5; -fx-padding:16;");
        Label commentLabel = new Label(commentaire);
        commentLabel.setWrapText(true);
        commentLabel.setStyle("-fx-font-size:14px; -fx-text-fill:#3d1a3b; -fx-line-spacing:4;");
        commentBox.getChildren().add(commentLabel);
        Button btnFermer = new Button("✓  Fermer");
        btnFermer.setStyle("-fx-background-color:#6d2269; -fx-text-fill:white;" +
                "-fx-font-weight:800; -fx-background-radius:22;" +
                "-fx-padding:10 28; -fx-cursor:hand; -fx-font-size:13px;");
        btnFermer.setOnAction(e -> fermerPopupAvecAnimation(overlay, popup));
        HBox btnRow2 = new HBox();
        btnRow2.setAlignment(Pos.CENTER_RIGHT);
        btnRow2.getChildren().add(btnFermer);
        card.getChildren().addAll(header, sep2, commentBox, btnRow2);
        overlay.getChildren().add(card);
        Scene scene2 = new Scene(overlay);
        scene2.setFill(null);
        popup.setScene(scene2);
        card.setScaleX(0.75); card.setScaleY(0.75); card.setOpacity(0);
        popup.show();
        new ParallelTransition(animScale(card, 0.75, 1.0), animFade(card, 0, 1)).play();
        javafx.stage.Window owner = cardContainer.getScene().getWindow();
        popup.setX(owner.getX() + (owner.getWidth()  - 560) / 2);
        popup.setY(owner.getY() + (owner.getHeight() - 380) / 2);
    }

    // ── Helpers animation ──────────────────────
    private ScaleTransition animScale(Node n, double from, double to) {
        ScaleTransition st = new ScaleTransition(Duration.millis(280), n);
        st.setFromX(from); st.setToX(to); st.setFromY(from); st.setToY(to);
        return st;
    }
    private FadeTransition animFade(Node n, double from, double to) {
        FadeTransition ft = new FadeTransition(Duration.millis(280), n);
        ft.setFromValue(from); ft.setToValue(to);
        return ft;
    }

    private void fermerPopupAvecAnimation(VBox overlay, Stage popup) {
        ParallelTransition pt = new ParallelTransition(animScale(overlay, 1, 0.9), animFade(overlay, 1, 0));
        pt.setOnFinished(e -> popup.close());
        pt.play();
    }

    // ══════════════════════════════════════
    //  CLEAR FORM
    // ══════════════════════════════════════
    private void clearForm() {
        cbType.setValue(null);
        dpDebut.setValue(null);
        dpFin.setValue(null);
        taDescription.clear();
        congeEnModification = null;
        fichierCertificat = null;
        if (vboxValidation != null) {
            vboxValidation.setVisible(false);
            vboxValidation.setManaged(false);
        }
        if (dernierResultatOcr != null) {
            vboxOcrResult.setVisible(false);
            vboxOcrResult.setManaged(false);
        }
    }
}