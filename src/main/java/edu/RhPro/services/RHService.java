package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RHService {
    private final Connection cnx = MyConnection.getInstance().getCnx();

    public void insertRH(int userId) throws SQLException {
        String sql = "INSERT INTO rh (user_id) VALUES (?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
