package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
}
