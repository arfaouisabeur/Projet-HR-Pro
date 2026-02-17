package edu.RhPro.services;

import edu.RhPro.entities.offreEmploi;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class OffreEmploiServiceTest {

    @Test
    void testCreateOffre() {

        offreEmploi offre = new offreEmploi(
                1,
                "Développeur Java",
                "Tunis",
                "CDI",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "OUVERTE",
                "Spring Boot"
        );

        assertNotNull(offre);
        assertEquals("Développeur Java", offre.getTitre());
        assertEquals("Tunis", offre.getLocalisation());
        assertEquals("CDI", offre.getTypeContrat());
    }

    @Test
    void testSettersAndGetters() {

        offreEmploi offre = new offreEmploi();

        offre.setId(3);
        offre.setTitre("Backend");
        offre.setLocalisation("Sfax");
        offre.setTypeContrat("Stage");
        offre.setStatut("FERMEE");
        offre.setDescription("API REST");
        offre.setRhId(99);

        assertEquals(3, offre.getId());
        assertEquals("Backend", offre.getTitre());
        assertEquals("Sfax", offre.getLocalisation());
        assertEquals("Stage", offre.getTypeContrat());
        assertEquals("FERMEE", offre.getStatut());
        assertEquals("API REST", offre.getDescription());
        assertEquals(99, offre.getRhId());
    }

    @Test
    void testDates() {

        offreEmploi offre = new offreEmploi();

        LocalDate pub = LocalDate.of(2025,1,1);
        LocalDate exp = LocalDate.of(2025,2,1);

        offre.setDatePublication(pub);
        offre.setDateExpiration(exp);

        assertEquals(pub, offre.getDatePublication());
        assertEquals(exp, offre.getDateExpiration());
    }
}
