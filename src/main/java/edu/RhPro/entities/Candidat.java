package edu.RhPro.entities;

public class Candidat extends User {

    private String cv;
    private String niveauEtude;
    private int experience;

    public Candidat() {
        super();
        setRole("CANDIDAT");
    }

    public Candidat(String nom, String prenom, String email, String motDePasse, String telephone, String adresse,
                    String cv, String niveauEtude, int experience) {
        super(nom, prenom, email, motDePasse, telephone, adresse, "CANDIDAT");
        this.cv = cv;
        this.niveauEtude = niveauEtude;
        this.experience = experience;
    }

    public String getCv() { return cv; }
    public void setCv(String cv) { this.cv = cv; }

    public String getNiveauEtude() { return niveauEtude; }
    public void setNiveauEtude(String niveauEtude) { this.niveauEtude = niveauEtude; }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }
}
