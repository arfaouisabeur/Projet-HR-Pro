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

            // basic validation
            if (nomField.getText().isBlank() || prenomField.getText().isBlank() ||
                    emailField.getText().isBlank() || passField.getText().isBlank()) {
                msgLabel.setText("Nom, prénom, email et mot de passe sont obligatoires.");
                return;
            }

            // Create user
            User u = new User(
                    nomField.getText().trim(),
                    prenomField.getText().trim(),
                    emailField.getText().trim(),
                    passField.getText().trim(),
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
}
