package edu.RhPro.services;

import edu.RhPro.entities.Prime;
import edu.RhPro.tools.MyConnection;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PrimeServiceTest {

    static PrimeService service;
    static long idPrimeTest = -1;
    static long validRhId = -1;
    static long validEmployeId = -1;
    static String marker;

    @BeforeAll
    static void setup() throws SQLException {
        service = new PrimeService();

        validRhId = fetchAnyIdNoClose("rh", "user_id");
        validEmployeId = fetchAnyIdNoClose("employe", "user_id");

        assumeTrue(validRhId > 0, "No RH in DB (table rh). Insert one RH first.");
        assumeTrue(validEmployeId > 0, "No Employe in DB (table employe). Insert one employe first.");

        marker = "TEST_JUNIT_PRIME_" + UUID.randomUUID();
    }

    private static long fetchAnyIdNoClose(String table, String col) throws SQLException {
        String sql = "SELECT " + col + " FROM " + table + " LIMIT 1";
        Connection cnx = MyConnection.getInstance().getCnx(); // DO NOT close
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return -1;
    }

    @Test
    @Order(1)
    void testAjouterPrime() throws SQLException {
        Prime p = new Prime();
        p.setMontant(new BigDecimal("150.00"));
        p.setDateAttribution(LocalDate.now());
        p.setDescription(marker);
        p.setRhId(validRhId);
        p.setEmployeId(validEmployeId);

        service.addEntity(p);

        List<Prime> primes = service.getData();
        Prime inserted = primes.stream()
                .filter(x -> marker.equals(x.getDescription()))
                .max(Comparator.comparingLong(Prime::getId))
                .orElse(null);

        assertNotNull(inserted, "Inserted prime not found");
        idPrimeTest = inserted.getId();
        assertTrue(idPrimeTest > 0);
    }

    @Test
    @Order(2)
    void testModifierPrime() throws SQLException {
        assumeTrue(idPrimeTest > 0, "Skipping because add failed");

        Prime p = new Prime();
        p.setId(idPrimeTest);
        p.setMontant(new BigDecimal("200.00"));
        p.setDateAttribution(LocalDate.now());
        p.setDescription(marker + "_UPDATED");
        p.setRhId(validRhId);
        p.setEmployeId(validEmployeId);

        service.updateEntity(p);

        List<Prime> primes = service.getData();
        assertTrue(primes.stream().anyMatch(x ->
                x.getId() == idPrimeTest &&
                        (marker + "_UPDATED").equals(x.getDescription()) &&
                        new BigDecimal("200.00").compareTo(x.getMontant()) == 0
        ));
    }

    @Test
    @Order(3)
    void testSupprimerPrime() throws SQLException {
        assumeTrue(idPrimeTest > 0, "Skipping because add failed");

        Prime p = new Prime();
        p.setId(idPrimeTest);
        service.deleteEntity(p);

        List<Prime> primes = service.getData();
        assertFalse(primes.stream().anyMatch(x -> x.getId() == idPrimeTest));
    }

    @AfterAll
    static void cleanup() throws SQLException {
        if (service == null) return; // in case setup crashed

        // delete any leftover test rows
        List<Prime> primes = service.getData();
        for (Prime pr : primes) {
            if (pr.getDescription() != null && pr.getDescription().startsWith("TEST_JUNIT_PRIME_")) {
                try { service.deleteEntity(pr); } catch (SQLException ignored) {}
            }
        }
    }
}
