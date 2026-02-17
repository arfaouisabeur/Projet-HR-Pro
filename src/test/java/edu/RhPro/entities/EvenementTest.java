package edu.RhPro.entities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
class EvenementTest {

    private Evenement evenement;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        evenement = new Evenement();
    }

    @Test
    void testConstructeurVide() {
        assertNotNull(evenement);
        assertEquals(0, evenement.getId());
        assertNull(evenement.getTitre());
        assertNull(evenement.getDateDebut());
        assertNull(evenement.getDateFin());
        assertNull(evenement.getLieu());
        assertNull(evenement.getDescription());
        assertNull(evenement.getImageUrl());
        assertEquals(0, evenement.getRhId());
    }

    @Test
    void testConstructeurSansId() {
        // Arrange
        String titre = "Conférence Java";
        LocalDateTime debut = now;
        LocalDateTime fin = now.plusHours(2);
        String lieu = "Salle A101";
        String description = "Formation Java avancée";
        String imageUrl = "https://example.com/java.jpg";
        long rhId = 5L;

        // Act
        Evenement event = new Evenement(titre, debut, fin, lieu, description, imageUrl, rhId);

        // Assert
        assertAll("Vérification du constructeur sans id",
                () -> assertEquals(0, event.getId(), "L'ID devrait être 0 par défaut"),
                () -> assertEquals(titre, event.getTitre()),
                () -> assertEquals(debut, event.getDateDebut()),
                () -> assertEquals(fin, event.getDateFin()),
                () -> assertEquals(lieu, event.getLieu()),
                () -> assertEquals(description, event.getDescription()),
                () -> assertEquals(imageUrl, event.getImageUrl()),
                () -> assertEquals(rhId, event.getRhId())
        );
    }

    @Test
    void testConstructeurAvecId() {
        // Arrange
        long id = 10L;
        String titre = "Conférence Spring";
        LocalDateTime debut = now.plusDays(1);
        LocalDateTime fin = now.plusDays(1).plusHours(3);
        String lieu = "Salle B202";
        String description = "Formation Spring Boot";
        String imageUrl = "https://example.com/spring.jpg";
        long rhId = 3L;

        // Act
        Evenement event = new Evenement(id, titre, debut, fin, lieu, description, imageUrl, rhId);

        // Assert
        assertAll("Vérification du constructeur avec id",
                () -> assertEquals(id, event.getId()),
                () -> assertEquals(titre, event.getTitre()),
                () -> assertEquals(debut, event.getDateDebut()),
                () -> assertEquals(fin, event.getDateFin()),
                () -> assertEquals(lieu, event.getLieu()),
                () -> assertEquals(description, event.getDescription()),
                () -> assertEquals(imageUrl, event.getImageUrl()),
                () -> assertEquals(rhId, event.getRhId())
        );
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        long id = 15L;
        String titre = "Nouveau titre";
        LocalDateTime debut = now;
        LocalDateTime fin = now.plusHours(4);
        String lieu = "Paris";
        String description = "Nouvelle description";
        String imageUrl = "nouvelle-url.jpg";
        long rhId = 7L;

        // Act
        evenement.setId(id);
        evenement.setTitre(titre);
        evenement.setDateDebut(debut);
        evenement.setDateFin(fin);
        evenement.setLieu(lieu);
        evenement.setDescription(description);
        evenement.setImageUrl(imageUrl);
        evenement.setRhId(rhId);

        // Assert
        assertAll("Vérification des setters/getters",
                () -> assertEquals(id, evenement.getId()),
                () -> assertEquals(titre, evenement.getTitre()),
                () -> assertEquals(debut, evenement.getDateDebut()),
                () -> assertEquals(fin, evenement.getDateFin()),
                () -> assertEquals(lieu, evenement.getLieu()),
                () -> assertEquals(description, evenement.getDescription()),
                () -> assertEquals(imageUrl, evenement.getImageUrl()),
                () -> assertEquals(rhId, evenement.getRhId())
        );
    }

    @Test
    void testDatesValidation() {
        // Arrange
        LocalDateTime debut = LocalDateTime.now();
        LocalDateTime fin = debut.plusHours(2);

        // Act
        evenement.setDateDebut(debut);
        evenement.setDateFin(fin);

        // Assert
        assertTrue(evenement.getDateFin().isAfter(evenement.getDateDebut()),
                "La date de fin devrait être après la date de début");
        assertFalse(evenement.getDateFin().isBefore(evenement.getDateDebut()),
                "La date de fin ne devrait pas être avant la date de début");
    }

    @Test
    void testChampsNull() {
        // Act
        evenement.setTitre(null);
        evenement.setLieu(null);
        evenement.setDescription(null);
        evenement.setImageUrl(null);

        // Assert
        assertAll("Vérification des champs null",
                () -> assertNull(evenement.getTitre()),
                () -> assertNull(evenement.getLieu()),
                () -> assertNull(evenement.getDescription()),
                () -> assertNull(evenement.getImageUrl())
        );
    }

    @Test
    void testToString() {
        // Arrange
        evenement.setId(1L);
        evenement.setTitre("Test Événement");
        evenement.setDateDebut(now);
        evenement.setDateFin(now.plusHours(1));
        evenement.setLieu("Tunis");
        evenement.setImageUrl("test.jpg");

        // Act
        String result = evenement.toString();

        // Assert
        assertAll("Vérification du toString",
                () -> assertTrue(result.contains("id=1")),
                () -> assertTrue(result.contains("titre='Test Événement'")),
                () -> assertTrue(result.contains("lieu='Tunis'")),
                () -> assertTrue(result.contains("imageUrl='test.jpg'"))
        );
    }
}
