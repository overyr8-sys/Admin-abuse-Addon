package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.mixin.MixinHandledScreen;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Random;

public class LegitAutoTotem extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLegit = settings.createGroup("Legit Options");

    private final Setting<Integer> minDelay = sgGeneral.add(new IntSetting.Builder()
        .name("min-delay-ms")
        .description("Minimum delay in milliseconds before grabbing totem.")
        .defaultValue(80)
        .min(0)
        .max(500)
        .sliderMin(0)
        .sliderMax(500)
        .build()
    );

    private final Setting<Integer> maxDelay = sgGeneral.add(new IntSetting.Builder()
        .name("max-delay-ms")
        .description("Maximum delay in milliseconds before grabbing totem.")
        .defaultValue(200)
        .min(0)
        .max(1000)
        .sliderMin(0)
        .sliderMax(1000)
        .build()
    );

    private final Setting<Integer> totemSlot = sgGeneral.add(new IntSetting.Builder()
        .name("totem-slot")
        .description("Hotbar slot to check first for totems (1-9). 0 = search all slots.")
        .defaultValue(0)
        .min(0)
        .max(9)
        .sliderMin(0)
        .sliderMax(9)
        .build()
    );

    private final Setting<Boolean> superLegit = sgLegit.add(new BoolSetting.Builder()
        .name("super-legit")
        .description("Opens inventory and waits for you to hover over a totem before equipping.")
        .defaultValue(false)
        .build()
    );

    private enum State {
        IDLE, WAITING_TO_OPEN, INVENTORY_OPEN, WAITING_TO_CLOSE
    }

    private State state = State.IDLE;
    private long actionTime = 0;
    private final Random random = new Random();

    public LegitAutoTotem() {
        super(AddonTemplate.CATEGORY, "legit-auto-totem", "Opens inventory and re-equips totem after a pop with random delay.");
    }

    @Override
    public void onActivate() {
        state = State.IDLE;
    }

    @Override
    public void onDeactivate() {
        if (state == State.INVENTORY_OPEN || state == State.WAITING_TO_CLOSE) {
            if (mc.currentScreen instanceof InventoryScreen) {
                mc.player.closeHandledScreen();
            }
        }
        state = State.IDLE;
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;
        if (mc.world == null || mc.player == null) return;
        if (packet.getStatus() != EntityStatuses.USE_TOTEM_OF_UNDYING) return;
        if (!(packet.getEntity(mc.world) instanceof PlayerEntity player)) return;
        if (player != mc.player) return;

        if (state == State.IDLE) {
            int delay = minDelay.get() + random.nextInt(Math.max(1, maxDelay.get() - minDelay.get() + 1));
            actionTime = System.currentTimeMillis() + delay;
            state = State.WAITING_TO_OPEN;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case WAITING_TO_OPEN -> {
                if (System.currentTimeMillis() < actionTime) return;
                mc.send(() -> mc.setScreen(new InventoryScreen(mc.player)));
                state = State.INVENTORY_OPEN;
            }

            case INVENTORY_OPEN -> {
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
                    state = State.IDLE;
                    return;
                }

                if (superLegit.get()) {
                    Slot hoveredSlot = getSlotUnderMouse(screen);
                    if (hoveredSlot != null && hoveredSlot.getStack().getItem() == Items.TOTEM_OF_UNDYING) {
                        equipTotem(hoveredSlot.id);
                        int delay = minDelay.get() + random.nextInt(Math.max(1, maxDelay.get() - minDelay.get() + 1));
                        actionTime = System.currentTimeMillis() + delay;
                        state = State.WAITING_TO_CLOSE;
                    }
                } else {
                    int foundSlot = findTotemSlot();
                    if (foundSlot == -1) {
                        mc.player.closeHandledScreen();
                        state = State.IDLE;
                        return;
                    }
                    equipTotem(foundSlot);
                    int delay = minDelay.get() + random.nextInt(Math.max(1, maxDelay.get() - minDelay.get() + 1));
                    actionTime = System.currentTimeMillis() + delay;
                    state = State.WAITING_TO_CLOSE;
                }
            }

            case WAITING_TO_CLOSE -> {
                if (System.currentTimeMillis() < actionTime) return;
                if (mc.currentScreen instanceof InventoryScreen) {
                    mc.player.closeHandledScreen();
                }
                state = State.IDLE;
            }
        }
    }

    private Slot getSlotUnderMouse(HandledScreen<?> screen) {
        MixinHandledScreen accessor = (MixinHandledScreen) screen;
        int screenX = accessor.getX();
        int screenY = accessor.getY();

        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

        for (Slot slot : screen.getScreenHandler().slots) {
            int x = screenX + slot.x;
            int y = screenY + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return slot;
            }
        }
        return null;
    }

    private void equipTotem(int slot) {
        mc.interactionManager.clickSlot(
            mc.player.playerScreenHandler.syncId,
            slot,
            40,
            SlotActionType.SWAP,
            mc.player
        );
    }

    private int findTotemSlot() {
        if (totemSlot.get() > 0) {
            int slot = totemSlot.get() - 1;
            if (mc.player.getInventory().getStack(slot).getItem() == Items.TOTEM_OF_UNDYING) return slot;
        }
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) return i;
        }
        return -1;
    }
}
