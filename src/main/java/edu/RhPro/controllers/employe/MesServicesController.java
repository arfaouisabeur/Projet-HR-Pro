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
import java.util.Optional;

public class MesServicesController {

    // Form
    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private Label lblMsg;

    // Table
    @FXML private TableView<RowService> table;
    @FXML private TableColumn<RowService, String> colTitre;
    @FXML private TableColumn<RowService, String> colDescription;
    @FXML private TableColumn<RowService, LocalDate> colDate;
    @FXML private TableColumn<RowService, String> colStatut;
    @FXML private TableColumn<RowService, String> colReponse;
    @FXML private TableColumn<RowService, Void> colModifier;
    @FXML private TableColumn<RowService, Void> colSupprimer;

    private final ServiceService serviceService = new ServiceService();
    private final ReponseService reponseService = new ReponseService();

    private Service serviceEnModification = null;

    @FXML
    public void initialize() {
        // Table columns
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateDemande"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colReponse.setCellValueFactory(new PropertyValueFactory<>("reponseRh"));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Ajouter boutons Modifier et Supprimer
        addModifierButton();
        addSupprimerButton();

        // Contrôle en temps réel
        addFieldListeners();

        loadData();
    }

    private void addFieldListeners() {
        tfTitre.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.isBlank()) {
                lblMsg.setText("Le titre ne peut pas être vide.");
                lblMsg.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
            } else if (newText.length() > 100) {
                lblMsg.setText("Le titre ne peut pas dépasser 100 caractères.");
                lblMsg.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
            } else {
                lblMsg.setText("");
            }
        });

        taDescription.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > 500) {
                lblMsg.setText("La description ne peut pas dépasser 500 caractères.");
                lblMsg.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
            } else {
                lblMsg.setText("");
            }
        });
    }

    @FXML
    private void onSubmit() {
        setMsg("", false);

        if (!validateForm()) return;

        long empId = Session.getCurrentUser().getId();
        String titre = tfTitre.getText().trim();
        String desc = taDescription.getText() != null ? taDescription.getText().trim() : "";

        try {
            if (serviceEnModification == null) {
                Service s = new Service(titre, desc, LocalDate.now(), "EN_ATTENTE", empId);
                serviceService.addEntity(s);
                setMsg("Demande envoyée ✅", false);
            } else {
                serviceEnModification.setTitre(titre);
                serviceEnModification.setDescription(desc);
                serviceService.updateEntity(serviceEnModification);
                setMsg("Demande modifiée ✅", false);
                serviceEnModification = null;
            }

            clearForm();
            loadData();

        } catch (SQLException e) {
            e.printStackTrace();
            setMsg("Erreur DB: " + e.getMessage(), true);
        }
    }

    private boolean validateForm() {
        if (Session.getCurrentUser() == null) {
            setMsg("Session expirée. Reconnectez-vous.", true);
            return false;
        }

        String titre = tfTitre.getText();
        if (titre == null || titre.isBlank()) {
            setMsg("Veuillez saisir un titre.", true);
            return false;
        }
        if (titre.length() > 100) {
            setMsg("Le titre ne peut pas dépasser 100 caractères.", true);
            return false;
        }

        String desc = taDescription.getText();
        if (desc != null && desc.length() > 500) {
            setMsg("La description ne peut pas dépasser 500 caractères.", true);
            return false;
        }

        return true;
    }

    private void addModifierButton() {
        colModifier.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("✏");

            {
                btn.setStyle("-fx-background-color:#6d2269; -fx-text-fill:white; -fx-font-weight:900;");
                btn.setOnAction(event -> {
                    RowService row = getTableView().getItems().get(getIndex());
                    Service s = row.getService();

                    if (!"EN_ATTENTE".equals(s.getStatut())) {
                        setMsg("Impossible de modifier (déjà traité)", true);
                        return;
                    }

                    serviceEnModification = s;
                    tfTitre.setText(s.getTitre());
                    taDescription.setText(s.getDescription());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    RowService row = getTableView().getItems().get(getIndex());
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
                btn.setStyle("-fx-background-color:#e31f1f; -fx-text-fill:white; -fx-font-weight:900;");
                btn.setOnAction(event -> {
                    RowService row = getTableView().getItems().get(getIndex());
                    Service s = row.getService();

                    if (!"EN_ATTENTE".equals(s.getStatut())) {
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
                            serviceService.deleteEntity(s);
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
                if (empty) setGraphic(null);
                else {
                    RowService row = getTableView().getItems().get(getIndex());
                    btn.setDisable(!"EN_ATTENTE".equals(row.getStatut()));
                    setGraphic(btn);
                }
            }
        });
    }

    @FXML
    public void refresh() { loadData(); }

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
                    repTxt = rep.getDecision() + (com != null && !com.isBlank() ? " | " + com : "");
                }
                rows.add(new RowService(s, repTxt));
            }

            table.setItems(rows);

        } catch (SQLException e) {
            e.printStackTrace();
            setMsg("Erreur DB: " + e.getMessage(), true);
        }
    }

    private void clearForm() {
        tfTitre.clear();
        taDescription.clear();
    }

    private void setMsg(String msg, boolean isError) {
        lblMsg.setText(msg);
        if (msg == null || msg.isBlank()) return;

        if (isError) lblMsg.setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:900;");
        else lblMsg.setStyle("-fx-text-fill:#ddd6fe; -fx-font-weight:900;");
    }

    // ✅ Row model
    public static class RowService {
        private final Service service;
        private final String reponseRh;

        public RowService(Service service, String reponseRh) {
            this.service = service;
            this.reponseRh = reponseRh;
        }

        public Service getService() { return service; }

        public String getTitre() { return service.getTitre(); }
        public String getDescription() { return service.getDescription(); }
        public LocalDate getDateDemande() { return service.getDateDemande(); }
        public String getStatut() { return service.getStatut(); }
        public String getReponseRh() { return reponseRh; }
    }
}
