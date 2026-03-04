package edu.RhPro.controllers.rh;

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

public class RhShellController {

    @FXML private StackPane contentPane;
    @FXML private Label userLabel;
    @FXML private Label dateLabel;

    @FXML
    public void initialize() {
        User u = Session.getCurrentUser();
        if (u != null) {
            userLabel.setText(u.getPrenom() + " " + u.getNom() + " (RH)");
        }



        // default page
        loadPage("/rh/OffresManageView.fxml");
    }

    private void loadPage(String fxml) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxml));
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ Existing
    @FXML public void goOffres() { loadPage("/rh/OffresManageView.fxml"); }
    @FXML public void goCandidatures() { loadPage("/rh/CandidaturesManageView.fxml"); }

    // ✅ Existing (only works if your fxml exists)
    @FXML public void goConges() { loadPage("/rh/CongesManageView.fxml"); }
    @FXML public void goServices() { loadPage("/rh/ServicesManageView.fxml"); }

    // ✅ Existing
    @FXML public void goSalaires() { loadPage("/rh/SalairesManageView.fxml"); }
    @FXML public void goPrimes() { loadPage("/rh/PrimesManageView.fxml"); }

    // ✅ NEW (added without removing anything)
    @FXML public void goProjets() { loadPage("/rh/ProjetsManageView.fxml"); }
    @FXML public void goEvents() { loadPage("/rh/EventsManageView.fxml"); }
    @FXML public void goUsers() { loadPage("/rh/UsersManageView.fxml"); }


    @FXML
    public void logout() {
        Session.clear();
        Router.go("/auth/Welcome.fxml", "RHPro", 520, 360);
    }
}
