package edu.RhPro.entities;

import java.time.LocalDate;

public class Projet {

    private int id;
    private String titre;
    private String description;
    private String statut;
    private int rhId;
    private int responsableEmployeId;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    // 🔹 Constructeur pour INSERT (sans id)
    public Projet(String titre, String description, String statut,
                  int rhId, int responsableEmployeId,
                  LocalDate dateDebut, LocalDate dateFin) {
        this.titre = titre;
        this.description = description;
        this.statut = statut;
        this.rhId = rhId;
        this.responsableEmployeId = responsableEmployeId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // 🔹 Constructeur pour SELECT (avec id)
    public Projet(int id, String titre, String description, String statut,
                  int rhId, int responsableEmployeId,
                  LocalDate dateDebut, LocalDate dateFin) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.statut = statut;
        this.rhId = rhId;
        this.responsableEmployeId = responsableEmployeId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // 🔹 Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getRhId() { return rhId; }
    public void setRhId(int rhId) { this.rhId = rhId; }

    public int getResponsableEmployeId() { return responsableEmployeId; }
    public void setResponsableEmployeId(int responsableEmployeId) { this.responsableEmployeId = responsableEmployeId; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    @Override
    public String toString() {
        return "Projet{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", statut='" + statut + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                '}';
    }
}

