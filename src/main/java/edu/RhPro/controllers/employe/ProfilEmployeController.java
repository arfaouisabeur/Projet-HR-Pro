package edu.RhPro.controllers.employe;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

import edu.RhPro.entities.User;
import edu.RhPro.utils.Session;
import edu.RhPro.services.UserService;

public class ProfilEmployeController implements Initializable {


    @FXML private Label nomLabel;
    @FXML private Label prenomLabel;
    @FXML private Label emailLabel;
    @FXML private Label telLabel;

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;

    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        currentUser = Session.getCurrentUser();
        System.out.println("Session Employé : " + currentUser);

        if (currentUser == null) {
            System.out.println("⚠ Aucun employé connecté !");
            return;
        }

        // Remplir affichage
        nomLabel.setText(currentUser.getNom());
        prenomLabel.setText(currentUser.getPrenom());
        emailLabel.setText(currentUser.getEmail());
        telLabel.setText(currentUser.getTelephone());

        // Remplir champs modifiables
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        telField.setText(currentUser.getTelephone());
    }

    @FXML
    private void updateProfil(ActionEvent event) throws SQLException {

        if (currentUser == null) return;

        currentUser.setNom(nomField.getText());
        currentUser.setPrenom(prenomField.getText());
        currentUser.setEmail(emailField.getText());
        currentUser.setTelephone(telField.getText());

        Connection cnx=null;
        UserService service = new UserService(cnx);
        try {
            service.updateUser(currentUser);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Mettre à jour affichage
        nomLabel.setText(currentUser.getNom());
        prenomLabel.setText(currentUser.getPrenom());
        emailLabel.setText(currentUser.getEmail());
        telLabel.setText(currentUser.getTelephone());

        System.out.println("Profil employé mis à jour !");
    }
}
