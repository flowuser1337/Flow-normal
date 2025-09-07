package com.flow.mod.licensing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LicenseValidator {
    private static final String API_URL = "https://flowclient.shop/api/verify_license.php";
    private static String cachedHWID = null;
    private static final String LICENSE_FILE = "flow_license.dat";
    private static final int CONNECTION_TIMEOUT = 5000; // 5 segundos
    
    /**
     * Verifica la licencia al iniciar el mod
     * @param licenseKey La clave de licencia a verificar
     * @return true si la licencia es válida, false de lo contrario
     */
    public static boolean validateLicense(String licenseKey) {
        try {
            String hwid = getHWID();
            
            // Intentar verificación en línea
            boolean onlineValidation = tryOnlineValidation(licenseKey, hwid);
            if (onlineValidation) {
                // Si la validación en línea es exitosa, guardar la licencia localmente
                saveLicenseLocally(licenseKey, hwid);
                System.out.println("[Flow] Licencia válida. ¡Bienvenido!");
                return true;
            }
            
            // Si la validación en línea falla, intentar verificación offline
            if (isValidOffline(licenseKey, hwid)) {
                System.out.println("[Flow] Validación offline exitosa. ¡Bienvenido!");
                return true;
            }
            
            // Si ambas validaciones fallan, mostrar error
            System.err.println("[Flow] Error: Licencia inválida o expirada.");
            crashGame("Licencia inválida o expirada. Por favor, compra una licencia en nuestra web.");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            // En caso de error, intentar verificación offline
            if (isValidOffline(licenseKey, getHWID())) {
                System.out.println("[Flow] Validación offline exitosa. ¡Bienvenido!");
                return true;
            }
            
            System.err.println("[Flow] Error al verificar la licencia: " + e.getMessage());
            crashGame("Error al verificar la licencia. Por favor, verifica tu conexión a internet.");
            return false;
        }
    }
    
    /**
     * Intenta validar la licencia en línea
     */
    private static boolean tryOnlineValidation(String licenseKey, String hwid) {
        try {
            JsonObject requestData = new JsonObject();
            requestData.addProperty("license_key", licenseKey);
            requestData.addProperty("hwid", hwid);
            
            String response = sendPostRequest(API_URL, requestData.toString());
            if (response == null || response.isEmpty()) {
                System.err.println("[Flow] Error: Respuesta vacía del servidor");
                return false;
            }
            
            try {
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                
                boolean isValid = jsonResponse.has("valid") && jsonResponse.get("valid").getAsBoolean();
                boolean hwidMatch = !jsonResponse.has("hwid_match") || jsonResponse.get("hwid_match").getAsBoolean();
                
                if (isValid && hwidMatch) {
                    return true;
                } else if (isValid && !hwidMatch) {
                    System.err.println("[Flow] Error: HWID no coincide con la base de datos.");
                    crashGame("HWID inválido. Esta licencia está registrada a otro dispositivo.");
                    return false;
                }
            } catch (JsonSyntaxException e) {
                System.err.println("[Flow] Error al parsear la respuesta del servidor: " + e.getMessage());
                System.err.println("[Flow] Respuesta recibida: " + response);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[Flow] Error en validación online: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Guarda la información de la licencia localmente para verificación offline
     */
    private static void saveLicenseLocally(String licenseKey, String hwid) {
        try {
            File file = new File(LICENSE_FILE);
            FileWriter writer = new FileWriter(file);
            writer.write(licenseKey + ":" + hwid);
            writer.close();
        } catch (Exception e) {
            System.err.println("[Flow] Error al guardar licencia: " + e.getMessage());
        }
    }
    
    /**
     * Verifica si la licencia es válida offline
     */
    private static boolean isValidOffline(String licenseKey, String hwid) {
        try {
            File file = new File(LICENSE_FILE);
            if (!file.exists()) {
                return false;
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            reader.close();
            
            if (line != null && !line.isEmpty()) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    return parts[0].equals(licenseKey) && parts[1].equals(hwid);
                }
            }
        } catch (Exception e) {
            System.err.println("[Flow] Error en validación offline: " + e.getMessage());
        }
        return false;
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
                    String[] macParts = firstLine.split(",");
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
        
        // Configurar la conexión con timeout
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setConnectTimeout(CONNECTION_TIMEOUT);
        con.setReadTimeout(CONNECTION_TIMEOUT);
        con.setDoOutput(true);
        
        try {
            // Enviar datos
            con.getOutputStream().write(jsonData.getBytes(StandardCharsets.UTF_8));
            
            // Obtener respuesta
            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                System.err.println("[Flow] Error del servidor: Código " + responseCode);
                return null;
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            return response.toString();
        } catch (SocketTimeoutException e) {
            System.err.println("[Flow] Tiempo de espera agotado al conectar con el servidor de licencias");
            throw e;
        } catch (ConnectException e) {
            System.err.println("[Flow] No se pudo conectar al servidor de licencias: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Hace crash del juego con un mensaje personalizado
     */
    private static void crashGame(String message) {
        try {
            // En un entorno real, esto lanzaría una excepción para crashear Minecraft
            throw new RuntimeException("[Flow] Error de licencia: " + message);
        } catch (Exception e) {
            System.exit(1); // Forzar el cierre del juego
        }
    }
}