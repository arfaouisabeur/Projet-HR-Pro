package edu.RhPro.controllers.rh;

import edu.RhPro.services.GeoapifyService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MapPickerController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button confirmButton;
    @FXML private Button cancelButton;
    @FXML private Label coordinatesLabel;
    @FXML private Label locationNameLabel;
    @FXML private ImageView mapImageView;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;
    @FXML private ListView<String> suggestionsList;
    @FXML private VBox suggestionsContainer;
    @FXML private Hyperlink openInBrowserLink;

    private GeoapifyService mapService = new GeoapifyService();
    private double selectedLat = 0;
    private double selectedLon = 0;
    private String selectedLocationName = "";
    private LocationPickListener listener;
    private List<GeoapifyService.Location> searchResults;
    private Task<?> currentSearchTask;

    // New fields for existing location
    private double initialLat = 0;
    private double initialLon = 0;
    private String initialLocationName = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupSearch();
        setupSuggestions();

        suggestionsContainer.setVisible(false);
        suggestionsContainer.setManaged(false);

        // Setup browser link
        openInBrowserLink.setOnAction(e -> {
            if (selectedLat != 0 && selectedLon != 0) {
                try {
                    String urlStr = mapService.getWebMapLink(selectedLat, selectedLon);
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(urlStr));
                } catch (Exception ex) {
                    showError("Impossible d'ouvrir le navigateur");
                }
            }
        });

        // Initially disable confirm button
        confirmButton.setDisable(true);

        // Set placeholder for map
        statusLabel.setText("Recherchez un lieu pour voir la carte");
    }

    private void setupSearch() {
        searchButton.setOnAction(e -> performSearch());
        searchField.setOnAction(e -> performSearch());

        // Real-time search as user types
        searchField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && newVal.length() > 2) {
                performSearchWithDelay();
            } else {
                suggestionsContainer.setVisible(false);
                suggestionsContainer.setManaged(false);
            }
        });
    }

    private void setupSuggestions() {
        suggestionsList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && searchResults != null) {
                int index = suggestionsList.getSelectionModel().getSelectedIndex();
                if (index >= 0 && index < searchResults.size()) {
                    GeoapifyService.Location location = searchResults.get(index);
                    selectLocation(location);
                    suggestionsContainer.setVisible(false);
                    suggestionsContainer.setManaged(false);
                }
            }
        });
    }

    /**
     * Set existing location to show when opening the picker
     */
    public void setExistingLocation(double lat, double lon, String locationName) {
        this.initialLat = lat;
        this.initialLon = lon;
        this.initialLocationName = locationName;

        if (lat != 0 && lon != 0 && locationName != null && !locationName.isEmpty()) {
            Platform.runLater(() -> {
                selectedLat = lat;
                selectedLon = lon;
                selectedLocationName = locationName;

                // Update UI
                searchField.setText(locationName);
                updateLocationInfo();
                loadStaticMap();

                // Enable confirm button
                confirmButton.setDisable(false);

                // Update status
                statusLabel.setText("Location existante chargée");
            });
        }
    }

    private void performSearchWithDelay() {
        // Cancel previous search task if running
        if (currentSearchTask != null && currentSearchTask.isRunning()) {
            currentSearchTask.cancel();
        }

        Task<Void> delayTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(500);
                return null;
            }
        };

        delayTask.setOnSucceeded(e -> {
            if (!searchField.getText().trim().isEmpty()) {
                performSearch();
            }
        });

        currentSearchTask = delayTask;
        new Thread(delayTask).start();
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        showLoading(true);
        updateStatus("Recherche en cours...");

        Task<List<GeoapifyService.Location>> searchTask = new Task<>() {
            @Override
            protected List<GeoapifyService.Location> call() throws Exception {
                return mapService.searchLocations(query);
            }
        };

        searchTask.setOnSucceeded(e -> {
            showLoading(false);
            searchResults = searchTask.getValue();

            if (searchResults != null && !searchResults.isEmpty()) {
                // Populate suggestions list
                suggestionsList.getItems().clear();
                for (GeoapifyService.Location loc : searchResults) {
                    suggestionsList.getItems().add(loc.getDisplayName());
                }
                suggestionsContainer.setVisible(true);
                suggestionsContainer.setManaged(true);

                // Auto-select first result
                selectLocation(searchResults.get(0));
            } else {
                updateStatus("Aucun résultat trouvé");
                suggestionsContainer.setVisible(false);
                suggestionsContainer.setManaged(false);
                clearMap();
            }
        });

        searchTask.setOnFailed(e -> {
            showLoading(false);
            updateStatus("Erreur de recherche");
            suggestionsContainer.setVisible(false);
            suggestionsContainer.setManaged(false);
            e.getSource().getException().printStackTrace();
            showError("Erreur lors de la recherche");
        });

        currentSearchTask = searchTask;
        new Thread(searchTask).start();
    }

    private void selectLocation(GeoapifyService.Location location) {
        selectedLat = location.getLat();
        selectedLon = location.getLon();
        selectedLocationName = location.getDisplayName();

        updateLocationInfo();
        loadStaticMap();
    }

    private void loadStaticMap() {
        if (selectedLat == 0 && selectedLon == 0) return;

        showLoading(true);
        updateStatus("Chargement de la carte...");

        double lat = selectedLat;
        double lon = selectedLon;

        Task<Image> mapTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                // Try zoom 15 first, then 13 as fallback
                Image img = mapService.loadStaticMap(lat, lon, 700, 350, 15);
                if (img == null || img.isError()) {
                    img = mapService.loadStaticMap(lat, lon, 700, 350, 13);
                }
                return img;
            }
        };

        mapTask.setOnSucceeded(e -> {
            showLoading(false);
            Image mapImage = mapTask.getValue();

            if (mapImage != null && !mapImage.isError()) {
                mapImageView.setImage(mapImage);
                mapImageView.setFitWidth(700);
                mapImageView.setFitHeight(350);
                mapImageView.setPreserveRatio(true);
                updateStatus("✅ Localisation " + (initialLat != 0 ? "chargée" : "sélectionnée"));
                openInBrowserLink.setVisible(true);
                openInBrowserLink.setManaged(true);
            } else {
                updateStatus("⚠ Carte non disponible — cliquez sur le lien ci-dessous pour voir dans le navigateur");
                openInBrowserLink.setVisible(true);
                openInBrowserLink.setManaged(true);
            }
        });

        mapTask.setOnFailed(e -> {
            showLoading(false);
            Throwable ex = e.getSource().getException();
            System.out.println("Map task failed: " + (ex != null ? ex.getMessage() : "unknown"));
            updateStatus("⚠ Impossible de charger la carte — utilisez le lien ci-dessous");
            openInBrowserLink.setVisible(true);
            openInBrowserLink.setManaged(true);
        });

        new Thread(mapTask).start();
    }

    private void updateLocationInfo() {
        coordinatesLabel.setText(String.format("Lat: %.6f, Lon: %.6f", selectedLat, selectedLon));
        locationNameLabel.setText(selectedLocationName);
        confirmButton.setDisable(false);
    }

    private void clearMap() {
        mapImageView.setImage(null);
        coordinatesLabel.setText("Aucun lieu sélectionné");
        locationNameLabel.setText("");
        openInBrowserLink.setVisible(false);
        openInBrowserLink.setManaged(false);
        confirmButton.setDisable(true);
    }

    private void showLoading(boolean loading) {
        progressIndicator.setVisible(loading);
        progressIndicator.setManaged(loading);
        mapImageView.setVisible(!loading);
        mapImageView.setManaged(!loading);
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void confirmLocation() {
        if (listener != null && selectedLat != 0 && selectedLon != 0) {
            listener.onLocationPicked(selectedLat, selectedLon, selectedLocationName);
        }
        close();
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        stage.close();
    }

    public void setLocationPickListener(LocationPickListener listener) {
        this.listener = listener;
    }

    public interface LocationPickListener {
        void onLocationPicked(double lat, double lon, String locationName);
    }
}