package edu.RhPro.controllers.rh;

import edu.RhPro.entities.offreEmploi;
import edu.RhPro.services.OffreEmploiService;
import edu.RhPro.utils.Session;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
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

    // Autocomplete localisation
    private final ContextMenu locSuggestionsMenu = new ContextMenu();
    private final PauseTransition locDebounce = new PauseTransition(Duration.millis(350));

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

        setupLocationAutocomplete(locField);

        clearAllErrors();
        addValidationListeners();
        refresh();
    }

    private void setupLocationAutocomplete(TextField field) {
        // Debounce: attend 350ms après la dernière frappe avant d'appeler l'API
        locDebounce.setOnFinished(e -> {
            String text = field.getText();
            if (text == null || text.trim().length() < 3) {
                Platform.runLater(locSuggestionsMenu::hide);
                return;
            }
            fetchLocations(text.trim());
        });

        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                locDebounce.stop();
                locSuggestionsMenu.hide();
                return;
            }
            locDebounce.stop();
            locDebounce.playFromStart();
        });

        // Fermer le menu quand le TextField perd le focus
        field.focusedProperty().addListener((obs, oldFocused, focused) -> {
            if (!focused) {
                locSuggestionsMenu.hide();
            }
        });
    }

    private void fetchLocations(String query) {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                String urlStr = "https://nominatim.openstreetmap.org/search?q="
                        + URLEncoder.encode(query, StandardCharsets.UTF_8)
                        + "&format=json&addressdetails=1&limit=8"
                        + "&countrycodes=tn"
                        + "&accept-language=fr"
                        + "&dedupe=1";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // User-Agent obligatoire pour Nominatim
                conn.setRequestProperty("User-Agent", "RHPro-JavaFX/1.0 (mohamed-idriss)");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (InputStream is = conn.getInputStream();
                     BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    JSONArray arr = new JSONArray(sb.toString());
                    List<String> results = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);

                        // 1) Filtrer types pour garder lieux utiles
                        String type = obj.optString("type", "");
                        if (!(type.equals("city") || type.equals("town") || type.equals("village")
                                || type.equals("administrative") || type.equals("suburb")
                                || type.equals("neighbourhood") || type.equals("residential"))) {
                            continue;
                        }

                        org.json.JSONObject address = obj.optJSONObject("address");

                        // 2) Construire un label court et précis : quartier + ville + gouvernorat
                        String suburb = (address != null)
                                ? address.optString("suburb", address.optString("neighbourhood", ""))
                                : "";

                        String city = (address != null)
                                ? address.optString("city",
                                address.optString("town",
                                        address.optString("village",
                                                address.optString("municipality", ""))))
                                : "";

                        // fallback si city vide
                        if ((city == null || city.isBlank()) && address != null) {
                            city = address.optString("county", "");
                        }

                        String state = (address != null) ? address.optString("state", "") : "";

                        // 3) Si rien trouvé, fallback display_name
                        String label;
                        if (!suburb.isBlank() && !city.isBlank()) {
                            label = suburb + ", " + city;
                        } else if (!city.isBlank()) {
                            label = city;
                        } else {
                            label = obj.optString("display_name", "");
                        }

                        // 4) Ajouter gouvernorat (state) si disponible
                        if (!state.isBlank() && !label.contains(state)) {
                            label = label + " (" + state + ")";
                        }

                        // 5) Nettoyage + éviter doublons
                        label = label.trim();
                        if (!label.isBlank() && !results.contains(label)) {
                            results.add(label);
                        }
                    }

                    return results;
                } finally {
                    conn.disconnect();
                }
            }
        };

        task.setOnSucceeded(ev -> {
            List<String> suggestions = task.getValue();
            Platform.runLater(() -> showLocationSuggestions(locField, suggestions));
        });

        task.setOnFailed(ev -> Platform.runLater(locSuggestionsMenu::hide));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showLocationSuggestions(TextField field, List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty() || !field.isFocused()) {
            locSuggestionsMenu.hide();
            return;
        }

        List<CustomMenuItem> menuItems = new ArrayList<>();
        for (String suggestion : suggestions) {
            Label lbl = new Label(suggestion);
            lbl.setWrapText(true);
            lbl.setMaxWidth(400);

            CustomMenuItem item = new CustomMenuItem(lbl, true);
            item.setOnAction(e -> {
                field.setText(suggestion);
                field.positionCaret(field.getText().length());
                locSuggestionsMenu.hide();
            });
            menuItems.add(item);
        }

        locSuggestionsMenu.getItems().setAll(menuItems);

// ✅ forcer refresh
        if (locSuggestionsMenu.isShowing()) {
            locSuggestionsMenu.hide();
        }
        locSuggestionsMenu.show(field, Side.BOTTOM, 0, 0);
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
            o.setRhId(getCurrentRhId()); // rh_id obligatoire
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

            long rhId = selected.getRhId();
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

    // ✅ STATS EN BAR CHART (comme ton image)
    @FXML
    public void onShowStats() {
        try { refresh(); } catch (Exception ignored) {}

        long ouvertes = data.stream()
                .filter(o -> normalizeStatut(o.getStatut()).equals("OUVERTE"))
                .count();

        long fermees = data.stream()
                .filter(o -> {
                    String s = normalizeStatut(o.getStatut());
                    return s.equals("FERMEE") || s.equals("FERMÉE");
                })
                .count();

        long total  = data.size();
        long autres = total - ouvertes - fermees;

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Statut");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Nombre d'offres");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Statistiques des offres");
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(25); // ✅ OK

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("OUVERTE", ouvertes));
        series.getData().add(new XYChart.Data<>("FERMEE", fermees));
        series.getData().add(new XYChart.Data<>("AUTRES", autres));
        barChart.getData().add(series);

        Button close = new Button("Fermer");
        close.setStyle("-fx-background-color:#111827; -fx-text-fill:white; -fx-background-radius:10; -fx-padding:8 16;");

        VBox root = new VBox(12, barChart, close);
        root.setStyle("-fx-padding:14; -fx-background-color:white;");

        Stage st = new Stage();
        st.setTitle("Stats Offres");
        st.initModality(Modality.APPLICATION_MODAL);
        st.setResizable(false);
        st.setScene(new Scene(root, 520, 380));

        close.setOnAction(e -> st.close());
        st.showAndWait();
    }

    // ✅ UNE SEULE méthode (pas 2) !
    private String normalizeStatut(String s) {
        return (s == null) ? "" : s.trim().toUpperCase();
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

        if (resetStatut) statutBox.getSelectionModel().select("OUVERTE");
        else statutBox.setValue(null);

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

    private long getCurrentRhId() {
        try {
            if (Session.getCurrentUser() != null) {
                long id = Session.getCurrentUser().getId();
                if (id > 0) return id;
            }
        } catch (Exception ignored) {}
        return 1L;
    }
}
