package dev.lvstrng.argon.module.modules.client;

import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.EncryptedString;

public class FastPot extends Module {

    public NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 4, 0, 1);

    public FastPot() {
        super(EncryptedString.of("FastPot"), EncryptedString.of("Reduces potion cooldown."), -1, Category.FLOW);
        addSettings(delay);
    }
}
