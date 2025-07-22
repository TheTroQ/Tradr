package me.dratii.tradr;

import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class BetterMerchant extends MerchantScreen implements AutoTrade {


    public BetterMerchant(MerchantScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
    }

    @Override
    public void trade(int tradeIndex) {


        TradeOfferList trades = handler.getRecipes();
        TradeOffer recipe = trades.get(tradeIndex);
        while (!recipe.isDisabled()
                && client.player.currentScreenHandler.getCursorStack().isEmpty()
                && inputSlotsAreEmpty()
                && hasEnoughItemsInInventory(recipe)
                && canReceiveOutput(recipe.getSellItem())) {
            transact(recipe);
        }
    }

    private boolean inputSlotsAreEmpty() {
        boolean result =
                handler.getSlot(0).getStack().isEmpty()
                        && handler.getSlot(1).getStack().isEmpty()
                        && handler.getSlot(2).getStack().isEmpty();
        return result;

    }

    private boolean hasEnoughItemsInInventory(TradeOffer recipe) {
        if (!hasEnoughItemsInInventory(recipe.getDisplayedFirstBuyItem()))
            return false;
        if (!hasEnoughItemsInInventory(recipe.getDisplayedSecondBuyItem()))
            return false;
        return true;
    }

    private boolean hasEnoughItemsInInventory(ItemStack stack) {
        int remaining = stack.getCount();
        for (int i = handler.slots.size() - 36; i < handler.slots.size(); i++) {
            ItemStack invstack = handler.getSlot(i).getStack();
            if (invstack == null)
                continue;
            if (areItemStacksMergable(stack, invstack)) {
                //System.out.println("taking "+invstack.getCount()+" items from slot # "+i);
                remaining -= invstack.getCount();
            }
            if (remaining <= 0)
                return true;
        }
        return false;
    }

    private boolean canReceiveOutput(ItemStack stack) {
        int remaining = stack.getCount();
        for (int i = handler.slots.size() - 36; i < handler.slots.size(); i++) {
            ItemStack invstack = handler.getSlot(i).getStack();
            if (invstack == null || invstack.isEmpty()) {
                //System.out.println("can put result into empty slot "+i);
                return true;
            }
            if (areItemStacksMergable(stack, invstack)
                    && stack.getMaxCount() >= stack.getCount() + invstack.getCount()) {
                //System.out.println("Can merge "+(invstack.getMaxStackSize()-invstack.getCount())+" items with slot "+i);
                remaining -= (invstack.getMaxCount() - invstack.getCount());
            }
            if (remaining <= 0)
                return true;
        }
        return false;
    }

    private void transact(TradeOffer recipe) {
        //System.out.println("fill input slots called");
        int putback0, putback1 = -1;
        putback0 = fillSlot(0, recipe.getDisplayedFirstBuyItem());
        putback1 = fillSlot(1, recipe.getDisplayedSecondBuyItem());

        getslot(2, recipe.getSellItem(), putback0, putback1);
        //System.out.println("putting back to slot "+putback0+" from 0, and to "+putback1+"from 1");
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
        this.onMouseClick(null, /* slot*/ 0, /* clickData*/ 99, SlotActionType.SWAP);
    }

    /**
     * @param slot  - the number of the (trading) slot that should receive items
     * @param stack - what the trading slot should receive
     * @return the number of the inventory slot into which these items should be put back
     * after the transaction. May be -1 if nothing needs to be put back.
     */
    private int fillSlot(int slot, ItemStack stack) {
        int remaining = stack.getCount();
        for (int i = handler.slots.size() - 36; i < handler.slots.size(); i++) {
            ItemStack invstack = handler.getSlot(i).getStack();
            if (invstack == null)
                continue;
            boolean needPutBack = false;
            if (areItemStacksMergable(stack, invstack)) {
                if (stack.getCount() + invstack.getCount() > stack.getMaxCount())
                    needPutBack = true;
                remaining -= invstack.getCount();
                // System.out.println("taking "+invstack.getCount()+" items from slot # "+i+", remaining is now "+remaining);
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
                && (!a.isDamageable() || a.getDamage() == b.getDamage())
                && ItemStack.areItemsAndComponentsEqual(a, b))
            return true;
        return false;
    }

    private void getslot(int slot, ItemStack stack, int... forbidden) {
        int remaining = stack.getCount();
        slotClick(slot);
        for (int i = handler.slots.size() - 36; i < handler.slots.size(); i++) {
            ItemStack invstack = handler.getSlot(i).getStack();
            if (invstack == null || invstack.isEmpty()) {
                continue;
            }
            if (areItemStacksMergable(stack, invstack)
                    && invstack.getCount() < invstack.getMaxCount()
            ) {
                // System.out.println("Can merge "+(invstack.getMaxStackSize()-invstack.getCount())+" items with slot "+i);
                remaining -= (invstack.getMaxCount() - invstack.getCount());
                slotClick(i);
            }
            if (remaining <= 0)
                return;
        }

        // When looking for an empty slot, don't take one that we want to put some input back to.
        for (int i = handler.slots.size() - 36; i < handler.slots.size(); i++) {
            boolean isForbidden = false;
            for (int f : forbidden) {
                if (i == f)
                    isForbidden = true;
            }
            if (isForbidden)
                continue;
            ItemStack invstack = handler.getSlot(i).getStack();
            if (invstack == null || invstack.isEmpty()) {
                slotClick(i);
                // System.out.println("putting result into empty slot "+i);
                return;
            }
        }
    }

    private void slotClick(int slot) {
        // System.out.println("Clicking slot "+slot);
        this.onMouseClick(null, slot, 0, SlotActionType.PICKUP);
    }

    public void cos() {
            var recipes = this.handler.getRecipes();
            for (int i = 0; i < recipes.size(); i++) {
                var trade = recipes.get(i);
                if (trade.getSellItem().getItem() == Items.EMERALD) {
                    trade(i);
                }
            }
    }
}
