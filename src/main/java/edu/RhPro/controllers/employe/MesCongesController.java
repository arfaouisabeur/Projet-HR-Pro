package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Conge;
import edu.RhPro.entities.Reponse;
import edu.RhPro.services.CongeService;
import edu.RhPro.services.ReponseService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MesCongesController {

    // Form
    @FXML private ComboBox<String> cbType;
    @FXML private DatePicker dpDebut;
    @FXML private DatePicker dpFin;
    @FXML private TextArea taDescription;
    @FXML private Label lblMsg;

    // Table
    @FXML private TableView<RowConge> table;
    @FXML private TableColumn<RowConge, String> colType;
    @FXML private TableColumn<RowConge, LocalDate> colDebut;
    @FXML private TableColumn<RowConge, LocalDate> colFin;
    @FXML private TableColumn<RowConge, String> colStatut;
    @FXML private TableColumn<RowConge, String> colDescription;
    @FXML private TableColumn<RowConge, String> colReponse;
    @FXML private TableColumn<RowConge, Void> colModifier;
    @FXML private TableColumn<RowConge, Void> colSupprimer;

    private final CongeService congeService = new CongeService();
    private final ReponseService reponseService = new ReponseService();

    private Conge congeEnModification = null;

    @FXML
    public void initialize() {

        cbType.setItems(FXCollections.observableArrayList("CONGE", "TT"));

        colType.setCellValueFactory(new PropertyValueFactory<>("typeConge"));
        colDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colReponse.setCellValueFactory(new PropertyValueFactory<>("reponseRh"));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addModifierButton();
        addSupprimerButton();

        loadData();
    }

    @FXML
    private void onSubmit() {

        setMsg("", false);

        if (Session.getCurrentUser() == null) {
            setMsg("Session expirée.", true);
            return;
        }

        String type = cbType.getValue();
        LocalDate debut = dpDebut.getValue();
        LocalDate fin = dpFin.getValue();

        if (!validateForm()) {
            return;
        }

        if (fin.isBefore(debut)) {
            setMsg("La date fin doit être après la date début.", true);
            return;
        }

        long empId = Session.getCurrentUser().getId();
        String desc = taDescription.getText();

        try {

            if (congeEnModification == null) {
                // ➜ CREATE
                Conge c = new Conge(type, debut, fin, "EN_ATTENTE", desc, empId);
                congeService.addEntity(c);
                setMsg("Demande envoyée ✅", false);
            } else {
                // ➜ UPDATE
                congeEnModification.setTypeConge(type);
                congeEnModification.setDateDebut(debut);
                congeEnModification.setDateFin(fin);
                congeEnModification.setDescription(desc);

                congeService.updateEntity(congeEnModification);
                setMsg("Demande modifiée ✅", false);
                congeEnModification = null;
            }

            clearForm();
            loadData();

        } catch (SQLException e) {
            e.printStackTrace();
            setMsg("Erreur DB: " + e.getMessage(), true);
        }
    }

    private void addModifierButton() {
        colModifier.setCellFactory(param -> new TableCell<>() {

            private final Button btn = new Button("✏");

            {
                btn.setStyle("-fx-background-color:#6d2269; -fx-text-fill:white; -fx-font-weight:900;");

                btn.setOnAction(event -> {
                    RowConge row = getTableView().getItems().get(getIndex());
                    Conge c = row.getConge();

                    if (!"EN_ATTENTE".equals(c.getStatut())) {
                        setMsg("Impossible de modifier (déjà traité)", true);
                        return;
                    }

                    congeEnModification = c;

                    cbType.setValue(c.getTypeConge());
                    dpDebut.setValue(c.getDateDebut());
                    dpFin.setValue(c.getDateFin());
                    taDescription.setText(c.getDescription());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    RowConge row = getTableView().getItems().get(getIndex());
                    btn.setDisable(!"EN_ATTENTE".equals(row.getStatut()));
                    setGraphic(btn);
                }
            }
        });
    }

    private void addSupprimerButton() {
        colSupprimer.setCellFactory(param -> new TableCell<>() {

            private final Button btn = new Button("🗑");

            {
                btn.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white; -fx-font-weight:900;");

                btn.setOnAction(event -> {
                    RowConge row = getTableView().getItems().get(getIndex());
                    Conge c = row.getConge();

                    if (!"EN_ATTENTE".equals(c.getStatut())) {
                        setMsg("Impossible de supprimer (déjà traité)", true);
                        return;
                    }

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirmation");
                    alert.setHeaderText("Supprimer cette demande ?");
                    alert.setContentText("Action irréversible.");

                    Optional<ButtonType> result = alert.showAndWait();

                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        try {
                            congeService.deleteEntity(c);
                            loadData();
                            setMsg("Demande supprimée ✅", false);
                        } catch (SQLException e) {
                            e.printStackTrace();
                            setMsg("Erreur DB: " + e.getMessage(), true);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    RowConge row = getTableView().getItems().get(getIndex());
                    btn.setDisable(!"EN_ATTENTE".equals(row.getStatut()));
                    setGraphic(btn);
                }
            }
        });
    }

    @FXML
    public void refresh() {
        loadData();
    }

    private void loadData() {
        try {

            if (Session.getCurrentUser() == null) return;

            long empId = Session.getCurrentUser().getId();
            List<Conge> conges = congeService.findByEmployeId(empId);

            var rows = FXCollections.<RowConge>observableArrayList();

            for (Conge c : conges) {
                Reponse rep = reponseService.getOneByCongeId(c.getId());

                String repTxt = "-";
                if (rep != null) {
                    String com = rep.getCommentaire();
                    repTxt = rep.getDecision()
                            + (com != null && !com.isBlank() ? " | " + com : "");
                }

                rows.add(new RowConge(c, repTxt));
            }

            table.setItems(rows);

        } catch (SQLException e) {
            e.printStackTrace();
            setMsg("Erreur DB: " + e.getMessage(), true);
        }
    }

    private void clearForm() {
        cbType.setValue(null);
        dpDebut.setValue(null);
        dpFin.setValue(null);
        taDescription.clear();
    }

    private void setMsg(String msg, boolean isError) {
        lblMsg.setText(msg);

        if (msg == null || msg.isBlank()) return;

        if (isError) {
            lblMsg.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
        } else {
            lblMsg.setStyle("-fx-text-fill:#047857; -fx-font-weight:900;");
        }
    }

    public static class RowConge {

        private final Conge conge;
        private final String reponseRh;

        public RowConge(Conge conge, String reponseRh) {
            this.conge = conge;
            this.reponseRh = reponseRh;
        }

        public Conge getConge() { return conge; }

        public String getTypeConge() { return conge.getTypeConge(); }
        public LocalDate getDateDebut() { return conge.getDateDebut(); }
        public LocalDate getDateFin() { return conge.getDateFin(); }
        public String getStatut() { return conge.getStatut(); }
        public String getDescription() {return conge.getDescription(); }
        public String getReponseRh() { return reponseRh; }
    }
    private boolean validateForm() {

        if (cbType.getValue() == null) {
            setMsg("Veuillez choisir un type de congé.", true);
            return false;
        }

        if (dpDebut.getValue() == null) {
            setMsg("Veuillez choisir une date de début.", true);
            return false;
        }

        if (dpFin.getValue() == null) {
            setMsg("Veuillez choisir une date de fin.", true);
            return false;
        }

        if (dpFin.getValue().isBefore(dpDebut.getValue())) {
            setMsg("La date fin doit être après la date début.", true);
            return false;
        }

        if (dpDebut.getValue().isBefore(LocalDate.now())) {
            setMsg("La date début ne peut pas être dans le passé.", true);
            return false;
        }

        if (taDescription.getText() == null || taDescription.getText().isBlank()) {
            setMsg("Veuillez saisir une description.", true);
            return false;
        }

        return true;
    }

}
