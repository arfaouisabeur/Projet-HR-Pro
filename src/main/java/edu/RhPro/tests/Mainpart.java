package edu.RhPro.tests;

import edu.RhPro.entities.Participation;
import edu.RhPro.services.ParticipationService;

import java.sql.SQLException;
import java.time.LocalDate;

public class Mainpart {
    public static void main(String[] args) {
        try {
            ParticipationService participationService = new ParticipationService();

            // ADD participation
            Participation p = new Participation(
                    LocalDate.of(2003,11,5),
                    "present",
                    2,
                    2
            );

            participationService.addEntity(p);
            System.out.println("✅ Événement updated : " + p);

        } catch (SQLException ex) {
            System.out.println("❌ SQL Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
