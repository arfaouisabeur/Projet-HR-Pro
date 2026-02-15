package edu.RhPro.entities;

public class Reponse {
    private long id;
    private String decision;
    private String commentaire;
    private long rhId;
    private Long employeId;
    private Long congeTtId;
    private Long demandeServiceId;

    // Constructeur vide
    public Reponse() {}

    // Constructeur pour une réponse à un congé
    public static Reponse forConge(String decision, String commentaire, long rhId, Long employeId, Long congeTtId) {
        Reponse reponse = new Reponse();
        reponse.decision = decision;
        reponse.commentaire = commentaire;
        reponse.rhId = rhId;
        reponse.employeId = employeId;
        reponse.congeTtId = congeTtId;
        reponse.demandeServiceId = null;
        return reponse;
    }

    // Constructeur pour une réponse à un service
    public static Reponse forService(String decision, String commentaire, long rhId, Long employeId, Long demandeServiceId) {
        Reponse reponse = new Reponse();
        reponse.decision = decision;
        reponse.commentaire = commentaire;
        reponse.rhId = rhId;
        reponse.employeId = employeId;
        reponse.congeTtId = null;
        reponse.demandeServiceId = demandeServiceId;
        return reponse;
    }

    // Constructeur complet
    public Reponse(long id, String decision, String commentaire, long rhId,
                   Long employeId, Long congeTtId, Long demandeServiceId) {
        this.id = id;
        this.decision = decision;
        this.commentaire = commentaire;
        this.rhId = rhId;
        this.employeId = employeId;
        this.congeTtId = congeTtId;
        this.demandeServiceId = demandeServiceId;
    }

    // Getters et Setters (inchangés)
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public long getRhId() { return rhId; }
    public void setRhId(long rhId) { this.rhId = rhId; }
    public Long getEmployeId() { return employeId; }
    public void setEmployeId(Long employeId) { this.employeId = employeId; }
    public Long getCongeTtId() { return congeTtId; }
    public void setCongeTtId(Long congeTtId) { this.congeTtId = congeTtId; }
    public Long getDemandeServiceId() { return demandeServiceId; }
    public void setDemandeServiceId(Long demandeServiceId) { this.demandeServiceId = demandeServiceId; }

    // Méthodes utilitaires
    public boolean isForConge() {
        return congeTtId != null && demandeServiceId == null;
    }

    public boolean isForService() {
        return demandeServiceId != null && congeTtId == null;
    }

    public boolean isValid() {
        return (congeTtId != null && demandeServiceId == null) ||
                (congeTtId == null && demandeServiceId != null);
    }

    @Override
    public String toString() {
        return "Reponse{" +
                "id=" + id +
                ", decision='" + decision + '\'' +
                ", commentaire='" + commentaire + '\'' +
                ", rhId=" + rhId +
                ", employeId=" + employeId +
                ", congeTtId=" + congeTtId +
                ", demandeServiceId=" + demandeServiceId +
                ", type=" + (isForConge() ? "CONGÉ" : isForService() ? "SERVICE" : "INVALIDE") +
                '}';
    }
}