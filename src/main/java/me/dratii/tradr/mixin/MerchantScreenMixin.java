package me.dratii.tradr.mixin;


import me.dratii.tradr.AutoTrade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
    @Shadow
    private int shopItem;

    public MerchantScreenMixin(MerchantMenu merchantContainer_1, Inventory playerInventory_1, Component text_1) {
        super(merchantContainer_1, playerInventory_1, text_1);
    }

    @Inject(method = "postButtonClick", at = @At("RETURN"))
    public void tradeOnSetRecipeIndex(CallbackInfo ci) {
        this.slotClicked(null, 0, 0, ContainerInput.QUICK_MOVE);
        this.slotClicked(null, 1, 0, ContainerInput.QUICK_MOVE);

        ((AutoTrade) this).trade(shopItem);
    }
}