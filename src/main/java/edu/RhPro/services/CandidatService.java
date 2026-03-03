package edu.RhPro.services;

import edu.RhPro.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CandidatService {
    private final Connection cnx = MyConnection.getInstance().getCnx();

    public void insertCandidat(int userId, String niveauEtude, int experience) throws SQLException {
        String sql = "INSERT INTO candidat (user_id, niveau_etude, experience) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, niveauEtude);
            ps.setInt(3, experience);
            ps.executeUpdate();
        }
    }
}
