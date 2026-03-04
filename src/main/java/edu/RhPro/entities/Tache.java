package edu.RhPro.entities;

import java.time.LocalDate;

public class Tache {

    private int id;
    private String titre;
    private String description;
    private String statut;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private int level;          // ✅ INT in Java + DB
    private int projetId;
    private int employeId;
    private Integer primeId;    // nullable

    // ✅ Constructor for INSERT (without id)
    public Tache(String titre, String description, String statut,
                 LocalDate dateDebut, LocalDate dateFin,
                 int level, int projetId, int employeId, Integer primeId) {
        this.titre = titre;
        this.description = description;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.level = level;
        this.projetId = projetId;
        this.employeId = employeId;
        this.primeId = primeId;
    }

    // ✅ Constructor for SELECT (with id)
    public Tache(int id, String titre, String description, String statut,
                 LocalDate dateDebut, LocalDate dateFin,
                 int level, int projetId, int employeId, Integer primeId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.level = level;
        this.projetId = projetId;
        this.employeId = employeId;
        this.primeId = primeId;
    }

    // ✅ Empty constructor (optional but useful)
    public Tache() {}

    // ===== GETTERS =====
    public int getId() { return id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public String getStatut() { return statut; }
    public LocalDate getDateDebut() { return dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public int getLevel() { return level; }
    public int getProjetId() { return projetId; }
    public int getEmployeId() { return employeId; }
    public Integer getPrimeId() { return primeId; }

    // ===== SETTERS =====
    public void setId(int id) { this.id = id; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setDescription(String description) { this.description = description; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public void setLevel(int level) { this.level = level; }            // ✅ int only
    public void setProjetId(int projetId) { this.projetId = projetId; }
    public void setEmployeId(int employeId) { this.employeId = employeId; }
    public void setPrimeId(Integer primeId) { this.primeId = primeId; }

    // ✅ UI helper: show like "L3" (ONLY DISPLAY)
    public String getLevelLabel() {
        return "L" + level;
    }

    @Override
    public String toString() {
        return "Tache{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", statut='" + statut + '\'' +
                ", level=" + level +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", projetId=" + projetId +
                ", employeId=" + employeId +
                ", primeId=" + primeId +
                '}';
    }
}