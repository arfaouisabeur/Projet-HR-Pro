package edu.RhPro.tests;

import edu.RhPro.entities.Service;
import edu.RhPro.services.ServiceService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MainService {
    public static void main(String[] args) {
        ServiceService serviceService = new ServiceService();

        try {
            // 1. AJOUT D'UNE DEMANDE DE SERVICE

            /*System.out.println("=== AJOUT D'UNE NOUVELLE DEMANDE ===");
            Service nouvelleDemande = new Service(
                "Demande de logiciel",
                "Besoin d'accès au logiciel simple ",
                LocalDate.now(),
                "En attente",
                4
            );

            serviceService.addEntity(nouvelleDemande);
            System.out.println("Demande ajoutée avec succès!");

            // Affichage de toutes les demandes après ajout
            List<Service> demandes = serviceService.getData();
            System.out.println("\nListe des demandes après ajout:");
            demandes.forEach(System.out::println);*/






            // 1. UPDATE D'UNE DEMANDE DE SERVICE

            /*System.out.println("=== AJOUT D'UNE NOUVELLE DEMANDE ===");
            Service nouvelleDemande = new Service(1,
                "Demande de support",
                "Besoin d'accès au logiciel simple ",
                LocalDate.now(),
                "En attente",
                4
            );

            serviceService.updateEntity(nouvelleDemande);
            System.out.println("Demande ajoutée avec succès!");

            // Affichage de toutes les demandes après ajout
            List<Service> demandes = serviceService.getData();
            System.out.println("\nListe des demandes après ajout:");
            demandes.forEach(System.out::println);*/











            // 1. DELETE D'UNE DEMANDE DE SERVICE

            System.out.println("=== AJOUT D'UNE NOUVELLE DEMANDE ===");
            Service nouvelleDemande = new Service(2,
                "Demande de logiciel",
                "Besoin d'accès au logiciel simple ",
                LocalDate.now(),
                "En attente",
                4
            );

            serviceService.deleteEntity(nouvelleDemande);
            System.out.println("Demande ajoutée avec succès!");

            // Affichage de toutes les demandes après ajout
            List<Service> demandes = serviceService.getData();
            System.out.println("\nListe des demandes après ajout:");
            demandes.forEach(System.out::println);

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'opération:");
            e.printStackTrace();
        }
    }
}