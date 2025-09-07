package dev.lvstrng.argon.mixin;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.event.events.TickListener;
import dev.lvstrng.argon.module.ModuleManager;
import dev.lvstrng.argon.module.modules.client.FastPot;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.ItemStack;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Argon.INSTANCE.getEventManager().fire(new TickListener.TickEvent());
    }

    // Injected code for FastPot module
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        if (Argon.INSTANCE == null) return; // Ensure the client is initialized

        ModuleManager moduleManager = Argon.INSTANCE.getModuleManager();
        if (moduleManager == null) return;

        FastPot fastPotModule = moduleManager.getModule(FastPot.class);

        if (fastPotModule != null && fastPotModule.isEnabled()) {
            // The MinecraftClient instance is `this` in a mixin
            MinecraftClient client = (MinecraftClient)(Object)this;
            if (client.player == null) return;

            ItemStack mainHandStack = client.player.getMainHandStack();
            if (mainHandStack.getItem() instanceof SplashPotionItem) {
                MinecraftClientAccessor accessor = (MinecraftClientAccessor) client;

                // Set the item use cooldown to the value from the module's setting
                if (accessor.getItemUseCooldown() > fastPotModule.delay.getValueInt()) {
                     accessor.setItemUseCooldown(fastPotModule.delay.getValueInt());
                }
            }
        }
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
        Argon.INSTANCE.getProfileManager().saveProfile();
    }
}