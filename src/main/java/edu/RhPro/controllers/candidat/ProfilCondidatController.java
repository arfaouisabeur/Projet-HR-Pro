package edu.RhPro.controllers.candidat;

import edu.RhPro.entities.User;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import java.net.URL;
import java.util.ResourceBundle;


public class ProfilCondidatController implements Initializable  {


    @FXML
    private Label nomLabel;
    @FXML private Label prenomLabel;
    @FXML private Label emailLabel;
    @FXML private Label telLabel;
    @FXML private Label adresseLabel;

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;
    @FXML private TextField adresseField;

    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        currentUser = Session.getCurrentUser();
        System.out.println("Utilisateur dans session : " + currentUser);

        if (currentUser == null) {
            System.out.println("⚠ Aucun utilisateur connecté !");
            return;
        }

        nomLabel.setText(currentUser.getNom());
        prenomLabel.setText(currentUser.getPrenom());
        emailLabel.setText(currentUser.getEmail());
        telLabel.setText(currentUser.getTelephone());
        adresseLabel.setText(currentUser.getAdresse());

        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        telField.setText(currentUser.getTelephone());
        adresseField.setText(currentUser.getAdresse());
    }
    @FXML
    private void updateProfil(ActionEvent event) {

        if (currentUser == null) {
            System.out.println("Aucun utilisateur en session");
            return;
        }

        currentUser.setNom(nomField.getText());
        currentUser.setPrenom(prenomField.getText());
        currentUser.setEmail(emailField.getText());
        currentUser.setTelephone(telField.getText());
        currentUser.setAdresse(adresseField.getText());

        System.out.println("Profil mis à jour !");
    }

}
