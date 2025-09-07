package dev.lvstrng.argon.gui;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.managers.AuthManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

public class AuthScreen extends Screen {

    private TextFieldWidget keyField;
    private ButtonWidget validateButton;
    private final AuthManager authManager;
    private TextWidget titleWidget;
    private TextWidget subtitleWidget;

    public AuthScreen(AuthManager authManager) {
        super(Text.of("Authentication"));
        this.authManager = authManager;
        System.out.println("AuthScreen constructor called!");
    }

    @Override
    protected void init() {
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.keyField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 10, 200, 20, Text.of("Enter your key"));
        this.validateButton = ButtonWidget.builder(Text.of("Enter Key"), button -> {
            String key = this.keyField.getText();
            if (authManager.validateKey(key)) {
                this.client.setScreen(null);
            } else {
                throw new RuntimeException("Authentication failed: Invalid key or HWID mismatch!");
            }
        }).dimensions(centerX - 100, centerY + 20, 200, 20).build();

        this.addDrawableChild(this.keyField);
        this.addDrawableChild(this.validateButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Fill the background with a solid black color
        context.fill(0, 0, this.width, this.height, 0xFF000000);

        // The widgets are rendered by the superclass
        super.render(context, mouseX, mouseY, delta);
    }
}
