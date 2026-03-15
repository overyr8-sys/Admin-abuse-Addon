package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;

import java.util.List;

public class FrameDupe extends Module {

    public static FrameDupe INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder()
        .name("speed")
        .description("Ticks between each dupe cycle. Lower = faster.")
        .defaultValue(5)
        .min(1)
        .max(20)
        .sliderMin(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> useWhitelist = sgWhitelist.add(new BoolSetting.Builder()
        .name("use-whitelist")
        .description("Only dupe items in the whitelist.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Item>> whitelist = sgWhitelist.add(new ItemListSetting.Builder()
        .name("whitelist")
        .description("Items to dupe. Leave empty to dupe anything.")
        .defaultValue()
        .visible(useWhitelist::get)
        .build()
    );

    private int tickTimer = 0;
    private int state = 0;
    private ItemFrameEntity targetFrame = null;

    public FrameDupe() {
        super(AddonTemplate.CATEGORY, "frame-dupe", "Automatically dupes items using an item frame dupe plugin.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        state = 0;
        tickTimer = 0;
        targetFrame = null;
        info("Frame Dupe enabled. Frames are protected from breaking.");
    }

    @Override
    public void onDeactivate() {
        targetFrame = null;
    }

    public int getState() {
        return state;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        tickTimer--;
        if (tickTimer > 0) return;
        tickTimer = speed.get();

        if (targetFrame == null || !targetFrame.isAlive()) {
            targetFrame = findItemFrame();
        }

        if (targetFrame == null) return;

        switch (state) {
            case 0 -> rightClickFrame();
            case 1 -> leftClickFrame();
        }
    }

    private void rightClickFrame() {
        int slot = findItemSlot();
        if (slot == -1) {
            info("No valid item found in hotbar to dupe!");
            return;
        }

        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));

        mc.interactionManager.interactEntity(
            mc.player,
            targetFrame,
            Hand.MAIN_HAND
        );

        state = 1;
    }

    private void leftClickFrame() {
        mc.interactionManager.attackEntity(mc.player, targetFrame);
        mc.player.swingHand(Hand.MAIN_HAND);
        state = 0;
    }

    private ItemFrameEntity findItemFrame() {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
            if (entityHit.getEntity() instanceof ItemFrameEntity frame) {
                return frame;
            }
        }

        Box searchBox = mc.player.getBoundingBox().expand(4.0);
        List<ItemFrameEntity> frames = mc.world.getEntitiesByClass(
            ItemFrameEntity.class,
            searchBox,
            e -> true
        );

        if (frames.isEmpty()) return null;

        return frames.stream()
            .min((a, b) -> Double.compare(
                a.squaredDistanceTo(mc.player),
                b.squaredDistanceTo(mc.player)
            ))
            .orElse(null);
    }

    private int findItemSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.AIR) continue;

            if (useWhitelist.get() && !whitelist.get().isEmpty()) {
                if (!whitelist.get().contains(stack.getItem())) continue;
            }

            return i;
        }
        return -1;
    }
}

