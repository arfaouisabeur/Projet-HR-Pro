package edu.RhPro.entities;

import java.time.LocalDate;

public class offreEmploi {
    private int id;
    private String titre;
    private String description;
    private String localisation;
    private String typeContrat;
    private LocalDate datePublication;
    private LocalDate dateExpiration;
    private String statut; // ACTIVE / FERMEE
    private Integer rhId;  // يمكن يكون null

    public offreEmploi() {}

    public offreEmploi(String titre, String description, String localisation, String typeContrat,
                       LocalDate datePublication, LocalDate dateExpiration, String statut, Integer rhId) {
        this.titre = titre;
        this.description = description;
        this.localisation = localisation;
        this.typeContrat = typeContrat;
        this.datePublication = datePublication;
        this.dateExpiration = dateExpiration;
        this.statut = statut;
        this.rhId = rhId;
    }

    public offreEmploi(int id, String titre, String description, String localisation, String typeContrat,
                       LocalDate datePublication, LocalDate dateExpiration, String statut, Integer rhId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.localisation = localisation;
        this.typeContrat = typeContrat;
        this.datePublication = datePublication;
        this.dateExpiration = dateExpiration;
        this.statut = statut;
        this.rhId = rhId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public String getTypeContrat() { return typeContrat; }
    public void setTypeContrat(String typeContrat) { this.typeContrat = typeContrat; }

    public LocalDate getDatePublication() { return datePublication; }
    public void setDatePublication(LocalDate datePublication) { this.datePublication = datePublication; }

    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Integer getRhId() { return rhId; }
    public void setRhId(Integer rhId) { this.rhId = rhId; }

    @Override
    public String toString() {
        return "OffreEmploi{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", localisation='" + localisation + '\'' +
                ", typeContrat='" + typeContrat + '\'' +
                ", datePublication=" + datePublication +
                ", dateExpiration=" + dateExpiration +
                ", statut='" + statut + '\'' +
                ", rhId=" + rhId +
                '}';
    }
}
