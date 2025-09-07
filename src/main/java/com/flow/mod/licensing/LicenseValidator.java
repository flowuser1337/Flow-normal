package com.flow.mod.licensing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LicenseValidator {
    private static final String API_URL = "https://tudominio.com/api/verify_license";
    private static String cachedHWID = null;

    /**
     * Verifica la licencia al iniciar el mod
     * @param licenseKey La clave de licencia a verificar
     * @return true si la licencia es válida, false de lo contrario
     */
    public static boolean validateLicense(String licenseKey) {
        try {
            String hwid = getHWID();
            JsonObject requestData = new JsonObject();
            requestData.addProperty("license_key", licenseKey);
            requestData.addProperty("hwid", hwid);
            
            String response = sendPostRequest(API_URL, requestData.toString());
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            
            boolean isValid = jsonResponse.get("valid").getAsBoolean();
            boolean hwidMatch = jsonResponse.get("hwid_match").getAsBoolean();
            
            if (isValid && hwidMatch) {
                System.out.println("[Flow] Licencia válida. ¡Bienvenido!");
                return true;
            } else if (isValid && !hwidMatch) {
                System.err.println("[Flow] Error: HWID no coincide con la base de datos.");
                crashMinecraft("HWID inválido. Esta licencia está registrada a otro dispositivo.");
                return false;
            } else {
                System.err.println("[Flow] Error: Licencia inválida o expirada.");
                crashMinecraft("Licencia inválida o expirada. Por favor, compra una licencia en nuestra web.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[Flow] Error al verificar la licencia: " + e.getMessage());
            crashMinecraft("Error al verificar la licencia. Por favor, verifica tu conexión a internet.");
            return false;
        }
    }
    
    /**
     * Obtiene el HWID único del sistema
     * @return String con el HWID generado
     */
    public static String getHWID() {
        if (cachedHWID != null) {
            return cachedHWID;
        }
        
        try {
            List<String> hwComponents = new ArrayList<>();
            
            // CPU ID
            String cpuInfo = getCPUInfo();
            hwComponents.add(cpuInfo);
            
            // Motherboard Serial
            String motherboardSerial = getMotherboardSerial();
            hwComponents.add(motherboardSerial);
            
            // Disk Serial
            String diskSerial = getDiskSerial();
            hwComponents.add(diskSerial);
            
            // MAC Address
            String macAddress = getMACAddress();
            hwComponents.add(macAddress);
            
            // Combinar todos los componentes y generar un hash
            StringBuilder combinedHwid = new StringBuilder();
            for (String component : hwComponents) {
                if (component != null && !component.isEmpty()) {
                    combinedHwid.append(component);
                }
            }
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combinedHwid.toString().getBytes(StandardCharsets.UTF_8));
            
            // Convertir bytes a formato hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            cachedHWID = hexString.toString();
            return cachedHWID;
        } catch (Exception e) {
            e.printStackTrace();
            return "unknown_hwid";
        }
    }
    
    // Métodos para obtener información del hardware
    private static String getCPUInfo() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Process process = Runtime.getRuntime().exec("wmic cpu get ProcessorId");
                process.getOutputStream().close();
                Scanner sc = new Scanner(process.getInputStream());
                sc.next();
                String serial = sc.next();
                sc.close();
                return serial;
            } else {
                return System.getProperty("os.arch");
            }
        } catch (Exception e) {
            return "";
        }
    }
    
    private static String getMotherboardSerial() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Process process = Runtime.getRuntime().exec("wmic baseboard get serialnumber");
                process.getOutputStream().close();
                Scanner sc = new Scanner(process.getInputStream());
                sc.next();
                String serial = sc.next();
                sc.close();
                return serial;
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private static String getDiskSerial() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Process process = Runtime.getRuntime().exec("wmic diskdrive get serialnumber");
                process.getOutputStream().close();
                Scanner sc = new Scanner(process.getInputStream());
                sc.next();
                String serial = sc.next();
                sc.close();
                return serial;
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private static String getMACAddress() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Process process = Runtime.getRuntime().exec("getmac /fo csv /nh");
                process.getOutputStream().close();
                Scanner sc = new Scanner(process.getInputStream());
                String firstLine = sc.nextLine();
                sc.close();
                if (!firstLine.isEmpty()) {
                    String[] macParts = firstLine.split("","");
                    if (macParts.length > 0) {
                        return macParts[0].replace("\"", "");
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Envía una solicitud POST al servidor
     */
    private static String sendPostRequest(String url, String jsonData) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        
        // Configurar la conexión
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
        
        // Enviar datos
        con.getOutputStream().write(jsonData.getBytes(StandardCharsets.UTF_8));
        
        // Obtener respuesta
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        return response.toString();
    }
    
    /**
     * Hace crash de Minecraft con un mensaje personalizado
     */
    private static void crashMinecraft(String message) {
        try {
            // En un entorno real, esto lanzaría una excepción para crashear Minecraft
            throw new RuntimeException("[Flow] Error de licencia: " + message);
        } catch (Exception e) {
            System.exit(1); // Forzar el cierre del juego
        }
    }
}