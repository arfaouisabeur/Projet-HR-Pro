package edu.RhPro.entities;

import java.time.LocalDateTime;

public class Evenement {

    private long id;
    private String titre;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private String description;
    private long rhId;

    // 🔹 Constructeur vide (OBLIGATOIRE)
    public Evenement() {
    }

    // 🔹 Constructeur sans id (pour INSERT)
    public Evenement(String titre, LocalDateTime dateDebut, LocalDateTime dateFin,
                     String lieu, String description, long rhId) {
        this.titre = titre;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.description = description;
        this.rhId = rhId;
    }

    public Evenement(long id, String titre, LocalDateTime dateDebut, LocalDateTime dateFin, String lieu, String description, long rhId) {
        this.id = id;
        this.titre = titre;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.description = description;
        this.rhId = rhId;
    }
// 🔹 Getters & Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getRhId() {
        return rhId;
    }

    public void setRhId(long rhId) {
        this.rhId = rhId;
    }

    @Override
    public String toString() {
        return "Evenment{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", lieu='" + lieu + '\'' +
                '}';
    }
}

