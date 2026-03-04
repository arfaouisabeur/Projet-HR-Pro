package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Evenement;
import edu.RhPro.services.EvenementService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarController {

    @FXML private Label monthYearLabel;
    @FXML private GridPane dayHeaderGrid;
    @FXML private GridPane daysGrid;
    @FXML private Label statusLabel;

    private YearMonth currentYearMonth;
    private final EvenementService eventService = new EvenementService();
    private List<Evenement> allEvents;
    private Map<LocalDate, List<Evenement>> eventsByDate;
    private Map<LocalDate, String> holidaysByDate; // holiday name

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String HOLIDAY_API_URL = "https://date.nager.at/api/v3/PublicHolidays/";

    @FXML
    public void initialize() {
        currentYearMonth = YearMonth.now();
        initDayHeaders();
        loadEventsAndRefresh();
    }

    private void initDayHeaders() {
        String[] dayNames = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        dayHeaderGrid.getChildren().clear();
        for (int i = 0; i < 7; i++) {
            Label label = new Label(dayNames[i]);
            label.setStyle("-fx-font-weight: 600; -fx-text-fill: #475569; -fx-alignment: center;");
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            dayHeaderGrid.add(label, i, 0);
        }
    }

    private void loadEventsAndRefresh() {
        // Load events from database (do in background)
        new Thread(() -> {
            try {
                allEvents = eventService.getData();
                eventsByDate = allEvents.stream()
                        .collect(Collectors.groupingBy(e -> e.getDateDebut().toLocalDate()));

                // Fetch holidays for current year
                fetchHolidays(currentYearMonth.getYear());

                Platform.runLater(this::refreshCalendar);
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> statusLabel.setText("❌ Erreur chargement événements"));
            }
        }).start();
    }

    private void fetchHolidays(int year) {
        String url = HOLIDAY_API_URL + year + "/TN"; // TN = Tunisia
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                holidaysByDate = new HashMap<>();
                JSONArray jsonArray = new JSONArray(response.body());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject holiday = jsonArray.getJSONObject(i);
                    String dateStr = holiday.getString("date");
                    String name = holiday.getString("localName");
                    LocalDate date = LocalDate.parse(dateStr);
                    holidaysByDate.put(date, name);
                }
            } else {
                System.err.println("API error: " + response.statusCode());
                holidaysByDate = Collections.emptyMap();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            holidaysByDate = Collections.emptyMap();
        }
    }

    private void refreshCalendar() {
        daysGrid.getChildren().clear();
        monthYearLabel.setText(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)
                + " " + currentYearMonth.getYear());

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
        int startColumn = dayOfWeek - 1; // 0-based for grid

        int daysInMonth = currentYearMonth.lengthOfMonth();

        LocalDate today = LocalDate.now();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            int row = (startColumn + day - 1) / 7;
            int col = (startColumn + day - 1) % 7;

            VBox cell = createDayCell(date, day, date.equals(today));
            daysGrid.add(cell, col, row);
        }
    }

    private VBox createDayCell(LocalDate date, int dayNumber, boolean isToday) {
        VBox cell = new VBox(5);
        cell.setPrefHeight(80);
        cell.setPrefWidth(100);
        cell.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5;");
        cell.setAlignment(Pos.TOP_CENTER);

        // Day number
        Label dayLabel = new Label(String.valueOf(dayNumber));
        dayLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");
        if (isToday) {
            dayLabel.setStyle(dayLabel.getStyle() + "-fx-text-fill: #3b82f6; -fx-font-weight: 700;");
        }
        cell.getChildren().add(dayLabel);

        // Event indicators
        List<Evenement> dayEvents = eventsByDate.getOrDefault(date, Collections.emptyList());
        if (!dayEvents.isEmpty()) {
            HBox dots = new HBox(3);
            dots.setAlignment(Pos.CENTER);
            int showCount = Math.min(dayEvents.size(), 3);
            for (int i = 0; i < showCount; i++) {
                Circle dot = new Circle(4, Color.web("#3b82f6"));
                dots.getChildren().add(dot);
            }
            if (dayEvents.size() > 3) {
                Label more = new Label("+" + (dayEvents.size() - 3));
                more.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
                dots.getChildren().add(more);
            }
            cell.getChildren().add(dots);

            // Tooltip with event titles
            StringBuilder tooltipText = new StringBuilder("Événements:\n");
            dayEvents.forEach(e -> tooltipText.append("• ").append(e.getTitre()).append("\n"));
            Tooltip.install(cell, new Tooltip(tooltipText.toString()));
        }

        // Holiday indicator
        String holidayName = holidaysByDate.get(date);
        if (holidayName != null) {
            Label holidayLabel = new Label("🎉");
            holidayLabel.setStyle("-fx-font-size: 16px;");
            Tooltip.install(holidayLabel, new Tooltip(holidayName));
            cell.getChildren().add(holidayLabel);
        }

        return cell;
    }

    @FXML
    private void previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        loadEventsAndRefresh();
    }

    @FXML
    private void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        loadEventsAndRefresh();
    }

    @FXML
    private void goToToday() {
        currentYearMonth = YearMonth.now();
        loadEventsAndRefresh();
    }
}