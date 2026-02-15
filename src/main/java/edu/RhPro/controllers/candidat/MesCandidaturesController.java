package edu.RhPro.controllers.candidat;

import edu.RhPro.entities.Candidature;
import edu.RhPro.services.CandidatureService;
import edu.RhPro.utils.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MesCandidaturesController {

    @FXML private TableView<Candidature> candTable;
    @FXML private TableColumn<Candidature, Integer> colId;
    @FXML private TableColumn<Candidature, LocalDate> colDate;
    @FXML private TableColumn<Candidature, String> colStatut;
    @FXML private TableColumn<Candidature, Long> colOffreId;
    @FXML private Label msgLabel;

    private final CandidatureService service = new CandidatureService();
    private final ObservableList<Candidature> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCandidature"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colOffreId.setCellValueFactory(new PropertyValueFactory<>("offreEmploiId"));

        candTable.setItems(data);
        refresh();
    }

    @FXML
    public void refresh() {
        try {
            long candidatId = Session.getCurrentUser().getId();
            List<Candidature> list = service.findByCandidatId(candidatId);
            data.setAll(list);
            msgLabel.setText("Total: " + list.size());
        } catch (SQLException e) {
            e.printStackTrace();
            msgLabel.setText("Erreur: " + e.getMessage());
        }
    }
}
