package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Tache;
import edu.RhPro.entities.Projet;
import edu.RhPro.services.TacheService;
import edu.RhPro.services.ProjetService;
import edu.RhPro.utils.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import com.google.gson.Gson;

public class ChatbotController {

    @FXML private VBox chatMessages;
    @FXML private TextField chatInput;
    @FXML private Button sendButton;

    private TacheService tacheService = new TacheService();
    private ProjetService projetService = new ProjetService();

    // Store current tasks for context
    private List<Tache> currentTasks = new ArrayList<>();

    // Gson for JSON conversion
    private Gson gson = new Gson();

    // Python integration via Socket
    private Socket pythonSocket;
    private PrintWriter pythonOut;
    private BufferedReader pythonIn;
    private boolean isConnected = false;

    @FXML
    public void initialize() {
        System.out.println("🚀 ChatbotController initializing...");
        System.out.println("chatMessages is null? " + (chatMessages == null));
        System.out.println("chatInput is null? " + (chatInput == null));
        System.out.println("sendButton is null? " + (sendButton == null));

        if (chatMessages == null || chatInput == null || sendButton == null) {
            System.err.println("❌ FXML injection failed! Check fx:id in FXML file.");
            return;
        }

        // Set up event handlers
        setupEventHandlers();

        // Try to connect to Python
        connectToPython();

        // Add welcome message
        addSystemMessage("Bonjour ! Je suis votre assistant. Posez-moi des questions sur vos tâches !");

        System.out.println("✅ ChatbotController initialized");
    }



    @FXML
    private void handleSendButton() {
        System.out.println("🖱️ handleSendButton called from FXML");
        sendMessage();
    }
    private void setupEventHandlers() {
        // Send button handler
        sendButton.setOnAction(event -> {
            System.out.println("🖱️ Send button clicked (programmatic)");
            sendMessage();
        });
        System.out.println("Button disabled: " + sendButton.isDisabled());

        // Enter key handler
        chatInput.setOnAction(event -> {
            System.out.println("⌨️ Enter pressed!");
            sendMessage();
        });

        // Add focus listener to verify TextField works
        chatInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                System.out.println("📝 Chat input focused");
            }
        });
    }

    public void updateTasks(List<Tache> tasks) {
        System.out.println("📊 Updating tasks in chatbot: " + tasks.size() + " tasks");
        this.currentTasks = tasks;

        if (isConnected && pythonOut != null) {
            try {
                List<Map<String, Object>> taskList = new ArrayList<>();
                for (Tache task : tasks) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", task.getId());
                    map.put("titre", task.getTitre());
                    map.put("description", task.getDescription());
                    map.put("statut", task.getStatut());
                    map.put("projetId", task.getProjetId());
                    map.put("employeId", task.getEmployeId());
                    taskList.add(map);
                }

                String jsonData = gson.toJson(taskList);
                pythonOut.println("__CONTEXT__" + jsonData);
                System.out.println("📤 Sent context to Python");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void connectToPython() {
        try {
            System.out.println("🔌 Connecting to Python server...");

            pythonSocket = new Socket();
            pythonSocket.connect(new InetSocketAddress("127.0.0.1", 5000), 3000);

            pythonOut = new PrintWriter(pythonSocket.getOutputStream(), true);
            pythonIn = new BufferedReader(new InputStreamReader(pythonSocket.getInputStream()));
            isConnected = true;

            System.out.println("✅ Connected to Python server!");

            // Start listener thread
            Thread listener = new Thread(this::listenForResponses);
            listener.setDaemon(true);
            listener.start();

            // Send initial context
            if (!currentTasks.isEmpty()) {
                updateTasks(currentTasks);
            }

            Platform.runLater(() -> {
                addSystemMessage("✅ Connecté à l'assistant IA");
            });

        } catch (IOException e) {
            System.out.println("❌ Failed to connect to Python: " + e.getMessage());
            isConnected = false;
            Platform.runLater(() -> {
                addSystemMessage("⚠️ Assistant hors ligne. Utilisation du mode dégradé.");
            });
        }
    }

    private void listenForResponses() {
        try {
            String response;
            while ((response = pythonIn.readLine()) != null) {
                final String finalResponse = response;
                System.out.println("📥 Received from Python: " + finalResponse);
                Platform.runLater(() -> {
                    System.out.println("📢 UI update: removing typing indicator and adding message");
                    chatMessages.getChildren().removeIf(node -> "typing-indicator".equals(node.getId()));
                    addAssistantMessage(finalResponse);
                });
            }
        } catch (IOException e) {
            System.out.println("❌ Connection lost: " + e.getMessage());
            isConnected = false;
            Platform.runLater(() -> addSystemMessage("❌ Connexion perdue avec l'assistant"));
        }
    }

    private void sendMessage() {
        String message = chatInput.getText().trim();
        System.out.println("📤 sendMessage() called with: '" + message + "'");

        if (message.isEmpty()) {
            System.out.println("⚠️ Empty message, ignoring");
            return;
        }

        // Add user message to chat
        addUserMessage(message);
        chatInput.clear();


        Label typingLabel = new Label("L'assistant tape...");
        typingLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic;");
        typingLabel.setId("typing-indicator");

        Platform.runLater(() -> {
            chatMessages.getChildren().add(typingLabel);
            autoScroll();
        });
        // Add typing indicator


        // Send to Python or handle in Java
        if (isConnected && pythonOut != null) {
            System.out.println("🚀 Sending to Python...");
            CompletableFuture.runAsync(() -> {
                pythonOut.println(message);
            });
        } else {
            System.out.println("💻 Using Java fallback...");
            // Small delay to simulate thinking
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}

                String response = processWithJava(message);
                Platform.runLater(() -> {
                    chatMessages.getChildren().removeIf(node -> "typing-indicator".equals(node.getId()));
                    addAssistantMessage(response);
                });
            });
        }
    }

    private String processWithJava(String message) {
        message = message.toLowerCase();

        if (message.contains("bonjour") || message.contains("salut") || message.contains("hello")) {
            return "Bonjour ! Comment puis-je vous aider avec vos tâches ?";
        }

        if (message.contains("combien") && (message.contains("tâche") || message.contains("taches"))) {
            return "Vous avez " + currentTasks.size() + " tâches assignées.";
        }

        if (message.contains("montre") || message.contains("affiche") || message.contains("liste")) {
            if (currentTasks.isEmpty()) {
                return "Vous n'avez aucune tâche pour le moment.";
            }
            StringBuilder sb = new StringBuilder("Voici vos tâches :\n");
            for (int i = 0; i < Math.min(5, currentTasks.size()); i++) {
                Tache t = currentTasks.get(i);
                sb.append("• ").append(t.getTitre()).append(" (").append(t.getStatut()).append(")\n");
            }
            if (currentTasks.size() > 5) {
                sb.append("... et ").append(currentTasks.size() - 5).append(" autre(s)");
            }
            return sb.toString();
        }

        if (message.contains("test")) {
            return "✅ Le chatbot fonctionne !";
        }

        return "Je n'ai pas compris. Tapez 'test' pour vérifier que ça marche.";
    }

    private void addUserMessage(String message) {
        System.out.println("Adding user message: " + message);
        chatMessages.getChildren().removeIf(node -> "typing-indicator".equals(node.getId()));

        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 0, 5, 0));

        VBox messageContent = new VBox(2);
        messageContent.setAlignment(Pos.CENTER_RIGHT);

        Label timeLabel = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
        timeLabel.setPadding(new Insets(0, 5, 0, 0));

        Text text = new Text(message);
        TextFlow bubble = new TextFlow(text);
        bubble.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 18px 18px 5px 18px; -fx-padding: 10 15;");
        bubble.setMaxWidth(300);
        text.setFill(Color.WHITE);

        messageContent.getChildren().addAll(bubble, timeLabel);
        messageBox.getChildren().add(messageContent);

        chatMessages.getChildren().add(messageBox);
        autoScroll();
    }

    private void addAssistantMessage(String message) {
        System.out.println("Adding assistant message: " + message);
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 0, 5, 0));

        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 24px;");
        avatar.setPadding(new Insets(0, 10, 0, 0));

        VBox messageContent = new VBox(2);

        Label timeLabel = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
        timeLabel.setPadding(new Insets(0, 0, 0, 5));

        Text text = new Text(message);
        TextFlow bubble = new TextFlow(text);
        bubble.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 18px 18px 18px 5px; -fx-padding: 10 15;");
        bubble.setMaxWidth(300);
        text.setFill(Color.BLACK);

        messageContent.getChildren().addAll(bubble, timeLabel);
        messageBox.getChildren().addAll(avatar, messageContent);

        chatMessages.getChildren().add(messageBox);
        autoScroll();
    }

    private void addSystemMessage(String message) {
        System.out.println("Adding system message: " + message);
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setPadding(new Insets(10, 0, 10, 0));

        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-font-style: italic; -fx-background-color: #f9fafb; -fx-background-radius: 20; -fx-padding: 8 15;");

        messageBox.getChildren().add(label);
        chatMessages.getChildren().add(messageBox);
        autoScroll();
    }
    private void autoScroll() {
        chatMessages.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                javafx.scene.Node scrollPane = newScene.getRoot().lookup("#chatScrollPane");
                if (scrollPane instanceof ScrollPane) {
                    ((ScrollPane) scrollPane).setVvalue(1.0);
                }
            }
        });
    }

    public void shutdown() {
        try {
            if (pythonSocket != null && !pythonSocket.isClosed()) {
                pythonSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}