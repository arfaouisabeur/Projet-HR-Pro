package edu.RhPro.services;

import edu.RhPro.entities.Conge;
import edu.RhPro.tools.MyConnection;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CongeServiceTest {

    static CongeService service;
    static long idCongeTest = -1;
    static long validEmployeId = -1;
    static String marker;

    @BeforeAll
    static void setup() throws SQLException {

        service = new CongeService();

        validEmployeId = fetchAnyEmployeId();
        assumeTrue(validEmployeId > 0,
                "No employe found in DB. Insert one employe first.");

        marker = "TEST_JUNIT_CONGE_" + UUID.randomUUID();
    }

    private static long fetchAnyEmployeId() throws SQLException {
        String sql = "SELECT user_id FROM employe LIMIT 1";
        Connection cnx = MyConnection.getInstance().getCnx(); // DO NOT close

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return -1;
    }

    @Test
    @Order(1)
    void testAjouterConge() throws SQLException {

        Conge c = new Conge();
        c.setTypeConge("Congé annuel");
        c.setDateDebut(LocalDate.now());
        c.setDateFin(LocalDate.now().plusDays(5));
        c.setStatut("EN_ATTENTE");
        c.setDescription(marker);
        c.setEmployeeId(validEmployeId);

        service.addEntity(c);

        List<Conge> conges = service.getData();

        Conge inserted = conges.stream()
                .filter(x -> marker.equals(x.getDescription()))
                .max(Comparator.comparingLong(Conge::getId))
                .orElse(null);

        assertNotNull(inserted, "Inserted conge not found");
        idCongeTest = inserted.getId();
        assertTrue(idCongeTest > 0);
    }

    @Test
    @Order(2)
    void testModifierConge() throws SQLException {

        assumeTrue(idCongeTest > 0,
                "Skipping because add failed");

        Conge c = new Conge();
        c.setId(idCongeTest);
        c.setTypeConge("Congé maladie");
        c.setDateDebut(LocalDate.now());
        c.setDateFin(LocalDate.now().plusDays(3));
        c.setStatut("EN_ATTENTE");
        c.setDescription(marker + "_UPDATED");
        c.setEmployeeId(validEmployeId);

        service.updateEntity(c);

        List<Conge> conges = service.getData();

        assertTrue(conges.stream().anyMatch(x ->
                x.getId() == idCongeTest &&
                        (marker + "_UPDATED").equals(x.getDescription()) &&
                        "Congé maladie".equals(x.getTypeConge())
        ));
    }



    @Test
    @Order(3)
    void testSupprimerConge() throws SQLException {

        assumeTrue(idCongeTest > 0,
                "Skipping because add failed");

        Conge c = new Conge();
        c.setId(idCongeTest);

        service.deleteEntity(c);

        List<Conge> conges = service.getData();

        assertFalse(conges.stream()
                .anyMatch(x -> x.getId() == idCongeTest));
    }

    @AfterAll
    static void cleanup() throws SQLException {

        if (service == null) return;

        // Supprimer les restes de tests
        List<Conge> conges = service.getData();
        for (Conge c : conges) {
            if (c.getDescription() != null &&
                    c.getDescription().startsWith("TEST_JUNIT_CONGE_")) {
                try { service.deleteEntity(c); }
                catch (SQLException ignored) {}
            }
        }
    }
}
