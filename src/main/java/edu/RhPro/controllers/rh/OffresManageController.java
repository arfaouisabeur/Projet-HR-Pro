package edu.RhPro.controllers.rh;

import edu.RhPro.entities.offreEmploi;
import edu.RhPro.services.OffreEmploiService;
import edu.RhPro.utils.Session;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.List;

public class OffresManageController {

    @FXML private TableView<offreEmploi> offreTable;

    @FXML private TableColumn<offreEmploi, Integer> colId;
    @FXML private TableColumn<offreEmploi, String> colTitre;
    @FXML private TableColumn<offreEmploi, String> colLoc;
    @FXML private TableColumn<offreEmploi, String> colType;
    @FXML private TableColumn<offreEmploi, LocalDate> colPub;
    @FXML private TableColumn<offreEmploi, LocalDate> colExp;
    @FXML private TableColumn<offreEmploi, String> colStatut;

    @FXML private TextField titreField;
    @FXML private TextField locField;
    @FXML private TextField typeField;
    @FXML private DatePicker pubPicker;
    @FXML private DatePicker expPicker;
    @FXML private ComboBox<String> statutBox;
    @FXML private TextArea descArea;
    @FXML private Label msgLabel;

    @FXML private Label titreErrorLabel;
    @FXML private Label locErrorLabel;
    @FXML private Label typeErrorLabel;
    @FXML private Label pubErrorLabel;
    @FXML private Label expErrorLabel;
    @FXML private Label statutErrorLabel;
    @FXML private Label descErrorLabel;

    private final OffreEmploiService service = new OffreEmploiService();
    private final ObservableList<offreEmploi> data = FXCollections.observableArrayList();

    private final String normalFieldStyle =
            "-fx-border-color: #ececf5; -fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;";
    private final String errorFieldStyle =
            "-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 14; -fx-background-radius: 14;";

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colLoc.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeContrat"));
        colPub.setCellValueFactory(new PropertyValueFactory<>("datePublication"));
        colExp.setCellValueFactory(new PropertyValueFactory<>("dateExpiration"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        offreTable.setItems(data);

        statutBox.setItems(FXCollections.observableArrayList("OUVERTE", "FERMEE"));
        statutBox.getSelectionModel().select("OUVERTE");

        offreTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) fillForm(selected);
        });

        clearAllErrors();
        addValidationListeners();
        refresh();
    }

    @FXML
    public void refresh() {
        try {
            List<offreEmploi> list = service.getAll();
            data.setAll(list);
            setInfo("✅ " + list.size() + " offre(s).");
        } catch (Exception e) {
            e.printStackTrace();
            setError("❌ Erreur chargement : " + e.getMessage());
        }
    }

    @FXML
    public void onAdd() {
        if (!validateForm()) {
            setWarn("⚠️ Corrigez les champs en rouge.");
            return;
        }

        try {
            offreEmploi o = buildFromForm(null);

            // ✅ rh_id obligatoire
            o.setRhId(getCurrentRhId());

            service.add(o);

            clearForm(true);
            refresh();
            setSuccess("✔ Offre ajoutée.");
        } catch (Exception e) {
            e.printStackTrace();
            setError("❌ Ajout impossible : " + e.getMessage());
        }
    }

    @FXML
    public void onUpdate() {
        offreEmploi selected = offreTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setWarn("⚠️ Sélectionnez une offre à modifier.");
            return;
        }

        if (!validateForm()) {
            setWarn("⚠️ Corrigez les champs en rouge.");
            return;
        }

        try {
            offreEmploi o = buildFromForm(selected.getId());

            // ✅ garder rh_id existant si valide, sinon prendre celui du user connecté
            long rhId = selected.getRhId(); // <-- ici rhId est long, donc pas de null
            if (rhId <= 0) rhId = getCurrentRhId();
            o.setRhId(rhId);

            service.update(o);

            refresh();
            setSuccess("✔ Offre modifiée.");
        } catch (Exception e) {
            e.printStackTrace();
            setError("❌ Modification impossible : " + e.getMessage());
        }
    }

    @FXML
    public void onDelete() {
        offreEmploi selected = offreTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setWarn("⚠️ Sélectionnez une offre à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer l'offre ID " + selected.getId() + " ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            service.delete(selected.getId());
            clearForm(true);
            refresh();
            setSuccess("✔ Offre supprimée.");
        } catch (Exception e) {
            e.printStackTrace();
            setError("❌ Suppression impossible : " + e.getMessage());
        }
    }

    @FXML
    public void onClose() {
        clearForm(true);
        setInfo("");
    }

    @FXML
    public void onShowStats() {
        // ✅ on s’assure d’avoir les données à jour
        try { refresh(); } catch (Exception ignored) {}

        long ouvertes = data.stream().filter(o -> "OUVERTE".equalsIgnoreCase(o.getStatut())).count();
        long fermees  = data.stream().filter(o -> "FERMEE".equalsIgnoreCase(o.getStatut())).count();

        Label title = new Label("Statistiques des offres");
        title.setStyle("-fx-font-size:16; -fx-font-weight:900;");

        Label l1 = new Label("OUVERTES : " + ouvertes);
        Label l2 = new Label("FERMÉES  : " + fermees);
        Label l3 = new Label("TOTAL    : " + data.size());
        l3.setStyle("-fx-font-weight:bold;");

        Button close = new Button("Fermer");
        close.setDefaultButton(true);

        VBox box = new VBox(12, title, l1, l2, l3, close);
        box.setStyle("-fx-padding:18; -fx-background-color:white;");

        Stage st = new Stage();
        st.setTitle("Stats Offres");
        st.initModality(Modality.APPLICATION_MODAL);
        st.setScene(new Scene(box, 300, 210));

        close.setOnAction(e -> st.close());
        st.showAndWait();
    }

    private offreEmploi buildFromForm(Integer id) {
        offreEmploi o = new offreEmploi();
        if (id != null) o.setId(id);

        o.setTitre(titreField.getText().trim());
        o.setLocalisation(locField.getText().trim());
        o.setTypeContrat(typeField.getText().trim());
        o.setDatePublication(pubPicker.getValue());
        o.setDateExpiration(expPicker.getValue());
        o.setStatut(statutBox.getValue());
        o.setDescription(descArea.getText().trim());
        return o;
    }

    private void fillForm(offreEmploi o) {
        titreField.setText(o.getTitre());
        locField.setText(o.getLocalisation());
        typeField.setText(o.getTypeContrat());
        pubPicker.setValue(o.getDatePublication());
        expPicker.setValue(o.getDateExpiration());

        if (o.getStatut() == null || o.getStatut().trim().isEmpty()) {
            statutBox.getSelectionModel().select("OUVERTE");
        } else {
            statutBox.setValue(o.getStatut());
        }

        descArea.setText(o.getDescription());
        clearAllErrors();
    }

    private void clearForm(boolean resetStatut) {
        titreField.clear();
        locField.clear();
        typeField.clear();
        pubPicker.setValue(null);
        expPicker.setValue(null);
        descArea.clear();
        offreTable.getSelectionModel().clearSelection();

        if (resetStatut) {
            statutBox.getSelectionModel().select("OUVERTE");
        } else {
            statutBox.setValue(null);
        }
        clearAllErrors();
    }

    private void addValidationListeners() {
        titreField.textProperty().addListener((obs, o, v) -> {
            if (v != null && v.trim().length() >= 5) hideError(titreField, titreErrorLabel);
        });
        locField.textProperty().addListener((obs, o, v) -> {
            if (v != null && !v.trim().isEmpty()) hideError(locField, locErrorLabel);
        });
        typeField.textProperty().addListener((obs, o, v) -> {
            if (v != null && v.trim().length() >= 2) hideError(typeField, typeErrorLabel);
        });
        descArea.textProperty().addListener((obs, o, v) -> {
            if (v != null && v.trim().length() >= 10 && !v.matches(".*\\d.*")) {
                hideError(descArea, descErrorLabel);
            }
        });
        statutBox.valueProperty().addListener((obs, o, v) -> {
            if (v != null) hideError(statutBox, statutErrorLabel);
        });
        pubPicker.valueProperty().addListener((obs, o, v) -> {
            if (v != null) {
                hideError(pubPicker, pubErrorLabel);
                validateDateOrder();
            }
        });
        expPicker.valueProperty().addListener((obs, o, v) -> {
            if (v != null) {
                hideError(expPicker, expErrorLabel);
                validateDateOrder();
            }
        });
    }

    private void validateDateOrder() {
        LocalDate pub = pubPicker.getValue();
        LocalDate exp = expPicker.getValue();
        if (pub != null && exp != null) {
            if (!exp.isAfter(pub)) showError(expPicker, expErrorLabel, "Expiration doit être après publication.");
            else hideError(expPicker, expErrorLabel);
        }
    }

    private boolean validateForm() {
        boolean ok = true;

        String titre = titreField.getText();
        if (titre == null || titre.trim().length() < 5) { showError(titreField, titreErrorLabel, "Titre: minimum 5 caractères."); ok = false; }
        else hideError(titreField, titreErrorLabel);

        String loc = locField.getText();
        if (loc == null || loc.trim().isEmpty()) { showError(locField, locErrorLabel, "Localisation obligatoire."); ok = false; }
        else hideError(locField, locErrorLabel);

        String type = typeField.getText();
        if (type == null || type.trim().length() < 2) { showError(typeField, typeErrorLabel, "Type contrat obligatoire."); ok = false; }
        else hideError(typeField, typeErrorLabel);

        if (pubPicker.getValue() == null) { showError(pubPicker, pubErrorLabel, "Choisir une date publication."); ok = false; }
        else hideError(pubPicker, pubErrorLabel);

        LocalDate pub = pubPicker.getValue();
        LocalDate exp = expPicker.getValue();
        if (exp == null) { showError(expPicker, expErrorLabel, "Choisir une date expiration."); ok = false; }
        else if (pub != null && !exp.isAfter(pub)) { showError(expPicker, expErrorLabel, "Expiration doit être après publication."); ok = false; }
        else hideError(expPicker, expErrorLabel);

        if (statutBox.getValue() == null) { showError(statutBox, statutErrorLabel, "Sélectionner un statut."); ok = false; }
        else hideError(statutBox, statutErrorLabel);

        String desc = descArea.getText();
        if (desc == null || desc.trim().length() < 10) { showError(descArea, descErrorLabel, "Description: minimum 10 caractères."); ok = false; }
        else if (desc.matches(".*\\d.*")) { showError(descArea, descErrorLabel, "Description: chiffres interdits."); ok = false; }
        else hideError(descArea, descErrorLabel);

        return ok;
    }

    private void showError(Control field, Label errorLabel, String message) {
        field.setStyle(errorFieldStyle);
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError(Control field, Label errorLabel) {
        field.setStyle(normalFieldStyle);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void clearAllErrors() {
        hideError(titreField, titreErrorLabel);
        hideError(locField, locErrorLabel);
        hideError(typeField, typeErrorLabel);
        hideError(pubPicker, pubErrorLabel);
        hideError(expPicker, expErrorLabel);
        hideError(statutBox, statutErrorLabel);
        hideError(descArea, descErrorLabel);
    }

    private void setSuccess(String s) { setMsg(s, "#16a34a", true); }
    private void setError(String s)   { setMsg(s, "#b91c1c", false); }
    private void setWarn(String s)    { setMsg(s, "#f59e0b", true); }
    private void setInfo(String s)    { setMsg(s, "#6b7280", false); }

    private void setMsg(String s, String color, boolean autoClear) {
        msgLabel.setText(s);
        msgLabel.setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold;");
        if (autoClear) autoClearMsg();
    }

    private void autoClearMsg() {
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> msgLabel.setText(""));
        pause.play();
    }

    // ✅ retourne toujours un long valide
    private long getCurrentRhId() {
        try {
            if (Session.getCurrentUser() != null) {
                long id = Session.getCurrentUser().getId(); // <-- idéalement getId() retourne long
                if (id > 0) return id;
            }
        } catch (Exception ignored) {}
        return 1L; // fallback
    }
}
