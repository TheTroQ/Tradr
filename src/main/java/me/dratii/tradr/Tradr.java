package me.dratii.tradr;

//import baritone.api.BaritoneAPI;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import static me.dratii.tradr.Globals.*;

public class Tradr implements ModInitializer {

    private static KeyBinding keyBinding;

    @Override
    public void onInitialize()
    {
        var mm = MiniMessage.miniMessage();

        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "TradrKEY", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_LEFT_ALT, // The keycode of the key
                "Tradr" // The translation key of the keybinding's category.
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                player = MinecraftClient.getInstance().player;
                Component parsed;
                enabled = !enabled;
                if (enabled) {
                    parsed = mm.deserialize("AutoTrader: <Green> Enabled");


                } else parsed = mm.deserialize("AutoTrader: <red>Disabled");
                assert player != null;
                player.sendActionBar(parsed);
            }

            //kurwa czarna magia
            if (client.currentScreen instanceof BetterMerchant betterMerchant && enabled) {
                betterMerchant.cos();
                client.currentScreen.close();
                openVillager = false;
            }
            if (enabled && !(client.currentScreen instanceof BetterMerchant) && !openVillager) {
                tradeNearbyVillager();
            }
            if (!enabled) {
                tradedVillagers.clear();
            }
        });

    }


    public void tradeNearbyVillager() {
        MinecraftClient mc = MinecraftClient.getInstance();
        assert mc.world != null;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof VillagerEntity villagerEntity && !tradedVillagers.contains(villagerEntity)) {
                Vec3d entityPos = entity.getPos();
                availableVillagers.add(villagerEntity);
                assert mc.player != null;
                if (entityPos.distanceTo(mc.player.getPos()) <= 3 && availableVillagers.contains(villagerEntity)) {
                    mc.player.swingHand(Hand.MAIN_HAND, true);
                    mc.player.networkHandler
                            .sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    mc.player.networkHandler
                            .sendPacket(PlayerInteractEntityC2SPacket.interact(entity, false, Hand.MAIN_HAND));
                    tradedVillagers.add(villagerEntity);
                    openVillager = true;
                    return;
                }
            }
        }
    }
}