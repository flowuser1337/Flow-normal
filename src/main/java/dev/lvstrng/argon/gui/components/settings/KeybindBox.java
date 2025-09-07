package dev.lvstrng.argon.gui.components.settings;

import dev.lvstrng.argon.gui.components.ModuleButton;
import dev.lvstrng.argon.module.setting.KeybindSetting;
import dev.lvstrng.argon.module.setting.Setting;
import dev.lvstrng.argon.utils.ColorUtils;
import dev.lvstrng.argon.utils.EncryptedString;
import dev.lvstrng.argon.utils.KeyUtils;
import dev.lvstrng.argon.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public final class KeybindBox extends RenderableSetting {
    private final KeybindSetting keybind;
    private Color currentAlpha;

    public KeybindBox(ModuleButton parent, Setting<?> setting, int offset) {
        super(parent, setting, offset);
        this.keybind = (KeybindSetting) setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        Color textColor = new Color(255, 255, 255);

        String displayText;
        if (!keybind.isListening()) {
            displayText = keybind.getName() + EncryptedString.of(": ").toString() + KeyUtils.getKey(keybind.getKey());
        } else {
            displayText = EncryptedString.of("Listening...").toString();
        }

        TextRenderer.drawStringBold(displayText, context, parentX() + 5, (parentY() + parentOffset() + offset) + 9, textColor.getRGB());

        if (!parent.parent.dragging) {
            int toHoverAlpha = isHovered(mouseX, mouseY) ? 30 : 0;

            if (currentAlpha == null)
                currentAlpha = new Color(255, 255, 255, toHoverAlpha);
            else currentAlpha = new Color(255, 255, 255, currentAlpha.getAlpha());

            if (currentAlpha.getAlpha() != toHoverAlpha)
                currentAlpha = ColorUtils.smoothAlphaTransition(0.05F, toHoverAlpha, currentAlpha);

            context.fill(parentX(), parentY() + parentOffset() + offset, parentX() + parentWidth(), parentY() + parentOffset() + offset + parentHeight(), currentAlpha.getRGB());
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovered(mouseX, mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            keybind.setListening(!keybind.isListening());
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keybind.isListening()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                keybind.setKey(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                keybind.setKey(keyCode);
            }
            keybind.setListening(false);
        }
    }
}