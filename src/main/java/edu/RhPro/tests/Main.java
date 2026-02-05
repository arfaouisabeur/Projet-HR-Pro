package edu.RhPro.tests;

import edu.RhPro.entities.Conge;
import edu.RhPro.services.CongeService;

import java.sql.SQLException;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        CongeService congeService = new CongeService();

        try {
            // Création d'un nouveau congé
            /*Conge nouveauConge = new Conge(
                    "Annuel",                  // Type de congé
                    LocalDate.of(2023, 12, 20),     // Date de début
                    LocalDate.of(2023, 12, 31),     // Date de fin
                    "En attente",                   // Statut
                    "Demande de congé de fin d'année", // Description
                    4
            );

            // Ajout du congé dans la base de données
            congeService.addEntity(nouveauConge);
            System.out.println("Congé ajouté avec succès!");*/




            // Update congé
            /*Conge nouveauConge = new Conge(7,
                    "Annuel",                  // Type de congé
                    LocalDate.of(2023, 12, 20),     // Date de début
                    LocalDate.of(2023, 12, 31),     // Date de fin
                    "En attente",                   // Statut
                    "yna3n omk", // Description
                    4
            );

            // Ajout du congé dans la base de données
            congeService.updateEntity(nouveauConge);
            System.out.println("Congé updated avec succès!");*/






            // delete congé
            Conge nouveauConge = new Conge(13,
                    "Annuel",                  // Type de congé
                    LocalDate.of(2023, 12, 20),     // Date de début
                    LocalDate.of(2023, 12, 31),     // Date de fin
                    "En attente",                   // Statut
                    "yna3n omk", // Description
                    4
            );

            // Ajout du congé dans la base de données
            congeService.deleteEntity(nouveauConge);
            System.out.println("Congé deleted avec succès!");


        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du congé:");
            e.printStackTrace();
        }
    }
}