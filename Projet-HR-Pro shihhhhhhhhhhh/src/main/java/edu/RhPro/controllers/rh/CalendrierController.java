package edu.RhPro.controllers.rh;

import edu.RhPro.entities.Conge;
import edu.RhPro.services.CongeService;
import edu.RhPro.services.HolidayApiService;
import edu.RhPro.services.HolidayApiService.Holiday;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class CalendrierController {

    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;
    @FXML private VBox legendeContainer;
    @FXML private VBox upcomingContainer;
    @FXML private Label statusLabel;
    @FXML private Label holidayCountLabel;
    @FXML private Label congeCountLabel;

    private YearMonth currentMonth;
    private final HolidayApiService holidayService = new HolidayApiService();
    private final CongeService congeService = new CongeService();

    private List<Holiday> holidays = new ArrayList<>();
    private List<Conge> conges = new ArrayList<>();

    private static final String[] DAYS_FR = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
        loadData();
        buildCalendar();
        buildUpcoming();
    }

    @FXML
    public void prevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        loadData();
        buildCalendar();
        buildUpcoming();
    }

    @FXML
    public void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        loadData();
        buildCalendar();
        buildUpcoming();
    }

    @FXML
    public void goToday() {
        currentMonth = YearMonth.now();
        loadData();
        buildCalendar();
        buildUpcoming();
    }

    @FXML
    public void refresh() {
        loadData();
        buildCalendar();
        buildUpcoming();
    }

    private void loadData() {
        // Load holidays for current year (and next if December)
        holidays = new ArrayList<>(holidayService.getHolidays(currentMonth.getYear()));
        if (currentMonth.getMonthValue() == 12) {
            holidays.addAll(holidayService.getHolidays(currentMonth.getYear() + 1));
        }

        // Load congés
        try {
            conges = congeService.getData().stream()
                    .filter(c -> "ACCEPTE".equals(c.getStatut()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            conges = new ArrayList<>();
        }

        // Update stats
        long monthHolidays = holidays.stream()
                .filter(h -> h.getDate().getYear() == currentMonth.getYear()
                        && h.getDate().getMonthValue() == currentMonth.getMonthValue())
                .count();
        holidayCountLabel.setText(monthHolidays + " jour(s) férié(s) ce mois");

        long activeConges = conges.stream()
                .filter(c -> c.getDateFin() != null &&
                        !c.getDateFin().isBefore(currentMonth.atDay(1)) &&
                        c.getDateDebut() != null &&
                        !c.getDateDebut().isAfter(currentMonth.atEndOfMonth()))
                .count();
        congeCountLabel.setText(activeConges + " congé(s) actif(s) ce mois");

        statusLabel.setText("✅ Données chargées depuis l'API Nager.Date (Tunisie)");
        statusLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px;");
    }

    private void buildCalendar() {
        calendarGrid.getChildren().clear();
        calendarGrid.setHgap(6);
        calendarGrid.setVgap(6);

        // Set column constraints (7 columns equal width)
        calendarGrid.getColumnConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            calendarGrid.getColumnConstraints().add(cc);
        }

        // Update month label
        String monthName = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
        monthYearLabel.setText(monthName + " " + currentMonth.getYear());

        // Day headers
        for (int i = 0; i < 7; i++) {
            Label dayHeader = new Label(DAYS_FR[i]);
            dayHeader.setMaxWidth(Double.MAX_VALUE);
            dayHeader.setAlignment(Pos.CENTER);
            boolean isWeekend = (i == 5 || i == 6);
            dayHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; " +
                    "-fx-text-fill: " + (isWeekend ? "#94a3b8" : "#475569") + "; " +
                    "-fx-padding: 6 0;");
            calendarGrid.add(dayHeader, i, 0);
        }

        // Calendar days
        LocalDate firstDay = currentMonth.atDay(1);
        // Monday = 0, ..., Sunday = 6
        int startCol = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int row = 1, col = startCol;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            VBox cell = buildDayCell(date, today);
            calendarGrid.add(cell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    private VBox buildDayCell(LocalDate date, LocalDate today) {
        VBox cell = new VBox(2);
        cell.setAlignment(Pos.TOP_CENTER);
        cell.setPadding(new Insets(6, 4, 6, 4));
        cell.setMinHeight(72);
        cell.setPrefHeight(80);
        cell.setMaxWidth(Double.MAX_VALUE);

        boolean isToday     = date.equals(today);
        boolean isWeekend   = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean isHoliday   = holidays.stream().anyMatch(h -> h.getDate().equals(date));
        boolean hasConge    = conges.stream().anyMatch(c ->
                c.getDateDebut() != null && c.getDateFin() != null &&
                        !date.isBefore(c.getDateDebut()) && !date.isAfter(c.getDateFin()));

        // Determine cell background color
        String bg, border, dayColor;
        if (isToday) {
            bg = "#7c3aed"; border = "#5b21b6"; dayColor = "white";
        } else if (isHoliday) {
            bg = "#fef2f2"; border = "#fca5a5"; dayColor = "#dc2626";
        } else if (isWeekend) {
            bg = "#f8fafc"; border = "#e2e8f0"; dayColor = "#94a3b8";
        } else if (hasConge) {
            bg = "#eff6ff"; border = "#93c5fd"; dayColor = "#1d4ed8";
        } else {
            bg = "white"; border = "#e2e8f0"; dayColor = "#0f172a";
        }

        cell.setStyle("-fx-background-color: " + bg + "; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: " + border + "; " +
                "-fx-border-width: 1.5; " +
                "-fx-border-radius: 10;");

        if (isToday) {
            DropShadow ds = new DropShadow();
            ds.setRadius(8); ds.setOffsetY(3);
            ds.setColor(Color.color(0.49, 0.23, 0.93, 0.4));
            cell.setEffect(ds);
        }

        // Day number
        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        dayNum.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + dayColor + ";");

        cell.getChildren().add(dayNum);

        // Holiday badge
        if (isHoliday) {
            Holiday h = holidays.stream().filter(ho -> ho.getDate().equals(date)).findFirst().orElse(null);
            if (h != null) {
                Label badge = new Label(truncate(h.getName(), 14));
                badge.setWrapText(true);
                badge.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; " +
                        "-fx-font-size: 8px; -fx-font-weight: 600; " +
                        "-fx-background-radius: 4; -fx-padding: 1 4;");
                badge.setMaxWidth(Double.MAX_VALUE);
                badge.setAlignment(Pos.CENTER);
                cell.getChildren().add(badge);
            }
        }

        // Congé badge
        if (hasConge && !isHoliday) {
            Label badge = new Label("Congé");
            badge.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; " +
                    "-fx-font-size: 8px; -fx-font-weight: 600; " +
                    "-fx-background-radius: 4; -fx-padding: 1 4;");
            badge.setMaxWidth(Double.MAX_VALUE);
            badge.setAlignment(Pos.CENTER);
            cell.getChildren().add(badge);
        }

        // Tooltip on hover
        String tooltipText = buildTooltip(date, isHoliday, hasConge, isWeekend);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setStyle("-fx-font-size: 12px;");
        Tooltip.install(cell, tooltip);

        cell.setCursor(Cursor.HAND);

        // Hover effect
        String finalBg = bg, finalBorder = border;
        cell.setOnMouseEntered(e -> {
            if (!isToday) {
                cell.setStyle("-fx-background-color: " + finalBg + "; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #7c3aed; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 10; " +
                        "-fx-scale-x: 1.04; -fx-scale-y: 1.04;");
            }
        });
        cell.setOnMouseExited(e -> {
            cell.setStyle("-fx-background-color: " + finalBg + "; " +
                    "-fx-background-radius: 10; " +
                    "-fx-border-color: " + finalBorder + "; " +
                    "-fx-border-width: 1.5; " +
                    "-fx-border-radius: 10;");
        });

        return cell;
    }

    private String buildTooltip(LocalDate date, boolean isHoliday, boolean hasConge, boolean isWeekend) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        StringBuilder sb = new StringBuilder(date.format(fmt));
        if (isHoliday) {
            Holiday h = holidays.stream().filter(ho -> ho.getDate().equals(date)).findFirst().orElse(null);
            if (h != null) sb.append("\n🎉 ").append(h.getName());
        }
        if (hasConge) {
            long count = conges.stream()
                    .filter(c -> c.getDateDebut() != null && c.getDateFin() != null &&
                            !date.isBefore(c.getDateDebut()) && !date.isAfter(c.getDateFin()))
                    .count();
            sb.append("\n🏖 ").append(count).append(" employé(s) en congé");
        }
        if (isWeekend) sb.append("\n📅 Week-end");
        return sb.toString();
    }

    private void buildUpcoming() {
        upcomingContainer.getChildren().clear();

        LocalDate today = LocalDate.now();
        LocalDate in60days = today.plusDays(60);

        // Collect upcoming holidays
        List<Holiday> upcoming = holidays.stream()
                .filter(h -> !h.getDate().isBefore(today) && !h.getDate().isAfter(in60days))
                .sorted(Comparator.comparing(Holiday::getDate))
                .limit(8)
                .collect(Collectors.toList());

        if (upcoming.isEmpty()) {
            // Load next year's holidays too
            List<Holiday> nextYear = holidayService.getHolidays(today.getYear() + 1);
            upcoming = nextYear.stream()
                    .filter(h -> !h.getDate().isBefore(today) && !h.getDate().isAfter(in60days))
                    .sorted(Comparator.comparing(Holiday::getDate))
                    .limit(8)
                    .collect(Collectors.toList());
        }

        if (upcoming.isEmpty()) {
            Label empty = new Label("Aucun jour férié dans les 60 prochains jours.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            upcomingContainer.getChildren().add(empty);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH);
        for (Holiday h : upcoming) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                    "-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 8;");

            // Date pill
            long daysUntil = today.until(h.getDate(), java.time.temporal.ChronoUnit.DAYS);
            String pillColor = daysUntil == 0 ? "#7c3aed" : daysUntil <= 7 ? "#dc2626" : "#2563eb";
            Label datePill = new Label(h.getDate().format(fmt).toUpperCase());
            datePill.setStyle("-fx-background-color: " + pillColor + "; -fx-text-fill: white; " +
                    "-fx-font-size: 10px; -fx-font-weight: 700; " +
                    "-fx-background-radius: 6; -fx-padding: 3 8;");
            datePill.setMinWidth(85);

            // Name
            VBox names = new VBox(1);
            Label nameLabel2 = new Label(h.getName());
            nameLabel2.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
            names.getChildren().add(nameLabel2);
            HBox.setHgrow(names, Priority.ALWAYS);

            // Days until badge
            String daysText = daysUntil == 0 ? "Aujourd'hui" : "J-" + daysUntil;
            Label daysLabel = new Label(daysText);
            daysLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; " +
                    "-fx-text-fill: " + pillColor + ";");

            row.getChildren().addAll(datePill, names, daysLabel);
            upcomingContainer.getChildren().add(row);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}