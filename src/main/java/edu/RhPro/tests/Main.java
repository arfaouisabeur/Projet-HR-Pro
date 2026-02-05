package edu.RhPro.tests;

import edu.RhPro.entities.Evenement;
import edu.RhPro.services.EvenementService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        try {
            EvenementService evenementService = new EvenementService();

            // ADD EVENEMENT
            Evenement e = new Evenement(
                    "Lomou Lomou",
                    LocalDateTime.of(2026, 12, 20, 9, 0),
                    LocalDateTime.of(2026, 12, 24, 18, 0),
                    "ynan bouk",
                    "PAYE",
                    1
            );

            evenementService.addEntity(e);
            System.out.println("✅ Événement updated : " + e);

        } catch (SQLException ex) {
            System.out.println("❌ SQL Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
