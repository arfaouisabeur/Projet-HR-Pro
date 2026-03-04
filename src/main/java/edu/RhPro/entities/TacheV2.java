package edu.RhPro.entities;

import java.time.LocalDate;

public class TacheV2 extends Tache {

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int level;

    public TacheV2() {
        super();
    }

    public TacheV2(Tache base, LocalDate dateDebut, LocalDate dateFin, int level) {
        super();
        if (base != null) {
            setId(base.getId());
            setTitre(base.getTitre());
            setDescription(base.getDescription());
            setStatut(base.getStatut());
            setProjetId(base.getProjetId());
            setEmployeId(base.getEmployeId());
            setPrimeId(base.getPrimeId());
        }
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.level = level;
    }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getLevelLabel() { return "L" + level; }
}