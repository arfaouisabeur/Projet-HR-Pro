package edu.RhPro.entities;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ProjetTest {

    private Projet projet;
    private LocalDate now;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();
        projet = new Projet(
                "Test Projet",
                "Description test",
                "DOING",
                1,
                2,
                now,
                now.plusMonths(1)
        );
    }

    @Test
    void testConstructeurSansId() {
        // Arrange
        String titre = "Application Mobile";
        String description = "Développement app mobile";
        String statut = "DOING";
        int rhId = 5;
        int responsableId = 3;
        LocalDate debut = LocalDate.now();
        LocalDate fin = debut.plusMonths(2);

        // Act
        Projet p = new Projet(titre, description, statut, rhId, responsableId, debut, fin);

        // Assert
        assertAll("Vérification constructeur sans id",
                () -> assertEquals(0, p.getId(), "L'ID devrait être 0 par défaut"),
                () -> assertEquals(titre, p.getTitre()),
                () -> assertEquals(description, p.getDescription()),
                () -> assertEquals(statut, p.getStatut()),
                () -> assertEquals(rhId, p.getRhId()),
                () -> assertEquals(responsableId, p.getResponsableEmployeId()),
                () -> assertEquals(debut, p.getDateDebut()),
                () -> assertEquals(fin, p.getDateFin())
        );
    }

    @Test
    void testConstructeurAvecId() {
        // Arrange
        int id = 10;
        String titre = "Site Web";
        String description = "Création site e-commerce";
        String statut = "DONE";
        int rhId = 2;
        int responsableId = 4;
        LocalDate debut = LocalDate.now().minusMonths(1);
        LocalDate fin = LocalDate.now();

        // Act
        Projet p = new Projet(id, titre, description, statut, rhId, responsableId, debut, fin);

        // Assert
        assertAll("Vérification constructeur avec id",
                () -> assertEquals(id, p.getId()),
                () -> assertEquals(titre, p.getTitre()),
                () -> assertEquals(description, p.getDescription()),
                () -> assertEquals(statut, p.getStatut()),
                () -> assertEquals(rhId, p.getRhId()),
                () -> assertEquals(responsableId, p.getResponsableEmployeId()),
                () -> assertEquals(debut, p.getDateDebut()),
                () -> assertEquals(fin, p.getDateFin())
        );
    }

    @Test
    void testConstructeurVide() {
        // On utilise le constructeur sans paramètres via setters
        Projet p = new Projet("", "", "", 0, 0, null, null);
        p.setId(5);
        p.setTitre("Nouveau");

        assertNotNull(p);
        assertEquals(5, p.getId());
        assertEquals("Nouveau", p.getTitre());
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        int id = 15;
        String titre = "Projet Test";
        String description = "Description mise à jour";
        String statut = "DOING";
        int rhId = 3;
        int responsableId = 6;
        LocalDate debut = LocalDate.now();
        LocalDate fin = debut.plusWeeks(3);

        // Act
        projet.setId(id);
        projet.setTitre(titre);
        projet.setDescription(description);
        projet.setStatut(statut);
        projet.setRhId(rhId);
        projet.setResponsableEmployeId(responsableId);
        projet.setDateDebut(debut);
        projet.setDateFin(fin);

        // Assert
        assertAll("Vérification setters/getters",
                () -> assertEquals(id, projet.getId()),
                () -> assertEquals(titre, projet.getTitre()),
                () -> assertEquals(description, projet.getDescription()),
                () -> assertEquals(statut, projet.getStatut()),
                () -> assertEquals(rhId, projet.getRhId()),
                () -> assertEquals(responsableId, projet.getResponsableEmployeId()),
                () -> assertEquals(debut, projet.getDateDebut()),
                () -> assertEquals(fin, projet.getDateFin())
        );
    }

    @Test
    void testDatesValidation() {
        // Arrange
        LocalDate debut = LocalDate.now();
        LocalDate fin = debut.plusDays(10);

        // Act
        projet.setDateDebut(debut);
        projet.setDateFin(fin);

        // Assert
        assertTrue(projet.getDateFin().isAfter(projet.getDateDebut()),
                "La date de fin devrait être après la date de début");
        assertFalse(projet.getDateFin().isBefore(projet.getDateDebut()),
                "La date de fin ne devrait pas être avant la date de début");
    }

    @Test
    void testStatutValide() {
        // Arrange
        String[] statutsValides = {"DOING", "DONE"};

        for (String statut : statutsValides) {
            // Act
            projet.setStatut(statut);

            // Assert
            assertEquals(statut, projet.getStatut());
        }
    }

    @Test
    void testChampsNull() {
        // Act
        projet.setTitre(null);
        projet.setDescription(null);
        projet.setStatut(null);
        projet.setDateDebut(null);
        projet.setDateFin(null);

        // Assert
        assertAll("Vérification champs null",
                () -> assertNull(projet.getTitre()),
                () -> assertNull(projet.getDescription()),
                () -> assertNull(projet.getStatut()),
                () -> assertNull(projet.getDateDebut()),
                () -> assertNull(projet.getDateFin())
        );
    }

    @Test
    void testToString() {
        // Arrange
        projet.setId(1);
        projet.setTitre("Test Projet");
        projet.setStatut("DOING");
        projet.setDateDebut(now);
        projet.setDateFin(now.plusMonths(1));

        // Act
        String result = projet.toString();

        // Assert
        assertAll("Vérification toString",
                () -> assertTrue(result.contains("id=1")),
                () -> assertTrue(result.contains("titre='Test Projet'")),
                () -> assertTrue(result.contains("statut='DOING'")),
                () -> assertTrue(result.contains("dateDebut=" + now)),
                () -> assertTrue(result.contains("dateFin=" + now.plusMonths(1)))
        );
    }
}