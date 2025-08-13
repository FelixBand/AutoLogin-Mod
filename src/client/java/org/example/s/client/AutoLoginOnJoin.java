package org.example.s.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class AutoLoginOnJoin {

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                // Send the /al command as chat
                mc.player.sendMessage(Text.of("/al"), false);
            }
        });
    }
}