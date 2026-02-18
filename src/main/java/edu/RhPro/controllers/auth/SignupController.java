package edu.RhPro.controllers.auth;

import edu.RhPro.entities.User;
import edu.RhPro.services.UserService;
import edu.RhPro.utils.Router;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SignupController {

    // === FXML fields ===
    @FXML private Label roleTitle;
    @FXML private VBox candidatBox;
    @FXML private VBox employeBox;

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passField;
    @FXML private TextField telField;
    @FXML private TextField adresseField;

    @FXML private TextField cvField;
    @FXML private TextField niveauField;
    @FXML private TextField expField;

    @FXML private TextField matriculeField;
    @FXML private TextField positionField;
    @FXML private DatePicker dateEmbauchePicker;

    @FXML private Label msgLabel;

    // Map champs -> Labels d'erreur
    private final Map<Control, Label> errorLabelsMap = new HashMap<>();

    // Styles
    private final String normalFieldStyle = "-fx-background-radius:14; -fx-border-radius:14; -fx-border-color:#ececf5; -fx-padding:12 14; -fx-font-size:13px;";
    private final String errorFieldStyle = "-fx-background-radius:14; -fx-border-radius:14; -fx-border-color:red; -fx-padding:12 14; -fx-font-size:13px;";
    private UserService userService;

    @FXML
    public void initialize() {
        // Détecter le rôle
        String role = Session.getSelectedRole();
        if (role == null) role = "CANDIDAT";
        roleTitle.setText("Rôle: " + role);

        candidatBox.setVisible("CANDIDAT".equalsIgnoreCase(role));
        candidatBox.setManaged("CANDIDAT".equalsIgnoreCase(role));

        employeBox.setVisible("EMPLOYE".equalsIgnoreCase(role));
        employeBox.setManaged("EMPLOYE".equalsIgnoreCase(role));

        // --- Initialiser map labels d'erreur pour tous les champs ---
        setupErrorLabels();

        // --- Ajouter listeners de validation en temps réel ---
        addValidationListeners();
    }

    private void setupErrorLabels() {
        // Nom & Prénom
        errorLabelsMap.put(nomField, (Label) nomField.getParent().lookup("#nomError"));
        errorLabelsMap.put(prenomField, (Label) prenomField.getParent().lookup("#prenomError"));

        // Email & mot de passe
        errorLabelsMap.put(emailField, (Label) emailField.getParent().lookup("#emailError"));
        errorLabelsMap.put(passField, (Label) passField.getParent().lookup("#passError"));

        // Téléphone & adresse (pas de label dans FXML, on crée un Label invisible)
        Label telLabel = new Label(); telLabel.setVisible(false); telLabel.setManaged(false);
        ((VBox) telField.getParent()).getChildren().add(telLabel);
        errorLabelsMap.put(telField, telLabel);

        Label adresseLabel = new Label(); adresseLabel.setVisible(false); adresseLabel.setManaged(false);
        ((VBox) adresseField.getParent()).getChildren().add(adresseLabel);
        errorLabelsMap.put(adresseField, adresseLabel);

        // Candidat
        errorLabelsMap.put(cvField, createLabelUnderField(cvField));
        errorLabelsMap.put(niveauField, createLabelUnderField(niveauField));
        errorLabelsMap.put(expField, createLabelUnderField(expField));

        // Employe
        errorLabelsMap.put(matriculeField, createLabelUnderField(matriculeField));
        errorLabelsMap.put(positionField, createLabelUnderField(positionField));
        errorLabelsMap.put(dateEmbauchePicker, createLabelUnderField(dateEmbauchePicker));
    }

    // Création dynamique d'un label d'erreur sous un champ
    private Label createLabelUnderField(Control field) {
        Label label = new Label();
        label.setStyle("-fx-text-fill:red; -fx-font-size:11px; -fx-font-weight:600;");
        label.setVisible(false);
        label.setManaged(false);

        if (field.getParent() instanceof VBox vbox) {
            vbox.getChildren().add(label);
        }
        return label;
    }

    private void addValidationListeners() {
        for (Control field : errorLabelsMap.keySet()) {
            if (field instanceof TextField tf) {
                tf.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && !newVal.trim().isEmpty()) hideError(tf);
                });
            }
            if (field instanceof PasswordField pf) {
                pf.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && newVal.length() >= 6) hideError(pf);
                });
            }
            if (field instanceof DatePicker dp) {
                dp.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) hideError(dp);
                });
            }
        }
    }

    private void showError(Control field, String message) {
        field.setStyle(errorFieldStyle);
        Label label = errorLabelsMap.get(field);
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void hideError(Control field) {
        field.setStyle(normalFieldStyle);
        Label label = errorLabelsMap.get(field);
        if (label != null) {
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Nom : lettres seulement
        if (nomField.getText() == null || !nomField.getText().matches("[a-zA-ZÀ-ÿ\\s'-]+")) {
            showError(nomField, "Le nom doit contenir uniquement des lettres");
            isValid = false;
        }

        // Prénom : lettres seulement
        if (prenomField.getText() == null || !prenomField.getText().matches("[a-zA-ZÀ-ÿ\\s'-]+")) {
            showError(prenomField, "Le prénom doit contenir uniquement des lettres");
            isValid = false;
        }

        // Email : format standard
        if (emailField.getText() == null || !emailField.getText().matches("^[\\w-.]+@[\\w-]+\\.[a-z]{2,}$")) {
            showError(emailField, "Email invalide");
            isValid = false;
        }

        // Mot de passe : minimum 6 caractères
        if (passField.getText() == null || passField.getText().length() < 6) {
            showError(passField, "Mot de passe trop court");
            isValid = false;
        }

        // Téléphone : exactement 8 chiffres
        if (telField.getText() == null || !telField.getText().matches("\\d{8}")) {
            showError(telField, "Numéro de téléphone invalide (8 chiffres)");
            isValid = false;
        }

        // Adresse : minimum 4 caractères
        if (adresseField.getText() == null || adresseField.getText().trim().length() < 4) {
            showError(adresseField, "Adresse trop courte");
            isValid = false;
        }

        // --- Validation CANDIDAT ---
        if (candidatBox.isVisible()) {
            // CV : minimum 10 caractères
            if (cvField.getText() == null || cvField.getText().trim().length() < 10) {
                showError(cvField, "CV trop court (min 10 caractères)");
                isValid = false;
            }

            // Niveau : uniquement "Licence" ou "Master"
            if (niveauField.getText() == null ||
                    !(niveauField.getText().equalsIgnoreCase("Licence") || niveauField.getText().equalsIgnoreCase("Master"))) {
                showError(niveauField, "Niveau doit être 'Licence' ou 'Master'");
                isValid = false;
            }

            // Expérience : uniquement nombres >= 0
            if (expField.getText() == null || !expField.getText().matches("\\d+")) {
                showError(expField, "Expérience invalide (nombre uniquement)");
                isValid = false;
            }
        }

// --- Validation EMPLOYE ---
        if (employeBox.isVisible()) {
            // Matricule : exactement 4 chiffres et unique
            String matricule = matriculeField.getText();
            if (matricule == null || !matricule.matches("\\d{4}")) {
                showError(matriculeField, "Matricule invalide (exactement 4 chiffres)");
                isValid = false;
            } else {
                // Vérifier unicité dans la base
                try {
                    if (userService.isMatriculeExist(matricule)) {
                        showError(matriculeField, "Matricule déjà utilisé");
                        isValid = false;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError(matriculeField, "Erreur vérification matricule");
                    isValid = false;
                }
            }

            // Poste : doit être parmi les postes valides
            String[] postesValides = {"Designer", "Développeur", "Chef Département", "Comptable",
                    "Manager", "RH", "Analyste", "Technicien"}; // tu peux compléter la liste
            String position = positionField.getText();
            if (position == null || !Arrays.stream(postesValides)
                    .anyMatch(p -> p.equalsIgnoreCase(position.trim()))) {
                showError(positionField, "Position invalide (ex: Développeur, Designer...)");
                isValid = false;
            }

            // Date embauche obligatoire
            if (dateEmbauchePicker.getValue() == null) {
                showError(dateEmbauchePicker, "Date embauche obligatoire");
                isValid = false;
            }
        }


        return isValid;
    }

    @FXML
    private void onSignup() {
        if (!validateForm()) {
            msgLabel.setText("⚠️ Veuillez corriger les erreurs");
            return;
        }

        // Ici tu peux appeler ta méthode addUser(User user)
        msgLabel.setText("✅ Formulaire valide !");
    }

    @FXML
    public void back() {
        Router.go("/auth/Welcome.fxml", "RHPro", 520, 360);
    }

}
