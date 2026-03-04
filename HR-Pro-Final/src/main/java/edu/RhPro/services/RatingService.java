package edu.RhPro.services;

import edu.RhPro.entities.Rating;
import edu.RhPro.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class RatingService {
    Connection connection = MyConnection.getInstance().getCnx();


    // Sentiment Analysis - Automatically determine stars based on comment
    private int analyzeSentiment(String commentaire) {
        if (commentaire == null || commentaire.trim().isEmpty()) {
            return 3; // Default to 3 stars if no comment
        }

        String lowerComment = commentaire.toLowerCase();

        // Keywords for different sentiments
        String[] excellentKeywords = {"excellent", "super", "fantastique", "génial", "parfait",
                "incroyable", "merveilleux", "exceptionnel", "top", "👌"};
        String[] goodKeywords = {"bien", "bon", "satisfait", "content", "agréable", "sympa",
                "correct", "pas mal", "👍"};
        String[] averageKeywords = {"moyen", "acceptable", "passable", "ordinaire", "bof",
                "ni bien ni mal", "ok"};
        String[] poorKeywords = {"mauvais", "déçu", "insatisfait", "pas content", "décevant",
                "désagréable", "pas bien", "à améliorer"};
        String[] terribleKeywords = {"terrible", "horrible", "catastrophe", "très mauvais",
                "pire", "inacceptable", "désastre", "honteux"};

        int score = 0;

        // Check for excellent keywords
        for (String keyword : excellentKeywords) {
            if (lowerComment.contains(keyword)) {
                score += 5;
            }
        }

        // Check for good keywords
        for (String keyword : goodKeywords) {
            if (lowerComment.contains(keyword)) {
                score += 4;
            }
        }

        // Check for average keywords
        for (String keyword : averageKeywords) {
            if (lowerComment.contains(keyword)) {
                score += 3;
            }
        }

        // Check for poor keywords
        for (String keyword : poorKeywords) {
            if (lowerComment.contains(keyword)) {
                score += 2;
            }
        }

        // Check for terrible keywords
        for (String keyword : terribleKeywords) {
            if (lowerComment.contains(keyword)) {
                score += 1;
            }
        }

        // Check for negation words that might reverse sentiment
        String[] negationWords = {"pas", "ne", "non", "manque", "sans"};
        boolean hasNegation = false;
        for (String negation : negationWords) {
            if (lowerComment.contains(negation)) {
                hasNegation = true;
                break;
            }
        }

        // If we have no keywords, check length and general positivity
        if (score == 0) {
            // Check for positive emojis
            if (lowerComment.contains("😊") || lowerComment.contains("😄") ||
                    lowerComment.contains("🥳") || lowerComment.contains("🎉")) {
                return 4;
            }

            // Check for negative emojis
            if (lowerComment.contains("😞") || lowerComment.contains("😠") ||
                    lowerComment.contains("😤") || lowerComment.contains("👎")) {
                return 2;
            }

            // Default based on comment length? Not reliable, so return 3
            return 3;
        }

        // Calculate average if multiple keywords found
        int keywordCount = 0;
        if (score > 0) {
            keywordCount = (int) Math.ceil(score / 5.0);
        }

        if (keywordCount > 0) {
            int averageScore = score / keywordCount;

            // Apply negation effect (flip sentiment if negation is present)
            if (hasNegation && keywordCount > 0) {
                averageScore = 6 - averageScore; // Flip 1<->5, 2<->4, 3 stays 3
            }

            // Ensure within 1-5 range
            return Math.max(1, Math.min(5, averageScore));
        }

        return 3; // Default to average
    }

    public void addEntity(Rating rating) throws SQLException {
        // Automatically determine stars based on comment
        int stars = analyzeSentiment(rating.getCommentaire());
        rating.setEtoiles(stars);

        String req = "INSERT INTO rating (evenement_id, employe_id, commentaire, etoiles, date_creation) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, rating.getEvenementId());
        ps.setLong(2, rating.getEmployeId());
        ps.setString(3, rating.getCommentaire());
        ps.setInt(4, rating.getEtoiles());
        ps.setTimestamp(5, Timestamp.valueOf(rating.getDateCreation()));

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            rating.setId(rs.getLong(1));
        }
    }


    public void updateEntity(Rating rating) throws SQLException {
        // Re-analyze sentiment on update
        int stars = analyzeSentiment(rating.getCommentaire());
        rating.setEtoiles(stars);

        String req = "UPDATE rating SET commentaire = ?, etoiles = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, rating.getCommentaire());
        ps.setInt(2, rating.getEtoiles());
        ps.setLong(3, rating.getId());
        ps.executeUpdate();
    }

    public void deleteEntity(Rating rating) throws SQLException {
        String req = "DELETE FROM rating WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setLong(1, rating.getId());
        ps.executeUpdate();
    }

    public List<Rating> getData() throws SQLException {
        List<Rating> ratings = new ArrayList<>();
        String req = "SELECT * FROM rating ORDER BY date_creation DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Rating rating = new Rating(
                    rs.getLong("id"),
                    rs.getLong("evenement_id"),
                    rs.getLong("employe_id"),
                    rs.getString("commentaire"),
                    rs.getInt("etoiles"),
                    rs.getTimestamp("date_creation").toLocalDateTime()
            );
            ratings.add(rating);
        }

        return ratings;
    }

    public List<Rating> getRatingsByEvenement(long evenementId) throws SQLException {
        List<Rating> ratings = new ArrayList<>();
        String req = "SELECT * FROM rating WHERE evenement_id = ? ORDER BY date_creation DESC";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setLong(1, evenementId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Rating rating = new Rating(
                    rs.getLong("id"),
                    rs.getLong("evenement_id"),
                    rs.getLong("employe_id"),
                    rs.getString("commentaire"),
                    rs.getInt("etoiles"),
                    rs.getTimestamp("date_creation").toLocalDateTime()
            );
            ratings.add(rating);
        }

        return ratings;
    }

    public Rating getRatingByEmployeAndEvenement(long employeId, long evenementId) throws SQLException {
        String req = "SELECT * FROM rating WHERE employe_id = ? AND evenement_id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setLong(1, employeId);
        ps.setLong(2, evenementId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new Rating(
                    rs.getLong("id"),
                    rs.getLong("evenement_id"),
                    rs.getLong("employe_id"),
                    rs.getString("commentaire"),
                    rs.getInt("etoiles"),
                    rs.getTimestamp("date_creation").toLocalDateTime()
            );
        }

        return null;
    }

    public double getAverageRatingForEvenement(long evenementId) throws SQLException {
        String req = "SELECT AVG(etoiles) as avg_rating FROM rating WHERE evenement_id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setLong(1, evenementId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getDouble("avg_rating");
        }

        return 0.0;
    }

    public Map<Long, Double> getAllAverageRatings() throws SQLException {
        Map<Long, Double> averages = new HashMap<>();
        String req = "SELECT evenement_id, AVG(etoiles) as avg_rating FROM rating GROUP BY evenement_id";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            averages.put(rs.getLong("evenement_id"), rs.getDouble("avg_rating"));
        }

        return averages;
    }

    public Map<Long, Integer> getRatingCounts() throws SQLException {
        Map<Long, Integer> counts = new HashMap<>();
        String req = "SELECT evenement_id, COUNT(*) as count FROM rating GROUP BY evenement_id";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            counts.put(rs.getLong("evenement_id"), rs.getInt("count"));
        }

        return counts;
    }
}
