package client.org.example.s.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

public class AutoLoginOnJoin {

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                // Automatically send the /al command when joining
                mc.player.sendChatMessage("/al");
            }
        });
    }
}
