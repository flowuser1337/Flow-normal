package dev.lvstrng.argon.managers;

import dev.lvstrng.argon.utils.HWID;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

public class AuthManager {

    private static final String API_BASE_URL = "http://mx-1.ba.supercores.host:30014/verify";
    private final Path configDir = Paths.get(System.getProperty("user.home"), ".argon");
    private final Path keyFile;
    private String cachedKey = null;

    public AuthManager() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.keyFile = findOrCreateKeyFile();
        loadKey();
    }

    private Path findOrCreateKeyFile() {
        try {
            return Files.list(configDir)
                    .filter(p -> p.getFileName().toString().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.dat"))
                    .findFirst()
                    .orElse(configDir.resolve(UUID.randomUUID() + ".dat"));
        } catch (Exception e) {
            return configDir.resolve(UUID.randomUUID() + ".dat");
        }
    }

    private void loadKey() {
        try {
            if (Files.exists(keyFile)) {
                byte[] decodedBytes = Base64.getDecoder().decode(Files.readAllBytes(keyFile));
                this.cachedKey = new String(decodedBytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            this.cachedKey = null;
        }
    }

    private void saveKey(String key) {
        try {
            byte[] encodedBytes = Base64.getEncoder().encode(key.getBytes(StandardCharsets.UTF_8));
            Files.write(keyFile, encodedBytes);
            this.cachedKey = key;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean performApiCall(String key, String hwid) {
        try {
            // MODIFIED: Build URL with query parameters
            String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8.toString());
            String encodedHwid = URLEncoder.encode(hwid, StandardCharsets.UTF_8.toString());
            URL url = new URL(API_BASE_URL + "?key=" + encodedKey + "&hwid=" + encodedHwid);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET"); // MODIFIED: Changed to GET
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();

            // Read response to ensure connection is fully processed
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                
            }

            return code == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        String currentHwid = HWID.getHWID();
        boolean isValid = performApiCall(key, currentHwid);
        if (isValid) {
            saveKey(key);
        }
        return isValid;
    }

    public boolean checkHWID() {
        if (this.cachedKey == null) {
            return false;
        }
        String currentHwid = HWID.getHWID();
        return performApiCall(this.cachedKey, currentHwid);
    }
}
