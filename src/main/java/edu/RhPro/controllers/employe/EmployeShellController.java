package edu.RhPro.controllers.employe;

import edu.RhPro.entities.User;
import edu.RhPro.utils.Router;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EmployeShellController {

    @FXML private StackPane contentPane;
    @FXML private Label userLabel;
    @FXML private Label dateLabel;

    @FXML
    public void initialize() {
        User u = Session.getCurrentUser();
        if (u != null) {
            userLabel.setText(u.getPrenom() + " " + u.getNom() + " (Employé)");
        }


        // ✅ default page (keep what you want)
        loadPage("/employe/MesSalairesView.fxml");
    }

    private void loadPage(String fxml) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxml));
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // money
    @FXML public void goSalaires()   { loadPage("/employe/MesSalairesView.fxml"); }
    @FXML public void goPrimes()     { loadPage("/employe/MesPrimesView.fxml"); }

    // ✅ already developed & tested by you
    @FXML public void goConges()     { loadPage("/employe/MesCongesView.fxml"); }
    @FXML public void goServices()   { loadPage("/employe/MesServicesView.fxml"); }

    // projects / tasks
    @FXML public void goMesProjets() { loadPage("/employe/MesProjetsView.fxml"); }
    @FXML public void goMesTaches()  { loadPage("/employe/MesTachesView.fxml"); }

    @FXML public void goEvents() { loadPage("/employe/EventsView.fxml"); }
    @FXML public void goMyParticipations() { loadPage("/employe/MyParticipationsView.fxml"); }

    @FXML
    public void logout() {
        Session.clear();
        Router.go("/auth/Welcome.fxml", "RHPro", 520, 360);
    }
}
