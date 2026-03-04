package edu.RhPro.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.image.Image;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GeoapifyService {

    // REPLACE THIS WITH YOUR ACTUAL API KEY FROM https://www.geoapify.com/
    private static final String GEOAPIFY_API_KEY = "05dbc3584fbc4e34b5efb88cf1ec9e5a";
    private static final String GEOCODING_URL = "https://api.geoapify.com/v1/geocode/search";
    private static final String STATIC_MAP_URL = "https://maps.geoapify.com/v1/staticmap";

    /**
     * Search for locations using Geoapify Geocoding API
     */
    public List<Location> searchLocations(String query) throws IOException, URISyntaxException {
        List<Location> results = new ArrayList<>();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            URIBuilder uriBuilder = new URIBuilder(GEOCODING_URL)
                    .addParameter("text", query)
                    .addParameter("limit", "5")
                    .addParameter("apiKey", GEOAPIFY_API_KEY);

            HttpGet request = new HttpGet(uriBuilder.build());
            request.setHeader("User-Agent", "RhProApp/1.0");

            try (CloseableHttpResponse response = client.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                // Parse Geoapify response
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray features = jsonResponse.getAsJsonArray("features");

                for (int i = 0; i < features.size(); i++) {
                    JsonObject feature = features.get(i).getAsJsonObject();
                    JsonObject properties = feature.getAsJsonObject("properties");
                    JsonObject geometry = feature.getAsJsonObject("geometry");
                    JsonArray coordinates = geometry.getAsJsonArray("coordinates");

                    Location location = new Location();

                    // Get formatted address
                    if (properties.has("formatted")) {
                        location.setDisplayName(properties.get("formatted").getAsString());
                    } else if (properties.has("address_line1") && properties.has("address_line2")) {
                        String addr1 = properties.get("address_line1").getAsString();
                        String addr2 = properties.get("address_line2").getAsString();
                        location.setDisplayName(addr1 + ", " + addr2);
                    } else {
                        location.setDisplayName(properties.get("name").getAsString());
                    }

                    // Geoapify returns [lon, lat]
                    location.setLon(coordinates.get(0).getAsDouble());
                    location.setLat(coordinates.get(1).getAsDouble());

                    // Extract additional info
                    if (properties.has("city")) {
                        location.setCity(properties.get("city").getAsString());
                    }
                    if (properties.has("country")) {
                        location.setCountry(properties.get("country").getAsString());
                    }
                    if (properties.has("state")) {
                        location.setState(properties.get("state").getAsString());
                    }

                    results.add(location);
                }
            }
        }
        return results;
    }

    /**
     * Get static map URL with marker
     */
    public String getStaticMapUrl(double lat, double lon, int width, int height, int zoom) {
        return String.format(
                "%s?style=osm-carto&width=%d&height=%d&center=lonlat:%f,%f&zoom=%d&marker=lonlat:%f,%f;color:%s;size:medium&apiKey=%s",
                STATIC_MAP_URL, width, height, lon, lat, zoom, lon, lat, "red", GEOAPIFY_API_KEY
        );
    }

    /**
     * Load static map image using HttpURLConnection (handles HTTPS + redirects)
     */
    public Image loadStaticMap(double lat, double lon, int width, int height, int zoom) {
        String mapUrl = getStaticMapUrl(lat, lon, width, height, zoom);
        System.out.println("Loading map: " + mapUrl);
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(mapUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "RhProApp/1.0");
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            System.out.println("Map HTTP response: " + code);
            if (code == 200) {
                try (java.io.InputStream is = conn.getInputStream()) {
                    byte[] bytes = is.readAllBytes();
                    Image img = new Image(new java.io.ByteArrayInputStream(bytes));
                    if (!img.isError()) return img;
                    System.out.println("Image error after download: " + img.getException());
                }
            } else {
                System.out.println("Map API error: HTTP " + code);
            }
        } catch (Exception e) {
            System.out.println("Map load exception: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get interactive web map link
     */
    public String getWebMapLink(double lat, double lon) {
        return String.format("https://www.openstreetmap.org/?mlat=%f&mlon=%f#map=15/%f/%f",
                lat, lon, lat, lon);
    }

    /**
     * Validate if coordinates are valid
     */
    public boolean isValidCoordinates(double lat, double lon) {
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    // Inner class for location data
    public static class Location {
        private String displayName;
        private double lat;
        private double lon;
        private String city;
        private String state;
        private String country;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }

        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getCoordinatesString() {
            return String.format("%.6f, %.6f", lat, lon);
        }

        public String getFullAddress() {
            StringBuilder sb = new StringBuilder();
            if (city != null) sb.append(city);
            if (state != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(state);
            }
            if (country != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(country);
            }
            return sb.toString();
        }
    }
}