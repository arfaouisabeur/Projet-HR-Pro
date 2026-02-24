package edu.RhPro.controllers.rh;

import com.twilio.Twilio;
import edu.RhPro.entities.Conge;
import edu.RhPro.services.CongeService;
import edu.RhPro.services.ReponseService;
import edu.RhPro.services.SmsService;
import java.sql.ResultSet;
import edu.RhPro.tools.MyConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CongesManageController {

    @FXML private TableView<Conge> table;
    @FXML private TableColumn<Conge, Long> colId;
    @FXML private TableColumn<Conge, Long> colEmploye;
    @FXML private TableColumn<Conge, String> colType;
    @FXML private TableColumn<Conge, LocalDate> colDebut;
    @FXML private TableColumn<Conge, LocalDate> colFin;
    @FXML private TableColumn<Conge, String> colDesc;

    @FXML private ComboBox<String> cbCriteria;
    @FXML private TextField tfSearch;
    @FXML private TextArea taCommentaire;
    @FXML private Label msgLabel;

    private final CongeService congeService = new CongeService();
    private final ReponseService reponseService = new ReponseService();

    private ObservableList<Conge> masterData;
    private FilteredList<Conge> filteredData;

    @FXML
    public void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmploye.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeConge"));
        colDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Critères
        cbCriteria.setItems(FXCollections.observableArrayList(
                "ID",
                "Employé",
                "Type",
                "Date Début",
                "Date Fin"
        ));
        cbCriteria.getSelectionModel().selectFirst();

        loadData();

        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        // Hover animation
        table.setRowFactory(tv -> {
            TableRow<Conge> row = new TableRow<>();

            row.hoverProperty().addListener((obs, wasHover, isHover) -> {
                if (isHover && !row.isEmpty()) {
                    row.setStyle("-fx-background-color:#ddd6fe;");
                } else {
                    row.setStyle("");
                }
            });

            return row;
        });
    }

    private void loadData() {
        try {
            List<Conge> list = congeService.findPending();
            masterData = FXCollections.observableArrayList(list);

            filteredData = new FilteredList<>(masterData, p -> true);
            SortedList<Conge> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(table.comparatorProperty());

            table.setItems(sortedData);
            msgLabel.setText(list.size() + " demande(s)");

        } catch (SQLException e) {
            msgLabel.setText("Erreur DB");
        }
    }

    private void applyFilter() {

        String keyword = tfSearch.getText();
        String criteria = cbCriteria.getValue();

        filteredData.setPredicate(conge -> {

            if (keyword == null || keyword.isEmpty())
                return true;

            switch (criteria) {

                case "ID":
                    return String.valueOf(conge.getId()).contains(keyword);

                case "Employé":
                    return String.valueOf(conge.getEmployeeId()).contains(keyword);

                case "Type":
                    return conge.getTypeConge() != null &&
                            conge.getTypeConge().toLowerCase().contains(keyword.toLowerCase());

                case "Date Début":
                    return conge.getDateDebut() != null &&
                            conge.getDateDebut().toString().contains(keyword);

                case "Date Fin":
                    return conge.getDateFin() != null &&
                            conge.getDateFin().toString().contains(keyword);

                default:
                    return true;
            }
        });

        highlightRows(keyword);
    }

    private void highlightRows(String keyword) {

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Conge item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty || keyword == null || keyword.isEmpty()) {
                    setStyle("");
                } else {
                    setStyle("-fx-background-color:#fef08a;");
                }
            }
        });
    }

    @FXML
    private void onReset() {
        tfSearch.clear();
        cbCriteria.getSelectionModel().selectFirst();
        filteredData.setPredicate(p -> true);
        table.refresh();
    }

    @FXML
    private void onAccept() {
        updateStatus("ACCEPTEE");
    }

    @FXML
    private void onRefuse() {
        updateStatus("REFUSEE");
    }

    private void updateStatus(String statut) {

        Conge selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            msgLabel.setText("Sélectionne une demande.");
            return;
        }

        String commentaire = taCommentaire.getText();

        if (commentaire == null || commentaire.trim().isEmpty()) {
            msgLabel.setText("Ajoute un commentaire avant de valider.");
            return;
        }

        try {

            Connection cnx = MyConnection.getInstance().getCnx();

            // 1️⃣ Mettre à jour le statut du congé
            PreparedStatement ps1 = cnx.prepareStatement(
                    "UPDATE conge_tt SET statut=? WHERE id=?"
            );

            ps1.setString(1, statut);
            ps1.setLong(2, selected.getId());
            ps1.executeUpdate();


            // 2️⃣ Vérifier si une réponse existe déjà
            PreparedStatement check = cnx.prepareStatement(
                    "SELECT id FROM reponse WHERE conge_tt_id=?"
            );
            check.setLong(1, selected.getId());

            boolean exists = check.executeQuery().next();


            if (exists) {

                // 3️⃣ Update réponse existante
                PreparedStatement ps2 = cnx.prepareStatement(
                        "UPDATE reponse SET decision=?, commentaire=? WHERE conge_tt_id=?"
                );

                ps2.setString(1, statut);
                ps2.setString(2, commentaire);
                ps2.setLong(3, selected.getId());
                ps2.executeUpdate();

            } else {

                // 4️⃣ Insérer nouvelle réponse
                PreparedStatement ps3 = cnx.prepareStatement(
                        "INSERT INTO reponse(decision, commentaire, rh_id, employe_id, conge_tt_id) VALUES(?,?,?,?,?)"
                );

                ps3.setString(1, statut);
                ps3.setString(2, commentaire);
                ps3.setLong(3, 1L); // ⚠️ idéalement Session RH
                ps3.setLong(4, selected.getEmployeeId());
                ps3.setLong(5, selected.getId());
                ps3.executeUpdate();
            }
                 // Envoi SMS
            sendSmsToEmployee(selected, statut);
            msgLabel.setText("Décision enregistrée ✅");
            taCommentaire.clear();
            loadData();

        } catch (SQLException e) {
            msgLabel.setText("Erreur DB ❌");
            e.printStackTrace();
        }
    }

    // ===================== AJOUT =====================
    @FXML
    private void onComment() {

        Conge selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("Sélectionne une demande pour commenter.");
            return;
        }

        String commentaire = taCommentaire.getText();
        if (commentaire == null || commentaire.trim().isEmpty()) {
            msgLabel.setText("Écris un commentaire avant de valider.");
            return;
        }

        try {
            Connection cnx = MyConnection.getInstance().getCnx();

            // Vérifier si une réponse existe déjà pour ce congé
            PreparedStatement check = cnx.prepareStatement(
                    "SELECT id FROM reponse WHERE conge_tt_id=?"
            );
            check.setLong(1, selected.getId());

            boolean exists = check.executeQuery().next();

            if (exists) {
                // Mettre à jour le commentaire
                PreparedStatement ps = cnx.prepareStatement(
                        "UPDATE reponse SET commentaire=? WHERE conge_tt_id=?"
                );
                ps.setString(1, commentaire);
                ps.setLong(2, selected.getId());
                ps.executeUpdate();
            } else {
                // Insérer une nouvelle réponse
                PreparedStatement ps = cnx.prepareStatement(
                        "INSERT INTO reponse(conge_tt_id, decision, commentaire, rh_id, employe_id) VALUES(?, ?, ?, ?, ?)"
                );
                ps.setLong(1, selected.getId());
                ps.setString(2, "-"); // décision pas encore prise
                ps.setString(3, commentaire);
                ps.setLong(4, 1L); // Remplacer par l'ID du RH courant
                ps.setLong(5, selected.getEmployeeId());
                ps.executeUpdate();
            }

            msgLabel.setText("Commentaire enregistré ✅");
            taCommentaire.clear();
            loadData(); // rafraîchir la table

        } catch (SQLException e) {
            msgLabel.setText("Erreur DB ❌");
            e.printStackTrace();
        }
    }


// =================================================

    // ================= AJOUT pour tri manuel par critère =================
    @FXML
    private void onFilter() {
        String criteria = cbCriteria.getValue();
        if (criteria == null || masterData == null) return;

        FXCollections.sort(masterData, (c1, c2) -> {
            switch (criteria) {
                case "ID": return Long.compare(c1.getId(), c2.getId());
                case "Employé": return Long.compare(c1.getEmployeeId(), c2.getEmployeeId());
                case "Type": return c1.getTypeConge().compareToIgnoreCase(c2.getTypeConge());
                case "Date Début": return c1.getDateDebut().compareTo(c2.getDateDebut());
                case "Date Fin": return c1.getDateFin().compareTo(c2.getDateFin());
                default: return 0;
            }
        });

        table.refresh();
    }


    // ===================== SMS =====================
    /*private void sendSmsToEmployee(Conge conge, String statut) {
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT u.telephone FROM employe e " +
                            "JOIN users u ON e.user_id = u.id " +
                            "WHERE e.user_id = ?"
            );
            ps.setLong(1, conge.getEmployeeId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String phone = rs.getString("telephone");
                if (phone != null && !phone.isEmpty()) {
                    // Normaliser numéro
                    if (!phone.startsWith("+")) {
                        phone = "+216" + phone;
                    }

                    String message = "Votre demande de congé a été " + statut + ".";
                    edu.RhPro.services.SmsService.sendSms(phone, message);
                    System.out.println("SMS envoyé au : " + phone);
                } else {
                    System.out.println("Numéro vide pour l'employé ID: " + conge.getEmployeeId());
                }
            } else {
                System.out.println("Employé introuvable pour ID: " + conge.getEmployeeId());
            }

        } catch (Exception e) {
            System.out.println("Erreur SMS : " + e.getMessage());
            e.printStackTrace();
        }
    }*/










    // ===================== SMS INTELLIGENT =====================
    private void sendSmsToEmployee(Conge conge, String statut) {
        try {

            // 🔎 Vérification logique métier
            LocalDate today = LocalDate.now();
            LocalDate debut = conge.getDateDebut();

            boolean isUrgentDate = debut != null &&
                    (debut.isEqual(today) ||
                            debut.isEqual(today.plusDays(1)) ||
                            debut.isEqual(today.plusDays(2)));

            boolean isUrgentType = conge.getTypeConge() != null &&
                    (conge.getTypeConge().toLowerCase().contains("maladie") ||
                            conge.getTypeConge().toLowerCase().contains("urgent"));

            // 👉 Si pas urgent → pas de SMS
            if (!isUrgentDate && !isUrgentType) {
                System.out.println("SMS non envoyé (pas urgent).");
                return;
            }

            Connection cnx = MyConnection.getInstance().getCnx();
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT u.telephone FROM employe e " +
                            "JOIN users u ON e.user_id = u.id " +
                            "WHERE e.user_id = ?"
            );
            ps.setLong(1, conge.getEmployeeId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String phone = rs.getString("telephone");

                if (phone != null && !phone.isEmpty()) {

                    // 📞 Normalisation numéro Tunisie
                    if (!phone.startsWith("+")) {
                        phone = "+216" + phone;
                    }

                    String message =
                            "ALERTE RH 🚨\n" +
                                    "Votre congé (" + conge.getTypeConge() + ") du "
                                    + conge.getDateDebut() +
                                    " a été " + statut + ".";

                    SmsService.sendSms(phone, message);

                    System.out.println("SMS URGENT envoyé au : " + phone);

                } else {
                    System.out.println("Numéro vide pour employé ID: " + conge.getEmployeeId());
                }
            }

        } catch (Exception e) {
            System.out.println("Erreur SMS : " + e.getMessage());
        }
    }}
