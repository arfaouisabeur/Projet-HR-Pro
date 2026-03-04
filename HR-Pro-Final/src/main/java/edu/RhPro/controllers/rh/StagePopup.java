package edu.RhPro.controllers.rh;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class StagePopup {
    public static void show(Parent root, String title, int w, int h) {
        Stage st = new Stage();
        st.setTitle(title);
        st.initModality(Modality.APPLICATION_MODAL);
        st.setScene(new Scene(root, w, h));
        st.showAndWait();
    }
}
