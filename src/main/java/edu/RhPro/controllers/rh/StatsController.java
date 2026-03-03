package edu.RhPro.controllers.rh;

import edu.RhPro.entities.User;
import edu.RhPro.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class StatsController implements Initializable {

    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    @FXML private Label totalLabel;
    @FXML private Label rhLabel;
    @FXML private Label employeLabel;
    @FXML private Label candidatLabel;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStats();
    }

    private void loadStats() {
        try {
            List<User> users = userService.getData();

            long nbRH       = users.stream().filter(u -> "RH".equals(u.getRole())).count();
            long nbEmploye  = users.stream().filter(u -> "EMPLOYE".equals(u.getRole())).count();
            long nbCandidat = users.stream().filter(u -> "CANDIDAT".equals(u.getRole())).count();
            long total      = users.size();

            // ── Labels stats cards ────────────────────────────────────
            totalLabel.setText(String.valueOf(total));
            rhLabel.setText(String.valueOf(nbRH));
            employeLabel.setText(String.valueOf(nbEmploye));
            candidatLabel.setText(String.valueOf(nbCandidat));

            // ── BarChart ──────────────────────────────────────────────
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Nombre d'utilisateurs");

            XYChart.Data<String, Number> dataRH  = new XYChart.Data<>("RH", nbRH);
            XYChart.Data<String, Number> dataEmp = new XYChart.Data<>("Employes", nbEmploye);
            XYChart.Data<String, Number> dataCand= new XYChart.Data<>("Candidats", nbCandidat);

            series.getData().addAll(dataRH, dataEmp, dataCand);
            barChart.getData().add(series);

            // ── Couleurs des barres après rendu ───────────────────────
            barChart.lookupAll(".data0.chart-bar")
                    .forEach(n -> n.setStyle("-fx-bar-fill: #5b2b82;"));
            barChart.lookupAll(".data1.chart-bar")
                    .forEach(n -> n.setStyle("-fx-bar-fill: #2c7be5;"));
            barChart.lookupAll(".data2.chart-bar")
                    .forEach(n -> n.setStyle("-fx-bar-fill: #f59e0b;"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}