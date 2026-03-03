package edu.RhPro.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import edu.RhPro.entities.Employe;
import edu.RhPro.entities.User;
import edu.RhPro.tools.MyConnection;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import edu.RhPro.entities.Employe;
import edu.RhPro.services.EmployeService;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.time.LocalDate;

public class EmployeService {
    private final Connection cnx = MyConnection.getInstance().getCnx();

    public void insertEmploye(int userId, String matricule, String position, LocalDate dateEmbauche) throws SQLException {
        String sql = "INSERT INTO employe (user_id, matricule, position, date_embauche) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, matricule);
            ps.setString(3, position);
            ps.setDate(4, dateEmbauche != null ? Date.valueOf(dateEmbauche) : null);
            ps.executeUpdate();
        }
    }
    public boolean matriculeExists(String matricule) throws Exception {

        String query = "SELECT COUNT(*) FROM employe WHERE matricule = ?";

        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, matricule);

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            return rs.getInt(1) > 0;
        }

        return false;
    }

    public Image generateEmployeQRCode(Employe employe) throws Exception {

        String data = "{"
                + "\"nom\":\"" + employe.getNom() + "\","
                + "\"prenom\":\"" + employe.getPrenom() + "\","
                + "\"email\":\"" + employe.getEmail() + "\","
                + "\"telephone\":\"" + employe.getTelephone() + "\","
                + "\"adresse\":\"" + employe.getAdresse() + "\","
                + "\"matricule\":\"" + employe.getMatricule() + "\","
                + "\"position\":\"" + employe.getPosition() + "\","
                + "\"date_embauche\":\"" + employe.getDateEmbauche() + "\""
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
    public Employe getEmployeByUserId(int userId) {
        Employe employe = null;
        String sql = "SELECT * FROM users WHERE user_id = ?"; // <- user_id comme dans ta table
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                employe = new Employe();
                employe.setUserId(rs.getInt("user_id"));
                employe.setMatricule(rs.getString("matricule"));
                employe.setPosition(rs.getString("position"));

                java.sql.Date sqlDate = rs.getDate("date_embauche");
                if (sqlDate != null) {
                    employe.setDateEmbauche(sqlDate.toLocalDate());
                }
            }

        } catch (SQLException ex) {
            System.out.println("Erreur getEmployeByUserId : " + ex.getMessage());
        }
        return employe;
    }


}



