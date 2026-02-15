package edu.RhPro.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Prime {
    private long id;
    private BigDecimal montant;
    private LocalDate dateAttribution;
    private String description;
    private long rhId;
    private long employeId;



    public Prime() {}

    public Prime(BigDecimal montant, LocalDate dateAttribution, String description,
                 long rhId, long employeId) {
        this.montant = montant;
        this.dateAttribution = dateAttribution;
        this.description = description;
        this.rhId = rhId;
        this.employeId = employeId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDate getDateAttribution() { return dateAttribution; }
    public void setDateAttribution(LocalDate dateAttribution) { this.dateAttribution = dateAttribution; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getRhId() { return rhId; }
    public void setRhId(long rhId) { this.rhId = rhId; }

    public long getEmployeId() { return employeId; }
    public void setEmployeId(long employeId) { this.employeId = employeId; }

    @Override
    public String toString() {
        return "Prime{" +
                "id=" + id +
                ", montant=" + montant +
                ", dateAttribution=" + dateAttribution +
                ", description='" + description + '\'' +
                ", rhId=" + rhId +
                ", employeId=" + employeId +
                '}';
    }
}
