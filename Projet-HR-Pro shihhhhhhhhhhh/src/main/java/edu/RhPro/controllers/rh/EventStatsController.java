package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Evenement;
import edu.RhPro.entities.Participation;
import edu.RhPro.services.EvenementService;
import edu.RhPro.services.ParticipationService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class EventStatsController {

    @FXML private VBox statsContainer;
    @FXML private Label totalEventsLabel;
    @FXML private Label totalParticipationsLabel;
    @FXML private Label totalAcceptedLabel;
    @FXML private Label totalPendingLabel;
    @FXML private VBox chartContainer;
    @FXML private VBox topListContainer;

    private final EvenementService eventService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();

    @FXML
    public void initialize() {
        loadStats();
    }

    @FXML
    public void refresh() {
        loadStats();
    }

    private void loadStats() {
        try {
            List<Evenement> events = eventService.getData();
            List<Participation> allParticipations = participationService.getData();

            // KPI counters
            totalEventsLabel.setText(String.valueOf(events.size()));
            totalParticipationsLabel.setText(String.valueOf(allParticipations.size()));
            totalAcceptedLabel.setText(String.valueOf(
                    allParticipations.stream().filter(p -> "ACCEPTE".equals(p.getStatut())).count()));
            totalPendingLabel.setText(String.valueOf(
                    allParticipations.stream().filter(p -> "EN_ATTENTE".equals(p.getStatut())).count()));

            // Count participations per event
            Map<Long, Long> countByEvent = allParticipations.stream()
                    .collect(Collectors.groupingBy(Participation::getEvenementId, Collectors.counting()));

            // Map event ID to event object
            Map<Long, Evenement> eventMap = events.stream()
                    .collect(Collectors.toMap(Evenement::getId, e -> e));

            // Sort by participation count desc
            List<Map.Entry<Long, Long>> sorted = countByEvent.entrySet().stream()
                    .filter(e -> eventMap.containsKey(e.getKey()))
                    .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                    .collect(Collectors.toList());

            buildBarChart(sorted, eventMap);
            buildTopList(sorted, eventMap, allParticipations);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void buildBarChart(List<Map.Entry<Long, Long>> sorted, Map<Long, Evenement> eventMap) {
        chartContainer.getChildren().clear();

        if (sorted.isEmpty()) {
            Label empty = new Label("Aucune participation enregistree.");
            empty.setStyle("-fx-text-fill: rgba(255,255,255,0.35); -fx-font-size: 14px;");
            chartContainer.getChildren().add(empty);
            return;
        }

        long maxCount = sorted.get(0).getValue();
        // Show top 8 max
        List<Map.Entry<Long, Long>> top = sorted.stream().limit(8).collect(Collectors.toList());

        /*String[] colors = {
                "#c084fc", "#60a5fa", "#34d399", "#fbbf24",
                "#f87171", "#38bdf8", "#e879f9", "#4ade80"
        };*/

        for (int i = 0; i < top.size(); i++) {
            Map.Entry<Long, Long> entry = top.get(i);
            Evenement ev = eventMap.get(entry.getKey());
            if (ev == null) continue;

            long count = entry.getValue();
            double ratio = maxCount > 0 ? (double) count / maxCount : 0;

            // Row: label + bar + count
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 0, 4, 0));

            // Event name label (fixed width)
            Label nameLabel = new Label(truncate(ev.getTitre(), 22));
            nameLabel.setPrefWidth(165);
            nameLabel.setMinWidth(165);
            nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: rgba(255,255,255,0.85);");

            // Bar container
            StackPane barWrapper = new StackPane();
            barWrapper.setPrefHeight(28);
            HBox.setHgrow(barWrapper, Priority.ALWAYS);
            barWrapper.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 6;");

            // Filled bar
            double barWidth = Math.max(ratio, 0.03); // min 3% so it's always visible
            HBox bar = new HBox();
            bar.setPrefHeight(28);
            bar.prefWidthProperty().bind(barWrapper.widthProperty().multiply(barWidth));
            bar.setStyle("-fx-background-color: " + getBarColor(i) + "; -fx-background-radius: 6;");
            barWrapper.getChildren().add(bar);
            StackPane.setAlignment(bar, Pos.CENTER_LEFT);

            // Count badge
            Label countLabel = new Label(count + " pers.");
            countLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: rgba(255,255,255,0.6);");
            countLabel.setMinWidth(55);

            // Medal for top 3
            if (i == 0) {
                nameLabel.setText("#1 " + truncate(ev.getTitre(), 19));
                nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #fbbf24;");
            } else if (i == 1) {
                nameLabel.setText("#2 " + truncate(ev.getTitre(), 19));
                nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #cbd5e1;");
            } else if (i == 2) {
                nameLabel.setText("#3 " + truncate(ev.getTitre(), 19));
                nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #fb923c;");
            }

            row.getChildren().addAll(nameLabel, barWrapper, countLabel);
            chartContainer.getChildren().add(row);
        }
    }

    private void buildTopList(List<Map.Entry<Long, Long>> sorted, Map<Long, Evenement> eventMap,
                              List<Participation> allParticipations) {
        topListContainer.getChildren().clear();

        if (sorted.isEmpty()) return;

        // Show top 5
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            Map.Entry<Long, Long> entry = sorted.get(i);
            Evenement ev = eventMap.get(entry.getKey());
            if (ev == null) continue;

            long total = entry.getValue();
            long accepted = allParticipations.stream()
                    .filter(p -> p.getEvenementId() == entry.getKey() && "ACCEPTE".equals(p.getStatut()))
                    .count();
            long pending = allParticipations.stream()
                    .filter(p -> p.getEvenementId() == entry.getKey() && "EN_ATTENTE".equals(p.getStatut()))
                    .count();
            long refused = allParticipations.stream()
                    .filter(p -> p.getEvenementId() == entry.getKey() && "REFUSE".equals(p.getStatut()))
                    .count();

            // Card
            VBox card = new VBox(6);
            card.setPadding(new Insets(12, 16, 12, 16));
            card.setStyle("-fx-background-color: #3D1A3B; -fx-background-radius: 12; " +
                    "-fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1; -fx-border-radius: 12;");

            // Top row: rank + title + total badge
            HBox topRow = new HBox(8);
            topRow.setAlignment(Pos.CENTER_LEFT);

            String rankStr = (i == 0) ? "#1" : (i == 1) ? "#2" : (i == 2) ? "#3" : "#" + (i + 1);
            Label rankLabel = new Label(rankStr);
            rankLabel.setStyle("-fx-font-size: 16px;");

            Label titleLabel = new Label(ev.getTitre());
            titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: white;");
            HBox.setHgrow(titleLabel, Priority.ALWAYS);

            Label totalBadge = new Label(total + " participants");
            totalBadge.setStyle("-fx-background-color: rgba(192,132,252,0.25); -fx-text-fill: #c084fc; " +
                    "-fx-background-radius: 20; -fx-border-color: #c084fc; -fx-border-width: 1; -fx-border-radius: 20; -fx-padding: 3 10; -fx-font-size: 11px; -fx-font-weight: 700;");

            topRow.getChildren().addAll(rankLabel, titleLabel, totalBadge);

            // Bottom row: status breakdown
            HBox statsRow = new HBox(10);
            statsRow.setAlignment(Pos.CENTER_LEFT);

            Label dateLabel = new Label(" " + (ev.getDateDebut() != null ?
                    ev.getDateDebut().toLocalDate().toString() : "N/A"));
            dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.4);");
            HBox.setHgrow(dateLabel, Priority.ALWAYS);

            Label accLabel = new Label("OK " + accepted + " acceptes");
            accLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #059669; -fx-font-weight: 600;");

            Label pendLabel = new Label(">> " + pending + " en attente");
            pendLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #f59e0b; -fx-font-weight: 600;");

            Label refLabel = new Label("X " + refused + " refuses");
            refLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc2626; -fx-font-weight: 600;");

            statsRow.getChildren().addAll(dateLabel, accLabel, pendLabel, refLabel);

            card.getChildren().addAll(topRow, statsRow);
            topListContainer.getChildren().add(card);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
    private String getBarColor(int index) {
        int pos = index % 8;
        if (pos == 0) return "#c084fc";
        if (pos == 1) return "#60a5fa";
        if (pos == 2) return "#34d399";
        if (pos == 3) return "#fbbf24";
        if (pos == 4) return "#f87171";
        if (pos == 5) return "#38bdf8";
        if (pos == 6) return "#e879f9";
        return "#4ade80";
    }

}
