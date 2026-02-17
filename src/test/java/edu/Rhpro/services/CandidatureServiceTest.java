package edu.RhPro.services;

import edu.RhPro.entities.Candidature;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CandidatureServiceTest {

    private Connection cnx;
    private CandidatureService service;

    @BeforeEach
    void setUp() throws Exception {
        // DB H2 en mémoire (se détruit à la fin du test)
        cnx = DriverManager.getConnection("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        service = new CandidatureService(cnx);

        // Création table candidature
        try (Statement st = cnx.createStatement()) {
            st.execute("DROP TABLE IF EXISTS candidature");
            st.execute("""
                CREATE TABLE candidature (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    date_candidature DATE NOT NULL,
                    statut VARCHAR(50) NOT NULL,
                    cv TEXT NULL,
                    candidat_id BIGINT NOT NULL,
                    offre_emploi_id BIGINT NOT NULL
                )
            """);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (cnx != null) cnx.close();
    }

    // ----------- Helpers -----------
    private int countRows() throws SQLException {
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM candidature")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int lastInsertedId() throws SQLException {
        // Compatible H2 : récupère le dernier ID auto-incrémenté
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(id) FROM candidature")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ----------- Tests VALIDATION (IllegalArgumentException) -----------

    @Test
    void add_refuseSiDateNull() {
        Candidature c = new Candidature(null, "EN_ATTENTE", null, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> service.add(c));
    }

    @Test
    void add_refuseSiStatutNull() {
        Candidature c = new Candidature(LocalDate.now(), null, null, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> service.add(c));
    }

    @Test
    void add_refuseSiCandidatIdInvalide() {
        Candidature c = new Candidature(LocalDate.now(), "EN_ATTENTE", null, 0, 1);
        assertThrows(IllegalArgumentException.class, () -> service.add(c));
    }

    @Test
    void add_refuseSiOffreIdInvalide() {
        Candidature c = new Candidature(LocalDate.now(), "EN_ATTENTE", null, 1, 0);
        assertThrows(IllegalArgumentException.class, () -> service.add(c));
    }

    @Test
    void update_refuseSiIdInvalide() {
        Candidature c = new Candidature(0, LocalDate.now(), "OK", null, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> service.update(c));
    }

    // ----------- Tests CRUD -----------

    @Test
    void add_insereUneCandidature() throws Exception {
        Candidature c = new Candidature(
                LocalDate.of(2025, 2, 1),
                "EN_ATTENTE",
                "cv.pdf",
                10,
                5
        );

        service.add(c);

        assertEquals(1, countRows());

        int id = lastInsertedId();
        Candidature fromDb = service.findById(id);

        assertNotNull(fromDb);
        assertEquals(LocalDate.of(2025, 2, 1), fromDb.getDateCandidature());
        assertEquals("EN_ATTENTE", fromDb.getStatut());
        assertEquals("cv.pdf", fromDb.getCv());
        assertEquals(10, fromDb.getCandidatId());
        assertEquals(5, fromDb.getOffreEmploiId());
    }

    @Test
    void add_cvNull_estEnregistreNull() throws Exception {
        Candidature c = new Candidature(LocalDate.now(), "EN_ATTENTE", null, 2, 3);

        service.add(c);

        int id = lastInsertedId();
        Candidature fromDb = service.findById(id);

        assertNotNull(fromDb);
        assertNull(fromDb.getCv()); // doit rester null
    }

    @Test
    void findAll_retourneToutesLesCandidatures() throws Exception {
        service.add(new Candidature(LocalDate.now(), "EN_ATTENTE", null, 1, 1));
        service.add(new Candidature(LocalDate.now().minusDays(1), "ACCEPTEE", "cv2", 2, 2));

        List<Candidature> all = service.findAll();

        assertEquals(2, all.size());
    }

    @Test
    void update_modifieLesChamps() throws Exception {
        // insert
        service.add(new Candidature(LocalDate.of(2025, 1, 1), "EN_ATTENTE", null, 10, 20));
        int id = lastInsertedId();

        // update
        Candidature updated = new Candidature(
                id,
                LocalDate.of(2025, 1, 5),
                "ACCEPTEE",
                "newcv.pdf",
                11,
                21
        );

        service.update(updated);

        Candidature fromDb = service.findById(id);
        assertNotNull(fromDb);

        assertEquals(LocalDate.of(2025, 1, 5), fromDb.getDateCandidature());
        assertEquals("ACCEPTEE", fromDb.getStatut());
        assertEquals("newcv.pdf", fromDb.getCv());
        assertEquals(11, fromDb.getCandidatId());
        assertEquals(21, fromDb.getOffreEmploiId());
    }

    @Test
    void delete_supprimeLaCandidature() throws Exception {
        service.add(new Candidature(LocalDate.now(), "EN_ATTENTE", null, 1, 1));
        int id = lastInsertedId();

        assertEquals(1, countRows());

        service.delete(id);

        assertEquals(0, countRows());
        assertNull(service.findById(id));
    }

    @Test
    void findByCandidatId_retourneSeulementCeCandidat() throws Exception {
        service.add(new Candidature(LocalDate.now(), "EN_ATTENTE", null, 100, 1));
        service.add(new Candidature(LocalDate.now().minusDays(2), "ACCEPTEE", null, 100, 2));
        service.add(new Candidature(LocalDate.now(), "REFUSEE", null, 200, 3));

        List<Candidature> list = service.findByCandidatId(100);

        assertEquals(2, list.size());
        assertTrue(list.stream().allMatch(c -> c.getCandidatId() == 100));
    }

    @Test
    void updateStatus_changeSeulementLeStatut() throws Exception {
        service.add(new Candidature(LocalDate.of(2025, 1, 1), "EN_ATTENTE", "cv.pdf", 10, 20));
        int id = lastInsertedId();

        service.updateStatus(id, "REFUSEE");

        Candidature fromDb = service.findById(id);
        assertNotNull(fromDb);

        assertEquals("REFUSEE", fromDb.getStatut());
        // autres champs inchangés
        assertEquals(LocalDate.of(2025, 1, 1), fromDb.getDateCandidature());
        assertEquals("cv.pdf", fromDb.getCv());
        assertEquals(10, fromDb.getCandidatId());
        assertEquals(20, fromDb.getOffreEmploiId());
    }
}
