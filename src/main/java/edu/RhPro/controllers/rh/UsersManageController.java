package edu.RhPro.controllers.rh;

import edu.RhPro.entities.User;
import edu.RhPro.services.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.*;
import javafx.stage.FileChooser;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder;


import java.io.FileOutputStream;

import java.util.List;

public class UsersManageController {

    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Label totalLabel;
    @FXML private Label rhLabel;
    @FXML private Label employeLabel;
    @FXML private Label candidatLabel;

    @FXML private TableView<User> table;
    @FXML private TableColumn<User, String> colId, colNom, colPrenom, colEmail, colTel, colRole;

    @FXML private TextField nomField, prenomField, emailField, telField, adresseField;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label msgLabel;

    private final UserService service = new UserService();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;

    @FXML
    public void initialize() {

        roleCombo.setItems(FXCollections.observableArrayList("RH", "EMPLOYE", "CANDIDAT"));
        sortCombo.setItems(FXCollections.observableArrayList("ID croissant", "ID decroissant", "Nom A-Z", "Nom Z-A"));
        sortCombo.setValue("ID croissant");

        // Colonnes
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrenom()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colTel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelephone() == null ? "" : c.getValue().getTelephone()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole() == null ? "" : c.getValue().getRole()));

        // Remplir form au clic
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, u) -> {
            if (u == null) return;
            nomField.setText(u.getNom());
            prenomField.setText(u.getPrenom());
            emailField.setText(u.getEmail());
            telField.setText(u.getTelephone());
            adresseField.setText(u.getAdresse());
            roleCombo.setValue(u.getRole());
            passwordField.setText("");
        });

        // Recherche en temps réel
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSort());

        // Tri au changement
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSort());

        refresh();
    }

    private void applyFilterAndSort() {
        String keyword = searchField.getText().toLowerCase().trim();

        // Filtrer
        List<User> filtered = allUsers.stream()
                .filter(u -> keyword.isEmpty()
                        || String.valueOf(u.getId()).contains(keyword)
                        || u.getNom().toLowerCase().contains(keyword)
                        || u.getPrenom().toLowerCase().contains(keyword)
                        || u.getEmail().toLowerCase().contains(keyword)
                        || (u.getRole() != null && u.getRole().toLowerCase().contains(keyword)))
                .collect(java.util.stream.Collectors.toList());

        // Trier
        String sort = sortCombo.getValue();
        if (sort != null) {
            switch (sort) {
                case "ID croissant"  -> filtered.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
                case "ID decroissant"-> filtered.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
                case "Nom A-Z"       -> filtered.sort((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()));
                case "Nom Z-A"       -> filtered.sort((a, b) -> b.getNom().compareToIgnoreCase(a.getNom()));
            }
        }

        table.setItems(FXCollections.observableArrayList(filtered));
        msgLabel.setText(filtered.size() + " utilisateur(s) affiches.");
        msgLabel.setStyle("-fx-text-fill:#6b7280;");
    }

    @FXML
    public void refresh() {
        try {
            List<User> list = service.getData();
            allUsers = FXCollections.observableArrayList(list);
            applyFilterAndSort();
            loadStats();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("Erreur chargement.");
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
        }
    }

    @FXML
    public void clearForm() {
        nomField.clear(); prenomField.clear(); emailField.clear();
        telField.clear(); adresseField.clear(); passwordField.clear();
        roleCombo.setValue(null);
        searchField.clear();
        table.getSelectionModel().clearSelection();
        msgLabel.setText("");
    }

    @FXML
    public void add() {
        try {
            if (nomField.getText().isBlank() || prenomField.getText().isBlank() || emailField.getText().isBlank()) {
                msgLabel.setText("Nom / prenom / email obligatoires.");
                msgLabel.setStyle("-fx-text-fill:#b91c1c;");
                return;
            }
            if (passwordField.getText().isBlank()) {
                msgLabel.setText("Mot de passe obligatoire.");
                msgLabel.setStyle("-fx-text-fill:#b91c1c;");
                return;
            }
            User u = new User();
            u.setNom(nomField.getText()); u.setPrenom(prenomField.getText());
            u.setEmail(emailField.getText()); u.setMot_de_passe(passwordField.getText());
            u.setTelephone(telField.getText()); u.setAdresse(adresseField.getText());
            u.setRole(roleCombo.getValue() == null ? "EMPLOYE" : roleCombo.getValue());

            service.addUser(u);
            msgLabel.setText("Utilisateur ajoute.");
            msgLabel.setStyle("-fx-text-fill:#15803d;");
            refresh(); clearForm();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("Erreur ajout (email deja utilise ?).");
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
        }
    }

    @FXML
    public void update() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("Selectionne un utilisateur."); return; }
        try {
            selected.setNom(nomField.getText()); selected.setPrenom(prenomField.getText());
            selected.setEmail(emailField.getText()); selected.setTelephone(telField.getText());
            selected.setAdresse(adresseField.getText()); selected.setRole(roleCombo.getValue());

            if (passwordField.getText() == null || passwordField.getText().isBlank()) {
                service.updateUserWithoutPassword(selected);
            } else {
                selected.setMot_de_passe(passwordField.getText());
                service.updateUser(selected);
            }
            msgLabel.setText("Mise a jour OK.");
            msgLabel.setStyle("-fx-text-fill:#15803d;");
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("Erreur update.");
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
        }
    }

    @FXML
    public void delete() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("Selectionne un utilisateur."); return; }
        try {
            service.removeUserById(selected.getId());
            msgLabel.setText("Supprime.");
            msgLabel.setStyle("-fx-text-fill:#15803d;");
            refresh(); clearForm();
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("Erreur suppression.");
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
        }
    }
    @FXML
    public void exportExcel() {
        // Récupérer les users actuellement affichés dans la table
        var users = table.getItems();
        if (users.isEmpty()) {
            msgLabel.setText("Aucun utilisateur a exporter.");
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
            return;
        }

        // Choisir où sauvegarder
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en Excel");
        fileChooser.setInitialFileName("utilisateurs_rhpro.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        java.io.File dest = fileChooser.showSaveDialog(table.getScene().getWindow());
        if (dest == null) return;

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Utilisateurs");

            // ── Style header ──────────────────────────────────────────────
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)91, (byte)43, (byte)130}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderColor(XSSFCellBorder.BorderSide.BOTTOM, new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);

            // ── Style lignes paires ───────────────────────────────────────
            XSSFCellStyle evenStyle = workbook.createCellStyle();
            evenStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)245,(byte)240,(byte)255}, null));
            evenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            evenStyle.setAlignment(HorizontalAlignment.LEFT);

            // ── Style lignes impaires ─────────────────────────────────────
            XSSFCellStyle oddStyle = workbook.createCellStyle();
            oddStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
            oddStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            oddStyle.setAlignment(HorizontalAlignment.LEFT);

            // ── Style role coloré ─────────────────────────────────────────
            // ✅ APRÈS - remplace par ça
            XSSFCellStyle rhStyle = workbook.createCellStyle();
            rhStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)220,(byte)252,(byte)231}, null));
            rhStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont rhFont = (XSSFFont) workbook.createFont();
            rhFont.setBold(true);
            rhFont.setColor(new XSSFColor(new byte[]{(byte)21,(byte)128,(byte)61}, null));
            rhStyle.setFont(rhFont);

            XSSFCellStyle empStyle = workbook.createCellStyle();
            empStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)219,(byte)234,(byte)254}, null));
            empStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont empFont = (XSSFFont) workbook.createFont();
            empFont.setBold(true);
            empFont.setColor(new XSSFColor(new byte[]{(byte)29,(byte)78,(byte)216}, null));
            empStyle.setFont(empFont);

            XSSFCellStyle candStyle = workbook.createCellStyle();
            candStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)254,(byte)243,(byte)199}, null));
            candStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont candFont = (XSSFFont) workbook.createFont();
            candFont.setBold(true);
            candFont.setColor(new XSSFColor(new byte[]{(byte)146,(byte)64,(byte)14}, null));
            candStyle.setFont(candFont);

            // ── Ligne header ──────────────────────────────────────────────
            String[] headers = {"ID", "Nom", "Prenom", "Email", "Telephone", "Adresse", "Role"};
            Row headerRow = sheet.createRow(0);
            headerRow.setHeight((short) 500);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Lignes données ────────────────────────────────────────────
            int rowNum = 1;
            for (User u : users) {
                Row row = sheet.createRow(rowNum);
                row.setHeight((short) 400);
                XSSFCellStyle baseStyle = (rowNum % 2 == 0) ? evenStyle : oddStyle;

                // Colonnes texte normales
                String[] values = {
                        String.valueOf(u.getId()),
                        u.getNom(),
                        u.getPrenom(),
                        u.getEmail(),
                        u.getTelephone() == null ? "" : u.getTelephone(),
                        u.getAdresse()   == null ? "" : u.getAdresse(),
                        u.getRole()      == null ? "" : u.getRole()
                };

                for (int i = 0; i < values.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(values[i]);

                    // Colorer la colonne Role selon la valeur
                    if (i == 6) {
                        switch (values[i]) {
                            case "RH"       -> cell.setCellStyle(rhStyle);
                            case "EMPLOYE"  -> cell.setCellStyle(empStyle);
                            case "CANDIDAT" -> cell.setCellStyle(candStyle);
                            default         -> cell.setCellStyle(baseStyle);
                        }
                    } else {
                        cell.setCellStyle(baseStyle);
                    }
                }
                rowNum++;
            }

            // ── Ajuster largeur colonnes automatiquement ──────────────────
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Ajouter un peu de marge
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
            }

            // ── Ligne total en bas ────────────────────────────────────────
            Row totalRow = sheet.createRow(rowNum + 1);
            XSSFCellStyle totalStyle = workbook.createCellStyle();
            totalStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)91,(byte)43,(byte)130}, null));
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalFont.setColor(IndexedColors.WHITE.getIndex());
            totalStyle.setFont(totalFont);

            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("Total : " + users.size() + " utilisateur(s)");
            totalLabel.setCellStyle(totalStyle);

            // ── Sauvegarder ───────────────────────────────────────────────
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                workbook.write(fos);
            }

            msgLabel.setText("Excel exporte : " + dest.getName());
            msgLabel.setStyle("-fx-text-fill:#15803d; -fx-font-weight:bold;");

        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("Erreur export : " + e.getMessage());
            msgLabel.setStyle("-fx-text-fill:#b91c1c;");
        }
    }
    private void loadStats() {
        try {
            long nbRH       = allUsers.stream().filter(u -> "RH".equalsIgnoreCase(u.getRole())).count();
            long nbEmploye  = allUsers.stream().filter(u -> "EMPLOYE".equalsIgnoreCase(u.getRole())).count();
            long nbCandidat = allUsers.stream().filter(u -> "CANDIDAT".equalsIgnoreCase(u.getRole())).count();

            totalLabel.setText(String.valueOf(allUsers.size()));
            rhLabel.setText(String.valueOf(nbRH));
            employeLabel.setText(String.valueOf(nbEmploye));
            candidatLabel.setText(String.valueOf(nbCandidat));

            barChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("RH", nbRH));
            series.getData().add(new XYChart.Data<>("Employes", nbEmploye));
            series.getData().add(new XYChart.Data<>("Candidats", nbCandidat));
            barChart.getData().add(series);

            // Couleurs barres
            javafx.application.Platform.runLater(() -> {
                barChart.lookupAll(".data0.chart-bar").forEach(n -> n.setStyle("-fx-bar-fill:#5b2b82;"));
                barChart.lookupAll(".data1.chart-bar").forEach(n -> n.setStyle("-fx-bar-fill:#2c7be5;"));
                barChart.lookupAll(".data2.chart-bar").forEach(n -> n.setStyle("-fx-bar-fill:#f59e0b;"));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}