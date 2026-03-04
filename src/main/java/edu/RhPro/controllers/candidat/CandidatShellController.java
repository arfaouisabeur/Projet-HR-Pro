package edu.RhPro.controllers.candidat;

import edu.RhPro.entities.User;
import edu.RhPro.utils.Router;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CandidatShellController {

    @FXML private StackPane contentPane;
    @FXML private Label userLabel;
    @FXML private Label dateLabel;
    @FXML private TextField searchField;

    @FXML
    public void initialize() {
        User u = Session.getCurrentUser();
        userLabel.setText(u.getPrenom() + " " + u.getNom());


        loadPage("/candidat/OffresView.fxml");
    }

    private void loadPage(String fxml) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxml));
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void goOffres() { loadPage("/candidat/OffresView.fxml"); }
    @FXML public void goMesCandidatures() { loadPage("/candidat/MesCandidaturesView.fxml"); }

    @FXML
    public void onSearch() {
        // For now: reload offers. Later we’ll wire actual filtering.
        loadPage("/candidat/OffresView.fxml");
    }

    @FXML
    public void logout() {
        Session.clear();
        Router.go("/auth/Welcome.fxml", "RHPro", 520, 360);
    }
}
