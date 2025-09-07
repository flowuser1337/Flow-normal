package dev.lvstrng.argon.module.modules.client;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.event.events.TickListener;
import dev.lvstrng.argon.mixin.ClientPlayerInteractionManagerAccessor;
import dev.lvstrng.argon.mixin.LivingEntityJumpAccessor;
import dev.lvstrng.argon.mixin.MinecraftClientAccessor;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.utils.EncryptedString;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.util.Hand;

import java.util.Random;

import static dev.lvstrng.argon.Argon.mc;

public class NoDelay extends Module implements TickListener {

    
    public BooleanSetting breakDelay = new BooleanSetting(EncryptedString.of("Break Delay"), true);
    public BooleanSetting noHitDelay = new BooleanSetting(EncryptedString.of("No Hit Delay"), true);

    private final Random random = new Random();

    public NoDelay() {
        super(EncryptedString.of("NoDelay"), EncryptedString.of("Removes various delays in the game."), -1, Category.FLOW);
        addSettings(breakDelay, noHitDelay);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;

        System.out.println("[NoDelay] Tick");

        FastPot fastPotModule = Argon.INSTANCE.getModuleManager().getModule(FastPot.class);
        if (fastPotModule != null && fastPotModule.isEnabled() && mc.player.getMainHandStack().getItem() instanceof SplashPotionItem) {
            System.out.println("[NoDelay] FastPot is active for splash potion, returning.");
            return; // Let FastPot handle the cooldown
        }

        if (noHitDelay.getValue()) {
            System.out.println("[NoDelay] Setting item use cooldown");
            // Introduce a small random delay to make it less detectable
            // This will set the cooldown to a random value between 0 and 2 ticks
            ((MinecraftClientAccessor) mc).setItemUseCooldown(random.nextInt(3));
        }

        if (breakDelay.getValue()) {
            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).setBlockBreakingCooldown(random.nextInt(3));
        }
    }
}