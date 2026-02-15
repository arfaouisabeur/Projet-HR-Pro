package edu.RhPro.tests;

import edu.RhPro.entities.offreEmploi;
import edu.RhPro.services.OffreEmploiService;

import java.time.LocalDate;

public class TestOffreEmploi {
    public static void main(String[] args) {
        try {
            OffreEmploiService service = new OffreEmploiService();

            // CREATE
            offreEmploi o = new offreEmploi(
                    "Developpeur Java",
                    "Stage ete",
                    "kairouan",
                    "Stage",
                    LocalDate.now(),
                    LocalDate.now().plusDays(30),
                    "ACTIVE",
                    1
            );
            service.add(o);
            System.out.println("✅ Offre ajoutée");

            // READ ALL
            System.out.println(" Toutes les offres:");
            service.findAll().forEach(System.out::println);

            // Exemple: fermer l'offre id=1 (بدّل id حسب عندك)
            // service.fermer(1);
            // System.out.println("✅ Offre fermée");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
//                                           ** TEST UPDATE **
//package edu.RhPro.tests;
//
//import edu.RhPro.entities.OffreEmploi;
//import edu.RhPro.services.OffreEmploiService;
//
//import java.time.LocalDate;
//
//public class TestUpdateOffreEmploi {
//    public static void main(String[] args) {
//        try {
//            OffreEmploiService service = new OffreEmploiService();
//
//            OffreEmploi offre = new OffreEmploi(
//                    1, // ID EXISTANT
//                    "Développeur Java Senior",
//                    "Offre mise à jour",
//                    "Tunis",
//                    "CDI",
//                    LocalDate.now(),
//                    LocalDate.now().plusDays(45),
//                    "ACTIVE",
//                    1
//            );
//
//            service.update(offre);
//            System.out.println("✅ Offre emploi mise à jour");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
//                                    ** TEST DELETE **
//package edu.RhPro.tests;
//
//import edu.RhPro.services.OffreEmploiService;
//
//public class TestDeleteOffreEmploi {
//    public static void main(String[] args) {
//        try {
//            OffreEmploiService service = new OffreEmploiService();
//
//            service.delete(1); // ID EXISTANT
//            System.out.println("🗑️ Offre emploi supprimée");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
