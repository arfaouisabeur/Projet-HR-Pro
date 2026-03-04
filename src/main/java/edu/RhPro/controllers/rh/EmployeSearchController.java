package edu.RhPro.controllers.rh;

import edu.RhPro.entities.User;
import edu.RhPro.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class EmployeSearchController {

    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbRole;
    @FXML private Label lblCount;

    @FXML private TableView<User> table;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;

    private final UserService userService = new UserService();

    private final ObservableList<User> masterData = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;

    private Consumer<User> onUserSelected;

    public void setOnUserSelected(Consumer<User> onUserSelected) {
        this.onUserSelected = onUserSelected;
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Aucun employé."));

        filteredData = new FilteredList<>(masterData, u -> true);
        SortedList<User> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        cbRole.setItems(FXCollections.observableArrayList("Tous"));
        cbRole.setValue("Tous");

        tfSearch.textProperty().addListener((o, a, b) -> applyFilters());
        cbRole.valueProperty().addListener((o, a, b) -> applyFilters());

        table.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    choose(row.getItem());
                }
            });
            return row;
        });

        loadUsers();
    }

    private void loadUsers() {
        try {
            List<User> list = userService.getData();
            masterData.setAll(list);

            // roles from DB (dynamic)
            Set<String> roles = new HashSet<>();
            for (User u : list) {
                if (u != null && u.getRole() != null && !u.getRole().isBlank()) roles.add(u.getRole());
            }
            ObservableList<String> roleItems = FXCollections.observableArrayList();
            roleItems.add("Tous");
            roleItems.addAll(roles);
            cbRole.setItems(roleItems);
            cbRole.setValue("Tous");

            applyFilters();
        } catch (SQLException e) {
            e.printStackTrace();
            table.setPlaceholder(new Label("Erreur DB lors du chargement des employés."));
        }
    }

    private void applyFilters() {
        String q = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase();
        String role = cbRole.getValue();

        filteredData.setPredicate(u -> {
            if (u == null) return false;

            // role
            if (role != null && !role.equalsIgnoreCase("Tous")) {
                if (u.getRole() == null || !u.getRole().equalsIgnoreCase(role)) return false;
            }

            // text search
            if (!q.isEmpty()) {
                String nom = safe(u.getNom()).toLowerCase();
                String prenom = safe(u.getPrenom()).toLowerCase();
                String email = safe(u.getEmail()).toLowerCase();

                return nom.contains(q) || prenom.contains(q) || email.contains(q);
            }

            return true;
        });

        lblCount.setText(String.valueOf(filteredData.size()));
    }

    @FXML
    public void clearFilters() {
        tfSearch.clear();
        cbRole.setValue("Tous");
        applyFilters();
    }

    @FXML
    public void onChoose() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Info");
            a.setHeaderText(null);
            a.setContentText("Sélectionne un employé.");
            a.showAndWait();
            return;
        }
        choose(selected);
    }

    @FXML
    public void onCancel() {
        if (onUserSelected != null) onUserSelected.accept(null);
    }

    private void choose(User u) {
        if (onUserSelected != null) onUserSelected.accept(u);
    }

    private String safe(String s) { return s == null ? "" : s; }
}