package edu.RhPro.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Candidature {

    private long id; // ✅ mieux en long (car ta DB est bigint)
    private LocalDate dateCandidature;
    private String statut;

    // ✅ CV (chemin + métadonnées)
    private String cvPath;            // ex: storage/cv/candidature_12_....pdf
    private String cvOriginalName;    // ex: CV_Mohamed.pdf
    private long cvSize;              // bytes
    private LocalDateTime cvUploadedAt;

    private long candidatId;
    private long offreEmploiId;

    public Candidature() {}

    // ✅ Constructeur pour créer une candidature (sans CV au début)
    public Candidature(LocalDate dateCandidature, String statut, long candidatId, long offreEmploiId) {
        this.dateCandidature = dateCandidature;
        this.statut = statut;
        this.candidatId = candidatId;
        this.offreEmploiId = offreEmploiId;
    }

    // ✅ Constructeur complet
    public Candidature(long id, LocalDate dateCandidature, String statut,
                       String cvPath, String cvOriginalName, long cvSize, LocalDateTime cvUploadedAt,
                       long candidatId, long offreEmploiId) {
        this.id = id;
        this.dateCandidature = dateCandidature;
        this.statut = statut;
        this.cvPath = cvPath;
        this.cvOriginalName = cvOriginalName;
        this.cvSize = cvSize;
        this.cvUploadedAt = cvUploadedAt;
        this.candidatId = candidatId;
        this.offreEmploiId = offreEmploiId;
    }

    // ---------- getters/setters ----------
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public LocalDate getDateCandidature() { return dateCandidature; }
    public void setDateCandidature(LocalDate dateCandidature) { this.dateCandidature = dateCandidature; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getCvPath() { return cvPath; }
    public void setCvPath(String cvPath) { this.cvPath = cvPath; }

    public String getCvOriginalName() { return cvOriginalName; }
    public void setCvOriginalName(String cvOriginalName) { this.cvOriginalName = cvOriginalName; }

    public long getCvSize() { return cvSize; }
    public void setCvSize(long cvSize) { this.cvSize = cvSize; }

    public LocalDateTime getCvUploadedAt() { return cvUploadedAt; }
    public void setCvUploadedAt(LocalDateTime cvUploadedAt) { this.cvUploadedAt = cvUploadedAt; }

    public long getCandidatId() { return candidatId; }
    public void setCandidatId(long candidatId) { this.candidatId = candidatId; }

    public long getOffreEmploiId() { return offreEmploiId; }
    public void setOffreEmploiId(long offreEmploiId) { this.offreEmploiId = offreEmploiId; }

    // ✅ utile pour TableView (afficher Oui/Non)
    public boolean hasCv() {
        return cvPath != null && !cvPath.isBlank();
    }

    @Override
    public String toString() {
        return "Candidature{" +
                "id=" + id +
                ", dateCandidature=" + dateCandidature +
                ", statut='" + statut + '\'' +
                ", cvOriginalName='" + cvOriginalName + '\'' +
                ", cvPath='" + cvPath + '\'' +
                ", candidatId=" + candidatId +
                ", offreEmploiId=" + offreEmploiId +
                '}';
    }
}