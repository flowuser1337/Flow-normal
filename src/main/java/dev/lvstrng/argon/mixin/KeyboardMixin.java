package dev.lvstrng.argon.mixin;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.event.events.ButtonListener.ButtonEvent;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

	@Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
	private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
		Argon.INSTANCE.getEventManager().fire(new ButtonEvent(key, window, action, scancode));
	}
}

