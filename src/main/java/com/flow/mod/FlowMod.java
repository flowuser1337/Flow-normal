package com.flow.mod;

import com.flow.mod.licensing.LicenseValidator;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import javax.swing.JOptionPane;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

@Mod(modid = FlowMod.MODID, name = FlowMod.NAME, version = FlowMod.VERSION)
public class FlowMod {
    public static final String MODID = "flowmod";
    public static final String NAME = "Flow Mod";
    public static final String VERSION = "1.0";
    
    private static final String LICENSE_FILE = "flow_license.properties";
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Verificar licencia antes de iniciar el mod
        String licenseKey = getLicenseKey(event.getModConfigurationDirectory().getParentFile());
        
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
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
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
                saveLicenseKey(Minecraft.getMinecraft().mcDataDir, licenseKey);
            }
        } else {
            // Si no se ingresó una clave, cerramos el juego
            System.exit(1);
        }
    }
}