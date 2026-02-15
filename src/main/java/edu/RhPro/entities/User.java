package edu.RhPro.entities;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String mot_de_passe;
    private String telephone;
    private String adresse;
    private String role;

    public User(){}

    public User(int id, String nom, String prenom, String email, String mot_de_passe, String telephone, String adresse, String role) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.mot_de_passe = mot_de_passe;
        this.telephone = telephone;
        this.adresse = adresse;
        this.role = role;
    }

    public User(String nom, String prenom, String email, String mot_de_passe, String telephone, String adresse, String role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.mot_de_passe = mot_de_passe;
        this.telephone = telephone;
        this.adresse = adresse;
        this.role = role;
    }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", nom=" + nom + ", prenom=" + prenom + ", email=" + email  + ", mot_de_passe=" + mot_de_passe + ", telephone=" + telephone + ", adresse=" + adresse + ", role=" + role + '}';
    }


    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }


    public String getNom() { return nom; }
    public void setNom(String nom) {this.nom = nom; }

    public String getPrenom() {
        return prenom;
    }
    public void setPrenom(String prenom) {this.prenom = prenom; }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {this.email = email; }

    public String getMot_de_passe() {
        return mot_de_passe;
    }

    public void setMot_de_passe(String mot_de_passe) {this.mot_de_passe = mot_de_passe; }
    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {this.telephone = telephone; }
    public String getAdresse() {
        return adresse;
    }
    public void setAdresse(String adresse) {this.adresse = adresse; }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {this.role = role; }















}
