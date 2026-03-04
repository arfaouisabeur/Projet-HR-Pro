package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Tache;
import edu.RhPro.services.GeminiApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.google.gson.Gson;

public class ChatbotController {

    // ── FXML ──────────────────────────────────────
    @FXML private VBox        chatMessages;
    @FXML private TextField   chatInput;
    @FXML private Button      sendButton;
    @FXML private ScrollPane  chatScrollPane;
    @FXML private HBox        offlineBanner;
    @FXML private Label       bannerLabel;      // ← nouveau : texte dynamique de la bannière
    @FXML private Label       statusLabel;
    @FXML private Circle      statusCircle;
    @FXML private Circle      onlineDot;

    @FXML private Label statTodo;
    @FXML private Label statDoing;
    @FXML private Label statDone;
    @FXML private Label statRate;

    // ── STATE ─────────────────────────────────────
    private List<Tache> currentTasks = new ArrayList<>();
    private final Gson  gson         = new Gson();

    private enum Mode { PYTHON, GEMINI, LOCAL }
    private Mode currentMode = Mode.LOCAL;

    // Python socket
    private Socket         pythonSocket;
    private PrintWriter    pythonOut;
    private BufferedReader pythonIn;

    // Gemini IA
    private final GeminiApiService geminiApi = new GeminiApiService();

    // ── INIT ──────────────────────────────────────
    @FXML
    public void initialize() {
        chatInput.setOnAction(e -> sendMessage());

        final String base  = "-fx-background-color: #3D1A3B; -fx-text-fill: white; -fx-font-size: 14px;" +
                "-fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40;" +
                "-fx-max-width: 40; -fx-max-height: 40; -fx-cursor: hand;";
        final String hover = "-fx-background-color: #5a2757; -fx-text-fill: white; -fx-font-size: 14px;" +
                "-fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40;" +
                "-fx-max-width: 40; -fx-max-height: 40; -fx-cursor: hand;";
        sendButton.setOnMouseEntered(e -> sendButton.setStyle(hover));
        sendButton.setOnMouseExited(e  -> sendButton.setStyle(base));

        // Message de bienvenue pendant le test de connexion
        addSystemMsg("⏳ Connexion à l'IA en cours...");

        // Détecter le meilleur mode en arrière-plan
        detectBestMode();
    }

    // ── DÉTECTION DU MODE ─────────────────────────
    private void detectBestMode() {
        CompletableFuture.runAsync(() -> {

            // 1. Python local en priorité
            try {
                pythonSocket = new Socket();
                pythonSocket.connect(new InetSocketAddress("127.0.0.1", 5000), 1500);
                pythonOut    = new PrintWriter(pythonSocket.getOutputStream(), true);
                pythonIn     = new BufferedReader(new InputStreamReader(pythonSocket.getInputStream()));
                currentMode  = Mode.PYTHON;
                Thread t = new Thread(this::listenPython);
                t.setDaemon(true);
                t.start();
                Platform.runLater(() -> {
                    setStatus("Python • En ligne", "#16a34a", "#22c55e");
                    setBanner(false, null);
                    addSystemMsg("✅ Connecté au serveur Python local !");
                });
                return;
            } catch (IOException ignored) {}

            // 2. Gemini — test complet avec diagnostic
            if (geminiApi.isConfigured()) {
                Platform.runLater(() -> addSystemMsg("🔍 Test de connexion Gemini..."));

                String connError = geminiApi.testConnection();
                if (connError == null) {
                    // ✅ Gemini fonctionne
                    currentMode = Mode.GEMINI;
                    Platform.runLater(() -> {
                        setStatus("Gemini IA • En ligne", "#1a73e8", "#4285f4");
                        setBanner(false, null);
                        // Effacer les messages de diagnostic
                        chatMessages.getChildren().clear();
                        addSystemMsg("✅ Gemini IA connecté ! Posez n'importe quelle question.");
                    });
                    return;
                } else {
                    // ❌ Gemini inaccessible — afficher le vrai problème
                    final String errMsg = connError;
                    Platform.runLater(() -> {
                        setStatus("Mode local", "#dc2626", "#ef4444");
                        setBanner(true, "⚠ " + errMsg);
                        chatMessages.getChildren().clear();
                        addSystemMsg("❌ Gemini inaccessible : " + errMsg);
                        addSystemMsg("💡 Conseil : Essayez avec le hotspot de votre téléphone puis relancez.");
                        addSystemMsg("Mode local activé — commandes basiques disponibles. Tapez 'aide'.");
                    });
                }
            } else {
                // Clé non configurée
                Platform.runLater(() -> {
                    setStatus("Mode local", "#dc2626", "#ef4444");
                    setBanner(true, "Clé Gemini non configurée dans GeminiApiService.java");
                    chatMessages.getChildren().clear();
                    addSystemMsg("⚠ Clé API non configurée. Ajoutez votre clé dans GeminiApiService.java");
                    addSystemMsg("👉 Clé gratuite sur : https://aistudio.google.com/app/apikey");
                });
            }

            currentMode = Mode.LOCAL;
        });
    }

    // ── CHIPS ──────────────────────────────────────
    @FXML private void handleChipTaches()  { quickSend("Montre-moi toutes mes tâches avec leur statut"); }
    @FXML private void handleChipConseil() { quickSend("Donne-moi un conseil de productivité personnalisé"); }
    @FXML private void handleChipStats()   { quickSend("Donne-moi mes statistiques complètes et une analyse"); }
    @FXML private void handleChipUrgent()  { quickSend("Quelles sont mes tâches les plus urgentes ?"); }
    @FXML private void handleSendButton()  { sendMessage(); }

    private void quickSend(String text) {
        chatInput.setText(text);
        sendMessage();
    }

    // ── UPDATE TASKS ──────────────────────────────
    public void updateTasks(List<Tache> tasks) {
        this.currentTasks = new ArrayList<>(tasks);
        Platform.runLater(this::refreshStats);

        if (currentMode == Mode.PYTHON && pythonOut != null) {
            try {
                List<Map<String, Object>> list = new ArrayList<>();
                for (Tache t : tasks) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",          t.getId());
                    m.put("titre",       t.getTitre());
                    m.put("description", t.getDescription());
                    m.put("statut",      t.getStatut());
                    m.put("projetId",    t.getProjetId());
                    list.add(m);
                }
                pythonOut.println("__CONTEXT__" + gson.toJson(list));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void refreshStats() {
        long todo  = currentTasks.stream().filter(t -> "TODO" .equals(t.getStatut())).count();
        long doing = currentTasks.stream().filter(t -> "DOING".equals(t.getStatut())).count();
        long done  = currentTasks.stream().filter(t -> "DONE" .equals(t.getStatut())).count();
        int  rate  = currentTasks.isEmpty() ? 0 : (int)((done * 100) / currentTasks.size());

        if (statTodo  != null) statTodo .setText(String.valueOf(todo));
        if (statDoing != null) statDoing.setText(String.valueOf(doing));
        if (statDone  != null) statDone .setText(String.valueOf(done));
        if (statRate  != null) statRate .setText(rate + "%");
    }

    // ── ENVOI MESSAGE ─────────────────────────────
    private void sendMessage() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;
        chatInput.clear();
        chatInput.setDisable(true);
        sendButton.setDisable(true);

        addUserBubble(text);
        addTypingBubble();

        switch (currentMode) {
            case PYTHON -> sendToPython(text);
            case GEMINI -> sendToGemini(text);
            default     -> sendToLocal(text);
        }
    }

    // ── PYTHON ────────────────────────────────────
    private void sendToPython(String text) {
        CompletableFuture.runAsync(() -> pythonOut.println(text));
    }

    private void listenPython() {
        try {
            String line;
            while ((line = pythonIn.readLine()) != null) {
                final String reply = line;
                Platform.runLater(() -> {
                    removeTyping();
                    addBotBubble(reply);
                    enableInput();
                });
            }
        } catch (IOException e) {
            currentMode = Mode.LOCAL;
            Platform.runLater(() -> {
                removeTyping();
                addSystemMsg("⚠ Connexion Python perdue. Mode local activé.");
                setStatus("Mode local", "#dc2626", "#ef4444");
                setBanner(true, "Connexion Python perdue");
                enableInput();
            });
        }
    }

    // ── GEMINI ────────────────────────────────────
    private void sendToGemini(String text) {
        CompletableFuture.runAsync(() -> {
            try {
                String reply = geminiApi.sendMessage(text, currentTasks);
                Platform.runLater(() -> {
                    removeTyping();
                    addBotBubble(reply);
                    enableInput();
                });
            } catch (IOException e) {
                e.printStackTrace();
                String reply = localReply(text);
                Platform.runLater(() -> {
                    removeTyping();
                    addSystemMsg("⚠ Gemini temporairement indisponible — réponse locale");
                    addBotBubble(reply);
                    enableInput();
                });
            }
        });
    }

    // ── LOCAL FALLBACK ────────────────────────────
    private void sendToLocal(String text) {
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            String reply = localReply(text);
            Platform.runLater(() -> {
                removeTyping();
                addBotBubble(reply);
                enableInput();
            });
        });
    }

    private String localReply(String input) {
        String m = input.toLowerCase()
                .replaceAll("[àâä]","a").replaceAll("[éèêë]","e")
                .replaceAll("[îï]","i").replaceAll("[ôö]","o")
                .replaceAll("[ùûü]","u").replaceAll("[ç]","c");

        if (m.matches(".*(bonjour|salut|hello|hey|coucou|bonsoir).*"))
            return "Bonjour ! 👋 Je suis en mode local.\n\nPour des réponses intelligentes, essayez sur le hotspot de votre téléphone et relancez l'appli.";

        if (m.matches(".*(combien|nombre|total).*tache.*"))
            return "Vous avez " + currentTasks.size() + " tâche(s) assignée(s).";

        if (m.matches(".*(montre|affiche|liste|voir|toutes).*tache.*")) {
            if (currentTasks.isEmpty()) return "Vous n'avez aucune tâche pour le moment.";
            StringBuilder sb = new StringBuilder("Vos tâches (" + currentTasks.size() + ") :\n");
            currentTasks.forEach(t -> sb.append("  • ").append(t.getTitre()).append(" [").append(t.getStatut()).append("]\n"));
            return sb.toString().trim();
        }

        if (m.matches(".*(todo|a faire|pas commence).*")) {
            List<Tache> l = byStatut("TODO");
            if (l.isEmpty()) return "✅ Aucune tâche à faire !";
            StringBuilder sb = new StringBuilder("Tâches à faire (" + l.size() + ") :\n");
            l.forEach(t -> sb.append("  • ").append(t.getTitre()).append("\n"));
            return sb.toString().trim();
        }

        if (m.matches(".*(doing|en cours).*")) {
            List<Tache> l = byStatut("DOING");
            if (l.isEmpty()) return "Aucune tâche en cours.";
            StringBuilder sb = new StringBuilder("En cours (" + l.size() + ") :\n");
            l.forEach(t -> sb.append("  • ").append(t.getTitre()).append("\n"));
            return sb.toString().trim();
        }

        if (m.matches(".*(done|termin|fini|complet).*")) {
            List<Tache> l = byStatut("DONE");
            if (l.isEmpty()) return "Aucune tâche terminée.";
            StringBuilder sb = new StringBuilder("Terminées (" + l.size() + ") :\n");
            l.forEach(t -> sb.append("  • ").append(t.getTitre()).append("\n"));
            return sb.toString().trim();
        }

        if (m.matches(".*(stat|productiv|taux|bilan|rapport|progress).*")) {
            long todo  = byStatut("TODO") .size();
            long doing = byStatut("DOING").size();
            long done  = byStatut("DONE") .size();
            int  rate  = currentTasks.isEmpty() ? 0 : (int)((done * 100) / currentTasks.size());
            String eval = rate >= 80 ? "🏆 Excellent !" : rate >= 50 ? "👍 Bien !" : "💪 Continuez !";
            return "📊 Bilan :\n  À faire : " + todo + "\n  En cours : " + doing +
                    "\n  Terminées : " + done + "\n  Complétion : " + rate + "%  " + eval;
        }

        if (m.matches(".*(conseil|astuce|productiv|recommand|amelior).*")) {
            long doing = byStatut("DOING").size();
            long todo  = byStatut("TODO") .size();
            long done  = byStatut("DONE") .size();
            int  rate  = currentTasks.isEmpty() ? 0 : (int)((done * 100) / currentTasks.size());
            if (done == currentTasks.size() && !currentTasks.isEmpty()) return "🎉 Félicitations ! Toutes vos tâches sont terminées !";
            if (doing > 3) return "⚠️ Vous avez " + doing + " tâches en cours. Concentrez-vous sur une seule à la fois !";
            if (todo > 5)  return "📋 " + todo + " tâches en attente. Priorisez les 3 plus importantes.";
            return "💡 Planifiez votre journée le matin et commencez par la tâche la plus difficile !";
        }

        if (m.matches(".*(urgent|priorit|important|critique).*")) {
            List<Tache> l = byStatut("TODO");
            if (l.isEmpty()) return "✅ Aucune tâche urgente !";
            StringBuilder sb = new StringBuilder("🚨 Tâches prioritaires :\n");
            l.forEach(t -> sb.append("  • ").append(t.getTitre()).append("\n"));
            return sb.toString().trim();
        }

        if (m.matches(".*(merci|thanks|super|bravo|parfait).*"))
            return "De rien ! 😊";

        if (m.matches(".*(aide|help|commande|que peux|que sais).*"))
            return "Commandes disponibles :\n" +
                    "  • 'mes tâches' → liste toutes vos tâches\n" +
                    "  • 'stats' → bilan de progression\n" +
                    "  • 'conseil' → conseils de productivité\n" +
                    "  • 'urgent' → tâches prioritaires\n" +
                    "  • 'en cours' / 'à faire' / 'terminées'\n\n" +
                    "💡 Essayez avec le hotspot téléphone pour activer Gemini IA !";

        return "Je suis en mode local et ne comprends pas encore cette question.\n\n" +
                "Tapez 'aide' pour les commandes disponibles.\n" +
                "💡 Activez Gemini IA pour des réponses libres : essayez avec le hotspot de votre téléphone.";
    }

    private List<Tache> byStatut(String statut) {
        return currentTasks.stream().filter(t -> statut.equals(t.getStatut())).collect(Collectors.toList());
    }

    // ── UI HELPERS ────────────────────────────────
    private void enableInput() {
        chatInput.setDisable(false);
        sendButton.setDisable(false);
        chatInput.requestFocus();
    }

    private void setStatus(String label, String textColor, String dotColor) {
        if (statusLabel != null) {
            statusLabel.setText(label);
            statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + textColor + "; -fx-font-weight: 600;");
        }
        if (statusCircle != null) statusCircle.setFill(Color.web(dotColor));
        if (onlineDot    != null) onlineDot   .setFill(Color.web(dotColor));
    }

    /**
     * Affiche ou cache la bannière avec un message dynamique.
     * @param visible  true pour afficher
     * @param message  texte à afficher (null = garder le texte actuel)
     */
    private void setBanner(boolean visible, String message) {
        if (offlineBanner != null) {
            offlineBanner.setVisible(visible);
            offlineBanner.setManaged(visible);
        }
        if (bannerLabel != null && message != null) {
            bannerLabel.setText(message);
        }
    }

    // ── BULLES DE CHAT ────────────────────────────
    private void addSystemMsg(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(4, 0, 4, 0));
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(270);
        lbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;" +
                "-fx-background-color: #f3f4f6; -fx-background-radius: 12;" +
                "-fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 12;" +
                "-fx-padding: 5 12;");
        row.getChildren().add(lbl);
        chatMessages.getChildren().add(row);
        scrollBottom();
    }

    private void addUserBubble(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(2, 0, 2, 50));
        VBox col = new VBox(3);
        col.setAlignment(Pos.CENTER_RIGHT);
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(220);
        bubble.setStyle("-fx-background-color: #3D1A3B; -fx-text-fill: white;" +
                "-fx-font-size: 13px; -fx-background-radius: 16 16 4 16;" +
                "-fx-padding: 10 13;" +
                "-fx-effect: dropshadow(gaussian,rgba(61,26,59,0.2),6,0,0,2);");
        Label time = new Label(now());
        time.setStyle("-fx-font-size: 9px; -fx-text-fill: #c4b5fd;");
        time.setPadding(new Insets(0, 3, 0, 0));
        col.getChildren().addAll(bubble, time);
        row.getChildren().add(col);
        chatMessages.getChildren().add(row);
        scrollBottom();
    }

    private void addBotBubble(String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.BOTTOM_LEFT);
        row.setPadding(new Insets(2, 50, 2, 0));
        StackPane av = new StackPane();
        av.setMinSize(30, 30); av.setMaxSize(30, 30);
        Circle bg = new Circle(15, Color.web("#3D1A3B"));
        Label  ic = new Label("\uD83E\uDD16");
        ic.setStyle("-fx-font-size: 13px;");
        av.getChildren().addAll(bg, ic);
        av.setAlignment(Pos.CENTER);
        VBox col = new VBox(3);
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(215);
        bubble.setStyle("-fx-background-color: white; -fx-text-fill: #1f2937;" +
                "-fx-font-size: 13px; -fx-background-radius: 16 16 16 4;" +
                "-fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 16 16 16 4;" +
                "-fx-padding: 10 13;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),5,0,0,2);");
        Label time = new Label(now());
        time.setStyle("-fx-font-size: 9px; -fx-text-fill: #d1d5db;");
        time.setPadding(new Insets(0, 0, 0, 3));
        col.getChildren().addAll(bubble, time);
        row.getChildren().addAll(av, col);
        chatMessages.getChildren().add(row);
        scrollBottom();
    }

    private void addTypingBubble() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.BOTTOM_LEFT);
        row.setId("typing-indicator");
        row.setPadding(new Insets(2, 50, 2, 0));
        StackPane av = new StackPane();
        av.setMinSize(30, 30); av.setMaxSize(30, 30);
        Circle bg = new Circle(15, Color.web("#3D1A3B"));
        Label  ic = new Label("\uD83E\uDD16");
        ic.setStyle("-fx-font-size: 13px;");
        av.getChildren().addAll(bg, ic);
        Label dots = new Label("• • •");
        dots.setStyle("-fx-background-color: white; -fx-text-fill: #9ca3af;" +
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-background-radius: 16 16 16 4;" +
                "-fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 16 16 16 4;" +
                "-fx-padding: 8 14;");
        row.getChildren().addAll(av, dots);
        chatMessages.getChildren().add(row);
        scrollBottom();
    }

    private void removeTyping() {
        chatMessages.getChildren().removeIf(n -> "typing-indicator".equals(n.getId()));
    }

    private void scrollBottom() {
        Platform.runLater(() -> {
            chatMessages.layout();
            if (chatScrollPane != null) {
                chatScrollPane.layout();
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public void shutdown() {
        try { if (pythonSocket != null && !pythonSocket.isClosed()) pythonSocket.close(); }
        catch (IOException e) { e.printStackTrace(); }
    }
}