package edu.RhPro.controllers.auth;

import edu.RhPro.entities.User;
import edu.RhPro.services.CandidatService;
import edu.RhPro.services.EmployeService;
import edu.RhPro.services.RHService;
import edu.RhPro.services.UserService;
import edu.RhPro.utils.Router;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class SignupController {

    @FXML private Label roleTitle;
    @FXML private Label msgLabel;

    @FXML private TextField nomField, prenomField, emailField, telField, adresseField;
    @FXML private PasswordField passField;

    // candidat
    @FXML private VBox candidatBox;
    @FXML private TextField cvField, niveauField, expField;

    // employe
    @FXML private VBox employeBox;
    @FXML private TextField matriculeField, positionField;
    @FXML private DatePicker dateEmbauchePicker;

    private final UserService userService = new UserService();
    private final CandidatService candidatService = new CandidatService();
    private final EmployeService employeService = new EmployeService();
    private final RHService rhService = new RHService();

    @FXML
    public void initialize() {
        String role = Session.getSelectedRole();
        if (role == null) role = "CANDIDAT"; // default
        roleTitle.setText("Rôle: " + role);

        // show/hide role fields
        if ("CANDIDAT".equalsIgnoreCase(role)) {
            candidatBox.setVisible(true);  candidatBox.setManaged(true);
        } else if ("EMPLOYE".equalsIgnoreCase(role)) {
            employeBox.setVisible(true);  employeBox.setManaged(true);
        }
        // RH: nothing extra
    }

    @FXML
    public void onSignup() {
        try {
            String role = Session.getSelectedRole();
            if (role == null) {
                msgLabel.setText("Choisis d'abord un rôle sur l'écran précédent.");
                return;
            }

            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim();
            String password = passField.getText().trim();

            // basic empty validation
            if (nom.isBlank() || prenom.isBlank() ||
                    email.isBlank() || password.isBlank()) {
                msgLabel.setText("Nom, prénom, email et mot de passe sont obligatoires.");
                return;
            }

            // ✅ Validation nom/prénom (lettres uniquement)
            if (!isValidName(nom)) {
                msgLabel.setText("Le nom ne doit contenir que des lettres.");
                return;
            }

            if (!isValidName(prenom)) {
                msgLabel.setText("Le prénom ne doit contenir que des lettres.");
                return;
            }

            // ✅ Validation email format
            if (!isValidEmail(email)) {
                msgLabel.setText("Email invalide. Il doit contenir '@'.");
                return;
            }

            // ✅ Validation mot de passe sécurisé
            if (!isValidPassword(password)) {
                msgLabel.setText("Mot de passe faible ! Min 8 caractères avec chiffre et symbole.");
                return;
            }

            // Create user
            User u = new User(
                    nom,
                    prenom,
                    email,
                    password,
                    telField.getText().trim(),
                    adresseField.getText().trim(),
                    role.toUpperCase()
            );

            // check email unique
            if (userService.findByEmail(u.getEmail()) != null) {
                msgLabel.setText("Cet email existe déjà.");
                return;
            }

            int userId = userService.addUserAndReturnId(u);

            // Insert role table row
            if ("CANDIDAT".equalsIgnoreCase(role)) {
                int exp = 0;
                try { exp = Integer.parseInt(expField.getText().trim()); } catch (Exception ignored) {}
                candidatService.insertCandidat(userId,
                        cvField.getText().trim(),
                        niveauField.getText().trim(),
                        exp
                );
            } else if ("EMPLOYE".equalsIgnoreCase(role)) {
                employeService.insertEmploye(userId,
                        matriculeField.getText().trim(),
                        positionField.getText().trim(),
                        dateEmbauchePicker.getValue()
                );
            } else if ("RH".equalsIgnoreCase(role)) {
                rhService.insertRH(userId);
            }

            msgLabel.setStyle("-fx-text-fill: #16a34a;");
            msgLabel.setText("✅ Compte créé ! Tu peux te connecter.");

        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setStyle("-fx-text-fill: #b91c1c;");
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void back() {
        Router.go("/auth/Welcome.fxml", "RHPro", 520, 360);
    }

    // ================= VALIDATIONS =================

    private boolean isValidName(String name) {
        return name.matches("^[a-zA-ZÀ-ÿ\\s]+$");
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPassword(String password) {
        return password.matches("^(?=.*[0-9])(?=.*[!@#$%^&*()_+=-]).{8,}$");
    }

}
