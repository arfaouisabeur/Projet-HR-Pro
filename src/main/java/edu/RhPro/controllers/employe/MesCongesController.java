package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Conge;
import edu.RhPro.entities.Reponse;
import edu.RhPro.services.CongeService;
import edu.RhPro.services.ReponseService;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

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

    private final CongeService congeService = new CongeService();
    private final ReponseService reponseService = new ReponseService();

    private Conge congeEnModification = null;
    private List<Conge> allConges;

    private final String normalStyle =
            "-fx-border-color:#ececf5; -fx-border-width:1; -fx-border-radius:14; -fx-background-radius:14;";

    private final String errorStyle =
            "-fx-border-color:red; -fx-border-width:2; -fx-border-radius:14; -fx-background-radius:14;";

    @FXML
    public void initialize() {
        cbType.getItems().addAll(
                "Congé annuel",
                "Congé maladie",
                "Congé maternité",
                "Congé professionnel",
                "Congé sabbatique",
                "Autre"
        );

        addValidationListeners();
        refresh();
    }

    /*** Animation ***/
    private void animateNode(Node node) {
        node.setOpacity(0);
        node.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(400), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(400), node);
        slide.setFromY(20);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    /*** Validation temps réel ***/
    private void addValidationListeners() {

        cbType.valueProperty().addListener((obs, o, n) -> {
            if (n != null) hideError(cbType, typeErrorLabel);
        });

        dpDebut.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                hideError(dpDebut, dateDebutErrorLabel);
                validateDateOrder();
            }
        });

        dpFin.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                hideError(dpFin, dateFinErrorLabel);
                validateDateOrder();
            }
        });

        taDescription.textProperty().addListener((obs, o, n) -> {
            if (n != null && n.trim().length() >= 5) {
                hideError(taDescription, descErrorLabel);
            }
        });
    }

    private void validateDateOrder() {
        LocalDate debut = dpDebut.getValue();
        LocalDate fin = dpFin.getValue();

        if (debut != null && fin != null && !fin.isAfter(debut)) {
            showError(dpFin, dateFinErrorLabel,
                    "La date fin doit être après début");
        } else {
            hideError(dpFin, dateFinErrorLabel);
        }
    }

    private void showError(Control field, Label label, String msg) {
        field.setStyle(errorStyle);
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideError(Control field, Label label) {
        field.setStyle(normalStyle);
        label.setVisible(false);
        label.setManaged(false);
    }

    /*** Correction ici (long) ***/
    private boolean isDuplicate(String type,
                                LocalDate debut,
                                LocalDate fin,
                                Long excludeId) {

        if (allConges == null) return false;

        for (Conge c : allConges) {

            // 🔥 CORRECTION
            if (excludeId != null && c.getId() == excludeId)
                continue;

            if (c.getTypeConge().equals(type)
                    && c.getDateDebut().equals(debut)
                    && c.getDateFin().equals(fin)) {
                return true;
            }
        }
        return false;
    }

    private boolean validateForm() {

        boolean isValid = true;

        if (cbType.getValue() == null) {
            showError(cbType, typeErrorLabel,
                    "Choisissez un type");
            isValid = false;
        } else hideError(cbType, typeErrorLabel);

        if (dpDebut.getValue() == null) {
            showError(dpDebut, dateDebutErrorLabel,
                    "Choisissez date début");
            isValid = false;
        } else hideError(dpDebut, dateDebutErrorLabel);

        if (dpFin.getValue() == null ||
                !dpFin.getValue().isAfter(dpDebut.getValue())) {
            showError(dpFin, dateFinErrorLabel,
                    "Date fin invalide");
            isValid = false;
        } else hideError(dpFin, dateFinErrorLabel);

        if (taDescription.getText() == null ||
                taDescription.getText().trim().length() < 5) {
            showError(taDescription, descErrorLabel,
                    "Description minimum 5 caractères");
            isValid = false;
        } else hideError(taDescription, descErrorLabel);

        if (isValid) {
            Long excludeId = (congeEnModification != null)
                    ? congeEnModification.getId()
                    : null;

            if (isDuplicate(cbType.getValue(),
                    dpDebut.getValue(),
                    dpFin.getValue(),
                    excludeId)) {

                showError(cbType, typeErrorLabel,
                        "Demande identique existe déjà");
                isValid = false;
            }
        }

        return isValid;
    }

    @FXML
    private void onSubmit() {

        lblMsg.setText("");

        if (!validateForm()) {
            lblMsg.setText("⚠ Corrigez les erreurs");
            lblMsg.setStyle("-fx-text-fill:#f59e0b;");
            return;
        }

        try {

            long empId =
                    Session.getCurrentUser().getId();

            if (congeEnModification == null) {

                Conge c = new Conge(
                        cbType.getValue(),
                        dpDebut.getValue(),
                        dpFin.getValue(),
                        "EN_ATTENTE",
                        taDescription.getText(),
                        empId
                );

                congeService.addEntity(c);
                lblMsg.setText("✅ Demande envoyée");

            } else {

                congeEnModification.setTypeConge(cbType.getValue());
                congeEnModification.setDateDebut(dpDebut.getValue());
                congeEnModification.setDateFin(dpFin.getValue());
                congeEnModification.setDescription(taDescription.getText());

                congeService.updateEntity(congeEnModification);
                lblMsg.setText("✅ Demande modifiée");
                congeEnModification = null;
            }

            lblMsg.setStyle("-fx-text-fill:#059669;");
            clearForm();
            refresh();

        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur base de données");
            lblMsg.setStyle("-fx-text-fill:#dc2626;");
        }
    }

    @FXML
    public void refresh() {
        loadData();
    }

    private void loadData() {
        try {

            cardContainer.getChildren().clear();

            long empId =
                    Session.getCurrentUser().getId();

            allConges =
                    congeService.findByEmployeId(empId);

            for (Conge c : allConges) {

                Reponse rep =
                        reponseService.getOneByCongeId(c.getId());

                String repTxt =
                        (rep == null) ? "-"
                                : rep.getDecision();

                VBox card = createCard(c, repTxt);
                cardContainer.getChildren().add(card);

                animateNode(card);
            }

        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur chargement");
        }
    }

    private VBox createCard(Conge c, String rep) {

        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:20;" +
                        "-fx-border-color:#ddd6fe;" +
                        "-fx-border-width:1.5;" +
                        "-fx-border-radius:20;"
        );

        // ===== TITRE TYPE =====
        Label typeTitle = new Label("TYPE");
        typeTitle.setStyle("-fx-font-size:11px; -fx-text-fill:#6d2269; -fx-font-weight:bold;");

        Label typeValue = new Label(c.getTypeConge());
        typeValue.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:black;");

        VBox typeBox = new VBox(3, typeTitle, typeValue);

        // ===== DATES =====
        Label dateTitle = new Label("PÉRIODE");
        dateTitle.setStyle("-fx-font-size:11px; -fx-text-fill:#6d2269; -fx-font-weight:bold;");

        Label dateValue = new Label(c.getDateDebut() + "  →  " + c.getDateFin());
        dateValue.setStyle("-fx-font-size:14px; -fx-font-weight:600; -fx-text-fill:black;");

        VBox dateBox = new VBox(3, dateTitle, dateValue);

        // ===== STATUT BADGE =====
        Label statutBadge = new Label(c.getStatut());
        statutBadge.setStyle(
                "-fx-padding:6 14;" +
                        "-fx-background-radius:30;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:white;"
        );

        String statut = c.getStatut();

        if (statut == null) statut = "";

        statut = statut.trim().toLowerCase();

        if (statut.contains("attente")) {
            statutBadge.setStyle(statutBadge.getStyle() + "-fx-background-color:#6d2269;");
        }
        else if (statut.contains("accept")) {
            statutBadge.setStyle(statutBadge.getStyle() + "-fx-background-color:#059669;");
        }
        else if (statut.contains("refus")) {
            statutBadge.setStyle(statutBadge.getStyle() + "-fx-background-color:#dc2626;");
        }
        else {
            statutBadge.setStyle(statutBadge.getStyle() + "-fx-background-color:gray;");
        }


        // ===== DESCRIPTION =====
        Label descTitle = new Label("DESCRIPTION");
        descTitle.setStyle("-fx-font-size:11px; -fx-text-fill:#6d2269; -fx-font-weight:bold;");

        Label descValue = new Label(c.getDescription());
        descValue.setWrapText(true);
        descValue.setStyle("-fx-font-size:13px; -fx-text-fill:#111111;");

        VBox descBox = new VBox(3, descTitle, descValue);

        // ===== RÉPONSE RH =====
        Label repTitle = new Label("RÉPONSE RH");
        repTitle.setStyle("-fx-font-size:11px; -fx-text-fill:#6d2269; -fx-font-weight:bold;");

        Label repValue = new Label(rep);
        repValue.setWrapText(true);
        repValue.setStyle("-fx-font-size:13px; -fx-text-fill:#111111;");

        VBox repBox = new VBox(3, repTitle, repValue);

        // ===== TOP ROW =====
        HBox topRow = new HBox(40, typeBox, dateBox, statutBadge);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // ===== BOUTONS ICONES =====
        Button edit = new Button("✏");
        edit.setStyle(
                "-fx-background-color:#000000;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:15px;" +
                        "-fx-background-radius:50;" +
                        "-fx-min-width:36;" +
                        "-fx-min-height:36;" +
                        "-fx-cursor:hand;"
        );

        Button del = new Button("🗑");
        del.setStyle(
                "-fx-background-color:#dc2626;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:15px;" +
                        "-fx-background-radius:50;" +
                        "-fx-min-width:36;" +
                        "-fx-min-height:36;" +
                        "-fx-cursor:hand;"
        );

        edit.setDisable(!"EN_ATTENTE".equals(c.getStatut()));
        del.setDisable(!"EN_ATTENTE".equals(c.getStatut()));

        edit.setOnAction(e -> {
            congeEnModification = c;
            cbType.setValue(c.getTypeConge());
            dpDebut.setValue(c.getDateDebut());
            dpFin.setValue(c.getDateFin());
            taDescription.setText(c.getDescription());
        });

        del.setOnAction(e -> {

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Suppression d'une demande");
            confirmation.setContentText("Voulez-vous vraiment supprimer cette demande ?");

            ButtonType btnOui = new ButtonType("Oui", ButtonBar.ButtonData.YES);
            ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            confirmation.getButtonTypes().setAll(btnOui, btnNon);

            confirmation.showAndWait().ifPresent(response -> {

                if (response == btnOui) {
                    try {
                        congeService.deleteEntity(c);
                        refresh();
                        lblMsg.setText("✅ Demande supprimée");
                        lblMsg.setStyle("-fx-text-fill:#dc2626;");
                    } catch (SQLException ex) {
                        lblMsg.setText("❌ Erreur suppression");
                        lblMsg.setStyle("-fx-text-fill:#dc2626;");
                    }
                }

            });

        });


        HBox actionBox = new HBox(12, edit, del);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottomRow = new HBox(spacer, actionBox);

        // ===== ASSEMBLAGE =====
        card.getChildren().addAll(
                topRow,
                new Separator(),
                descBox,
                repBox,
                bottomRow
        );

        return card;
    }




    private void clearForm() {
        cbType.setValue(null);
        dpDebut.setValue(null);
        dpFin.setValue(null);
        taDescription.clear();
        lblMsg.setText("");
    }
}
