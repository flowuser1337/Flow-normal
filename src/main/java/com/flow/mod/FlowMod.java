package com.flow.mod;

import com.flow.mod.licensing.LicenseValidator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import javax.swing.JOptionPane;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class FlowMod implements ModInitializer {
    public static final String MODID = "flowmod";
    public static final String NAME = "Flow Mod";
    public static final String VERSION = "1.0";
    
    private static final String LICENSE_FILE = "flow_license.properties";
    
    @Override
    public void onInitialize() {
        // Verificar licencia antes de iniciar el mod
        File gameDir = FabricLoader.getInstance().getGameDir().toFile();
        String licenseKey = getLicenseKey(gameDir);
        
        if (licenseKey == null || licenseKey.isEmpty()) {
            // Solicitar clave si no existe
            requestLicenseKey();
        } else {
            boolean isValid = LicenseValidator.validateLicense(licenseKey);
            if (!isValid) {
                // La validación fallará y el juego se cerrará en el validador
                System.exit(1);
            }
        }
        
        // Inicialización del mod (solo si la licencia es válida)
        System.out.println("¡Flow Mod iniciado correctamente!");
    }
    
    /**
     * Obtiene la clave de licencia almacenada o null si no existe
     */
    private String getLicenseKey(File gameDir) {
        File licenseFile = new File(gameDir, LICENSE_FILE);
        
        if (!licenseFile.exists()) {
            return null;
        }
        
        Properties props = new Properties();
        try (FileReader reader = new FileReader(licenseFile)) {
            props.load(reader);
            return props.getProperty("license_key");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Guarda la clave de licencia para futuras sesiones
     */
    private void saveLicenseKey(File gameDir, String licenseKey) {
        File licenseFile = new File(gameDir, LICENSE_FILE);
        
        Properties props = new Properties();
        props.setProperty("license_key", licenseKey);
        
        try (FileWriter writer = new FileWriter(licenseFile)) {
            props.store(writer, "Flow Mod License Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Solicita la clave de licencia al usuario
     */
    private void requestLicenseKey() {
        // Muestra un diálogo para solicitar la clave de licencia
        String licenseKey = JOptionPane.showInputDialog(
            null, 
            "Por favor, ingresa tu clave de licencia para Flow Mod:",
            "Flow Mod - Activación",
            JOptionPane.INFORMATION_MESSAGE
        );
        
        if (licenseKey != null && !licenseKey.isEmpty()) {
            boolean isValid = LicenseValidator.validateLicense(licenseKey);
            if (isValid) {
                // Si la clave es válida, la guardamos para futuros inicios
                saveLicenseKey(FabricLoader.getInstance().getGameDir().toFile(), licenseKey);
            }
        } else {
            // Si no se ingresó una clave, cerramos el juego
            System.exit(1);
        }
    }
}