package edu.RhPro.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Salaire {
    private long id;
    private int mois;
    private int annee;
    private BigDecimal montant;
    private LocalDate datePaiement;
    private String statut;
    private long rhId;
    private long employeId;

    public Salaire() {}

    public Salaire(int mois, int annee, BigDecimal montant, LocalDate datePaiement,
                   String statut, long rhId, long employeId) {
        this.mois = mois;
        this.annee = annee;
        this.montant = montant;
        this.datePaiement = datePaiement;
        this.statut = statut;
        this.rhId = rhId;
        this.employeId = employeId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getMois() { return mois; }
    public void setMois(int mois) { this.mois = mois; }

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDate getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDate datePaiement) { this.datePaiement = datePaiement; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public long getRhId() { return rhId; }
    public void setRhId(long rhId) { this.rhId = rhId; }

    public long getEmployeId() { return employeId; }
    public void setEmployeId(long employeId) { this.employeId = employeId; }

    @Override
    public String toString() {
        return "Salaire{" +
                "id=" + id +
                ", mois=" + mois +
                ", annee=" + annee +
                ", montant=" + montant +
                ", datePaiement=" + datePaiement +
                ", statut='" + statut + '\'' +
                ", rhId=" + rhId +
                ", employeId=" + employeId +
                '}';
    }
}
