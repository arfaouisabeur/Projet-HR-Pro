package edu.RhPro.controllers.auth;

import edu.RhPro.entities.User;
import edu.RhPro.services.CandidatService;
import edu.RhPro.services.EmployeService;
import edu.RhPro.services.RHService;
import edu.RhPro.services.UserService;
import edu.RhPro.utils.Router;
import edu.RhPro.utils.Session;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SignupController {

    @FXML
    private Label nomError, prenomError, emailError, passError,
            telError, adresseError,
            niveauError, expError,
            matriculeError, positionError, dateError;

    @FXML
    private Label roleTitle;
    @FXML
    private Label msgLabel;
    @FXML
    private Label flagLabel;

    @FXML
    private TextField nomField, prenomField, emailField, telField, adresseField;
    @FXML
    private PasswordField passField;

    @FXML
    private VBox candidatBox;
    @FXML
    private TextField niveauField, expField;

    @FXML
    private VBox employeBox;
    @FXML
    private TextField matriculeField, positionField;
    @FXML
    private DatePicker dateEmbauchePicker;

    private final UserService userService = new UserService();
    private final CandidatService candidatService = new CandidatService();
    private final EmployeService employeService = new EmployeService();
    private final RHService rhService = new RHService();

    private ContextMenu suggestionsMenu = new ContextMenu();
    private List<String> allCities = new ArrayList<>();
    private String detectedCountryCode;

    @FXML
    public void initialize() {
        //flag
        flagLabel.setStyle("-fx-font-size: 24px;");
        flagLabel.setText("🇹🇳");


        // Role et affichage sections
        String role = Session.getSelectedRole();
        if (role == null) role = "CANDIDAT";
        roleTitle.setText("Rôle: " + role);

        if ("CANDIDAT".equalsIgnoreCase(role)) {
            candidatBox.setVisible(true);
            candidatBox.setManaged(true);
        } else if ("EMPLOYE".equalsIgnoreCase(role)) {
            employeBox.setVisible(true);
            employeBox.setManaged(true);
        }

        // Détecte pays et applique indicatif / drapeau / villes
        detectUserCountry();

        // Autocomplete dynamique villes
        setupDynamicAutoComplete();

    }

    // ===================== VALIDATIONS =====================
    private boolean isValidName(String name) { return name != null && name.matches("[a-zA-ZÀ-ÿ\\s'-]+"); }
    private boolean isValidEmail(String email) { return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"); }
    private boolean isValidPhone(String phone) { return phone != null && phone.matches("\\+\\d{1,4}\\s?\\d{8}"); }
    private boolean isValidAdresse(String adresse) { return adresse != null && adresse.trim().length() >= 4; }
    private boolean isValidNiveau(String niveau) { return niveau != null && (niveau.equalsIgnoreCase("Licence") || niveau.equalsIgnoreCase("Master")); }
    private boolean isValidExperience(String exp) { return exp != null && exp.matches("\\d+"); }
    private boolean isValidMatricule(String matricule) { return matricule != null && matricule.matches("\\d{4}"); }

    private void clearErrors() {
        nomError.setText(""); prenomError.setText(""); emailError.setText(""); passError.setText("");
        telError.setText(""); adresseError.setText("");
        if (niveauError != null) niveauError.setText("");
        if (expError != null) expError.setText("");
        if (matriculeError != null) matriculeError.setText("");
        if (positionError != null) positionError.setText("");
        if (dateError != null) dateError.setText("");
    }

    private boolean validateForm() {
        clearErrors();
        boolean valid = true;
        String role = Session.getSelectedRole();

        if (!isValidName(nomField.getText())) { nomError.setText("Nom invalide"); valid = false; }
        if (!isValidName(prenomField.getText())) { prenomError.setText("Prénom invalide"); valid = false; }
        if (!isValidEmail(emailField.getText())) { emailError.setText("Email invalide"); valid = false; }
        if (passField.getText() == null || passField.getText().length() < 6) { passError.setText("Min 6 caractères"); valid = false; }
        if (!isValidPhone(telField.getText())) { telError.setText("Format: +XXX XXXXXXXX"); valid = false; }
        if (!isValidAdresse(adresseField.getText())) { adresseError.setText("Min 4 caractères"); valid = false; }

        if ("CANDIDAT".equalsIgnoreCase(role)) {
            if (!isValidNiveau(niveauField.getText())) { niveauError.setText("Licence ou Master"); valid = false; }
            if (!isValidExperience(expField.getText())) { expError.setText("Nombre uniquement"); valid = false; }
        }

        if ("EMPLOYE".equalsIgnoreCase(role)) {
            if (!isValidMatricule(matriculeField.getText())) { matriculeError.setText("Exactement 4 chiffres"); valid = false; }
            if (!positionField.getText().matches("[a-zA-Z\\s]+")) { positionError.setText("Position invalide"); valid = false; }
            if (dateEmbauchePicker.getValue() == null) { dateError.setText("Date obligatoire"); valid = false; }
        }

        return valid;
    }

    // ===================== SIGNUP =====================
    @FXML
    public void onSignup() throws Exception {
        if (!validateForm()) return;

        try {
            String role = Session.getSelectedRole();
            User u = new User(
                    nomField.getText().trim(),
                    prenomField.getText().trim(),
                    emailField.getText().trim(),
                    passField.getText().trim(),
                    telField.getText().trim(),
                    adresseField.getText().trim(),
                    role.toUpperCase()
            );

            if (userService.findByEmail(u.getEmail()) != null) {
                msgLabel.setText("Cet email existe déjà.");
                return;
            }

            int userId = userService.addUserAndReturnId(u);

            if ("CANDIDAT".equalsIgnoreCase(role)) {
                int exp = Integer.parseInt(expField.getText().trim());
                candidatService.insertCandidat(userId, niveauField.getText().trim(), exp);
            } else if ("EMPLOYE".equalsIgnoreCase(role)) {
                employeService.insertEmploye(userId, matriculeField.getText().trim(), positionField.getText().trim(), dateEmbauchePicker.getValue());
            } else if ("RH".equalsIgnoreCase(role)) {
                rhService.insertRH(userId);
            }

            msgLabel.setStyle("-fx-text-fill: #16a34a;");
            msgLabel.setText("✅ Compte créé avec succès !");
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill: #b91c1c;");
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void back() { Router.go("/auth/Welcome.fxml", "RHPro", 520, 360); }

    // ===================== DETECTION PAYS ET VILLES =====================
    private void detectUserCountry() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // 1️⃣ Détection IP
                    URL url = new URL("http://ip-api.com/json");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(5000);
                    con.setReadTimeout(5000);

                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();
                    con.disconnect();

                    JSONObject ipJson = new JSONObject(response.toString());
                    String country = ipJson.getString("country");
                    String countryCode = ipJson.getString("countryCode").toLowerCase();
                    detectedCountryCode = countryCode;

                    // 2️⃣ REST Countries pour indicatif + drapeau
                    URL url2 = new URL("https://restcountries.com/v3.1/alpha/" + countryCode);
                    HttpURLConnection con2 = (HttpURLConnection) url2.openConnection();
                    con2.setConnectTimeout(5000);
                    con2.setReadTimeout(5000);

                    BufferedReader in2 = new BufferedReader(new InputStreamReader(con2.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response2 = new StringBuilder();
                    while ((line = in2.readLine()) != null) response2.append(line);
                    in2.close();
                    con2.disconnect();

                    JSONArray countryArray = new JSONArray(response2.toString());
                    JSONObject countryObj = countryArray.getJSONObject(0);
                    String prefix = "";
                    JSONObject idd = countryObj.optJSONObject("idd");


                    if (idd != null) {

                        String root = idd.optString("root", "");

                        JSONArray suffixes = idd.optJSONArray("suffixes");

                        if (suffixes != null && suffixes.length() > 0) {
                            prefix = root + suffixes.getString(0);
                        } else {
                            prefix = root;
                        }
                    }


// 🔥 récupération image drapeau
                    String flagUrl = countryObj.getJSONObject("flags").getString("png");
                    final String finalPrefix = prefix;
                    Platform.runLater(() -> {
                        telField.setText(finalPrefix + " ");

                        ImageView flagImage = new ImageView(new Image(flagUrl));
                        flagImage.setFitHeight(24);
                        flagImage.setPreserveRatio(true);

                        flagLabel.setGraphic(flagImage);
                    });


                    // Charger villes
                    loadCities(country);

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        telField.setText("+216 ");
                        flagLabel.setText("🇹🇳");
                        detectedCountryCode = "tn";
                        loadCities("Tunisia");
                    });
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void loadCities(String country) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String query = URLEncoder.encode(country, StandardCharsets.UTF_8);
                URL url = new URL("https://nominatim.openstreetmap.org/search?country=" + query + "&featureClass=P&format=json&limit=100");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("User-Agent", "JavaFX App");

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();
                con.disconnect();

                List<String> cities = extractCitiesFromJson(response.toString());
                Platform.runLater(() -> {
                    allCities.clear();
                    allCities.addAll(cities);
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    private List<String> extractCitiesFromJson(String json) {
        List<String> cities = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String display = obj.getString("display_name");
                String city = display.split(",")[0].trim();
                if (!cities.contains(city)) cities.add(city);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return cities;
    }

    private void setupDynamicAutoComplete() {
        adresseField.textProperty().addListener((obs, oldText, newText) -> {
            if (detectedCountryCode == null || newText.length() < 2) {
                suggestionsMenu.hide();
                return;
            }
            searchPlaces(newText);
        });
    }

    private void searchPlaces(String query) {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String urlString = "https://nominatim.openstreetmap.org/search?q=" + encoded +
                        "&countrycodes=" + detectedCountryCode + "&format=json&limit=8";
                URL url = new URL(urlString);

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("User-Agent", "JavaFX App");

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();
                con.disconnect();

                return extractCitiesFromJson(response.toString());
            }
        };

        task.setOnSucceeded(e -> {
            List<String> results = task.getValue();
            Platform.runLater(() -> {
                if (results == null || results.isEmpty()) {
                    suggestionsMenu.hide();
                    return;
                }
                suggestionsMenu.getItems().clear();
                for (String place : results) {
                    MenuItem item = new MenuItem(place);
                    item.setOnAction(ev -> {
                        adresseField.setText(place);
                        suggestionsMenu.hide();
                    });
                    suggestionsMenu.getItems().add(item);
                }
                suggestionsMenu.show(adresseField, Side.BOTTOM, 0, 0);
            });
        });

        new Thread(task).start();
    }
}
