package edu.RhPro.services;

import edu.RhPro.entities.User;
import edu.RhPro.interfaces.IUser;
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class UserService implements IUser {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ================= ADD USER =================
    @Override
    public void addUser(User user) throws SQLException {
        if (emailExists(user.getEmail())) {
            throw new SQLException("Cet email est déjà utilisé.");
        }

        String sql = "INSERT INTO users (nom, prenom, email, mot_de_passe, telephone, adresse, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {

            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, hashPassword(user.getMot_de_passe()));
            pst.setString(5, user.getTelephone());
            pst.setString(6, user.getAdresse());
            pst.setString(7, user.getRole());

            pst.executeUpdate();
        }
    }

    // ================= ADD USER + RETURN ID =================
    @Override
    public int addUserAndReturnId(User user) throws SQLException {

        if (emailExists(user.getEmail())) {
            throw new SQLException("Cet email est déjà utilisé.");
        }

        String sql = "INSERT INTO users (nom, prenom, email, mot_de_passe, telephone, adresse, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());

            // ✅ HASH ICI AUSSI !!!
            ps.setString(4, hashPassword(user.getMot_de_passe()));

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

    // ================= UPDATE =================
    @Override
    public void updateUser(User user) throws SQLException {

        if (user.getId() == 0)
            throw new SQLException("ID utilisateur invalide.");

        String sql = "UPDATE users SET nom=?, prenom=?, email=?, mot_de_passe=?, telephone=?, adresse=?, role=? WHERE id=?";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {

            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, hashPassword(user.getMot_de_passe()));
            pst.setString(5, user.getTelephone());
            pst.setString(6, user.getAdresse());
            pst.setString(7, user.getRole());
            pst.setInt(8, user.getId());

            pst.executeUpdate();
        }
    }

    // ================= DELETE =================
    @Override
    public void removeUser(User user) throws SQLException {
        removeUserById(user.getId());
    }

    public void removeUserById(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    // ================= GET ALL =================
    @Override
    public List<User> getData() throws SQLException {

        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }

    // ================= FIND BY EMAIL =================
    public User findByEmail(String email) throws SQLException {

        String sql = "SELECT * FROM users WHERE email=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    // ================= AUTHENTICATE =================
    public User authenticate(String email, String password) throws SQLException {

        User user = findByEmail(email);
        if (user == null) return null;

        // ✅ Comparaison directe (mot de passe non hashé en base)
        return user.getMot_de_passe().equals(password) ? user : null;
    }



    // ================= HELPERS =================
    private User mapUser(ResultSet rs) throws SQLException {

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

    public boolean emailExists(String email) throws SQLException {

        String query = "SELECT COUNT(*) FROM users WHERE email=?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {

            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    public boolean isMatriculeExist(String matricule) throws SQLException {

        String query = "SELECT COUNT(*) FROM users WHERE matricule=?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {

            pst.setString(1, matricule);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private String hashPassword(String password) {

        try {
            java.security.MessageDigest md =
                    java.security.MessageDigest.getInstance("SHA-256");

            byte[] hashedBytes =
                    md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();

            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Erreur hash password", e);
        }
    }

    public void updateUserWithoutPassword(User user) throws SQLException {

        String sql = "UPDATE users SET nom=?, prenom=?, email=?, telephone=?, adresse=?, role=? WHERE id=?";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {

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

    public User getByNom(String nom) {

        User user = null;
        String sql = "SELECT * FROM user WHERE nom = ?";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {

            pst.setString(1, nom);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                user.setTelephone(String.valueOf(rs.getInt("telephone")));
                user.setAdresse(rs.getString("adresse"));
                user.setRole(rs.getString("role"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user;
    }

    public Image generateUserQRCode(User user) throws Exception {

        String data = "{"
                + "\"nom\":\"" + user.getNom() + "\","
                + "\"prenom\":\"" + user.getPrenom() + "\","
                + "\"email\":\"" + user.getEmail() + "\","
                + "\"telephone\":\"" + user.getTelephone() + "\""
                + "\"adresse\":\"" + user.getAdresse() + "\""
                + "\"role\":\"" + user.getRole() + "\""
                + "}";

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, 300, 300);

        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < 300; x++) {
            for (int y = 0; y < 300; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);

        return new Image(new ByteArrayInputStream(os.toByteArray()));
    }
    // Sauvegarder le chemin de l'avatar
    public void updateAvatar(int userId, String avatarPath) throws SQLException {
        String sql = "UPDATE users SET avatar_path = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, avatarPath);
            pst.setInt(2, userId);
            pst.executeUpdate();
        }
    }

    // Récupérer le chemin de l'avatar
    public String getAvatarPath(int userId) throws SQLException {
        String sql = "SELECT avatar_path FROM users WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getString("avatar_path");
            }
        }
        return null;
    }


}
