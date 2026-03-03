package edu.RhPro.controllers.rh;

import edu.RhPro.services.*;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.List;
import javafx.stage.FileChooser;
import java.io.File;

import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javafx.scene.control.TextArea;
import javafx.scene.control.Dialog;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

public class CandidaturesManageController {

    @FXML private TableView<CandidatureAdminRow> candTable;
    @FXML private TableColumn<CandidatureAdminRow, Integer> colId;
    @FXML private TableColumn<CandidatureAdminRow, LocalDate> colDate;
    @FXML private TableColumn<CandidatureAdminRow, String> colStatut;
    @FXML private TableColumn<CandidatureAdminRow, String> colCandidat;
    @FXML private TableColumn<CandidatureAdminRow, String> colEmail;
    @FXML private TableColumn<CandidatureAdminRow, String> colOffre;
    @FXML private TableColumn<CandidatureAdminRow, String> colLoc;
    @FXML private TableColumn<CandidatureAdminRow, String> colType;

    @FXML private ComboBox<String> statusBox;
    @FXML private Label msgLabel;
    @FXML private ComboBox<String> filterStatutBox;
    @FXML private TableColumn<CandidatureAdminRow, Integer> colScore;

    @FXML private TableColumn<CandidatureAdminRow, String> colCv;

    private final CandidatureService service = new CandidatureService();
    private final ObservableList<CandidatureAdminRow> data = FXCollections.observableArrayList();
    private final MatchingService matchingService = new MatchingService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCandidature"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colCandidat.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCandidatFullName()));
        colEmail.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCandidatEmail()));
        colOffre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOffreTitre()));
        colLoc.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOffreLocalisation()));
        colType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOffreTypeContrat()));
        colScore.setCellValueFactory(new PropertyValueFactory<>("matchScore"));
        candTable.setItems(data);

        statusBox.setItems(FXCollections.observableArrayList("ENVOYEE", "EN_COURS", "ACCEPTEE", "REFUSEE"));
        statusBox.getSelectionModel().select("EN_COURS");

        filterStatutBox.setItems(FXCollections.observableArrayList("TOUS", "ENVOYEE", "EN_COURS", "ACCEPTEE", "REFUSEE"));
        filterStatutBox.getSelectionModel().select("TOUS");
        colCv.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCvStatus()));

        refresh();
    }

    @FXML
    public void refresh() {
        try {
            List<CandidatureAdminRow> list = service.findAllForAdmin();
            data.setAll(list);
            msgLabel.setText("Total: " + list.size());
            msgLabel.setStyle("-fx-text-fill:#6b7280;");
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void applyStatus() {
        try {
            CandidatureAdminRow selected = candTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une candidature dans le tableau.");
                return;
            }

            String newStatus = statusBox.getValue();
            if (newStatus == null || newStatus.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Statut requis", "Veuillez choisir un statut.");
                return;
            }

            if (newStatus.equalsIgnoreCase(selected.getStatut())) {
                showAlert(Alert.AlertType.INFORMATION, "Aucun changement", "Cette candidature a déjà le statut : " + newStatus);
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText(null);
            confirm.setContentText("Changer le statut de la candidature ID " + selected.getId() + " vers : " + newStatus + " ?");
            ButtonType ok = new ButtonType("Oui", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(ok, cancel);

            if (confirm.showAndWait().orElse(cancel) != ok) {
                return;
            }

            service.updateStatus(selected.getId(), newStatus);

            // ✅ Message succès (Label)
            msgLabel.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:bold;");
            msgLabel.setText("✔ Statut modifié avec succès vers : " + newStatus);

            // ✅ Effacer le message après 3 secondes
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(event -> msgLabel.setText(""));
            pause.play();

            refresh();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le statut :\n" + e.getMessage());
        }
    }

    @FXML
    public void onFilter() {
        try {
            String selectedStatut = filterStatutBox.getValue();
            List<CandidatureAdminRow> list = service.findAllForAdmin();

            if (selectedStatut != null && !"TOUS".equals(selectedStatut)) {
                list = list.stream()
                        .filter(c -> selectedStatut.equalsIgnoreCase(c.getStatut()))
                        .toList();
            }

            data.setAll(list);

            msgLabel.setText("Total: " + list.size());
            msgLabel.setStyle("-fx-text-fill:#6b7280;");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de filtrer :\n" + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    @FXML
    public void onOpenCv() {
        CandidatureAdminRow selected = candTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Choisissez une candidature.");
            return;
        }

        if (selected.getCvPath() == null || selected.getCvPath().isBlank()) {
            showAlert(Alert.AlertType.INFORMATION, "CV", "Aucun CV pour cette candidature.");
            return;
        }

        service.openCvInApp(selected.getCvPath(), "CV - Candidature " + selected.getId());
    }
    private void openPdfInApp(File pdfFile, String title) {

        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        javafx.scene.web.WebEngine engine = webView.getEngine();

        // URL du fichier local
        String url = pdfFile.toURI().toString();

        engine.load(url);

        javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane(webView);
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 650);

        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void onDownloadCv() {
        try {
            CandidatureAdminRow selected = candTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Sélection", "Choisissez une candidature.");
                return;
            }

            if (selected.getCvPath() == null || selected.getCvPath().isBlank()) {
                showAlert(Alert.AlertType.INFORMATION, "CV", "Aucun CV pour cette candidature.");
                return;
            }

            FileChooser fc = new FileChooser();
            fc.setTitle("Télécharger le CV");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));

            String defaultName = (selected.getCvOriginalName() != null && !selected.getCvOriginalName().isBlank())
                    ? selected.getCvOriginalName()
                    : "cv_candidature_" + selected.getId() + ".pdf";
            fc.setInitialFileName(defaultName);

            File dest = fc.showSaveDialog(candTable.getScene().getWindow());
            if (dest == null) return;

            service.downloadCvTo(selected.getId(), dest);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "CV téléchargé.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }
    @FXML
    public void onComputeMatchingIA() {
        try {
            for (CandidatureAdminRow c : data) {

                if (c.getCvPath() == null || c.getCvPath().isBlank())
                    continue;

                // 1) Lire texte CV (pour l'instant simplifié)
                String cvText = c.getCvOriginalName(); // temporaire

                // 2) Construire texte offre
                String offreText = c.getOffreTitre() + " " +
                        c.getOffreTypeContrat() + " " +
                        c.getOffreLocalisation();

                // 3) Calcul score
                int score = matchingService.computeScore(cvText, offreText);

                // 4) Sauvegarder en DB
                service.updateMatchScore(c.getId(), score);
            }

            refresh();

            showAlert(Alert.AlertType.INFORMATION, "Matching",
                    "Scores calculés avec succès ✅");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }
    @FXML
    private void onSendContract() {

        // 1) récupérer la ligne sélectionnée
        CandidatureAdminRow selected = candTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Veuillez sélectionner un candidat.");
            return;
        }

        String nom = selected.getCandidatNom();     // adapte selon ton modèle
        String email = selected.getCandidatEmail(); // adapte selon ton modèle

        // 2) lancer l’envoi en background (important)
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                SignatureService sign = new SignatureService();
                return sign.sendContractByEmail(nom, email); // retourne signature_request_id
            }
        };


        task.setOnSucceeded(e -> {
            String reqId = task.getValue();

            try {
                service.updateSignatureRequestId(selected.getId(), reqId);
                refresh();
                candTable.refresh();
                showInfo("Contrat envoyé ✅\nID: " + reqId);
            } catch (Exception ex) {
                showError("Erreur sauvegarde ID : " + ex.getMessage());
            }
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showError("Erreur envoi contrat ❌\n" + (ex != null ? ex.getMessage() : ""));
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
    @FXML
    private void onCheckSignature() {

        CandidatureAdminRow selected = candTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Sélectionnez une candidature.");
            return;
        }

        String reqId = selected.getSignatureRequestId(); // à adapter selon ton modèle
        if (reqId == null || reqId.isBlank()) {
            showInfo("Aucun contrat envoyé pour cette candidature.");
            return;
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                SignatureService s = new SignatureService();
                return s.isContractSigned(reqId);
            }
        };

        task.setOnSucceeded(e -> {
            boolean signed = task.getValue();
            if (signed) {
                showInfo("✅ Contrat SIGNÉ !");
                // option : update DB => contract_status="SIGNED"
                // candidatureService.updateContractStatus(selected.getId(), "SIGNED");
            } else {
                showInfo("⏳ Pas encore signé.");
            }
        });

        task.setOnFailed(e -> showError("Erreur: " + task.getException().getMessage()));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
    @FXML
    public void onResumeCv() {

        CandidatureAdminRow selected =
                candTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Choisissez une candidature.");
            return;
        }

        CvSummaryService service = new CvSummaryService();

        String summary = service.summarize(selected.getCvPath());

        // zone texte pour afficher le résumé
        TextArea textArea = new TextArea(summary);
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPrefWidth(650);
        textArea.setPrefHeight(450);

        // bouton copier
        Button copyButton = new Button("Copier le résumé");

        copyButton.setOnAction(e -> {

            ClipboardContent content = new ClipboardContent();
            content.putString(summary);

            Clipboard.getSystemClipboard().setContent(content);

        });

        VBox layout = new VBox(10, textArea, copyButton);
        layout.setStyle("-fx-padding:15;");

        Dialog<Void> dialog = new Dialog<>();

        dialog.setTitle("Résumé du CV (Gemini)");
        dialog.getDialogPane().setContent(layout);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        dialog.showAndWait();
    }
    @FXML
    public void onAnalyseCv() {

        CandidatureAdminRow selected =
                candTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Choisissez une candidature.");
            return;
        }

        CvSummaryService service = new CvSummaryService();

        String analysis = service.analyze(selected.getCvPath());

        TextArea textArea = new TextArea(analysis);
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPrefWidth(650);
        textArea.setPrefHeight(450);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Analyse complète du CV");
        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        dialog.showAndWait();
    }
}
