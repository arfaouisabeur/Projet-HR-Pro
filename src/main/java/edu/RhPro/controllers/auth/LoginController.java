package edu.RhPro.controllers.auth;

import edu.RhPro.entities.User;
import edu.RhPro.services.UserService;
import edu.RhPro.utils.Router;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.Connection;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passField;
    @FXML private Label msgLabel;

    private Connection cnx;
    private final UserService userService = new UserService(cnx);

    @FXML
    public void onLogin() {
        try {
            String email = emailField.getText().trim();
            String pass = passField.getText().trim();

            User u = userService.authenticate(email, pass);
            if (u == null) {
                msgLabel.setText("Email ou mot de passe incorrect.");
                return;
            }

            // Optional: check selected role match
            String selected = Session.getSelectedRole();
            if (selected != null && !selected.equalsIgnoreCase(u.getRole())) {
                msgLabel.setText("Vous avez choisi le rôle " + selected + " mais ce compte est " + u.getRole());
                return;
            }

            Session.setCurrentUser(u);


            if ("RH".equalsIgnoreCase(u.getRole())) {
                Router.go("/rh/RhShell.fxml", "RHPro - RH", 1400, 820);
            } else if ("EMPLOYE".equalsIgnoreCase(u.getRole())) {
                Router.go("/employe/EmployeShell.fxml", "RHPro - Employé", 1400, 820);
            } else {
                Router.go("/candidat/CandidatShell.fxml", "RHPro - Candidat", 1400, 820);
            }



        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void back() {
        Router.go("/auth/Welcome.fxml", "RHPro", 520, 360);
    }
}
