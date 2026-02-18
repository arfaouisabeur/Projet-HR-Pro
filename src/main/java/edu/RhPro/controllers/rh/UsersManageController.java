package edu.RhPro.controllers.rh;

import edu.RhPro.entities.User;
import edu.RhPro.services.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.Connection;
import edu.RhPro.tools.MyConnection;
import edu.RhPro.services.UserService;


import java.util.List;

public class UsersManageController {

    @FXML private TableView<User> table;
    @FXML private TableColumn<User, String> colId, colNom, colPrenom, colEmail, colTel, colRole;

    @FXML private TextField nomField, prenomField, emailField, telField, adresseField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label msgLabel;

    private Connection cnx;
    private final UserService service = new UserService(cnx);

    @FXML
    public void initialize() {

        roleCombo.setItems(FXCollections.observableArrayList("RH", "EMPLOYE", "CANDIDAT"));

        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrenom()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colTel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelephone() == null ? "" : c.getValue().getTelephone()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole() == null ? "" : c.getValue().getRole()));

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, u) -> {
            if (u == null) return;
            nomField.setText(u.getNom());
            prenomField.setText(u.getPrenom());
            emailField.setText(u.getEmail());
            telField.setText(u.getTelephone());
            adresseField.setText(u.getAdresse());
            roleCombo.setValue(u.getRole());
            passwordField.setText(""); // never show real password
        });

        refresh();
    }

    @FXML
    public void refresh() {
        try {
            List<User> list = service.getData();
            table.setItems(FXCollections.observableArrayList(list));
            msgLabel.setText("✅ " + list.size() + " utilisateur(s).");
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur chargement.");
        }
    }

    @FXML
    public void clearForm() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        telField.clear();
        adresseField.clear();
        passwordField.clear();
        roleCombo.setValue(null);
        table.getSelectionModel().clearSelection();
        msgLabel.setText("");
    }

    @FXML
    public void add() {
        try {
            if (nomField.getText().isBlank() || prenomField.getText().isBlank() || emailField.getText().isBlank()) {
                msgLabel.setText("⚠️ Nom / prénom / email obligatoires.");
                return;
            }
            if (passwordField.getText().isBlank()) {
                msgLabel.setText("⚠️ Mot de passe obligatoire à la création.");
                return;
            }

            User u = new User();
            u.setNom(nomField.getText());
            u.setPrenom(prenomField.getText());
            u.setEmail(emailField.getText());
            u.setMot_de_passe(passwordField.getText());
            u.setTelephone(telField.getText());
            u.setAdresse(adresseField.getText());
            u.setRole(roleCombo.getValue() == null ? "EMPLOYE" : roleCombo.getValue());

            service.addUser(u);

            msgLabel.setText("✅ Utilisateur ajouté.");
            refresh();
            clearForm();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur ajout (email déjà utilisé ?).");
        }
    }

    @FXML
    public void update() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("⚠️ Sélectionne un utilisateur."); return; }

        try {
            selected.setNom(nomField.getText());
            selected.setPrenom(prenomField.getText());
            selected.setEmail(emailField.getText());
            selected.setTelephone(telField.getText());
            selected.setAdresse(adresseField.getText());
            selected.setRole(roleCombo.getValue());

            // if password is empty => keep current password
            if (passwordField.getText() == null || passwordField.getText().isBlank()) {
                service.updateUserWithoutPassword(selected);
            } else {
                selected.setMot_de_passe(passwordField.getText());
                service.updateUser(selected);
            }

            msgLabel.setText("✅ Mise à jour OK.");
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur update.");
        }
    }

    @FXML
    public void delete() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("⚠️ Sélectionne un utilisateur."); return; }

        try {
            service.removeUserById(selected.getId());
            msgLabel.setText("✅ Supprimé.");
            refresh();
            clearForm();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("❌ Erreur suppression (FK ?).");
        }
    }
}
