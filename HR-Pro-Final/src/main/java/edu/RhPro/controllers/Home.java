package edu.RhPro.controllers;

import edu.RhPro.utils.Router;
import javafx.application.Application;
import javafx.stage.Stage;

public class Home extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Router.init(stage);
        Router.go("/auth/Welcome.fxml", "RHPro", 520, 360);
    }
}
