package edu.RhPro.services;

import edu.RhPro.entities.Projet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
class ProjetServiceTest extends BaseServiceTest {

    private ProjetService projetService;
    private Projet testProjet;

    @BeforeEach
    void setUp() {
        projetService = new ProjetService();

        // Créer un projet de test avec un timestamp unique
        String uniqueId = String.valueOf(System.currentTimeMillis());
        testProjet = new Projet(
                "TEST_Application_" + uniqueId,
                "TEST_Développement application mobile",
                "DOING",
                1, // rhId
                2, // responsableEmployeId
                LocalDate.now(),
                LocalDate.now().plusMonths(2)
        );
    }

    @Test
    @Order(1)
    void testAddProjet() throws SQLException {
        // Act
        projetService.addProjet(testProjet);

        // Assert - Vérifier que le projet a été ajouté
        List<Projet> projets = projetService.getAllProjets();
        boolean found = projets.stream()
                .anyMatch(p -> p.getTitre().equals(testProjet.getTitre()) &&
                        p.getDescription().equals(testProjet.getDescription()));

        assertTrue(found, "Le projet de test devrait être trouvé dans la base");

        // Récupérer l'ID du projet pour les tests suivants
        Projet added = projets.stream()
                .filter(p -> p.getTitre().equals(testProjet.getTitre()))
                .findFirst()
                .orElse(null);

        if (added != null) {
            testProjet.setId(added.getId());
        }
    }

    @Test
    @Order(2)
    void testGetAllProjets() throws SQLException {
        // Arrange - Ajouter d'abord un projet
        projetService.addProjet(testProjet);

        // Act
        List<Projet> projets = projetService.getAllProjets();

        // Assert
        assertNotNull(projets, "La liste ne devrait pas être null");
        assertFalse(projets.isEmpty(), "La liste ne devrait pas être vide");

        // Vérifier que notre projet de test est présent
        boolean testProjetFound = projets.stream()
                .anyMatch(p -> p.getTitre().startsWith("TEST_"));
        assertTrue(testProjetFound, "Le projet de test devrait être présent");
    }

    @Test
    @Order(3)
    void testGetProjetById() throws SQLException {
        // Arrange
        projetService.addProjet(testProjet);

        // Récupérer l'ID du projet
        List<Projet> projets = projetService.getAllProjets();
        Projet saved = projets.stream()
                .filter(p -> p.getTitre().equals(testProjet.getTitre()))
                .findFirst()
                .orElse(null);

        assertNotNull(saved, "Le projet devrait exister");
        int id = saved.getId();

        // Act
        Projet found = projetService.getProjetById(id);

        // Assert
        assertNotNull(found, "Le projet devrait être trouvé par ID");
        assertEquals(testProjet.getTitre(), found.getTitre());
        assertEquals(testProjet.getDescription(), found.getDescription());
        assertEquals(testProjet.getStatut(), found.getStatut());
        assertEquals(testProjet.getRhId(), found.getRhId());
        assertEquals(testProjet.getResponsableEmployeId(), found.getResponsableEmployeId());
        assertEquals(testProjet.getDateDebut(), found.getDateDebut());
        assertEquals(testProjet.getDateFin(), found.getDateFin());
    }

    @Test
    @Order(4)
    void testUpdateProjet() throws SQLException {
        // Arrange
        projetService.addProjet(testProjet);

        // Récupérer l'ID
        List<Projet> projets = projetService.getAllProjets();
        Projet saved = projets.stream()
                .filter(p -> p.getTitre().equals(testProjet.getTitre()))
                .findFirst()
                .orElse(null);

        assertNotNull(saved);
        testProjet.setId(saved.getId());

        // Modifier le projet
        String nouveauTitre = "TEST_Update_" + System.currentTimeMillis();
        String nouveauStatut = "DONE";
        String nouvelleDescription = "TEST_Description mise à jour";

        testProjet.setTitre(nouveauTitre);
        testProjet.setStatut(nouveauStatut);
        testProjet.setDescription(nouvelleDescription);

        // Act
        projetService.updateProjet(testProjet);

        // Assert
        Projet updated = projetService.getProjetById(testProjet.getId());

        assertNotNull(updated, "Le projet mis à jour devrait exister");
        assertAll("Vérification des champs mis à jour",
                () -> assertEquals(nouveauTitre, updated.getTitre(), "Le titre devrait être mis à jour"),
                () -> assertEquals(nouveauStatut, updated.getStatut(), "Le statut devrait être mis à jour"),
                () -> assertEquals(nouvelleDescription, updated.getDescription(), "La description devrait être mise à jour")
        );
    }

    @Test
    @Order(5)
    void testFindByResponsableId() throws SQLException {
        // Arrange
        projetService.addProjet(testProjet);
        int responsableId = testProjet.getResponsableEmployeId();

        // Act
        List<Projet> projets = projetService.findByResponsableId(responsableId);

        // Assert
        assertNotNull(projets, "La liste ne devrait pas être null");
        assertTrue(projets.size() > 0, "Au moins un projet devrait être trouvé");

        boolean found = projets.stream()
                .anyMatch(p -> p.getTitre().startsWith("TEST_"));
        assertTrue(found, "Le projet de test devrait être trouvé par responsable");
    }

    @Test
    @Order(6)
    void testDeleteProjet() throws SQLException {
        // Arrange
        projetService.addProjet(testProjet);

        // Récupérer l'ID
        List<Projet> projets = projetService.getAllProjets();
        Projet saved = projets.stream()
                .filter(p -> p.getTitre().equals(testProjet.getTitre()))
                .findFirst()
                .orElse(null);

        assertNotNull(saved);
        int idToDelete = saved.getId();

        // Act
        projetService.deleteProjet(idToDelete);

        // Assert
        Projet deleted = projetService.getProjetById(idToDelete);
        assertNull(deleted, "Le projet supprimé ne devrait plus exister");
    }

    @Test
    void testAddProjetWithNullFields() {
        // Arrange
        Projet invalidProjet = new Projet(
                null, // Titre null
                "Description",
                "DOING",
                1, 2,
                LocalDate.now(),
                LocalDate.now().plusDays(1)
        );

        // Act & Assert
        assertThrows(SQLException.class, () -> {
            projetService.addProjet(invalidProjet);
        }, "Ajouter un projet avec titre null devrait lancer SQLException");
    }



    @Test
    void testGetNonExistentProjetById() throws SQLException {
        // Act
        Projet found = projetService.getProjetById(99999);

        // Assert
        assertNull(found, "La recherche d'un ID inexistant devrait retourner null");
    }

    @Test
    void testDeleteNonExistentProjet() {
        // Act & Assert - Ne devrait PAS lancer d'exception
        assertDoesNotThrow(() -> {
            projetService.deleteProjet(99999);
        }, "La suppression d'un projet inexistant ne devrait pas lancer d'exception");
    }

    @Test
    void testUpdateNonExistentProjet() {
        // Arrange
        Projet nonExistentProjet = new Projet(
                99999,
                "TEST_NonExistent",
                "Description",
                "DOING",
                1, 2,
                LocalDate.now(),
                LocalDate.now().plusDays(1)
        );

        // Act & Assert - Ne devrait PAS lancer d'exception
        assertDoesNotThrow(() -> {
            projetService.updateProjet(nonExistentProjet);
        }, "La modification d'un projet inexistant ne devrait pas lancer d'exception");
    }

    @Test
    void testFindByResponsableIdWithNoResults() throws SQLException {
        // Act
        List<Projet> projets = projetService.findByResponsableId(99999);

        // Assert
        assertNotNull(projets, "La liste ne devrait pas être null");
        assertTrue(projets.isEmpty(), "La liste devrait être vide pour un responsable inexistant");
    }


    @Test
    void testAddProjetWithInvalidDates() {
        // Arrange
        Projet projetDatesInvalides = new Projet(
                "TEST_Projet_Dates_Invalides_" + System.currentTimeMillis(),
                "Description test",
                "DOING",
                1, 2,
                LocalDate.now().plusDays(5), // date début > date fin
                LocalDate.now()
        );

        // Act & Assert - Ne devrait PAS lancer d'exception
        assertDoesNotThrow(() -> {
            projetService.addProjet(projetDatesInvalides);
        }, "L'ajout avec dates invalides ne devrait pas lancer d'exception (pas de contrainte CHECK)");
    }

    // Test avec responsables valides
    @Test
    void testAddMultipleProjets() throws SQLException {
        // Arrange - Utiliser des responsables qui existent
        int responsableId1 = 2; // À remplacer par un ID valide
        int responsableId2 = 4; // À remplacer par un ID valide

        Projet projet1 = new Projet(
                "TEST_Projet_1_" + System.currentTimeMillis(),
                "Description 1",
                "DOING",
                1, responsableId1,
                LocalDate.now(),
                LocalDate.now().plusMonths(1)
        );

        Projet projet2 = new Projet(
                "TEST_Projet_2_" + System.currentTimeMillis(),
                "Description 2",
                "DOING",
                1, responsableId2,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusMonths(2)
        );

        // Act
        projetService.addProjet(projet1);
        projetService.addProjet(projet2);

        // Assert
        List<Projet> allProjets = projetService.getAllProjets();
        long testProjectsCount = allProjets.stream()
                .filter(p -> p.getTitre().startsWith("TEST_"))
                .count();

        assertTrue(testProjectsCount >= 2, "Au moins 2 projets de test devraient exister");
    }
}
