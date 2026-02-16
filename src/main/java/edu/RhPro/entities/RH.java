package edu.RhPro.entities;

public class RH extends User {

    // add fields here if you have (department, grade, etc)
    public RH() {
        super();
        setRole("RH");
    }

    // If you want constructor with User data:
    public RH(String nom, String prenom, String email, String motDePasse, String telephone, String adresse) {
        super(nom, prenom, email, motDePasse, telephone, adresse, "RH");
    }
}
