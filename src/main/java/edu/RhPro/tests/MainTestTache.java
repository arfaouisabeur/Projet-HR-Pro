package edu.RhPro.tests;

import edu.RhPro.entities.Tache;
import edu.RhPro.services.TacheService;

public class MainTestTache {

    public static void main(String[] args) {

        try {
            TacheService service = new TacheService();

            // ➕ Ajout d'une tâche (SANS id)
            Tache t1 = new Tache(
                    "Analyse",
                    "Anesoins",
                    "A_FAIRE",
                    1,   // projet_id
                    2,   // employe_id
                    null // date_fin (optionnelle)
            );

            service.addTache(t1);

            System.out.println("✅ Tâche ajoutée avec succès !");

            // 📋 Affichage de toutes les tâches
            System.out.println("📋 Liste des tâches :");
            service.getAllTaches().forEach(System.out::println);

        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
        }
    }
}
