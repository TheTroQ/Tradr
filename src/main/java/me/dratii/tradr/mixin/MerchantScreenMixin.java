package me.dratii.tradr.mixin;


import me.dratii.tradr.AutoTrade;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends HandledScreen<MerchantScreenHandler> {
    @Shadow
    private int selectedIndex;

    public MerchantScreenMixin(MerchantScreenHandler merchantContainer_1, PlayerInventory playerInventory_1, Text text_1) {
        super(merchantContainer_1, playerInventory_1, text_1);
    }

    @Inject(method = "syncRecipeIndex", at = @At("RETURN"))
    public void tradeOnSetRecipeIndex(CallbackInfo ci) {
        this.onMouseClick(null, 0, 0, SlotActionType.QUICK_MOVE);
        this.onMouseClick(null, 1, 0, SlotActionType.QUICK_MOVE);

        ((AutoTrade) this).trade(selectedIndex);
    }
}