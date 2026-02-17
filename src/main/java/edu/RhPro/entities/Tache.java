package edu.RhPro.entities;

public class Tache {

    private int id;
    private String titre;
    private String description;
    private String statut;
    private int projetId;
    private int employeId;
    private Integer primeId;

    // 🔹 Constructeur pour INSERT (sans id)
    public Tache(String titre, String description, String statut,
                 int projetId, int employeId, Integer primeId) {
        this.titre = titre;
        this.description = description;
        this.statut = statut;
        this.projetId = projetId;
        this.employeId = employeId;
        this.primeId = primeId;
    }

    // 🔹 Constructeur pour SELECT (avec id)
    public Tache(int id, String titre, String description, String statut,
                 int projetId, int employeId, Integer primeId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.statut = statut;
        this.projetId = projetId;
        this.employeId = employeId;
        this.primeId = primeId;
    }

    // getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public String getStatut() { return statut; }
    public int getProjetId() { return projetId; }
    public int getEmployeId() { return employeId; }
    public Integer getPrimeId() { return primeId; }

    public void setStatut(String statut) { this.statut = statut; }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProjetId(int projetId) {
        this.projetId = projetId;
    }

    public void setEmployeId(int employeId) {
        this.employeId = employeId;
    }

    public void setPrimeId(Integer primeId) {
        this.primeId = primeId;
    }

    @Override
    public String toString() {
        return "Tache{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}
