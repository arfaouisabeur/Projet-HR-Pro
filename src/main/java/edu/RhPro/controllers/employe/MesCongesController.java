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
    @FXML private TableColumn<RowConge, String> colReponse;

    private final CongeService congeService = new CongeService();
    private final ReponseService reponseService = new ReponseService();

    @FXML
    public void initialize() {
        cbType.setItems(FXCollections.observableArrayList("CONGE", "TT"));

        // table columns mapping (RowConge getters)
        colType.setCellValueFactory(new PropertyValueFactory<>("typeConge"));
        colDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colReponse.setCellValueFactory(new PropertyValueFactory<>("reponseRh"));

        // ✅ set resize policy here (NOT in FXML)
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadData();
    }

    @FXML
    private void onSubmit() {
        setMsg("", false);

        if (Session.getCurrentUser() == null) {
            setMsg("Session expirée. Reconnectez-vous.", true);
            return;
        }

        String type = cbType.getValue();
        LocalDate debut = dpDebut.getValue();
        LocalDate fin = dpFin.getValue();

        if (type == null || debut == null || fin == null) {
            setMsg("Veuillez remplir Type + Dates.", true);
            return;
        }

        if (fin.isBefore(debut)) {
            setMsg("La date fin doit être après la date début.", true);
            return;
        }

        long empId = Session.getCurrentUser().getId();
        String desc = taDescription.getText();

        Conge c = new Conge(type, debut, fin, "EN_ATTENTE", desc, empId);

        try {
            congeService.addEntity(c);

            // clear form
            cbType.setValue(null);
            dpDebut.setValue(null);
            dpFin.setValue(null);
            taDescription.clear();

            setMsg("Demande envoyée ✅", false);
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
            setMsg("Erreur DB: " + e.getMessage(), true);
        }
    }

    // Called by Refresh button in FXML
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

                rows.add(new RowConge(
                        c.getTypeConge(),
                        c.getDateDebut(),
                        c.getDateFin(),
                        c.getStatut(),
                        repTxt
                ));
            }

            table.setItems(rows);

        } catch (SQLException e) {
            e.printStackTrace();
            setMsg("Erreur DB: " + e.getMessage(), true);
        }
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

    // ✅ Row model for TableView
    public static class RowConge {
        private final String typeConge;
        private final LocalDate dateDebut;
        private final LocalDate dateFin;
        private final String statut;
        private final String reponseRh;

        public RowConge(String typeConge, LocalDate dateDebut, LocalDate dateFin, String statut, String reponseRh) {
            this.typeConge = typeConge;
            this.dateDebut = dateDebut;
            this.dateFin = dateFin;
            this.statut = statut;
            this.reponseRh = reponseRh;
        }

        public String getTypeConge() { return typeConge; }
        public LocalDate getDateDebut() { return dateDebut; }
        public LocalDate getDateFin() { return dateFin; }
        public String getStatut() { return statut; }
        public String getReponseRh() { return reponseRh; }
    }
}
