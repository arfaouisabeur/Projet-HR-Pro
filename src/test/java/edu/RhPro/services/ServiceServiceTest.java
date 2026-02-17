package edu.RhPro.services;

import edu.RhPro.entities.Service;
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
public class ServiceServiceTest {

    static ServiceService service;
    static long idServiceTest = -1;
    static long validEmployeId = -1;
    static String marker;

    @BeforeAll
    static void setup() throws SQLException {

        service = new ServiceService();

        validEmployeId = fetchAnyEmployeId();
        assumeTrue(validEmployeId > 0,
                "No employe found in DB. Insert one employe first.");

        marker = "TEST_JUNIT_SERVICE_" + UUID.randomUUID();
    }

    private static long fetchAnyEmployeId() throws SQLException {
        String sql = "SELECT user_id FROM employe LIMIT 1";
        Connection cnx = MyConnection.getInstance().getCnx();

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return -1;
    }

    // ✅ 1 - ADD
    @Test
    @Order(1)
    void testAjouterService() throws SQLException {

        Service s = new Service();
        s.setTitre("Demande matériel");
        s.setDescription(marker);
        s.setDateDemande(LocalDate.now());
        s.setStatut("EN_ATTENTE");
        s.setEmployeeId(validEmployeId);

        service.addEntity(s);

        List<Service> services = service.getData();

        Service inserted = services.stream()
                .filter(x -> marker.equals(x.getDescription()))
                .max(Comparator.comparingLong(Service::getId))
                .orElse(null);

        assertNotNull(inserted, "Inserted service not found");
        idServiceTest = inserted.getId();
        assertTrue(idServiceTest > 0);
    }

    // ✅ 2 - UPDATE
    @Test
    @Order(2)
    void testModifierService() throws SQLException {

        assumeTrue(idServiceTest > 0,
                "Skipping because add failed");

        Service s = new Service();
        s.setId(idServiceTest);
        s.setTitre("Demande matériel urgent");
        s.setDescription(marker + "_UPDATED");
        s.setDateDemande(LocalDate.now());
        s.setStatut("EN_ATTENTE");
        s.setEmployeeId(validEmployeId);

        service.updateEntity(s);

        List<Service> services = service.getData();

        assertTrue(services.stream().anyMatch(x ->
                x.getId() == idServiceTest &&
                        (marker + "_UPDATED").equals(x.getDescription()) &&
                        "Demande matériel urgent".equals(x.getTitre())
        ));
    }


    // ✅ 4 - DELETE
    @Test
    @Order(3)
    void testSupprimerService() throws SQLException {

        assumeTrue(idServiceTest > 0,
                "Skipping because add failed");

        Service s = new Service();
        s.setId(idServiceTest);

        service.deleteEntity(s);

        List<Service> services = service.getData();

        assertFalse(services.stream()
                .anyMatch(x -> x.getId() == idServiceTest));
    }

    // ✅ CLEANUP
    @AfterAll
    static void cleanup() throws SQLException {

        if (service == null) return;

        List<Service> services = service.getData();

        for (Service s : services) {
            if (s.getDescription() != null &&
                    s.getDescription().startsWith("TEST_JUNIT_SERVICE_")) {
                try { service.deleteEntity(s); }
                catch (SQLException ignored) {}
            }
        }
    }
}
