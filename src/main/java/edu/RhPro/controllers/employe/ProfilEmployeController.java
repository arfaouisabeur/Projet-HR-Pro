package edu.RhPro.controllers.employe;
import javafx.scene.control.Button;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ResourceBundle;

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle; // ← iText, pas java.awt

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import edu.RhPro.entities.Employe;
import edu.RhPro.services.EmployeService;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;


import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

import edu.RhPro.entities.User;
import edu.RhPro.utils.Session;
import edu.RhPro.services.UserService;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import javax.swing.*;

public class ProfilEmployeController implements Initializable {


    @FXML private Label nomLabel;
    @FXML private Label prenomLabel;
    @FXML private Label emailLabel;
    @FXML private Label telLabel;

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;

    @FXML private Button btnExportPdf;
    @FXML private Label exportMsg;
    private javafx.scene.image.Image currentQrImage = null;

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

        UserService service = new UserService();
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

    @FXML
    private ImageView qrImageView; // Assure-toi que c'est lié dans le FXML

    private final EmployeService employeService = new EmployeService();


    private final UserService userService = new UserService();

    @FXML
    private void generateQR() {
        try {
            User user = Session.getCurrentUser();

            if (user != null) {
                Image qrImage = userService.generateUserQRCode(user);
                qrImageView.setImage(qrImage);

                // ✅ Stocker dans currentQrImage pour l'export PDF
                currentQrImage = qrImage;

                // ✅ Activer le bouton export
                btnExportPdf.setDisable(false);
                exportMsg.setText("QR genere ! Clique 'Exporter' pour le PDF.");
                exportMsg.setStyle("-fx-text-fill: #15803d;");

            } else {
                System.out.println("Aucun utilisateur connecte !");
            }

        } catch (Exception e) {
            e.printStackTrace();
            exportMsg.setText("Erreur : " + e.getMessage());
            exportMsg.setStyle("-fx-text-fill: #b91c1c;");
        }
    }
    @FXML
    private void exportQRtoPDF() {

        if (currentQrImage == null || currentUser == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder le PDF");
        fileChooser.setInitialFileName("QRCode_" + currentUser.getNom() + "_" + currentUser.getPrenom() + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        java.io.File dest = fileChooser.showSaveDialog(qrImageView.getScene().getWindow());
        if (dest == null) return;

        try {
            // Convertir JavaFX Image → BufferedImage → bytes PNG
            BufferedImage buffered = javafx.embed.swing.SwingFXUtils.fromFXImage(currentQrImage, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            // Créer le PDF — page carrée taille QR
            Document document = new Document(new com.itextpdf.text.Rectangle(300, 300));
            PdfWriter.getInstance(document, new FileOutputStream(dest));
            document.open();

            // QR Code seul, pleine page, centré
            com.itextpdf.text.Image qrPdf = com.itextpdf.text.Image.getInstance(imageBytes);
            qrPdf.scaleToFit(280, 280);
            qrPdf.setAlignment(Element.ALIGN_CENTER);
            document.add(qrPdf);

            document.close();

            exportMsg.setText("PDF exporte : " + dest.getName());
            exportMsg.setStyle("-fx-text-fill: #15803d; -fx-font-weight:bold;");

        } catch (Exception e) {
            e.printStackTrace();
            exportMsg.setText("Erreur export PDF : " + e.getMessage());
            exportMsg.setStyle("-fx-text-fill: #b91c1c;");
        }
    }
}


