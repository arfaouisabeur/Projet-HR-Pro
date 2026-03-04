package edu.RhPro.services;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class ImgbbService {

    private static final String API_KEY = "311a06725247e13e1c427b74cb3dd3c2"; // Get from https://api.imgbb.com/
    private static final String UPLOAD_URL = "https://api.imgbb.com/1/upload";

    /**
     * Upload an image file to ImgBB and return the URL
     */
    public String uploadImage(File imageFile) throws IOException {
        // Read file and convert to base64
        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        String base64Image = Base64.getEncoder().encodeToString(fileContent);

        return uploadBase64Image(base64Image);
    }

    /**
     * Upload base64 encoded image to ImgBB
     */
    public String uploadBase64Image(String base64Image) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(UPLOAD_URL);

            // Build multipart request
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody("key", API_KEY)
                    .addTextBody("image", base64Image)
                    .build();

            post.setEntity(entity);

            try (CloseableHttpResponse response = client.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                // Parse JSON response
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

                if (jsonResponse.has("data")) {
                    JsonObject data = jsonResponse.getAsJsonObject("data");
                    return data.get("url").getAsString();
                } else if (jsonResponse.has("error")) {
                    throw new IOException("Upload failed: " + jsonResponse.get("error").toString());
                } else {
                    throw new IOException("Unexpected response format");
                }
            }
        }
    }
}
