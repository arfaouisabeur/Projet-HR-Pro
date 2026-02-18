package tn.User.services;

import edu.RhPro.entities.User;
import edu.RhPro.services.UserService;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private static Connection connection;
    private UserService userService;

    @BeforeAll
    static void setupDatabase() throws Exception {

        Class.forName("org.h2.Driver");

        connection = DriverManager.getConnection(
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nom VARCHAR(50),
                    prenom VARCHAR(50),
                    email VARCHAR(100) UNIQUE,
                    mot_de_passe VARCHAR(255),
                    telephone VARCHAR(20),
                    adresse VARCHAR(255),
                    role VARCHAR(50)
                )
            """);
        }
    }

    @BeforeEach
    void init() throws Exception {
        userService = new UserService(connection); // ici connection = H2 in-memory
        try (Statement st = connection.createStatement()) {
            st.execute("DELETE FROM users"); // nettoie la table avant chaque test
        }
    }


    private User createUser() {
        User u = new User();
        u.setNom("Ali");
        u.setPrenom("Ahmed");
        u.setEmail("ali@test.com");
        u.setMot_de_passe("123456");
        u.setTelephone("12345678");
        u.setAdresse("Tunis");
        u.setRole("ADMIN");
        return u;
    }

    // ================= TESTS =================

    @Test
    void testAddUser() throws Exception {
        userService.addUser(createUser());

        User found = userService.findByEmail("ali@test.com");
        assertNotNull(found);
    }

    @Test
    void testAddUser_EmailAlreadyExists() throws Exception {
        userService.addUser(createUser());

        assertThrows(SQLException.class, () ->
                userService.addUser(createUser()));
    }

    @Test
    void testAuthenticate() throws Exception {
        userService.addUser(createUser());

        User auth = userService.authenticate("ali@test.com", "123456");
        assertNotNull(auth);
    }

    @Test
    void testRemoveUser() throws Exception {
        userService.addUser(createUser());
        User saved = userService.findByEmail("ali@test.com");

        userService.removeUserById(saved.getId());

        assertNull(userService.findByEmail("ali@test.com"));
    }

    @Test
    void testGetAll() throws Exception {
        userService.addUser(createUser());

        List<User> list = userService.getData();
        assertFalse(list.isEmpty());
    }

    @AfterAll
    static void closeDB() throws Exception {
        connection.close();
    }
}
