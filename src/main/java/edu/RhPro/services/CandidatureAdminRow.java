package edu.RhPro.services;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CandidatureAdminRow {
    private long id;
    private LocalDate dateCandidature;
    private String statut;

    // ✅ CV (nouveau)
    private String cvPath;
    private String cvOriginalName;
    private long cvSize;
    private LocalDateTime cvUploadedAt;

    private Integer matchScore;          // score 0..100 (peut être null)
    private LocalDateTime matchUpdatedAt; // date calcul (optionnel)

    private long candidatUserId;
    private String candidatNom;
    private String candidatPrenom;
    private String candidatEmail;

    private long offreId;
    private String offreTitre;
    private String offreLocalisation;
    private String offreTypeContrat;
    private String signatureRequestId;   // id Dropbox Sign
    private String contractStatus;       // NONE / SENT / SIGNED

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public LocalDate getDateCandidature() { return dateCandidature; }
    public void setDateCandidature(LocalDate dateCandidature) { this.dateCandidature = dateCandidature; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    // ✅ CV getters/setters
    public String getCvPath() { return cvPath; }
    public void setCvPath(String cvPath) { this.cvPath = cvPath; }

    public String getCvOriginalName() { return cvOriginalName; }
    public void setCvOriginalName(String cvOriginalName) { this.cvOriginalName = cvOriginalName; }

    public long getCvSize() { return cvSize; }
    public void setCvSize(long cvSize) { this.cvSize = cvSize; }

    public LocalDateTime getCvUploadedAt() { return cvUploadedAt; }
    public void setCvUploadedAt(LocalDateTime cvUploadedAt) { this.cvUploadedAt = cvUploadedAt; }

    public long getCandidatUserId() { return candidatUserId; }
    public void setCandidatUserId(long candidatUserId) { this.candidatUserId = candidatUserId; }

    public String getCandidatNom() { return candidatNom; }
    public void setCandidatNom(String candidatNom) { this.candidatNom = candidatNom; }

    public String getCandidatPrenom() { return candidatPrenom; }
    public void setCandidatPrenom(String candidatPrenom) { this.candidatPrenom = candidatPrenom; }

    public String getCandidatEmail() { return candidatEmail; }
    public void setCandidatEmail(String candidatEmail) { this.candidatEmail = candidatEmail; }

    public long getOffreId() { return offreId; }
    public void setOffreId(long offreId) { this.offreId = offreId; }

    public String getOffreTitre() { return offreTitre; }
    public void setOffreTitre(String offreTitre) { this.offreTitre = offreTitre; }

    public String getOffreLocalisation() { return offreLocalisation; }
    public void setOffreLocalisation(String offreLocalisation) { this.offreLocalisation = offreLocalisation; }

    public String getOffreTypeContrat() { return offreTypeContrat; }
    public void setOffreTypeContrat(String offreTypeContrat) { this.offreTypeContrat = offreTypeContrat; }

    public String getCandidatFullName() {
        return (candidatPrenom == null ? "" : candidatPrenom) + " " + (candidatNom == null ? "" : candidatNom);
    }

    // ✅ Pratique pour TableView: "Oui/Non"
    public String getCvStatus() {
        return (cvPath != null && !cvPath.isBlank()) ? "Oui" : "Non";
    }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }

    public LocalDateTime getMatchUpdatedAt() { return matchUpdatedAt; }
    public void setMatchUpdatedAt(LocalDateTime matchUpdatedAt) { this.matchUpdatedAt = matchUpdatedAt; }

    public String getSignatureRequestId() { return signatureRequestId; }
    public void setSignatureRequestId(String signatureRequestId) {
        this.signatureRequestId = signatureRequestId;
    }

    public String getContractStatus() { return contractStatus; }
    public void setContractStatus(String contractStatus) { this.contractStatus = contractStatus; }
}