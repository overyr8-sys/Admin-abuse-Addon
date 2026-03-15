package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class HoleSnipe extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Range to look for targets.")
        .defaultValue(5.0)
        .min(1.0)
        .max(10.0)
        .sliderMin(1.0)
        .sliderMax(10.0)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between each snipe attempt.")
        .defaultValue(1)
        .min(0)
        .max(10)
        .sliderMin(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Auto switch to end crystals.")
        .defaultValue(true)
        .build()
    );

    private int tickTimer = 0;

    public HoleSnipe() {
        super(AddonTemplate.CATEGORY, "hole-snipe", "Places a crystal in the gap of an enemy's surround.");
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

        // Find crystal slot
        int crystalSlot = -1;
        if (autoSwitch.get()) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) {
                    crystalSlot = i;
                    break;
                }
            }
            if (crystalSlot == -1) return;
        } else {
            if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) return;
        }

        // Find target and their surround gap
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;
            if (mc.player.distanceTo(player) > range.get()) continue;

            BlockPos gap = findSurroundGap(player);
            if (gap == null) continue;

            // The gap block itself - place crystal on the obsidian below the gap
            BlockPos below = gap.down();

            // Make sure the gap is air and below is solid
            if (!mc.world.getBlockState(gap).isAir()) continue;
            if (mc.world.getBlockState(below).isAir()) continue;

            // Make sure no crystal already there
            if (!mc.world.getOtherEntities(null, new Box(gap)).isEmpty()) continue;

            // Switch to crystals
            if (autoSwitch.get()) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(crystalSlot));
            }

            // Place crystal in the gap
            Vec3d hitVec = new Vec3d(gap.getX() + 0.5, gap.getY(), gap.getZ() + 0.5);
            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, below, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            mc.player.swingHand(Hand.MAIN_HAND);
            break;
        }
    }

    private BlockPos findSurroundGap(PlayerEntity player) {
        BlockPos feet = player.getBlockPos();

        // Check all 4 sides at feet level
        Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

        BlockPos gapPos = null;
        int solidCount = 0;

        for (Direction dir : sides) {
            BlockPos side = feet.offset(dir);
            var block = mc.world.getBlockState(side).getBlock();

            boolean isSolid = block == Blocks.OBSIDIAN ||
                block == Blocks.BEDROCK ||
                block == Blocks.CRYING_OBSIDIAN ||
                block == Blocks.NETHERITE_BLOCK ||
                !mc.world.getBlockState(side).isAir();

            if (isSolid) {
                solidCount++;
            } else {
                // This is a gap - check if there's a solid block below it to place crystal on
                BlockPos belowGap = side.down();
                if (!mc.world.getBlockState(belowGap).isAir()) {
                    gapPos = side;
                }
            }
        }

        // Only snipe if there's exactly 1 gap (surround with one block missing)
        if (solidCount == 3 && gapPos != null) {
            return gapPos;
        }

        return null;
    }
}
