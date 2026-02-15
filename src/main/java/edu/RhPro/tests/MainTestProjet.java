package edu.RhPro.tests;

import edu.RhPro.entities.Projet;
import edu.RhPro.services.ProjetService;

import java.time.LocalDate;

public class MainTestProjet {

    public static void main(String[] args) {

        try {
            ProjetService service = new ProjetService();

            // ➕ Ajout d'un projet (SANS id)
            Projet p1 = new Projet(
                    "Site Web",
                    "Création du sit de l'entreprise",
                    "A_FAIRE",
                    1, // rh_id
                    2, // responsable_employe_id
                    LocalDate.of(2026, 2, 5),  // date_debut
                    LocalDate.of(2026, 5, 30)  // date_fin
            );

            // ➕ Ajout d'un autre projet
            Projet p2 = new Projet(
                    "Application Mobile",
                    "Développement de l'application mobile",
                    "EN:",
                    1, // rh_id
                    2, // responsable_employe_id
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 7, 31)
            );

            // Ajout des projets dans le service
            service.addProjet(p1);
            service.addProjet(p2);


            System.out.println("✅ Projet ajouté et suppression effectuée avec succès !");

            // 📋 Affichage de tous les projets
            System.out.println("📋 Liste des projets :");
            service.getAllProjets().forEach(System.out::println);

        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
        }
    }
}
