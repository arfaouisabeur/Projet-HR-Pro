package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Reponse;
import edu.RhPro.entities.Service;
import edu.RhPro.services.ReponseService;
import edu.RhPro.services.ServiceService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MesServicesController {

    // Form
    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private Label lblMsg;

    // Table
    @FXML private TableView<RowService> table;
    @FXML private TableColumn<RowService, String> colTitre;
    @FXML private TableColumn<RowService, LocalDate> colDate;
    @FXML private TableColumn<RowService, String> colStatut;
    @FXML private TableColumn<RowService, String> colReponse;

    private final ServiceService serviceService = new ServiceService();
    private final ReponseService reponseService = new ReponseService();

    @FXML
    public void initialize() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateDemande"));
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

        String titre = tfTitre.getText();
        if (titre == null || titre.isBlank()) {
            setMsg("Veuillez saisir un titre.", true);
            return;
        }

        long empId = Session.getCurrentUser().getId();
        String desc = taDescription.getText();

        Service s = new Service(
                titre.trim(),
                desc,
                LocalDate.now(),
                "EN_ATTENTE",
                empId
        );

        try {
            serviceService.addEntity(s);

            tfTitre.clear();
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
            List<Service> services = serviceService.findByEmployeId(empId);

            var rows = FXCollections.<RowService>observableArrayList();

            for (Service s : services) {
                Reponse rep = reponseService.getOneByServiceId(s.getId());

                String repTxt = "-";
                if (rep != null) {
                    String com = rep.getCommentaire();
                    repTxt = rep.getDecision()
                            + (com != null && !com.isBlank() ? " | " + com : "");
                }

                rows.add(new RowService(
                        s.getTitre(),
                        s.getDateDemande(),
                        s.getStatut(),
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
    public static class RowService {
        private final String titre;
        private final LocalDate dateDemande;
        private final String statut;
        private final String reponseRh;

        public RowService(String titre, LocalDate dateDemande, String statut, String reponseRh) {
            this.titre = titre;
            this.dateDemande = dateDemande;
            this.statut = statut;
            this.reponseRh = reponseRh;
        }

        public String getTitre() { return titre; }
        public LocalDate getDateDemande() { return dateDemande; }
        public String getStatut() { return statut; }
        public String getReponseRh() { return reponseRh; }
    }
}
