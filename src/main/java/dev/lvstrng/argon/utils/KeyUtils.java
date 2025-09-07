package dev.lvstrng.argon.utils;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import static dev.lvstrng.argon.Argon.mc;

public final class KeyUtils {

	public static CharSequence getKey(int keyCode) {
		if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return EncryptedString.of("None");

		if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
			return String.valueOf(keyCode - GLFW.GLFW_KEY_0);
		}

		if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
			return String.valueOf((char) ('A' + (keyCode - GLFW.GLFW_KEY_A)));
		}
		
		if (keyCode >= GLFW.GLFW_MOUSE_BUTTON_1 && keyCode <= GLFW.GLFW_MOUSE_BUTTON_8) {
			return "Mouse " + (keyCode - GLFW.GLFW_MOUSE_BUTTON_1 + 1);
		}

		String keyName = GLFW.glfwGetKeyName(keyCode, 0);
		if (keyName == null) return EncryptedString.of("None");
		return StringUtils.capitalize(keyName);
	}

	public static boolean isKeyPressed(int keyCode) {
		if (keyCode <= 8)
			return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;

		return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
	}
}
