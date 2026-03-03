package edu.RhPro.controllers.candidat;

import ai.djl.modality.Classifications;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import edu.RhPro.entities.User;
import edu.RhPro.services.UserService;
import edu.RhPro.utils.Session;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import javafx.scene.control.Button;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Base64;
import com.google.gson.JsonObject; // Ajoutez Gson via Maven: <dependency><groupId>com.google.code.gson</groupId><artifactId>gson</artifactId><version>2.10.1</version></dependency>import com.google.gson.JsonParser;

import okhttp3.*;
import org.w3c.dom.Text;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.stage.FileChooser;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.SepiaTone;
import javafx.scene.effect.GaussianBlur;
import java.io.File;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;




public class ProfilCondidatController implements Initializable  {
    @FXML private Button btnSaveAvatar;
    private byte[] lastGeneratedAvatar = null; // stocke l'avatar généré en mémoire
    private final UserService userService = new UserService();


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
    @FXML private ImageView imageViewOriginal;
    @FXML private ImageView imageViewAvatar;
    @FXML private Label txtResult;
    private File uploadedFile;

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

        // Charger l'avatar existant au démarrage
        try {
            String avatarPath = userService.getAvatarPath(currentUser.getId());
            if (avatarPath != null) {
                java.io.File f = new java.io.File(avatarPath);
                if (f.exists()) {
                    imageViewAvatar.setImage(new javafx.scene.image.Image(f.toURI().toString()));
                    txtResult.setText("Avatar chargé depuis la sauvegarde.");
                }
            }
        } catch (Exception e) {
            System.out.println("Pas d'avatar sauvegardé.");
        }
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
    @FXML private TextField txtName;
    @FXML private ImageView imageView;

    // 🎨 Génération avatar gratuit via DiceBear

    // Upload image



    private static final String HF_TOKEN = "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"; // ← colle ton token ici
    private static final String API_URL = "https://api-inference.huggingface.co/models/bryandlee/animegan2-pytorch";// Mets ta clé ici

        @FXML
        private void uploadImage() {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

            uploadedFile = fileChooser.showOpenDialog(null);

            if (uploadedFile != null) {
                imageViewOriginal.setImage(new Image(uploadedFile.toURI().toString()));
                txtResult.setText("Image uploadée !");
            }
        }

    // 2️⃣ Générer l’avatar
    @FXML
    private void generateAvatar() {
        if (uploadedFile == null) {
            txtResult.setText("Upload une image d'abord !");
            return;
        }

        txtResult.setText("⏳ Génération en cours...");

        new Thread(() -> {
            try {
                String apiKey = "sk-OoRAlOly1v37DPNWvlIuENgd7pBQpFuZEMrmM9b2Yyq5uKBo"; // ← ta clé ici

                String boundary = "----JavaBoundary" + Long.toHexString(System.currentTimeMillis());

                java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                        new java.net.URL("https://api.stability.ai/v2beta/stable-image/control/style")
                                .openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Accept", "image/*");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (java.io.OutputStream out = conn.getOutputStream()) {
                    // -- champ image
                    String imgHeader = "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"image\"; filename=\""
                            + uploadedFile.getName() + "\"\r\n"
                            + "Content-Type: image/jpeg\r\n\r\n";
                    out.write(imgHeader.getBytes("UTF-8"));
                    java.nio.file.Files.copy(uploadedFile.toPath(), out);
                    out.write("\r\n".getBytes("UTF-8"));

                    // -- champ prompt
                    String prompt = "anime style portrait, manga art, high quality";
                    String promptPart = "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"prompt\"\r\n\r\n"
                            + prompt + "\r\n";
                    out.write(promptPart.getBytes("UTF-8"));

                    // -- champ fidelity (0.0 à 1.0)
                    String fidelityPart = "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"fidelity\"\r\n\r\n"
                            + "0.7\r\n";
                    out.write(fidelityPart.getBytes("UTF-8"));

                    // -- champ output format
                    String formatPart = "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"output_format\"\r\n\r\n"
                            + "png\r\n";
                    out.write(formatPart.getBytes("UTF-8"));

                    // -- fin
                    out.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
                    out.flush();
                }

                int status = conn.getResponseCode();
                System.out.println("Status Stability AI: " + status);

                if (status != 200) {
                    String err = new String(conn.getErrorStream().readAllBytes());
                    throw new RuntimeException("Erreur API " + status + ": " + err);
                }

                // L'API retourne directement l'image PNG
                byte[] imageBytes = conn.getInputStream().readAllBytes();

                javafx.application.Platform.runLater(() -> {
                    lastGeneratedAvatar = imageBytes; // ← stocker en mémoire
                    javafx.scene.image.Image avatarImage =
                            new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
                    imageViewAvatar.setImage(avatarImage);
                    btnSaveAvatar.setDisable(false); // ← activer le bouton
                    txtResult.setText("✅ Avatar généré ! Clique 'Sauvegarder' pour le conserver.");
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        txtResult.setText("❌ Erreur : " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void saveAvatar() {
        if (lastGeneratedAvatar == null || currentUser == null) return;

        try {
            // Créer le dossier avatars s'il n'existe pas
            java.io.File avatarsDir = new java.io.File("avatars");
            avatarsDir.mkdirs();

            // Nom de fichier unique par utilisateur
            String fileName = "avatar_user_" + currentUser.getId() + ".png";
            java.io.File avatarFile = new java.io.File(avatarsDir, fileName);

            // Écrire l'image sur le disque
            java.nio.file.Files.write(avatarFile.toPath(), lastGeneratedAvatar);

            // Sauvegarder le chemin en base de données
            userService.updateAvatar(currentUser.getId(), avatarFile.getAbsolutePath());

            txtResult.setText("💾 Avatar sauvegardé !");
            System.out.println("Avatar sauvegardé : " + avatarFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            txtResult.setText("❌ Erreur sauvegarde : " + e.getMessage());
        }
    }

    // Ajouter setter pour uploadedFile
    public void setUploadedFile(File file) {
        this.uploadedFile = file;
    }
}

