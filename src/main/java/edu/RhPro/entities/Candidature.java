package edu.RhPro.entities;

import java.time.LocalDate;

public class Candidature {
    private int id;
    private LocalDate dateCandidature;
    private String statut;
    private String cv; // optionnel
    private long candidatId;
    private long offreEmploiId;

    public Candidature() {}

    public Candidature(LocalDate dateCandidature, String statut, String cv, long candidatId, long offreEmploiId) {
        this.dateCandidature = dateCandidature;
        this.statut = statut;
        this.cv = cv;
        this.candidatId = candidatId;
        this.offreEmploiId = offreEmploiId;
    }

    public Candidature(int id, LocalDate dateCandidature, String statut, String cv, long candidatId, long offreEmploiId) {
        this.id = id;
        this.dateCandidature = dateCandidature;
        this.statut = statut;
        this.cv = cv;
        this.candidatId = candidatId;
        this.offreEmploiId = offreEmploiId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDateCandidature() { return dateCandidature; }
    public void setDateCandidature(LocalDate dateCandidature) { this.dateCandidature = dateCandidature; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getCv() { return cv; }
    public void setCv(String cv) { this.cv = cv; }

    public long getCandidatId() { return candidatId; }
    public void setCandidatId(long candidatId) { this.candidatId = candidatId; }

    public long getOffreEmploiId() { return offreEmploiId; }
    public void setOffreEmploiId(long offreEmploiId) { this.offreEmploiId = offreEmploiId; }

    @Override
    public String toString() {
        return "Candidature{" +
                "id=" + id +
                ", dateCandidature=" + dateCandidature +
                ", statut='" + statut + '\'' +
                ", candidatId=" + candidatId +
                ", offreEmploiId=" + offreEmploiId +
                '}';
    }
}
