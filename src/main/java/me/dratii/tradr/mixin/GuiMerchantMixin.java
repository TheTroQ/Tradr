package me.dratii.tradr.mixin;

import me.dratii.tradr.BetterMerchant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MenuScreens.class)
public abstract class GuiMerchantMixin {

    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void displayVillagerTradeGui(MenuType type, Minecraft minecraft,
                                                int containerId, Component title, CallbackInfo ci) {

        if (type == MenuType.MERCHANT) {
            MerchantMenu container = MenuType.MERCHANT.create(containerId, minecraft.player.getInventory());
            BetterMerchant screen = new BetterMerchant(container, minecraft.player.getInventory(), title);
            minecraft.player.containerMenu = container;
            minecraft.setScreen(screen);
            ci.cancel();
        }
    }
}