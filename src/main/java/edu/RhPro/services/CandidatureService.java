package edu.RhPro.services;

import edu.RhPro.entities.Candidature;
import edu.RhPro.tools.MyConnection;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import edu.RhPro.utils.LocalPdfServer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CandidatureService {


    private final Connection cnx;
    private final Path cvDir = Paths.get("storage", "cv");

    public CandidatureService() {
        this(MyConnection.getInstance().getCnx());
    }

    public CandidatureService(Connection cnx) {
        this.cnx = cnx;
    }

    // -----------------------------
    // ✅ CREATE (retourne ID)
    // -----------------------------
    public long add(Candidature c) throws SQLException {
        if (c.getDateCandidature() == null) throw new IllegalArgumentException("date_candidature obligatoire");
        if (c.getStatut() == null) throw new IllegalArgumentException("statut obligatoire");
        if (c.getCandidatId() <= 0) throw new IllegalArgumentException("candidat_id obligatoire");
        if (c.getOffreEmploiId() <= 0) throw new IllegalArgumentException("offre_emploi_id obligatoire");

        String sql = """
            INSERT INTO candidature
            (date_candidature, statut, cv_path, candidat_id, offre_emploi_id, cv_original_name, cv_size, cv_uploaded_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(c.getDateCandidature()));
            ps.setString(2, c.getStatut());

            // au début, souvent NULL
            if (c.getCvPath() == null) ps.setNull(3, Types.VARCHAR);
            else ps.setString(3, c.getCvPath());

            ps.setLong(4, c.getCandidatId());
            ps.setLong(5, c.getOffreEmploiId());

            if (c.getCvOriginalName() == null) ps.setNull(6, Types.VARCHAR);
            else ps.setString(6, c.getCvOriginalName());

            ps.setLong(7, c.getCvSize());

            if (c.getCvUploadedAt() == null) ps.setNull(8, Types.TIMESTAMP);
            else ps.setTimestamp(8, Timestamp.valueOf(c.getCvUploadedAt()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Impossible de récupérer l'id généré de la candidature.");
    }

    // -----------------------------
    // ✅ UPDATE (tous champs)
    // -----------------------------
    public void update(Candidature c) throws SQLException {
        if (c.getId() <= 0) throw new IllegalArgumentException("id obligatoire pour update");

        String sql = """
            UPDATE candidature
            SET date_candidature=?,
                statut=?,
                cv_path=?,
                candidat_id=?,
                offre_emploi_id=?,
                cv_original_name=?,
                cv_size=?,
                cv_uploaded_at=?
            WHERE id=?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(c.getDateCandidature()));
            ps.setString(2, c.getStatut());

            if (c.getCvPath() == null) ps.setNull(3, Types.VARCHAR);
            else ps.setString(3, c.getCvPath());

            ps.setLong(4, c.getCandidatId());
            ps.setLong(5, c.getOffreEmploiId());

            if (c.getCvOriginalName() == null) ps.setNull(6, Types.VARCHAR);
            else ps.setString(6, c.getCvOriginalName());

            ps.setLong(7, c.getCvSize());

            if (c.getCvUploadedAt() == null) ps.setNull(8, Types.TIMESTAMP);
            else ps.setTimestamp(8, Timestamp.valueOf(c.getCvUploadedAt()));

            ps.setLong(9, c.getId());

            ps.executeUpdate();
        }
    }

    // -----------------------------
    // ✅ DELETE
    // -----------------------------
    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM candidature WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // -----------------------------
    // ✅ READ
    // -----------------------------
    public Candidature findById(long id) throws SQLException {
        String sql = "SELECT * FROM candidature WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public List<Candidature> findAll() throws SQLException {
        List<Candidature> list = new ArrayList<>();
        String sql = "SELECT * FROM candidature ORDER BY date_candidature DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Candidature> findByCandidatId(long candidatId) throws SQLException {
        List<Candidature> list = new ArrayList<>();
        String sql = "SELECT * FROM candidature WHERE candidat_id=? ORDER BY date_candidature DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, candidatId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void updateStatus(long candidatureId, String newStatus) throws SQLException {
        String sql = "UPDATE candidature SET statut=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setLong(2, candidatureId);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------
    // ✅ CV FUNCTIONS (upload / open / download)
    // -------------------------------------------------------

    public void saveCvPath(long candidatureId, File pdf, long maxBytes) throws SQLException, IOException {
        if (pdf == null) throw new IllegalArgumentException("Aucun fichier sélectionné.");
        if (!pdf.getName().toLowerCase().endsWith(".pdf")) throw new IllegalArgumentException("Le fichier doit être un PDF.");
        if (pdf.length() > maxBytes) throw new IllegalArgumentException("Fichier trop volumineux (max " + (maxBytes / (1024 * 1024)) + "MB).");

        if (!Files.exists(cvDir)) Files.createDirectories(cvDir);

        String safeOriginal = pdf.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String targetName = "candidature_" + candidatureId + "_" + System.currentTimeMillis() + "_" + safeOriginal;

        Path saved = Files.copy(pdf.toPath(), cvDir.resolve(targetName), StandardCopyOption.REPLACE_EXISTING);

        String sql = """
            UPDATE candidature
            SET cv_path=?, cv_original_name=?, cv_size=?, cv_uploaded_at=CURRENT_TIMESTAMP
            WHERE id=?
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, saved.toAbsolutePath().toString());
            ps.setString(2, pdf.getName());
            ps.setLong(3, pdf.length());
            ps.setLong(4, candidatureId);
            ps.executeUpdate();
        }
    }

    public String getCvPath(long candidatureId) throws SQLException {
        String sql = "SELECT cv_path FROM candidature WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, candidatureId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("cv_path");
            }
        }
        return null;
    }

    public void openCv(long candidatureId) throws SQLException, IOException {
        String path = getCvPath(candidatureId);
        if (path == null || path.isBlank()) throw new IllegalStateException("Aucun CV pour cette candidature.");

        File f = new File(path);
        if (!f.exists()) throw new IOException("CV introuvable: " + path);

        if (!Desktop.isDesktopSupported()) throw new IOException("Desktop API non supportée.");
        Desktop.getDesktop().open(f);
    }

    public void downloadCvTo(long candidatureId, File destination) throws SQLException, IOException {
        String path = getCvPath(candidatureId);
        if (path == null || path.isBlank()) throw new IllegalStateException("Aucun CV pour cette candidature.");

        Files.copy(Paths.get(path), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // -----------------------------
    // ✅ Mapping ResultSet -> Entity
    // -----------------------------
    private Candidature map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");

        Date dc = rs.getDate("date_candidature");
        LocalDate dateCandidature = (dc != null) ? dc.toLocalDate() : LocalDate.now();

        String statut = rs.getString("statut");

        String cvPath = rs.getString("cv_path");
        String cvOriginalName = rs.getString("cv_original_name");
        long cvSize = rs.getLong("cv_size");
        Timestamp ts = rs.getTimestamp("cv_uploaded_at");
        LocalDateTime cvUploadedAt = (ts != null) ? ts.toLocalDateTime() : null;

        long candidatId = rs.getLong("candidat_id");
        long offreEmploiId = rs.getLong("offre_emploi_id");

        // ⚠️ utilise ton constructeur complet (celui qu'on a ajouté dans l'entity)
        return new Candidature(
                id, dateCandidature, statut,
                cvPath, cvOriginalName, cvSize, cvUploadedAt,
                candidatId, offreEmploiId
        );
    }
    public List<CandidatureAdminRow> findAllForAdmin() throws SQLException {
        List<CandidatureAdminRow> list = new ArrayList<>();

        String sql =
                "SELECT c.id, c.date_candidature, c.statut, " +
                        "       c.cv_path, c.cv_original_name, c.cv_size, c.cv_uploaded_at, c.match_score, c.match_updated_at, " +
                        "       c.signature_request_id, c.contract_status, " +   // ✅ AJOUT ICI
                        "       u.id AS candidat_user_id, u.nom, u.prenom, u.email, " +
                        "       o.id AS offre_id, o.titre AS offre_titre, " +
                        "       o.localisation AS offre_localisation, o.type_contrat AS offre_type " +
                        "FROM candidature c " +
                        "JOIN users u ON c.candidat_id = u.id " +
                        "JOIN offre_emploi o ON c.offre_emploi_id = o.id " +
                        "ORDER BY c.date_candidature DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                CandidatureAdminRow r = new CandidatureAdminRow();

                r.setId(rs.getLong("id"));

                Date dc = rs.getDate("date_candidature");
                r.setDateCandidature(dc != null ? dc.toLocalDate() : LocalDate.now());

                r.setStatut(rs.getString("statut"));

                // ✅ CV
                r.setCvPath(rs.getString("cv_path"));
                r.setCvOriginalName(rs.getString("cv_original_name"));
                r.setCvSize(rs.getLong("cv_size"));

                Timestamp ts = rs.getTimestamp("cv_uploaded_at");
                r.setCvUploadedAt(ts != null ? ts.toLocalDateTime() : null);
                // ✅ MATCHING
                r.setMatchScore((Integer) rs.getObject("match_score"));

                Timestamp mts = rs.getTimestamp("match_updated_at");
                r.setMatchUpdatedAt(mts != null ? mts.toLocalDateTime() : null);

                // candidat
                r.setCandidatUserId(rs.getLong("candidat_user_id"));
                r.setCandidatNom(rs.getString("nom"));
                r.setCandidatPrenom(rs.getString("prenom"));
                r.setCandidatEmail(rs.getString("email"));

                // offre
                r.setOffreId(rs.getLong("offre_id"));
                r.setOffreTitre(rs.getString("offre_titre"));
                r.setOffreLocalisation(rs.getString("offre_localisation"));
                r.setOffreTypeContrat(rs.getString("offre_type"));
                r.setSignatureRequestId(rs.getString("signature_request_id"));
                r.setContractStatus(rs.getString("contract_status"));

                list.add(r);
            }
        }

        return list;
    }
    public boolean hasAlreadyApplied(long candidatId, long offreId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM candidature WHERE candidat_id=? AND offre_emploi_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, candidatId);
            ps.setLong(2, offreId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
    public void updateMatchScore(long candidatureId, Integer score) throws SQLException {
        String sql = """
        UPDATE candidature
        SET match_score=?, match_updated_at=CURRENT_TIMESTAMP
        WHERE id=?
    """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            if (score == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, score);

            ps.setLong(2, candidatureId);
            ps.executeUpdate();
        }
    }
    public void updateSignatureRequestId(long candidatureId, String reqId) {
        String sql = "UPDATE candidature SET signature_request_id = ?, contract_status = 'SENT' WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            // Debug: voir quelle DB est utilisée
            System.out.println("DB URL = " + cnx.getMetaData().getURL());
            System.out.println("UPDATE candidature id=" + candidatureId + " reqId=" + reqId);

            ps.setString(1, reqId);
            ps.setLong(2, candidatureId);

            int updated = ps.executeUpdate();
            System.out.println("Rows updated = " + updated);

            if (updated == 0) {
                throw new RuntimeException("Aucune ligne modifiée. ID candidature introuvable: " + candidatureId);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur updateSignatureRequestId: " + e.getMessage(), e);
        }

    }
    public void openCvInApp(String cvPath, String title) {

        if (cvPath == null || cvPath.isBlank()) {
            throw new IllegalArgumentException("cvPath vide.");
        }

        File pdfFile = new File(cvPath);
        if (!pdfFile.exists()) {
            throw new IllegalArgumentException("Fichier introuvable: " + cvPath);
        }

        int port = LocalPdfServer.startIfNeeded();

        // URL HTTP du PDF
        String pdfHttp = "http://127.0.0.1:" + port + "/cv?path=" +
                URLEncoder.encode(cvFilePathSafe(pdfFile.getAbsolutePath()), StandardCharsets.UTF_8);

        // URL viewer PDF.js servi par le serveur
        String viewer = "http://127.0.0.1:" + port + "/pdfjs/web/viewer.html?file=" +
                URLEncoder.encode(pdfHttp, StandardCharsets.UTF_8);

        Platform.runLater(() -> {
            WebView wv = new WebView();
            wv.getEngine().load(viewer);

            Stage stage = new Stage();
            stage.setTitle(title != null ? title : "CV");
            stage.setScene(new Scene(wv, 1100, 750));
            stage.show();
        });
    }

    private String cvFilePathSafe(String path) {
        // garde le chemin tel quel, mais tu peux mettre des contrôles ici si tu veux
        return path;
    }

}