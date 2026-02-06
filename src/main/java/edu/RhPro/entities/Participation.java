package edu.RhPro.entities;
import java.time.LocalDate;

public class Participation {
    private long id;
    private LocalDate dateInscription;
    private String statut;
    private long evenementId;
    private long employeId;

    // 🔹 Constructeur vide (obligatoire)
    public Participation() {
    }

    // 🔹 Constructeur sans id (INSERT)
    public Participation(LocalDate dateInscription, String statut, long evenementId, long employeId) {
        this.dateInscription = dateInscription;
        this.statut = statut;
        this.evenementId = evenementId;
        this.employeId = employeId;
    }

    // 🔹 Constructeur avec id (SELECT)
    public Participation(long id, LocalDate dateInscription, String statut, long evenementId, long employeId) {
        this.id = id;
        this.dateInscription = dateInscription;
        this.statut = statut;
        this.evenementId = evenementId;
        this.employeId = employeId;
    }

    // 🔹 Getters & Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDate getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDate dateInscription) {
        this.dateInscription = dateInscription;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
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

    @Override
    public String toString() {
        return "Participation{" +
                "id=" + id +
                ", dateInscription=" + dateInscription +
                ", statut='" + statut + '\'' +
                '}';
    }
}
