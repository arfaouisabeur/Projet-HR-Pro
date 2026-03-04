package edu.RhPro.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public class OpenStreetMapService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    /**
     * Search for locations and get multiple results
     */
    public List<Location> searchLocations(String query) throws IOException, URISyntaxException {
        List<Location> results = new ArrayList<>();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            URIBuilder uriBuilder = new URIBuilder(NOMINATIM_URL)
                    .addParameter("q", query)
                    .addParameter("format", "json")
                    .addParameter("limit", "5");

            HttpGet request = new HttpGet(uriBuilder.build());
            request.setHeader("User-Agent", "RhProApp/1.0");

            try (CloseableHttpResponse response = client.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonArray jsonResults = JsonParser.parseString(responseBody).getAsJsonArray();

                for (int i = 0; i < jsonResults.size(); i++) {
                    JsonObject jsonLocation = jsonResults.get(i).getAsJsonObject();

                    Location location = new Location();
                    location.setDisplayName(jsonLocation.get("display_name").getAsString());
                    location.setLat(Double.parseDouble(jsonLocation.get("lat").getAsString()));
                    location.setLon(Double.parseDouble(jsonLocation.get("lon").getAsString()));

                    // Extract city/country
                    String[] parts = location.getDisplayName().split(", ");
                    if (parts.length > 0) {
                        location.setCity(parts[0]);
                    }
                    if (parts.length > 1) {
                        location.setCountry(parts[parts.length - 1]);
                    }

                    results.add(location);
                }
            }
        }
        return results;
    }

    /**
     * Generate a static map image URL for a location using multiple providers
     */
    public String getStaticMapUrl(double lat, double lon, int zoom, int width, int height) {
        // Try multiple providers - return an array of URLs to try
        return getStaticMapUrlOSM(lat, lon, zoom, width, height);
    }

    /**
     * OpenStreetMap static map (may have issues)
     */
    private String getStaticMapUrlOSM(double lat, double lon, int zoom, int width, int height) {
        return String.format(
                "https://staticmap.openstreetmap.de/staticmap.php?center=%f,%f&zoom=%d&size=%dx%d&maptype=mapnik",
                lat, lon, zoom, width, height
        );
    }

    /**
     * Alternative 1: MapQuest static map (requires API key but more reliable)
     * Get free key from: https://developer.mapquest.com/
     */
    public String getStaticMapUrlMapQuest(double lat, double lon, int zoom, int width, int height, String apiKey) {
        return String.format(
                "https://www.mapquestapi.com/staticmap/v5/map?key=%s&center=%f,%f&zoom=%d&size=%d,%d",
                apiKey, lat, lon, zoom, width, height
        );
    }

    /**
     * Alternative 2: Use a tile server directly (simpler, no API key needed)
     * This returns a URL for a single tile - less flexible but works
     */
    public String getTileUrl(double lat, double lon, int zoom, int width, int height) {
        // Convert lat/lon to tile numbers (simplified)
        int tileX = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int tileY = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) +
                1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));

        return String.format(
                "https://tile.openstreetmap.org/%d/%d/%d.png",
                zoom, tileX, tileY
        );
    }

    /**
     * Alternative 3: Generate a link to the web version (user can click)
     */
    public String getWebMapLink(double lat, double lon) {
        return String.format("https://www.openstreetmap.org/?mlat=%f&mlon=%f#map=15/%f/%f", lat, lon, lat, lon);
    }

    // Inner class for location data
    public static class Location {
        private String displayName;
        private double lat;
        private double lon;
        private String city;
        private String country;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }

        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getCoordinatesString() {
            return String.format("%.6f, %.6f", lat, lon);
        }
    }
}