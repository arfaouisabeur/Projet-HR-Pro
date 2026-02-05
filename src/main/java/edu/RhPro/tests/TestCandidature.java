package edu.RhPro.tests;

import edu.RhPro.entities.Candidature;
import edu.RhPro.services.CandidatureService;

import java.time.LocalDate;

public class TestCandidature {
    public static void main(String[] args) {
        try {
            CandidatureService service = new CandidatureService();

            long CANDIDAT_ID = 3;     // 🔁 change selon ta table candidat
            long OFFRE_ID = 3;        // ton offre_emploi id=1 existe déjà

            // CREATE
            Candidature c = new Candidature(
                    LocalDate.now(),
                    "EN_ATTENTE",
                    null,
                    CANDIDAT_ID,
                    OFFRE_ID
            );
            service.add(c);
            System.out.println("✅ Candidature ajoutée");

            // READ ALL
            System.out.println("📌 Toutes les candidatures :");
            service.findAll().forEach(System.out::println);

            // UPDATE (sur id=1 par exemple)
            Candidature toUpdate = service.findById(1);
            if (toUpdate != null) {
                toUpdate.setStatut("ACCEPTEE");
                service.update(toUpdate);
                System.out.println("✅ Candidature mise à jour");
            }

            // DELETE (exemple)
            // service.delete(1);
            // System.out.println("🗑️ Candidature supprimée");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
