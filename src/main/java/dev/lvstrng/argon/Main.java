package dev.lvstrng.argon;

import dev.lvstrng.argon.managers.SecurityManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

import java.io.IOException;
import java.net.URISyntaxException;

public final class Main implements ModInitializer {
	@Override
	public void onInitialize() {
		SecurityManager.check();
		
		try {
			new Argon();

			ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
				Argon.INSTANCE.getProfileManager().saveProfile();
			});
		} catch (InterruptedException | IOException ignored) {}
	}
}