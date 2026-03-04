package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BaseServiceTest {

    protected Connection connection;

    @BeforeEach
    void setUpDatabase() {
        try {
            // Utiliser MyConnection.getInstance().getCnx() comme dans votre code principal
            connection = MyConnection.getInstance().getCnx();

            // Vérifier que la connexion n'est pas null
            if (connection == null) {
                System.err.println("⚠️ Attention: La connexion à la base de données est null");
                System.err.println("Vérifiez que MySQL est démarré (XAMPP)");
                return;
            }

            if (connection.isClosed()) {
                System.err.println("⚠️ Attention: La connexion à la base de données est fermée");
                return;
            }

            System.out.println("✅ Connexion à la base de données établie pour les tests");
            cleanTestData();

        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion à la base de données: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @AfterEach
    void tearDown() {
        try {
            if (connection != null && !connection.isClosed()) {
                cleanTestData();
            }
        } catch (SQLException e) {
            System.out.println("Note lors du nettoyage: " + e.getMessage());
        }
    }

    private void cleanTestData() throws SQLException {
        if (connection == null || connection.isClosed()) {
            System.out.println("⚠️ Pas de connexion, nettoyage ignoré");
            return;
        }

        try (Statement stmt = connection.createStatement()) {
            // Supprimer les données de test
            stmt.execute("DELETE FROM evenement WHERE titre LIKE 'TEST_%'");
            System.out.println("🧹 Données de test nettoyées");
        } catch (SQLException e) {
            // Ignorer si la table n'existe pas
            System.out.println("Note lors du nettoyage: " + e.getMessage());
        }
    }
}