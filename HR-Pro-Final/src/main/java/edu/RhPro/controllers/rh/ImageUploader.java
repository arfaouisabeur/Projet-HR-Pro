package edu.RhPro.controllers.rh;

import edu.RhPro.services.ImgbbService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

public class ImageUploader extends VBox {

    private final ImageView imageView;
    private final Label statusLabel;
    private final Button uploadButton;
    private final ProgressIndicator progressIndicator;
    private final ImgbbService imgbbService = new ImgbbService();

    private String uploadedUrl;
    private OnImageUploadedListener listener;

    public ImageUploader() {
        setSpacing(10);
        setAlignment(Pos.CENTER);
        setStyle("-fx-padding: 10; -fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-border-width: 1;");

        // Image preview
        imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(130);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 4;");

        // Default placeholder
        setPlaceholderImage();

        // Status label
        statusLabel = new Label("Cliquez pour choisir une image");
        statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setMaxSize(30, 30);

        // Upload button
        uploadButton = new Button("📷 Choisir une image");
        uploadButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 15; -fx-cursor: hand; -fx-font-size: 11px;");
        uploadButton.setOnAction(e -> chooseImage());

        getChildren().addAll(imageView, uploadButton, statusLabel, progressIndicator);

        // Make the whole component clickable
        this.setOnMouseClicked(e -> chooseImage());
    }

    private void setPlaceholderImage() {
        // Try to load a placeholder image, or just set null
        try {
            // You can add a placeholder image in your resources
            // Image placeholder = new Image(getClass().getResourceAsStream("/images/placeholder.png"));
            // imageView.setImage(placeholder);
            imageView.setImage(null);
        } catch (Exception e) {
            imageView.setImage(null);
        }
    }

    private void chooseImage() {
        if (getScene() == null || getScene().getWindow() == null) {
            statusLabel.setText("Erreur: fenêtre non disponible");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File selectedFile = fileChooser.showOpenDialog(getScene().getWindow());
        if (selectedFile != null) {
            uploadImage(selectedFile);
        }
    }

    private void uploadImage(File file) {
        // Show preview
        try {
            Image preview = new Image(file.toURI().toString(), 180, 130, true, true);
            imageView.setImage(preview);
        } catch (Exception e) {
            statusLabel.setText("Erreur de prévisualisation");
        }

        // Show loading state
        uploadButton.setDisable(true);
        progressIndicator.setVisible(true);
        statusLabel.setText("Téléchargement...");
        statusLabel.setStyle("-fx-text-fill: #2563eb;");

        // Upload in background
        new Thread(() -> {
            try {
                String url = imgbbService.uploadImage(file);

                Platform.runLater(() -> {
                    uploadedUrl = url;
                    statusLabel.setText("✓ Téléchargé avec succès");
                    statusLabel.setStyle("-fx-text-fill: #059669;");
                    progressIndicator.setVisible(false);
                    uploadButton.setDisable(false);

                    System.out.println("Image uploaded successfully: " + url); // Debug log

                    if (listener != null) {
                        listener.onImageUploaded(url);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("✗ Échec: " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #dc2626;");
                    progressIndicator.setVisible(false);
                    uploadButton.setDisable(false);
                    setPlaceholderImage();
                    ex.printStackTrace();
                });
            }
        }).start();
    }

    public String getUploadedUrl() {
        return uploadedUrl;
    }

    public void setImageUrl(String url) {
        if (url != null && !url.isEmpty()) {
            try {
                Image image = new Image(url, 180, 130, true, true);
                imageView.setImage(image);
                uploadedUrl = url;
                statusLabel.setText("✓ Image chargée");
                statusLabel.setStyle("-fx-text-fill: #059669;");
            } catch (Exception e) {
                statusLabel.setText("⚠ Impossible de charger l'image");
                statusLabel.setStyle("-fx-text-fill: #f59e0b;");
            }
        }
    }

    public void setOnImageUploaded(OnImageUploadedListener listener) {
        this.listener = listener;
    }

    public interface OnImageUploadedListener {
        void onImageUploaded(String url);
    }
}