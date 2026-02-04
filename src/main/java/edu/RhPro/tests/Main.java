package edu.RhPro.tests;

import edu.RhPro.entities.Prime;
import edu.RhPro.entities.Salaire;
import edu.RhPro.services.PrimeService;
import edu.RhPro.services.SalaireService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        try {
            SalaireService salaireService = new SalaireService();
            PrimeService primeService = new PrimeService();

            // ADD SALAIRE
            Salaire s = new Salaire(
                    2, 2026,
                    new BigDecimal("1500.00"),
                    LocalDate.now(),
                    "PAYE",
                    1, 2
            );
            salaireService.addEntity(s);
            System.out.println("✅ Salaire ajouté : " + s);

            // ADD PRIME
            Prime p = new Prime(
                    new BigDecimal("200.00"),
                    LocalDate.now(),
                    "Prime test",
                    1, 2
            );
            primeService.addEntity(p);
            System.out.println("✅ Prime ajoutée : " + p);

            // SHOW (just to confirm)
            System.out.println("\n--- Dernier état ---");
            System.out.println("Salaires count = " + salaireService.getData().size());
            System.out.println("Primes count   = " + primeService.getData().size());

        } catch (SQLException e) {
            System.out.println("❌ SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
