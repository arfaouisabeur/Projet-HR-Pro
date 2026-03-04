package edu.RhPro.entities;

import java.time.LocalDateTime;

public class Rating {
    private long id;
    private long evenementId;
    private long employeId;
    private String commentaire;
    private int etoiles; // Will be set automatically based on comment sentiment
    private LocalDateTime dateCreation;

    // Empty constructor
    public Rating() {
    }

    // Constructor without id
    public Rating(long evenementId, long employeId, String commentaire, int etoiles) {
        this.evenementId = evenementId;
        this.employeId = employeId;
        this.commentaire = commentaire;
        this.etoiles = etoiles;
        this.dateCreation = LocalDateTime.now();
    }

    // Full constructor
    public Rating(long id, long evenementId, long employeId, String commentaire,
                  int etoiles, LocalDateTime dateCreation) {
        this.id = id;
        this.evenementId = evenementId;
        this.employeId = employeId;
        this.commentaire = commentaire;
        this.etoiles = etoiles;
        this.dateCreation = dateCreation;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(long evenementId) {
        this.evenementId = evenementId;
    }

    public long getEmployeId() {
        return employeId;
    }

    public void setEmployeId(long employeId) {
        this.employeId = employeId;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public int getEtoiles() {
        return etoiles;
    }

    public void setEtoiles(int etoiles) {
        this.etoiles = etoiles;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
