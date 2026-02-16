package edu.RhPro.controllers.auth;

import edu.RhPro.utils.Router;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class WelcomeController {

    @FXML private Label roleLabel;



    private void setRole(String role) {
        Session.setSelectedRole(role);

    }



    @FXML public void chooseCandidat() { setRole("CANDIDAT"); }
    @FXML public void chooseEmploye() { setRole("EMPLOYE"); }
    @FXML public void chooseRH() { setRole("RH"); }

    @FXML
    public void goLogin() {
        Router.go("/auth/Login.fxml", "RHPro - Connexion", 520, 360);
    }

    @FXML
    public void goSignup() {
        Router.go("/auth/Signup.fxml", "RHPro - Inscription", 700, 720);
    }
}
