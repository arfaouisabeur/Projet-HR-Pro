package edu.RhPro.services;

import edu.RhPro.entities.Salaire;
import edu.RhPro.tools.MyConnection;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SalaireServiceTest {

    static SalaireService service;

    static long idSalaireTest = -1;
    static long validRhId = -1;
    static long validEmployeId = -1;

    // Use future year to avoid touching real salaries
    static final int TEST_MOIS = 2;
    static final int TEST_ANNEE = 2099;

    @BeforeAll
    static void setup() throws SQLException {
        service = new SalaireService();

        validRhId = fetchAnyIdNoClose("rh", "user_id");
        validEmployeId = fetchAnyIdNoClose("employe", "user_id");

        assumeTrue(validRhId > 0, "No RH in DB (table rh). Insert one RH first.");
        assumeTrue(validEmployeId > 0, "No Employe in DB (table employe). Insert one employe first.");
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

    private static Salaire findInsertedRow(List<Salaire> salaires) {
        return salaires.stream()
                .filter(s -> s.getEmployeId() == validEmployeId
                        && s.getMois() == TEST_MOIS
                        && s.getAnnee() == TEST_ANNEE)
                .max(Comparator.comparingLong(Salaire::getId))
                .orElse(null);
    }

    @Test
    @Order(1)
    void testAjouterSalaire() throws SQLException {
        Salaire s = new Salaire();
        s.setMois(TEST_MOIS);
        s.setAnnee(TEST_ANNEE);
        s.setMontant(new BigDecimal("2500.00"));
        s.setDatePaiement(LocalDate.now());
        s.setStatut("EN_ATTENTE");
        s.setRhId(validRhId);
        s.setEmployeId(validEmployeId);

        service.addEntity(s);

        List<Salaire> salaires = service.getData();
        Salaire inserted = findInsertedRow(salaires);

        assertNotNull(inserted, "Inserted salaire not found");
        idSalaireTest = inserted.getId();
        assertTrue(idSalaireTest > 0);

        assertEquals("EN_ATTENTE", inserted.getStatut());
        assertEquals(0, new BigDecimal("2500.00").compareTo(inserted.getMontant()));
    }

    @Test
    @Order(2)
    void testModifierSalaire() throws SQLException {
        assumeTrue(idSalaireTest > 0, "Skipping because add failed");

        Salaire s = new Salaire();
        s.setId(idSalaireTest);
        s.setMois(TEST_MOIS);
        s.setAnnee(TEST_ANNEE);
        s.setMontant(new BigDecimal("2700.00"));
        s.setDatePaiement(LocalDate.now());
        s.setStatut("PAYE");
        s.setRhId(validRhId);
        s.setEmployeId(validEmployeId);

        service.updateEntity(s);

        List<Salaire> salaires = service.getData();
        boolean ok = salaires.stream().anyMatch(x ->
                x.getId() == idSalaireTest &&
                        "PAYE".equals(x.getStatut()) &&
                        new BigDecimal("2700.00").compareTo(x.getMontant()) == 0
        );

        assertTrue(ok, "Update did not apply");
    }

    @Test
    @Order(3)
    void testSupprimerSalaire() throws SQLException {
        assumeTrue(idSalaireTest > 0, "Skipping because add failed");

        Salaire s = new Salaire();
        s.setId(idSalaireTest);
        service.deleteEntity(s);

        List<Salaire> salaires = service.getData();
        assertFalse(salaires.stream().anyMatch(x -> x.getId() == idSalaireTest));
    }

    @AfterAll
    static void cleanup() throws SQLException {
        if (service == null) return;

        // Safety: delete any leftover rows we created (emp + mois + annee)
        List<Salaire> salaires = service.getData();
        for (Salaire sa : salaires) {
            if (sa.getEmployeId() == validEmployeId
                    && sa.getMois() == TEST_MOIS
                    && sa.getAnnee() == TEST_ANNEE) {
                try { service.deleteEntity(sa); } catch (SQLException ignored) {}
            }
        }
    }
}
