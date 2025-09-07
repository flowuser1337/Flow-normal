package dev.lvstrng.argon.module;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.event.events.ButtonListener;
import dev.lvstrng.argon.module.modules.client.ClickGUI;
import dev.lvstrng.argon.module.modules.client.Friends;
import dev.lvstrng.argon.module.modules.client.SelfDestruct;
import dev.lvstrng.argon.module.modules.client.TriggerBot;
import dev.lvstrng.argon.module.modules.client.Sprint;
import dev.lvstrng.argon.module.modules.client.NoDelay;
import dev.lvstrng.argon.module.modules.client.FastPot;
import dev.lvstrng.argon.module.modules.client.AimAssist;

import dev.lvstrng.argon.module.setting.KeybindSetting;
import dev.lvstrng.argon.utils.EncryptedString;

import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager implements ButtonListener {
	private final List<Module> modules = new ArrayList<>();

	public ModuleManager() {
		addModules();
		addKeybinds();
	}

	public void addModules() {
		//Client
		add(new TriggerBot());
		add(new Sprint());
		add(new NoDelay());
		add(new FastPot());
		add(new Friends());
		add(new ClickGUI());
		add(new SelfDestruct());
		add(new AimAssist());
	}

	public List<Module> getEnabledModules() {
		return modules.stream()
				.filter(Module::isEnabled)
				.toList();
	}


	public List<Module> getModules() {
		return modules;
	}

	public void addKeybinds() {
		Argon.INSTANCE.getEventManager().add(ButtonListener.class, this);

		for (Module module : modules)
			                        module.addSetting(new KeybindSetting(EncryptedString.of("Keybind").toString(), module.getKey(), true, module).setDescription(EncryptedString.of("Key to enabled the module")));
	}

	public List<Module> getModulesInCategory(Category category) {
		return modules.stream()
				.filter(module -> module.getCategory() == category)
				.toList();
	}

	@SuppressWarnings("unchecked")
	public <T extends Module> T getModule(Class<T> moduleClass) {
		return (T) modules.stream()
				.filter(moduleClass::isInstance)
				.findFirst()
				.orElse(null);
	}

	public void add(Module module) {
		modules.add(module);
	}

	@Override
	public void onButtonPress(ButtonEvent event) {
		if (Argon.mc.currentScreen instanceof ChatScreen) {
			return;
		}
		modules.forEach(module -> {
			if((module.getKey() == event.button || module.getKey() == event.scancode) && event.action == GLFW.GLFW_PRESS) {
				module.toggle();
				Argon.INSTANCE.getProfileManager().saveProfile();
			}
		});
	}
}