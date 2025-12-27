package org.example.s.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.Click; // Fixed: Correct package for 1.21.11
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.text.Text;
import org.example.s.PasswordGenerator;

import java.util.HashMap;
import java.util.Map;

public class AutoLoginConfigScreen extends Screen {
    public static JSONConfigHandler.PlayerConfig config = JSONConfigHandler.getCurrentPlayerConfig();

    private int scrollOffset = 0;
    private final Screen parent;

    private TextFieldWidget ipField;
    private TextFieldWidget passwordField;
    private ButtonWidget addButton;
    private ButtonWidget generateButton;
    private ButtonWidget deleteButton;
    private String selectedServer = null;
    private final Map<String, String> servers = new HashMap<>();
    private TextFieldWidget loginCommandField;
    private CheckboxWidget autosendToggle;
    private ButtonWidget toggleCheckButton;
    private ButtonWidget saveTriggerButton;

    public AutoLoginConfigScreen(Screen parent) {
        super(Text.translatable("config.autologin_mod.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        config = JSONConfigHandler.getCurrentPlayerConfig();

        int fieldLeft = width / 4;
        int fieldWidth = 200;

        ipField = new TextFieldWidget(textRenderer, fieldLeft, height / 4 + 60, fieldWidth, 20, Text.translatable("config.autologin_mod.ip"));
        passwordField = new TextFieldWidget(textRenderer, fieldLeft, height / 4 + 100, fieldWidth, 20, Text.translatable("config.autologin_mod.password"));
        loginCommandField = new TextFieldWidget(textRenderer, fieldLeft, height / 4 + 140, fieldWidth, 20, Text.translatable("config.autologin_mod.login_command"));

        addButton = ButtonWidget.builder(Text.translatable("config.autologin_mod.add"), button -> addServer())
                .dimensions(fieldLeft + fieldWidth + 10, height / 4 + 60, 100, 20).build();

        generateButton = ButtonWidget.builder(Text.translatable("config.autologin_mod.generate"), button -> generatePassword())
                .dimensions(fieldLeft + fieldWidth + 10, height / 4 + 100, 100, 20).build();

        saveTriggerButton = ButtonWidget.builder(Text.translatable("config.autologin_mod.save_trigger"), button -> saveConfig())
                .dimensions(fieldLeft + fieldWidth + 10, height / 4 + 140, 100, 20).build();

        toggleCheckButton = ButtonWidget.builder(Text.translatable(config.check_enabled ? "config.autologin_mod.enabled" : "config.autologin_mod.disabled"), button -> toggleLoginCheck())
                .dimensions(width / 2 - 50, 10, 100, 20).build();

        deleteButton = ButtonWidget.builder(Text.translatable("config.autologin_mod.delete"), button -> deleteServer())
                .dimensions(width / 4 * 3, (int) (height * 0.74), 100, 20).build();

        int checkboxY = height / 4 + 165;
        autosendToggle = CheckboxWidget.builder(Text.translatable("config.autologin_mod.autosend"), textRenderer)
                .pos(fieldLeft, checkboxY)
                .checked(config != null && config.autosend)
                .callback((cb, val) -> {
                    if (config == null) config = JSONConfigHandler.getCurrentPlayerConfig();
                    config.autosend = val;
                    JSONConfigHandler.saveCurrentPlayerConfig(config);
                })
                .build();

        addDrawableChild(ipField);
        addDrawableChild(passwordField);
        addDrawableChild(loginCommandField);
        addDrawableChild(autosendToggle);

        addSelectableChild(ipField);
        addSelectableChild(passwordField);
        addSelectableChild(loginCommandField);

        addDrawableChild(addButton);
        addDrawableChild(generateButton);
        addDrawableChild(saveTriggerButton);
        addDrawableChild(toggleCheckButton);
        addDrawableChild(deleteButton);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), button -> {
            assert client != null;
            client.setScreen(parent);
        }).dimensions(5, 5, 60, 20).build());

        loadConfig();
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int listX = width / 4 * 3;
        int listY = height / 4 + 20;
        int listWidth = width / 4;
        int listHeight = height / 2 - 40;
        int entryHeight = 20;
        int visibleEntries = listHeight / entryHeight;

        context.fill(listX - 1, listY - 1, listX + listWidth + 1, listY + listHeight + 1, 0x80000000);
        String username = MinecraftClient.getInstance().getSession().getUsername();
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("config.autologin_mod.saved_servers", username), listX + listWidth / 2, listY - 20, 0xFFFFFFFF);

        int renderedCount = 0;
        int entryIndex = 0;
        for (Map.Entry<String, String> entry : servers.entrySet()) {
            if (entryIndex < scrollOffset) { entryIndex++; continue; }
            if (renderedCount >= visibleEntries) break;

            String ip = entry.getKey();
            int entryY = listY + renderedCount * entryHeight;

            if (ip.equals(selectedServer)) {
                context.fill(listX, entryY, listX + listWidth, entryY + entryHeight, 0x30FFFFFF);
            }

            String displayText = ip + "=" + entry.getValue();
            String truncatedText = textRenderer.trimToWidth(displayText, listWidth - 10);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(truncatedText), listX + listWidth / 2, entryY + (entryHeight - textRenderer.fontHeight) / 2, 0xFFFFFFFF);

            renderedCount++;
            entryIndex++;
        }

        int fieldLeft = width / 4;
        int fieldCenter = fieldLeft + 100;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("config.autologin_mod.ip"), fieldCenter, height / 4 + 45, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("config.autologin_mod.password"), fieldCenter, height / 4 + 85, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("config.autologin_mod.login_command"), fieldCenter, height / 4 + 125, 0xFFFFFFFF);

        String statusText = Text.translatable("config.autologin_mod.status", config.check_enabled ? Text.translatable("config.autologin_mod.enabled") : Text.translatable("config.autologin_mod.disabled")).getString();
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(statusText), width / 2, 40, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listX = width / 4 * 3;
        int listY = height / 4 + 20;
        int listHeight = height / 2 - 40;
        int visibleEntries = listHeight / 20;

        if (mouseX >= listX && mouseX < listX + (width / 4) && mouseY >= listY && mouseY < listY + listHeight) {
            int maxScroll = Math.max(0, servers.size() - visibleEntries);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)verticalAmount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(Click click, boolean handled) {
        boolean superHandled = super.mouseClicked(click, handled);
        if (!superHandled && !handled && click.button() == 0) {
            int listX = width / 4 * 3;
            int listY = height / 4 + 20;
            int entryHeight = 20;

            if (click.x() >= listX && click.x() < listX + (width / 4) && click.y() >= listY && click.y() < listY + (height / 2 - 40)) {
                int actualIndex = scrollOffset + (int)((click.y() - listY) / entryHeight);
                if (actualIndex >= 0 && actualIndex < servers.size()) {
                    selectedServer = (String) servers.keySet().toArray()[actualIndex];
                    return true;
                }
            } else {
                selectedServer = null;
            }
        }
        return superHandled;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (ipField.keyPressed(input)) return true;
        if (passwordField.keyPressed(input)) return true;
        
        int keyCode = input.key(); // Fixed: Changed from .code() to .key()
        int modifiers = input.modifiers();

        if (loginCommandField.keyPressed(input)) {
            if (keyCode == 257 || keyCode == 335) saveConfig();
            return true;
        }

        if (keyCode == 261 && selectedServer != null) { deleteServer(); return true; }

        if (keyCode == 67 && (modifiers & 2) != 0 && selectedServer != null) {
            String pass = servers.get(selectedServer);
            if (pass != null && client != null) client.keyboard.setClipboard(pass);
            return true;
        }

        if (selectedServer != null) {
            if (keyCode == 264) { selectNextServer(1); return true; }
            if (keyCode == 265) { selectNextServer(-1); return true; }
        }

        return super.keyPressed(input);
    }

    private void selectNextServer(int direction) {
        String[] keys = servers.keySet().toArray(new String[0]);
        int idx = -1;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(selectedServer)) { idx = i; break; }
        int newIdx = Math.max(0, Math.min(keys.length - 1, idx + direction));
        selectedServer = keys[newIdx];
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (ipField.charTyped(input)) return true;
        if (passwordField.charTyped(input)) return true;
        if (loginCommandField.charTyped(input)) return true;
        return super.charTyped(input);
    }

    private void toggleLoginCheck() {
        config.check_enabled = !config.check_enabled;
        toggleCheckButton.setMessage(Text.translatable(config.check_enabled ? "config.autologin_mod.enabled" : "config.autologin_mod.disabled"));
        JSONConfigHandler.saveCurrentPlayerConfig(config);
    }

    private void addServer() {
        if (!ipField.getText().isEmpty() && !passwordField.getText().isEmpty()) {
            servers.put(ipField.getText().trim(), passwordField.getText().trim());
            saveConfig();
            ipField.setText(""); passwordField.setText("");
        }
    }

    private void generatePassword() {
        if (!ipField.getText().isEmpty()) {
            String p = PasswordGenerator.generate(15);
            passwordField.setText(p);
            servers.put(ipField.getText().trim(), p);
            saveConfig();
        }
    }

    private void deleteServer() {
        if (selectedServer != null) { servers.remove(selectedServer); saveConfig(); selectedServer = null; }
    }

    private void loadConfig() {
        config = JSONConfigHandler.getCurrentPlayerConfig();
        servers.clear();
        if (config != null && config.passwords != null) servers.putAll(config.passwords);
        if (loginCommandField != null && config != null) loginCommandField.setText(config.triggers != null ? config.triggers : "");
    }

    private void saveConfig() {
        if (config != null) {
            config.triggers = loginCommandField.getText();
            config.autosend = autosendToggle.isChecked();
            config.passwords.clear(); config.passwords.putAll(servers);
            JSONConfigHandler.saveCurrentPlayerConfig(config);
        }
    }
}