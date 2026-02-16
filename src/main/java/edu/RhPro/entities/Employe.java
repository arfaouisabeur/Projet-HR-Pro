package edu.RhPro.entities;

import java.time.LocalDate;

public class Employe extends User {

    private String matricule;
    private String position;
    private LocalDate dateEmbauche;

    public Employe() {
        super();
        setRole("EMPLOYE");
    }

    public Employe(String nom, String prenom, String email, String motDePasse, String telephone, String adresse,
                   String matricule, String position, LocalDate dateEmbauche) {
        super(nom, prenom, email, motDePasse, telephone, adresse, "EMPLOYE");
        this.matricule = matricule;
        this.position = position;
        this.dateEmbauche = dateEmbauche;
    }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public LocalDate getDateEmbauche() { return dateEmbauche; }
    public void setDateEmbauche(LocalDate dateEmbauche) { this.dateEmbauche = dateEmbauche; }
}
