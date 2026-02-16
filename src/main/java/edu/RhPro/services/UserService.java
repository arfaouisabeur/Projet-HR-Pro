package edu.RhPro.services;

import edu.RhPro.entities.User;
import edu.RhPro.interfaces.IUser;
import edu.RhPro.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IUser {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addUser(User user) throws SQLException {
        String requete = "INSERT INTO users (nom, prenom, email, mot_de_passe, telephone, adresse, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(requete)) {
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, user.getMot_de_passe());
            pst.setString(5, user.getTelephone());
            pst.setString(6, user.getAdresse());
            pst.setString(7, user.getRole());
            pst.executeUpdate();
        }
    }

    @Override
    public void updateUser(User user) throws SQLException {
        String requete = "UPDATE users SET nom=?, prenom=?, email=?, mot_de_passe=?, telephone=?, adresse=?, role=? WHERE id=?";

        try (PreparedStatement pst = cnx.prepareStatement(requete)) {
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, user.getMot_de_passe());
            pst.setString(5, user.getTelephone());
            pst.setString(6, user.getAdresse());
            pst.setString(7, user.getRole());
            pst.setInt(8, user.getId());
            pst.executeUpdate();
        }
    }

    // ✅ NEW: update without touching password
    public void updateUserWithoutPassword(User user) throws SQLException {
        String requete = "UPDATE users SET nom=?, prenom=?, email=?, telephone=?, adresse=?, role=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(requete)) {
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, user.getTelephone());
            pst.setString(5, user.getAdresse());
            pst.setString(6, user.getRole());
            pst.setInt(7, user.getId());
            pst.executeUpdate();
        }
    }

    @Override
    public void removeUser(User user) throws SQLException {
        String requete = "DELETE FROM users WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(requete);
        pst.setInt(1, user.getId());
        pst.executeUpdate();
    }

    // ✅ NEW: delete by id
    public void removeUserById(int id) throws SQLException {
        String requete = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(requete)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    @Override
    public int addUserAndReturnId(User user) throws SQLException {
        String sql = "INSERT INTO users (nom, prenom, email, mot_de_passe, telephone, adresse, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getMot_de_passe());
            ps.setString(5, user.getTelephone());
            ps.setString(6, user.getAdresse());
            ps.setString(7, user.getRole());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Impossible de récupérer l'ID utilisateur.");
    }

    @Override
    public List<User> getData() throws SQLException {
        List<User> users = new ArrayList<>();

        String requete = "SELECT * FROM users";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(requete);

        while (rs.next()) {
            User u = new User();
            u.setId(rs.getInt("id"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setMot_de_passe(rs.getString("mot_de_passe"));
            u.setTelephone(rs.getString("telephone"));
            u.setAdresse(rs.getString("adresse"));
            u.setRole(rs.getString("role"));

            users.add(u);
        }

        return users;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setNom(rs.getString("nom"));
                    u.setPrenom(rs.getString("prenom"));
                    u.setEmail(rs.getString("email"));
                    u.setMot_de_passe(rs.getString("mot_de_passe"));
                    u.setTelephone(rs.getString("telephone"));
                    u.setAdresse(rs.getString("adresse"));
                    u.setRole(rs.getString("role"));
                    return u;
                }
            }
        }
        return null;
    }

    public User authenticate(String email, String password) throws SQLException {
        User u = findByEmail(email);
        if (u == null) return null;
        return (u.getMot_de_passe() != null && u.getMot_de_passe().equals(password)) ? u : null;
    }

    // ✅ NEW: role update only (optional helper)
    public void updateRoleOnly(int userId, String role) throws SQLException {
        String sql = "UPDATE users SET role=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }
}
