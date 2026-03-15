package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class HotbarManager extends Module {

    private final SettingGroup sgSlot1 = settings.createGroup("Slot 1");
    private final SettingGroup sgSlot2 = settings.createGroup("Slot 2");
    private final SettingGroup sgSlot3 = settings.createGroup("Slot 3");
    private final SettingGroup sgSlot4 = settings.createGroup("Slot 4");
    private final SettingGroup sgSlot5 = settings.createGroup("Slot 5");
    private final SettingGroup sgSlot6 = settings.createGroup("Slot 6");
    private final SettingGroup sgSlot7 = settings.createGroup("Slot 7");
    private final SettingGroup sgSlot8 = settings.createGroup("Slot 8");
    private final SettingGroup sgSlot9 = settings.createGroup("Slot 9");

    // Slot 1
    private final Setting<Boolean> slot1Enable = sgSlot1.add(new BoolSetting.Builder().name("enabled").defaultValue(false).build());
    private final Setting<List<Item>> slot1Item = sgSlot1.add(new ItemListSetting.Builder().name("item").description("Item to keep in slot 1.").defaultValue().visible(slot1Enable::get).build());

    // Slot 2
    private final Setting<Boolean> slot2Enable = sgSlot2.add(new BoolSetting.Builder().name("enabled").defaultValue(false).build());
    private final Setting<List<Item>> slot2Item = sgSlot2.add(new ItemListSetting.Builder().name("item").description("Item to keep in slot 2.").defaultValue().visible(slot2Enable::get).build());

    // Slot 3
    private final Setting<Boolean> slot3Enable = sgSlot3.add(new BoolSetting.Builder().name("enabled").defaultValue(false).build());
    private final Setting<List<Item>> slot3Item = sgSlot3.add(new ItemListSetting.Builder().name("item").description("Item to keep in slot 3.").defaultValue().visible(slot3Enable::get).build());

    // Slot 4
    private final Setting<Boolean> slot4Enable = sgSlot4.add(new BoolSetting.Builder().name("enabled").defaultValue(false).build());
    private final Setting<List<Item>> slot4Item = sgSlot4.add(new ItemListSetting.Builder().name("item").description("Item to keep in slot 4.").defaultValue().visible(slot4Enable::get).build());

    // Slot 5
    private final Setting<Boolean> slot5Enable = sgSlot5.add(new BoolSetting.Builder().name("enabled").defaultValue(false).build());
    private final Setting<List<Item>> slot5Item = sgSlot5.add(new ItemListSetting.Builder().name("item").description("Item to keep in slot 5.").defaultValue().visible(slot5Enable::get).build());

    // Slot 6
    private final Setting<Boolean> slot6Enable = sgSlot6.add(new BoolSetting.Builder().name("enabled").defaultValue(false).build());
    private final Setting<List<Item>> slot6Item = sgSlot6.add(new ItemListSetting.Builder().name("item").description("Item to keep in slot 6.").defaultValue().visible(slot6Enable::get).build());

    // Slot 7
    private final Setting<Boolean> slot7Enable = sgSlot7.add(new BoolSetting.Builder().name("enabled").defaultValue(false).build());
    private final Setting<List<Item>> slot7Item = sgSlot7.add(new ItemListSetting.Builder().name("item").description("Item to keep in slot 7.").defaultValue().visible(slot7Enable::get).build());

    // Slot 8
    private final Setting<Boolean> slot8Enable = sgSlot8.add(new BoolSetting.Builder().name("enabled").defaultValue(false).build());
    private final Setting<List<Item>> slot8Item = sgSlot8.add(new ItemListSetting.Builder().name("item").description("Item to keep in slot 8.").defaultValue().visible(slot8Enable::get).build());

    // Slot 9
    private final Setting<Boolean> slot9Enable = sgSlot9.add(new BoolSetting.Builder().name("enabled").defaultValue(false).build());
    private final Setting<List<Item>> slot9Item = sgSlot9.add(new ItemListSetting.Builder().name("item").description("Item to keep in slot 9.").defaultValue().visible(slot9Enable::get).build());

    private final Setting<Boolean>[] enables;
    private final Setting<List<Item>>[] items;

    @SuppressWarnings("unchecked")
    public HotbarManager() {
        super(AddonTemplate.CATEGORY, "hotbar-manager", "Automatically fills hotbar slots with items from your inventory.");
        enables = new Setting[]{slot1Enable, slot2Enable, slot3Enable, slot4Enable, slot5Enable, slot6Enable, slot7Enable, slot8Enable, slot9Enable};
        items = new Setting[]{slot1Item, slot2Item, slot3Item, slot4Item, slot5Item, slot6Item, slot7Item, slot8Item, slot9Item};
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return; // don't run while inventory is open

        for (int slot = 0; slot < 9; slot++) {
            if (!enables[slot].get()) continue;
            List<Item> wantedItems = items[slot].get();
            if (wantedItems.isEmpty()) continue;

            // Check if hotbar slot already has the right item
            ItemStack current = mc.player.getInventory().getStack(slot);
            if (!current.isEmpty() && wantedItems.contains(current.getItem())) continue;

            // Find item in inventory (slots 9-35)
            int invSlot = findInInventory(wantedItems);
            if (invSlot == -1) continue;

            // Swap inventory slot to hotbar slot using pick up packet
            // invSlot is inventory slot 9-35, convert to screen slot
            int screenInvSlot = invSlot; // inventory slots are already screen slots in player inventory
            mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                screenInvSlot,
                slot, // hotbar number 0-8
                SlotActionType.SWAP,
                mc.player
            );
        }
    }

    private int findInInventory(List<Item> wantedItems) {
        // Search inventory slots 9-35 (not hotbar)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && wantedItems.contains(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }
}
