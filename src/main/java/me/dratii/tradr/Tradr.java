package me.dratii.tradr;

//import baritone.api.BaritoneAPI;

import eu.midnightdust.lib.config.MidnightConfig;
import me.dratii.tradr.modmenu.ConfigScreen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import static me.dratii.tradr.Globals.*;

public class Tradr implements ModInitializer {

    private static KeyBinding keyBinding;
    private static final KeyBinding.Category TRADR_CATEGORY = KeyBinding.Category.create(Identifier.of("tradr", "main"));

    @Override
    public void onInitialize()
    {
        MidnightConfig.init("tradr", ConfigScreen.class);
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "TradrKEY", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_LEFT_ALT, // The keycode of the key
                TRADR_CATEGORY // The translation key of the keybinding's category.
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                enabled = !enabled;
                if (enabled) {
                    MinecraftClient.getInstance().player.sendMessage(Text.of("Enabled"),true);
                } else{
                    MinecraftClient.getInstance().player.sendMessage(Text.of("Disabled"),true);
                }
            }

            //kurwa czarna magia
            if (client.currentScreen instanceof BetterMerchant betterMerchant && enabled) {
                betterMerchant.autotrade();
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
                Vec3d entityPos = entity.getEntityPos();
                availableVillagers.add(villagerEntity);
                assert mc.player != null;
                if (entityPos.distanceTo(mc.player.getEntityPos()) <= 3 && availableVillagers.contains(villagerEntity)) {
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