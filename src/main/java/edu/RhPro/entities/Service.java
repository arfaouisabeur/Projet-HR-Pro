package edu.RhPro.entities;

import java.time.LocalDate;

public class Service {
    private long id;
    private String titre;
    private String description;
    private LocalDate dateDemande;
    private String statut;
    private long employeeId;

    public Service() {
    }

    public Service(String titre, String description, LocalDate dateDemande, String statut, long employeeId) {
        this.titre = titre;
        this.description = description;
        this.dateDemande = dateDemande;
        this.statut = statut;
        this.employeeId = employeeId;
    }
    public Service(long id, String titre, String description, LocalDate dateDemande, String statut, long employeeId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateDemande = dateDemande;
        this.statut = statut;
        this.employeeId = employeeId;
    }
    public long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDateDemande() {
        return dateDemande;
    }

    public String getStatut() {
        return statut;
    }

    public long getEmployeeId() {
        return employeeId;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDateDemande(LocalDate dateDemande) {
        this.dateDemande = dateDemande;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public void setEmployeeId(long employeeId) {
        this.employeeId = employeeId;
    }

    @Override
    public String toString() {
        return "Service{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", dateDemande=" + dateDemande +
                ", statut='" + statut + '\'' +
                ", employeeId=" + employeeId +
                '}';
    }
}
