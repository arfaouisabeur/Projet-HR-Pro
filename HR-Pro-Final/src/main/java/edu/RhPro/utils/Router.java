package edu.RhPro.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Router {

    private static Stage stage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void go(String fxmlPath, String title, int w, int h) {
        try {
            Parent root = FXMLLoader.load(Router.class.getResource(fxmlPath));
            Scene scene = new Scene(root, w, h);
            stage.setTitle(title);
            stage.setScene(scene);

            // ✅ Enhancements
            stage.setMinWidth(1100);
            stage.setMinHeight(650);
            stage.setMaximized(true); // ✅ full screen look

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Stage getStage() {
        return stage;
    }
}
