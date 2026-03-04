package edu.RhPro.entities;

import java.time.LocalDate;

public class Conge {
    private long id;
    private String typeConge;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut;
    private String description;
    private long employeeId;

    public Conge() {
    }

    public Conge(String typeConge, LocalDate dateDebut, LocalDate dateFin, String statut, String description, long employeeId) {
        this.typeConge = typeConge;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.description = description;
        this.employeeId = employeeId;
    }

    public Conge(long id, String typeConge, LocalDate dateDebut, LocalDate dateFin, String statut, String description, long employeeId) {
        this.id = id;
        this.typeConge = typeConge;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.description = description;
        this.employeeId = employeeId;
    }

    public long getId() {
        return id;
    }

    public String getTypeConge() {
        return typeConge;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public String getStatut() {
        return statut;
    }

    public String getDescription() {
        return description;
    }

    public long getEmployeeId() {
        return employeeId;
    }

    public void setTypeConge(String typeConge) {
        this.typeConge = typeConge;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEmployeeId(long employeeId) {
        this.employeeId = employeeId;
    }

    @Override
    public String toString() {
        return "Conge{" +
                "id=" + id +
                ", typeConge='" + typeConge + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", statut='" + statut + '\'' +
                ", description='" + description + '\'' +
                ", employeeId=" + employeeId +
                '}';
    }
}
