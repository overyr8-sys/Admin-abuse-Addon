package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class ShieldBreaker extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Range to detect shielding players.")
        .defaultValue(4.0)
        .min(1.0)
        .max(6.0)
        .sliderMin(1.0)
        .sliderMax(6.0)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("switch-back-delay")
        .description("Ticks to wait before switching back to original item.")
        .defaultValue(5)
        .min(1)
        .max(20)
        .sliderMin(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> onlyLooking = sgGeneral.add(new BoolSetting.Builder()
        .name("only-looking")
        .description("Only break shield when you are looking at the player.")
        .defaultValue(true)
        .build()
    );

    private int prevSlot = -1;
    private int switchBackTimer = 0;
    private boolean switched = false;

    public ShieldBreaker() {
        super(AddonTemplate.CATEGORY, "shield-breaker", "Auto swaps to axe to break enemy shield then switches back.");
    }

    @Override
    public void onActivate() {
        prevSlot = -1;
        switchBackTimer = 0;
        switched = false;
    }

    @Override
    public void onDeactivate() {
        if (switched && prevSlot != -1) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
            switched = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Handle switch back timer
        if (switched && switchBackTimer > 0) {
            switchBackTimer--;
            if (switchBackTimer <= 0) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
                switched = false;
                prevSlot = -1;
            }
            return;
        }

        // Find a shielding player
        AbstractClientPlayerEntity target = findShieldingPlayer();
        if (target == null) return;

        // Check if we already have an axe in hand
        ItemStack held = mc.player.getMainHandStack();
        if (held.getItem() instanceof AxeItem) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            return;
        }

        // Find axe in hotbar
        int axeSlot = findAxeSlot();
        if (axeSlot == -1) return;

        // Save current slot and switch to axe
        prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(axeSlot));
        switched = true;

        // Attack
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        // Start switch back timer
        switchBackTimer = delay.get();
    }

    private AbstractClientPlayerEntity findShieldingPlayer() {
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;
            if (mc.player.distanceTo(player) > range.get()) continue;
            if (!player.isBlocking()) continue;

            if (onlyLooking.get()) {
                if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) continue;
                EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
                if (entityHit.getEntity() != player) continue;
            }

            return player;
        }
        return null;
    }

    private int findAxeSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }
}
