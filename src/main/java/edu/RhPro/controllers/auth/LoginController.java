package edu.RhPro.controllers.auth;

import edu.RhPro.entities.User;
import edu.RhPro.services.UserService;
import edu.RhPro.utils.CaptchaGenerator;
import edu.RhPro.utils.EmailService;
import edu.RhPro.utils.Router;
import edu.RhPro.utils.Session;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.control.Alert;

import java.sql.SQLException;


public class LoginController {

    // ── Connexion normale ─────────────────────────────────────────────
    @FXML private TextField emailField;
    @FXML private PasswordField passField;
    @FXML private Label msgLabel;
    @FXML private Label emailError;
    @FXML private ImageView imageViewCaptcha;
    @FXML private TextField captchaField;
    @FXML private Label captchaError;

    // ── Forgot Password ───────────────────────────────────────────────
    @FXML private VBox forgotPasswordBox;   // Zone "entrer email pour reset"
    @FXML private VBox otpBox;             // Zone "entrer le code OTP"
    @FXML private TextField forgotEmailField;
    @FXML private TextField otpField;
    @FXML private Label forgotMsg;
    @FXML private Label otpMsg;
    @FXML private Label timerLabel;

    private final UserService userService = new UserService();
    private final CaptchaGenerator captchaGenerator = new CaptchaGenerator();
    private Timeline countdownTimeline;

    @FXML
    public void initialize() {
        refreshCaptcha();
        // Cacher les zones forgot password au démarrage
        forgotPasswordBox.setVisible(false);
        forgotPasswordBox.setManaged(false);
        otpBox.setVisible(false);
        otpBox.setManaged(false);
    }

    // ── CAPTCHA ───────────────────────────────────────────────────────
    @FXML
    private void refreshCaptcha() {
        if (imageViewCaptcha != null)
            imageViewCaptcha.setImage(captchaGenerator.generateCaptchaImage());
    }

    private boolean verifyCaptcha() {
        if (!captchaField.getText().equalsIgnoreCase(captchaGenerator.getCaptchaText())) {
            captchaError.setText("Captcha incorrect !");
            refreshCaptcha();
            return false;
        }
        captchaError.setText("");
        return true;
    }

    // ── LOGIN NORMAL ──────────────────────────────────────────────────
    @FXML
    public void onLogin() {
        try {
            if (!verifyCaptcha()) return;

            String email = emailField.getText().trim();
            String pass = passField.getText().trim();

            User u = userService.authenticate(email, pass);
            if (u == null) {
                msgLabel.setText("Email ou mot de passe incorrect.");
                return;
            }

            String selected = Session.getSelectedRole();
            if (selected != null && !selected.equalsIgnoreCase(u.getRole())) {
                msgLabel.setText("Vous avez choisi le rôle " + selected + " mais ce compte est " + u.getRole());
                return;
            }

            Session.setCurrentUser(u);

            if ("RH".equalsIgnoreCase(u.getRole())) {
                Router.go("/rh/RhShell.fxml", "RHPro - RH", 1400, 820);
            } else if ("EMPLOYE".equalsIgnoreCase(u.getRole())) {
                Router.go("/employe/EmployeShell.fxml", "RHPro - Employé", 1400, 820);
            } else {
                Router.go("/candidat/CandidatShell.fxml", "RHPro - Candidat", 1400, 820);
            }

        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }

    // ── FORGOT PASSWORD — Étape 1 : afficher le champ email ──────────
    @FXML
    public void showForgotPassword() {
        forgotPasswordBox.setVisible(true);
        forgotPasswordBox.setManaged(true);
        otpBox.setVisible(false);
        otpBox.setManaged(false);
        forgotMsg.setText("");
    }

    // ── FORGOT PASSWORD — Étape 2 : envoyer le code OTP ──────────────
    @FXML
    public void sendOtpCode() {
        String email = forgotEmailField.getText().trim();

        if (email.isEmpty()) {
            forgotMsg.setText("Entre ton email !");
            forgotMsg.setStyle("-fx-text-fill: #b91c1c;");
            return;
        }

        forgotMsg.setText("⏳ Envoi en cours...");
        forgotMsg.setStyle("-fx-text-fill: #6b7280;");

        // Envoi dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                // Vérifier que l'email existe en base
                if (!userService.emailExists(email)) {
                    Platform.runLater(() -> {
                        forgotMsg.setText("❌ Aucun compte trouvé avec cet email.");
                        forgotMsg.setStyle("-fx-text-fill: #b91c1c;");
                    });
                    return;
                }

                EmailService.sendOtp(email);

                Platform.runLater(() -> {
                    forgotMsg.setText("✅ Code envoyé à " + email);
                    forgotMsg.setStyle("-fx-text-fill: #15803d;");

                    // Afficher la zone OTP
                    otpBox.setVisible(true);
                    otpBox.setManaged(true);
                    forgotPasswordBox.setVisible(false);
                    forgotPasswordBox.setManaged(false);

                    startCountdown();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    forgotMsg.setText("❌ Erreur envoi email : " + e.getMessage());
                    forgotMsg.setStyle("-fx-text-fill: #b91c1c;");
                });
            }
        }).start();
    }

    // ── FORGOT PASSWORD — Étape 3 : vérifier le code OTP ─────────────
    @FXML
    public void verifyOtpCode() {

        String email = forgotEmailField.getText().trim();
        String code = otpField.getText().trim();

        if (code.isEmpty()) {
            otpMsg.setText("Entre le code reçu par email !");
            otpMsg.setStyle("-fx-text-fill: #b91c1c;");
            return;
        }

        try {
            if (EmailService.verifyOtp(email, code)) {

                if (countdownTimeline != null) countdownTimeline.stop();
                EmailService.clearOtp(email);

                // 🔥 Connexion directe
                User user = userService.findByEmail(email);
                if (user == null) {
                    otpMsg.setText("Utilisateur introuvable !");
                    otpMsg.setStyle("-fx-text-fill: #b91c1c;");
                    return;
                }

                Session.setCurrentUser(user);
                otpMsg.setText("✅ Connexion réussie !");
                otpMsg.setStyle("-fx-text-fill: #15803d;");

                // Redirection selon rôle
                if ("RH".equalsIgnoreCase(user.getRole())) {
                    Router.go("/rh/RhShell.fxml", "RHPro - RH", 1400, 820);
                } else if ("EMPLOYE".equalsIgnoreCase(user.getRole())) {
                    Router.go("/employe/EmployeShell.fxml", "RHPro - Employé", 1400, 820);
                } else {
                    Router.go("/candidat/CandidatShell.fxml", "RHPro - Candidat", 1400, 820);
                }

            } else {

                long remaining = EmailService.getRemainingSeconds(email);

                if (remaining <= 0) {
                    otpMsg.setText("❌ Code expiré !");
                } else {
                    otpMsg.setText("❌ Code incorrect !");
                }

                otpMsg.setStyle("-fx-text-fill: #b91c1c;");
                otpField.clear();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            otpMsg.setText("Erreur lors de la connexion.");
            otpMsg.setStyle("-fx-text-fill: #b91c1c;");
        }
    }


    // ── Annuler le forgot password ────────────────────────────────────
    @FXML
    public void cancelForgot() {
        if (countdownTimeline != null) countdownTimeline.stop();
        EmailService.clearOtp(forgotEmailField.getText().trim());
        forgotPasswordBox.setVisible(false);
        forgotPasswordBox.setManaged(false);
        otpBox.setVisible(false);
        otpBox.setManaged(false);
        forgotMsg.setText("");
        otpMsg.setText("");
        forgotEmailField.clear();
        otpField.clear();
    }

    // ── Compte à rebours 5 minutes ────────────────────────────────────
    private void startCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long remaining = EmailService.getRemainingSeconds(forgotEmailField.getText().trim());
            long min = remaining / 60;
            long sec = remaining % 60;
            timerLabel.setText(String.format("⏱️ Code valide : %02d:%02d", min, sec));

            if (remaining <= 0) {
                countdownTimeline.stop();
                timerLabel.setText("❌ Code expiré !");
                timerLabel.setStyle("-fx-text-fill: #b91c1c;");
                otpField.setDisable(true);
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }



    @FXML
    public void back() {
        Router.go("/auth/Welcome.fxml", "RHPro", 520, 360);
    }
}