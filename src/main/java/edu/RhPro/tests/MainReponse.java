package edu.RhPro.tests;

import edu.RhPro.entities.Reponse;
import edu.RhPro.services.ReponseService;

import java.sql.SQLException;
import java.util.List;

public class MainReponse {

    public static void main(String[] args) {

        ReponseService reponseService = new ReponseService();

        try {

            // =======================
            // AJOUT REPONSE CONGE
            // =======================


            /*System.out.println("=== AJOUT REPONSE CONGE ===");

            Reponse rConge = Reponse.forConge(
                    "Acceptée",
                    "Votre congé est validé",
                    1,      // rh_id
                    4L,     // employe_id
                    8L      // conge_tt_id
            );

            reponseService.addEntity(rConge);
            System.out.println("Réponse congé ajoutée !");



            // =======================
            // AJOUT REPONSE SERVICE
            // =======================


            System.out.println("=== AJOUT REPONSE SERVICE ===");

            Reponse rService = Reponse.forService(
                    "Refusée",
                    "Demande incomplète",
                    1,      // rh_id
                    4L,     // employe_id
                    null      // demande_service_id
            );

            reponseService.addEntity(rService);
            System.out.println("Réponse service ajoutée !");



            // =======================
            // AFFICHAGE
            // =======================

            System.out.println("\n=== LISTE DES REPONSES ===");
            List<Reponse> reponses = reponseService.getData();
            reponses.forEach(System.out::println);*/


            // =======================
            // UPDATE
            // =======================


            /*System.out.println("\n=== UPDATE REPONSE ===");

            Reponse rUpdate = new Reponse(
                    30,                      // id reponse
                    "Acceptée",
                    "congéeeeeeeee non validé",
                    1,
                    4L,
                    8L,
                    null
            );

            reponseService.updateEntity(rUpdate);
            System.out.println("Réponse modifiée !");*/



            // =======================
            // DELETE
            // =======================


            System.out.println("\n=== DELETE REPONSE ===");

            Reponse rDelete = new Reponse();
            rDelete.setId(30);   // id de la réponse

            reponseService.deleteEntity(rDelete);
            System.out.println("Réponse supprimée !");



        } catch (SQLException e) {
            System.err.println("Erreur Reponse:");
            e.printStackTrace();
        }
    }
}
