package edu.RhPro.services;

import edu.RhPro.entities.Evenement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
class EvenementServiceTest extends BaseServiceTest {

    private EvenementService evenementService;
    private Evenement testEvent;

    @BeforeEach
    void setUp() {
        evenementService = new EvenementService();

        // Créer un événement de test avec un timestamp unique
        String uniqueId = String.valueOf(System.currentTimeMillis());
        testEvent = new Evenement(
                "TEST_Conference_Java_" + uniqueId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2).plusHours(2),
                "TEST_Salle Paris",
                "TEST_Description de la conférence Java",
                "https://test.com/java.jpg",
                1L
        );
    }

    @Test
    @Order(1)
    void testAddEntity() throws SQLException {
        // Act
        evenementService.addEntity(testEvent);

        // Assert
        assertNotEquals(0, testEvent.getId(), "L'ID devrait être généré après l'ajout");
        assertTrue(testEvent.getId() > 0, "L'ID devrait être positif");

        // Vérifier que l'événement existe dans la base
        List<Evenement> events = evenementService.getData();
        boolean found = events.stream()
                .anyMatch(e -> e.getId() == testEvent.getId() &&
                        e.getTitre().startsWith("TEST_"));

        assertTrue(found, "L'événement de test devrait être trouvé dans la base");
    }

    @Test
    @Order(2)
    void testGetData() throws SQLException {
        // Arrange - Ajouter d'abord un événement
        evenementService.addEntity(testEvent);

        // Act
        List<Evenement> events = evenementService.getData();

        // Assert
        assertNotNull(events, "La liste ne devrait pas être null");
        assertFalse(events.isEmpty(), "La liste ne devrait pas être vide");

        // Vérifier que notre événement de test est présent
        boolean testEventFound = events.stream()
                .anyMatch(e -> e.getTitre().startsWith("TEST_"));
        assertTrue(testEventFound, "L'événement de test devrait être présent");
    }

    @Test
    @Order(3)
    void testUpdateEntity() throws SQLException {
        // Arrange
        evenementService.addEntity(testEvent);
        String nouveauTitre = "TEST_Conference_Update_" + System.currentTimeMillis();
        String nouveauLieu = "TEST_Lyon";
        String nouvelleDescription = "TEST_Nouvelle description";
        String nouvelleImage = "https://test.com/update.jpg";

        // Modifier l'événement
        testEvent.setTitre(nouveauTitre);
        testEvent.setLieu(nouveauLieu);
        testEvent.setDescription(nouvelleDescription);
        testEvent.setImageUrl(nouvelleImage);

        // Act
        evenementService.updateEntity(testEvent);

        // Assert
        List<Evenement> events = evenementService.getData();
        Evenement updated = events.stream()
                .filter(e -> e.getId() == testEvent.getId())
                .findFirst()
                .orElse(null);

        assertNotNull(updated, "L'événement mis à jour devrait exister");
        assertAll("Vérification des champs mis à jour",
                () -> assertEquals(nouveauTitre, updated.getTitre(), "Le titre devrait être mis à jour"),
                () -> assertEquals(nouveauLieu, updated.getLieu(), "Le lieu devrait être mis à jour"),
                () -> assertEquals(nouvelleDescription, updated.getDescription(), "La description devrait être mise à jour"),
                () -> assertEquals(nouvelleImage, updated.getImageUrl(), "L'URL de l'image devrait être mise à jour")
        );
    }

    @Test
    @Order(4)
    void testSearchByTitre() throws SQLException {
        // Arrange
        evenementService.addEntity(testEvent);
        String searchTerm = "TEST_Conference";

        // Act
        List<Evenement> results = evenementService.searchByTitre(searchTerm);

        // Assert
        assertNotNull(results, "Les résultats ne devraient pas être null");
        assertTrue(results.size() > 0, "Au moins un résultat devrait être trouvé");

        boolean found = results.stream()
                .anyMatch(e -> e.getTitre().contains(searchTerm) &&
                        e.getId() == testEvent.getId());
        assertTrue(found, "L'événement de test devrait être trouvé par recherche");
    }

    @Test
    @Order(5)
    void testDeleteEntity() throws SQLException {
        // Arrange
        evenementService.addEntity(testEvent);
        long idToDelete = testEvent.getId();

        // Act
        evenementService.deleteEntity(testEvent);

        // Assert
        List<Evenement> events = evenementService.getData();
        boolean found = events.stream()
                .anyMatch(e -> e.getId() == idToDelete);

        assertFalse(found, "L'événement supprimé ne devrait plus exister");
    }

    @Test
    void testAddEventWithNullFields() {
        // Arrange
        Evenement invalidEvent = new Evenement(
                null, // Titre null
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                "Lieu test",
                "Description",
                "image.jpg",
                1L
        );

        // Act & Assert
        assertThrows(SQLException.class, () -> {
            evenementService.addEntity(invalidEvent);
        }, "Ajouter un événement avec titre null devrait lancer SQLException");
    }

    @Test
    void testAddEventWithPastDates() throws SQLException {
        // Arrange
        Evenement pastEvent = new Evenement(
                "TEST_Event_Passe",
                LocalDateTime.now().minusDays(5), // Date dans le passé
                LocalDateTime.now().minusDays(4),
                "Lieu test",
                "Description",
                "image.jpg",
                1L
        );

        // Act
        evenementService.addEntity(pastEvent);

        // Assert - Vérifier que l'événement a bien été ajouté même avec dates passées
        assertNotEquals(0, pastEvent.getId(), "L'événement devrait être ajouté même avec dates passées");
    }

    @Test
    void testDeleteNonExistentEvent() throws SQLException {
        // Arrange
        Evenement nonExistentEvent = new Evenement();
        nonExistentEvent.setId(99999L); // ID qui n'existe pas

        // Act - Cela ne devrait PAS lancer d'exception
        evenementService.deleteEntity(nonExistentEvent);

        // Assert - Vérifier que rien n'a été supprimé
        List<Evenement> events = evenementService.getData();
        boolean found = events.stream()
                .anyMatch(e -> e.getId() == 99999L);
        assertFalse(found, "L'événement avec ID 99999 ne devrait pas exister");

        // Optionnel : vérifier que le nombre d'événements n'a pas changé de façon suspecte
        System.out.println("✅ La suppression d'un événement inexistant n'a pas causé d'erreur");
    }

    @Test
    void testUpdateNonExistentEvent() throws SQLException {
        // Arrange
        Evenement nonExistentEvent = new Evenement(
                99999L, // ID qui n'existe pas
                "TEST_NonExistent",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                "Lieu",
                "Description",
                "image.jpg",
                1L
        );

        // Act - Cela ne devrait PAS lancer d'exception non plus
        evenementService.updateEntity(nonExistentEvent);

        // Assert - Vérifier que l'événement n'a pas été créé/modifié
        List<Evenement> events = evenementService.getData();
        boolean found = events.stream()
                .anyMatch(e -> e.getId() == 99999L);
        assertFalse(found, "L'événement avec ID 99999 ne devrait pas exister après update");

        System.out.println("✅ La modification d'un événement inexistant n'a pas causé d'erreur");
    }

    @Test
    void testSearchWithEmptyKeyword() throws SQLException {
        // Act
        List<Evenement> results = evenementService.searchByTitre("");

        // Assert
        assertNotNull(results, "La recherche avec mot-clé vide devrait retourner une liste non null");
        // Peut être vide ou contenir des résultats selon votre implémentation
    }

    @Test
    void testAddMultipleEvents() throws SQLException {
        // Arrange
        Evenement event1 = new Evenement(
                "TEST_Event_1_" + System.currentTimeMillis(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Lieu 1",
                "Description 1",
                "image1.jpg",
                1L
        );

        Evenement event2 = new Evenement(
                "TEST_Event_2_" + System.currentTimeMillis(),
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                "Lieu 2",
                "Description 2",
                "image2.jpg",
                1L
        );

        // Act
        evenementService.addEntity(event1);
        evenementService.addEntity(event2);

        // Assert
        assertAll("Vérification des IDs générés",
                () -> assertNotEquals(0, event1.getId()),
                () -> assertNotEquals(0, event2.getId()),
                () -> assertNotEquals(event1.getId(), event2.getId(), "Les IDs devraient être différents")
        );

        // Vérifier le nombre total d'événements de test
        List<Evenement> allEvents = evenementService.getData();
        long testEventsCount = allEvents.stream()
                .filter(e -> e.getTitre().startsWith("TEST_"))
                .count();

        assertTrue(testEventsCount >= 2, "Au moins 2 événements de test devraient exister");
    }

}
