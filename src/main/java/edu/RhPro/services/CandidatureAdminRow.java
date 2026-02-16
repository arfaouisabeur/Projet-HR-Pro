package edu.RhPro.services;

import java.time.LocalDate;

public class CandidatureAdminRow {
    private int id;
    private LocalDate dateCandidature;
    private String statut;

    private int candidatUserId;
    private String candidatNom;
    private String candidatPrenom;
    private String candidatEmail;

    private int offreId;
    private String offreTitre;
    private String offreLocalisation;
    private String offreTypeContrat;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDateCandidature() { return dateCandidature; }
    public void setDateCandidature(LocalDate dateCandidature) { this.dateCandidature = dateCandidature; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getCandidatUserId() { return candidatUserId; }
    public void setCandidatUserId(int candidatUserId) { this.candidatUserId = candidatUserId; }

    public String getCandidatNom() { return candidatNom; }
    public void setCandidatNom(String candidatNom) { this.candidatNom = candidatNom; }

    public String getCandidatPrenom() { return candidatPrenom; }
    public void setCandidatPrenom(String candidatPrenom) { this.candidatPrenom = candidatPrenom; }

    public String getCandidatEmail() { return candidatEmail; }
    public void setCandidatEmail(String candidatEmail) { this.candidatEmail = candidatEmail; }

    public int getOffreId() { return offreId; }
    public void setOffreId(int offreId) { this.offreId = offreId; }

    public String getOffreTitre() { return offreTitre; }
    public void setOffreTitre(String offreTitre) { this.offreTitre = offreTitre; }

    public String getOffreLocalisation() { return offreLocalisation; }
    public void setOffreLocalisation(String offreLocalisation) { this.offreLocalisation = offreLocalisation; }

    public String getOffreTypeContrat() { return offreTypeContrat; }
    public void setOffreTypeContrat(String offreTypeContrat) { this.offreTypeContrat = offreTypeContrat; }

    public String getCandidatFullName() {
        return (candidatPrenom == null ? "" : candidatPrenom) + " " + (candidatNom == null ? "" : candidatNom);
    }
}
