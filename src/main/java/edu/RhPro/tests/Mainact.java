package edu.RhPro.tests;

import edu.RhPro.entities.Activite;
import edu.RhPro.entities.Activite;
import edu.RhPro.services.ActiviteService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class Mainact {
    public static void main(String[] args) {
        try {
            ActiviteService activiteService = new ActiviteService();

            // ADD activite
            Activite a = new Activite(
                    "formation",
                    "tttttttt",
                    2
            );

            activiteService.addEntity(a);
            System.out.println("✅ Événement updated : " + a);

        } catch (SQLException ex) {
            System.out.println("❌ SQL Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}




