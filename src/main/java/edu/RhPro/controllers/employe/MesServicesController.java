package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Reponse;
import edu.RhPro.entities.Service;
import edu.RhPro.services.ReponseService;
import edu.RhPro.services.ServiceService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MesServicesController {

    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private Label lblMsg;

    @FXML private Label lblTitreError;
    @FXML private Label lblDescriptionError;

    @FXML private VBox cardsContainer;

    private final ServiceService serviceService = new ServiceService();
    private final ReponseService reponseService = new ReponseService();

    private Service serviceEnModification = null;
    private List<Service> allServices;

    private final String normalStyle =
            "-fx-border-color:#ddd6fe; -fx-border-width:1; -fx-border-radius:12; -fx-background-radius:12;";

    private final String errorStyle =
            "-fx-border-color:red; -fx-border-width:2; -fx-border-radius:12; -fx-background-radius:12;";

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
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
        tfTitre.textProperty().addListener((obs,o,n) -> {
            if(n != null && !n.trim().isEmpty()) {
                hideError(tfTitre, lblTitreError);
            }
        });

        taDescription.textProperty().addListener((obs,o,n) -> {
            if(n != null && n.trim().length() >= 5 && n.length() <= 500) {
                hideError(taDescription, lblDescriptionError);
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        if(tfTitre.getText() == null || tfTitre.getText().trim().isEmpty()) {
            showError(tfTitre, lblTitreError, "Le titre est obligatoire");
            isValid = false;
        } else {
            hideError(tfTitre, lblTitreError);
        }

        String desc = taDescription.getText();
        if(desc == null || desc.trim().length() < 5) {
            showError(taDescription, lblDescriptionError, "Description minimum 5 caractères");
            isValid = false;
        } else if(desc.length() > 500) {
            showError(taDescription, lblDescriptionError, "Description maximum 500 caractères");
            isValid = false;
        } else {
            hideError(taDescription, lblDescriptionError);
        }

        return isValid;
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

    @FXML
    private void onSubmit() {
        lblMsg.setText("");
        if(!validateForm()) {
            lblMsg.setText("⚠ Corrigez les erreurs");
            lblMsg.setStyle("-fx-text-fill:#f59e0b;");
            return;
        }

        try {
            long empId = Session.getCurrentUser().getId();

            if(serviceEnModification == null) {
                Service s = new Service(
                        tfTitre.getText().trim(),
                        taDescription.getText().trim(),
                        LocalDate.now(),
                        "EN_ATTENTE",
                        empId
                );
                serviceService.addEntity(s);
                lblMsg.setText("✅ Demande envoyée");
            } else {
                serviceEnModification.setTitre(tfTitre.getText().trim());
                serviceEnModification.setDescription(taDescription.getText().trim());
                serviceService.updateEntity(serviceEnModification);
                lblMsg.setText("✅ Demande modifiée");
                serviceEnModification = null;
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
            cardsContainer.getChildren().clear();
            long empId = Session.getCurrentUser().getId();
            allServices = serviceService.findByEmployeId(empId);

            for(Service s : allServices) {
                Reponse rep = reponseService.getOneByServiceId(s.getId());
                String repTxt = (rep == null) ? "-" : rep.getDecision() + (rep.getCommentaire()!=null ? " | "+rep.getCommentaire() : "");

                VBox card = createCard(s, repTxt);
                cardsContainer.getChildren().add(card);
                animateNode(card);
            }

        } catch(SQLException e) {
            lblMsg.setText("❌ Erreur chargement");
        }
    }

    private VBox createCard(Service s, String repTxt) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:20;" +
                        "-fx-border-color:#ddd6fe;" +
                        "-fx-border-width:1.5;" +
                        "-fx-border-radius:20;"
        );

        // TITRE
        Label titleLbl = new Label("TITRE");
        titleLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#6d2269; -fx-font-weight:bold;");
        Label titleVal = new Label(s.getTitre());
        titleVal.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:black;");
        VBox titleBox = new VBox(3, titleLbl, titleVal);

        // STATUT BADGE
        Label statutBadge = new Label(s.getStatut());
        statutBadge.setStyle(
                "-fx-padding:6 14;" +
                        "-fx-background-radius:30;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:white;"
        );
        String statut = s.getStatut();
        if (statut == null) statut = "";
        statut = statut.trim().toLowerCase();

        if (statut.contains("attente")) {
            statutBadge.setStyle(statutBadge.getStyle() + "-fx-background-color:#6d2269;"); // violet
        }
// TRAITEE ou ACCEPTÉE → vert
        else if (statut.contains("traitée") || statut.contains("acceptee") || statut.contains("accept")) {
            statutBadge.setText("ACCEPTÉE"); // force le texte du badge
            statutBadge.setStyle(statutBadge.getStyle() + "-fx-background-color:#059669;"); // vert
        }
        else if (statut.contains("refus")) {
            statutBadge.setStyle(statutBadge.getStyle() + "-fx-background-color:#dc2626;"); // rouge
        }
        else {
            statutBadge.setStyle(statutBadge.getStyle() + "-fx-background-color:gray;");
        }



        // DATE DE LA DEMANDE
        Label dateLbl = new Label("DATE DE LA DEMANDE");
        dateLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#6d2269; -fx-font-weight:bold;");
        // ⚡ Utiliser le getter exact de ton entité
        Label dateVal = new Label(s.getDateDemande().format(formatter));
        dateVal.setStyle("-fx-font-size:13px; -fx-text-fill:#111111;");
        VBox dateBox = new VBox(3, dateLbl, dateVal);

        // DESCRIPTION
        Label descTitle = new Label("DESCRIPTION");
        descTitle.setStyle("-fx-font-size:11px; -fx-text-fill:#6d2269; -fx-font-weight:bold;");
        Label descVal = new Label(s.getDescription());
        descVal.setWrapText(true);
        descVal.setStyle("-fx-font-size:13px; -fx-text-fill:#111111;");
        VBox descBox = new VBox(3, descTitle, descVal);

        // REPONSE RH
        Label repTitle = new Label("RÉPONSE RH");
        repTitle.setStyle("-fx-font-size:11px; -fx-text-fill:#6d2269; -fx-font-weight:bold;");
        Label repVal = new Label(repTxt);
        repVal.setWrapText(true);
        repVal.setStyle("-fx-font-size:13px; -fx-text-fill:#111111;");
        VBox repBox = new VBox(3, repTitle, repVal);

        // TOP ROW
        HBox topRow = new HBox(40, titleBox, statutBadge);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // BOUTONS
        Button edit = new Button("✏");
        Button del = new Button("🗑");

        edit.setStyle(
                "-fx-background-color:#000000;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:15;" +
                        "-fx-background-radius:50;" +
                        "-fx-min-width:36;" +
                        "-fx-min-height:36;" +
                        "-fx-cursor:hand;"
        );
        del.setStyle(
                "-fx-background-color:#dc2626;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:15;" +
                        "-fx-background-radius:50;" +
                        "-fx-min-width:36;" +
                        "-fx-min-height:36;" +
                        "-fx-cursor:hand;"
        );

        edit.setDisable(!"EN_ATTENTE".equals(s.getStatut()));
        del.setDisable(!"EN_ATTENTE".equals(s.getStatut()));

        edit.setOnAction(e -> {
            serviceEnModification = s;
            tfTitre.setText(s.getTitre());
            taDescription.setText(s.getDescription());
        });

        del.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Suppression d'une demande");
            confirm.setContentText("Voulez-vous vraiment supprimer cette demande ?");

            Optional<ButtonType> res = confirm.showAndWait();
            if(res.isPresent() && res.get() == ButtonType.OK) {
                try {
                    serviceService.deleteEntity(s);
                    refresh();
                    lblMsg.setText("✅ Demande supprimée");
                    lblMsg.setStyle("-fx-text-fill:#dc2626;");
                } catch(SQLException ex) {
                    lblMsg.setText("❌ Erreur suppression");
                    lblMsg.setStyle("-fx-text-fill:#dc2626;");
                }
            }
        });

        HBox actionBox = new HBox(12, edit, del);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomRow = new HBox(spacer, actionBox);

        // ASSEMBLAGE
        card.getChildren().addAll(
                topRow,
                new Separator(),
                dateBox,   // ✅ Date de la demande
                descBox,
                repBox,
                bottomRow
        );

        return card;
    }

    private void clearForm() {
        tfTitre.clear();
        taDescription.clear();
        lblMsg.setText("");
    }
}
