package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class TNTPlacer extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Radius around you to place TNT.")
        .defaultValue(3)
        .min(1)
        .max(5)
        .sliderMin(1)
        .sliderMax(5)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between each TNT placement.")
        .defaultValue(1)
        .min(0)
        .max(10)
        .sliderMin(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> ignite = sgGeneral.add(new BoolSetting.Builder()
        .name("ignite")
        .description("Ignite TNT after placing with flint and steel.")
        .defaultValue(true)
        .build()
    );

    private int tickTimer = 0;

    public TNTPlacer() {
        super(AddonTemplate.CATEGORY, "tnt-placer", "Places TNT in a radius around and below you.");
    }

    @Override
    public void onActivate() {
        tickTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        tickTimer--;
        if (tickTimer > 0) return;
        tickTimer = delay.get();

        int tntSlot = findInHotbar(Items.TNT);
        if (tntSlot == -1) {
            error("No TNT in hotbar!");
            toggle();
            return;
        }

        int flintSlot = ignite.get() ? findInHotbar(Items.FLINT_AND_STEEL) : -1;

        BlockPos playerPos = mc.player.getBlockPos();
        int r = radius.get();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                // Try placing at player level and one below
                for (int y = 0; y >= -1; y--) {
                    BlockPos placePos = playerPos.add(x, y, z);
                    BlockPos below = placePos.down();

                    // Skip player's own position
                    if (placePos.equals(playerPos) || placePos.equals(playerPos.up())) continue;

                    // Must be air
                    if (!mc.world.getBlockState(placePos).isAir()) continue;

                    // Block below must be solid
                    if (!mc.world.getBlockState(below).isSolidBlock(mc.world, below)) continue;

                    // Place TNT
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(tntSlot));
                    Vec3d hitVec = new Vec3d(below.getX() + 0.5, below.getY() + 1.0, below.getZ() + 0.5);
                    BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, below, false);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                    mc.player.swingHand(Hand.MAIN_HAND);

                    // Ignite
                    if (ignite.get() && flintSlot != -1) {
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(flintSlot));
                        Vec3d igniteHit = new Vec3d(placePos.getX() + 0.5, placePos.getY() + 1, placePos.getZ() + 0.5);
                        BlockHitResult igniteResult = new BlockHitResult(igniteHit, Direction.UP, placePos, false);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, igniteResult);
                    }
                    break;
                }
            }
        }
    }

    private int findInHotbar(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }
}
