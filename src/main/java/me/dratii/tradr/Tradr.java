package me.dratii.tradr;

//import baritone.api.BaritoneAPI;

import eu.midnightdust.lib.config.MidnightConfig;
import me.dratii.tradr.modmenu.ConfigScreen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static me.dratii.tradr.Globals.*;

public class Tradr implements ModInitializer {
    public static final String MOD_ID = "Tradr";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static KeyMapping keyBinding;
    private static final KeyMapping.Category TRADR_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tradr", "main"));

    @Override
    public void onInitialize()
    {
        MidnightConfig.init("tradr", ConfigScreen.class);
        keyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "TradrKEY", // The translation key of the keybinding's name
                InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_LEFT_ALT, // The keycode of the key
                TRADR_CATEGORY // The translation key of the keybinding's category.
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.consumeClick()) {
                enabled = !enabled;
                if (enabled) {
                    Minecraft.getInstance().player.sendOverlayMessage(Component.literal("Enabled"));
                } else{
                    Minecraft.getInstance().player.sendOverlayMessage(Component.literal("Disabled"));
                }
            }

            //kurwa czarna magia
            if (client.screen instanceof BetterMerchant betterMerchant && enabled) {
                betterMerchant.autotrade();
                client.screen.onClose();
                openVillager = false;
            }
            if (enabled && !(client.screen instanceof BetterMerchant) && !openVillager) {
                tradeNearbyVillager();
            }
            if (!enabled) {
                tradedVillagers.clear();
            }
        });

    }


    public void tradeNearbyVillager() {
        Minecraft mc = Minecraft.getInstance();
        if(mc.level != null)
         for (Entity entity : mc.level.getEntities().getAll()) {
            if (entity instanceof Villager villagerEntity && !tradedVillagers.contains(villagerEntity) && (villagerEntity.getVillagerData().profession() != Holder.direct(VillagerProfession.NITWIT).value()) && (villagerEntity.getVillagerData().profession() != Holder.direct(VillagerProfession.NONE).value())) {
                Vec3 entityPos = entity.position();
                availableVillagers.add(villagerEntity);
                if(mc.player != null)
                    if (entityPos.distanceTo(mc.player.position()) <= 3 && availableVillagers.contains(villagerEntity)) {
                        mc.player.swing(InteractionHand.MAIN_HAND, true);
                        mc.player.connection
                            .send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                        mc.player.connection
                            .send(new ServerboundInteractPacket(entity.getId(),InteractionHand.MAIN_HAND,entity.getEyePosition(),false));
                        tradedVillagers.add(villagerEntity);
                        openVillager = true;
                        return;
                }
            }
        }
    }
}