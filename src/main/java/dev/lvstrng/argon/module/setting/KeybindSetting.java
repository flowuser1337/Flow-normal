package dev.lvstrng.argon.module.setting;

import dev.lvstrng.argon.module.Module;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

// This class now correctly inherits from Setting and includes the moduleKey logic.
public class KeybindSetting extends Setting<Integer> {
    private int key;
    private final Module module;
    private final boolean moduleKey; // Field to track if this is a module toggle keybind
    private boolean listening;

    public KeybindSetting(String name, int key, boolean moduleKey, Module module) {
        super(name, key, ""); // Call the parent constructor to set the name
        this.key = key;
        this.module = module;
        this.moduleKey = moduleKey;
        this.listening = false;
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
        setValue(key); // Also update the parent's value
        if (moduleKey) {
            module.setKey(key);
        }
    }

    public Module getModule() {
        return module;
    }

    public boolean isListening() {
        return listening;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    // This method is now present, fixing the error in ProfileManager
    public boolean isModuleKey() {
        return this.moduleKey;
    }

    public boolean isPressed() {
        return key != GLFW.GLFW_KEY_UNKNOWN && key != -1 && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }
}