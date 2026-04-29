package me.dratii.tradr;

import me.dratii.tradr.modmenu.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class BetterMerchant extends MerchantScreen implements AutoTrade {


    public BetterMerchant(MerchantMenu handler, Inventory inv, Component title) {
        super(handler, inv, title);
    }

    @Override
    public void trade(int tradeIndex) {


        MerchantOffers trades = this.menu.getOffers();
        MerchantOffer recipe = trades.get(tradeIndex);
        while (true /*!recipe.isOutOfStock()
                && inputSlotsAreEmpty()
                && hasEnoughItemsInInventory(recipe)
                && canReceiveOutput(recipe.getResult())*/) {
            var b1 = !recipe.isOutOfStock();
            var b2 = inputSlotsAreEmpty();
            var b3 = hasEnoughItemsInInventory(recipe);
            var b4 = canReceiveOutput(recipe.getResult());
            if (b1 && b2 && b3 && b4) {
                transact(recipe);
            } else break;
        }
    }

    private boolean inputSlotsAreEmpty() {
        return this.menu.getSlot(0).container.isEmpty()
                && this.menu.getSlot(1).container.isEmpty()
                && this.menu.getSlot(2).container.isEmpty();

    }

    private boolean hasEnoughItemsInInventory(MerchantOffer recipe) {
        if (!hasEnoughItemsInInventory(recipe.getCostA()))
            return false;
        if (!hasEnoughItemsInInventory(recipe.getCostB()))
            return false;
        return true;
    }

    private boolean hasEnoughItemsInInventory(ItemStack stack) {
        int remaining = stack.getCount();
        for (int i = this.menu.slots.size() - 36; i < this.menu.slots.size(); i++) {
            ItemStack invstack = this.menu.getSlot(i).getItem();
            if (invstack == null)
                continue;
            if (areItemStacksMergable(stack, invstack)) {
                Tradr.LOGGER.info("taking " + invstack.getCount() + " items from slot # " + i);
                remaining -= invstack.getCount();
            }
            if (remaining <= 0)
                return true;
        }
        return false;
    }

    private boolean canReceiveOutput(ItemStack stack) {
        int remaining = stack.getCount();
        for (int i = this.menu.slots.size() - 36; i < this.menu.slots.size(); i++) {
            ItemStack invstack = this.menu.getSlot(i).getItem();
            if (invstack == null || invstack.isEmpty()) {
                Tradr.LOGGER.info("can put result into empty slot " + i);
                return true;
            }
            if (areItemStacksMergable(stack, invstack)
                    && stack.getMaxStackSize() >= stack.getCount() + invstack.getCount()) {
                Tradr.LOGGER.info("Can merge " + (invstack.getMaxStackSize() - invstack.getCount()) + " items with slot " + i);
                remaining -= (invstack.getMaxStackSize() - invstack.getCount());
            }
            if (remaining <= 0)
                return true;
        }
        return false;
    }

    private void transact(MerchantOffer recipe) {
        Tradr.LOGGER.info("fill input slots called");
        int putback0, putback1 = -1;
        putback0 = fillSlot(0, recipe.getCostA());
        putback1 = fillSlot(1, recipe.getCostB());

        getslot(2, recipe.getResult(), putback0, putback1);
        Tradr.LOGGER.info("putting back to slot " + putback0 + " from 0, and to " + putback1 + "from 1");
        if (putback0 != -1) {
            slotClick(0);
            slotClick(putback0);
        }
        if (putback1 != -1) {
            slotClick(1);
            slotClick(putback1);
        }
        // This is a serious hack.
        // ScreenHandler checks:
        //    if (actionType == SlotActionType.SWAP && clickData >= 0 && clickData < 9)
        // so this is a NOP on (a normal) server, but our mixin can watch for it and force an inventory resend.
        this.slotClicked(null, /* slot*/ 0, /* clickData*/ 99, ContainerInput.SWAP);
    }

    /**
     * @param slot  - the number of the (trading) slot that should receive items
     * @param stack - what the trading slot should receive
     * @return the number of the inventory slot into which these items should be put back
     * after the transaction. May be -1 if nothing needs to be put back.
     */
    private int fillSlot(int slot, ItemStack stack) {
        int remaining = stack.getCount();
        for (int i = this.menu.slots.size() - 36; i < this.menu.slots.size(); i++) {
            ItemStack invstack = this.menu.getSlot(i).getItem();
            if (invstack == null)
                continue;
            boolean needPutBack = false;
            if (areItemStacksMergable(stack, invstack)) {
                if (stack.getCount() + invstack.getCount() > stack.getMaxStackSize())
                    needPutBack = true;
                remaining -= invstack.getCount();
                Tradr.LOGGER.info("taking {} items from slot # {}, remaining is now {}", invstack.getCount(), i, remaining);
                slotClick(i);
                slotClick(slot);
            }
            if (needPutBack) {
                slotClick(i);
            }
            if (remaining <= 0)
                return remaining < 0 ? i : -1;
        }
        // We should not be able to arrive here, since hasEnoughItemsInInventory should have been
        // called before fillSlot. But if we do, something went wrong; in this case better do a bit less.
        return -1;
    }

    private boolean areItemStacksMergable(ItemStack a, ItemStack b) {
        if (a == null || b == null)
            return false;
        if (a.getItem() == b.getItem()
                && (!a.isDamaged() || a.getDamageValue() == b.getDamageValue())
                && ItemStack.isSameItem(a, b))
            return true;
        return false;
    }

    private void getslot(int slot, ItemStack stack, int... forbidden) {
        int remaining = stack.getCount();
        slotClick(slot);
        for (int i = this.menu.slots.size() - 36; i < this.menu.slots.size(); i++) {
            ItemStack invstack = this.menu.getSlot(i).getItem();
            if (invstack == null || invstack.isEmpty()) {
                continue;
            }
            if (areItemStacksMergable(stack, invstack)
                    && invstack.getCount() < invstack.getMaxStackSize()
            ) {
                Tradr.LOGGER.info("Can merge " + (invstack.getMaxStackSize() - invstack.getCount()) + " items with slot " + i);
                remaining -= (invstack.getMaxStackSize() - invstack.getCount());
                slotClick(i);
            }
            if (remaining <= 0)
                return;
        }

        // When looking for an empty slot, don't take one that we want to put some input back to.
        for (int i = this.menu.slots.size() - 36; i < this.menu.slots.size(); i++) {
            boolean isForbidden = false;
            for (int f : forbidden) {
                if (i == f)
                    isForbidden = true;
            }
            if (isForbidden)
                continue;
            ItemStack invstack = this.menu.getSlot(i).getItem();
            if (invstack == null || invstack.isEmpty()) {
                slotClick(i);
                Tradr.LOGGER.info("putting result into empty slot " + i);
                return;
            }
        }
    }

    private void slotClick(int slot) {
        Tradr.LOGGER.info("Clicking slot {}", slot);
        this.slotClicked(null, slot, 0, ContainerInput.PICKUP);
    }

    public void autotrade() {
        var recipes = this.menu.getOffers();
        for (int i = 0; i < recipes.size(); i++) {
            var trade = recipes.get(i);
            if (trade.getResult().getItem() == BuiltInRegistries.ITEM.getValue(ConfigScreen.TradeFor)) {
                trade(i);
            }
        }
    }
}
