package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class CrystalAura extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range").description("Range to look for targets.")
        .defaultValue(10.0).min(1.0).max(15.0).sliderMin(1.0).sliderMax(15.0).build()
    );
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch").description("Auto switch to end crystals.").defaultValue(true).build()
    );
    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
        .name("switch-back").description("Switch back to previous slot after placing.").defaultValue(true).visible(autoSwitch::get).build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-range").description("Range to place crystals.")
        .defaultValue(5.0).min(1.0).max(10.0).sliderMin(1.0).sliderMax(10.0).build()
    );
    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
        .name("place-delay").description("Ticks between each crystal placement.")
        .defaultValue(0).min(0).max(10).sliderMin(0).sliderMax(10).build()
    );
    private final Setting<Double> minPlaceDamage = sgPlace.add(new DoubleSetting.Builder()
        .name("min-damage").description("Minimum damage a crystal must deal to place.")
        .defaultValue(4.0).min(0.0).max(20.0).sliderMin(0.0).sliderMax(20.0).build()
    );
    private final Setting<Boolean> predictMovement = sgPlace.add(new BoolSetting.Builder()
        .name("predict-movement").description("Predicts where the target will move.").defaultValue(true).build()
    );

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-range").description("Range to explode crystals.")
        .defaultValue(5.0).min(1.0).max(10.0).sliderMin(1.0).sliderMax(10.0).build()
    );
    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
        .name("break-delay").description("Ticks between each crystal explosion.")
        .defaultValue(0).min(0).max(10).sliderMin(0).sliderMax(10).build()
    );
    private final Setting<Double> minBreakDamage = sgBreak.add(new DoubleSetting.Builder()
        .name("min-damage").description("Minimum damage a crystal must deal to break.")
        .defaultValue(2.0).min(0.0).max(20.0).sliderMin(0.0).sliderMax(20.0).build()
    );

    private final Setting<Double> maxSelfDamage = sgSafety.add(new DoubleSetting.Builder()
        .name("max-self-damage").description("Maximum damage to deal to yourself.")
        .defaultValue(8.0).min(0.0).max(20.0).sliderMin(0.0).sliderMax(20.0).build()
    );
    private final Setting<Boolean> antiSuicide = sgSafety.add(new BoolSetting.Builder()
        .name("anti-suicide").description("Never place or break if it would kill you.").defaultValue(true).build()
    );
    private final Setting<Boolean> pauseOnEat = sgSafety.add(new BoolSetting.Builder()
        .name("pause-on-eat").description("Pause when eating.").defaultValue(true).build()
    );

    private int placeTimer = 0;
    private int breakTimer = 0;
    private int prevSlot = -1;

    public CrystalAura() {
        super(AddonTemplate.CATEGORY, "abuse-aura", "Fully automatic crystal PVP with customizable place, break and safety settings.");
    }

    @Override
    public void onActivate() {
        placeTimer = 0;
        breakTimer = 0;
        prevSlot = -1;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (pauseOnEat.get() && mc.player.isUsingItem()) return;

        PlayerEntity target = findTarget();
        if (target == null) return;

        // Break
        if (breakTimer <= 0) {
            EndCrystalEntity bestCrystal = findBestCrystal(target);
            if (bestCrystal != null) {
                double selfDmg = estimateDamage(mc.player, bestCrystal.getX(), bestCrystal.getY(), bestCrystal.getZ());
                double targetDmg = estimateDamage(target, bestCrystal.getX(), bestCrystal.getY(), bestCrystal.getZ());
                if (targetDmg >= minBreakDamage.get() && selfDmg <= maxSelfDamage.get()) {
                    if (!antiSuicide.get() || selfDmg < mc.player.getHealth()) {
                        mc.interactionManager.attackEntity(mc.player, bestCrystal);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        breakTimer = breakDelay.get();
                    }
                }
            }
        } else breakTimer--;

        // Place
        if (placeTimer <= 0) {
            int crystalSlot = findCrystalSlot();
            if (crystalSlot == -1) return;

            BlockPos bestPos = findBestPlacement(target);
            if (bestPos != null) {
                double selfDmg = estimateDamage(mc.player, bestPos.getX() + 0.5, bestPos.getY() + 1.0, bestPos.getZ() + 0.5);
                double targetDmg = estimateDamage(target, bestPos.getX() + 0.5, bestPos.getY() + 1.0, bestPos.getZ() + 0.5);
                if (targetDmg >= minPlaceDamage.get() && selfDmg <= maxSelfDamage.get()) {
                    if (!antiSuicide.get() || selfDmg < mc.player.getHealth()) {
                        if (autoSwitch.get()) {
                            prevSlot = mc.player.getInventory().getSelectedSlot();
                            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(crystalSlot));
                        }
                        Vec3d hitVec = new Vec3d(bestPos.getX() + 0.5, bestPos.getY() + 1.0, bestPos.getZ() + 0.5);
                        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, bestPos, false);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                        if (autoSwitch.get() && switchBack.get() && prevSlot != -1) {
                            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
                        }
                        placeTimer = placeDelay.get();
                    }
                }
            }
        } else placeTimer--;
    }

    private PlayerEntity findTarget() {
        PlayerEntity closest = null;
        double closestDist = targetRange.get();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;
            double dist = mc.player.distanceTo(player);
            if (dist < closestDist) { closestDist = dist; closest = player; }
        }
        return closest;
    }

    private EndCrystalEntity findBestCrystal(PlayerEntity target) {
        EndCrystalEntity best = null;
        double bestDamage = minBreakDamage.get();
        for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class,
            mc.player.getBoundingBox().expand(breakRange.get()), e -> true)) {
            if (mc.player.distanceTo(crystal) > breakRange.get()) continue;
            double dmg = estimateDamage(target, crystal.getX(), crystal.getY(), crystal.getZ());
            if (dmg > bestDamage) { bestDamage = dmg; best = crystal; }
        }
        return best;
    }

    private BlockPos findBestPlacement(PlayerEntity target) {
        BlockPos bestPos = null;
        double bestDamage = minPlaceDamage.get();

        Vec3d targetPos;
        if (predictMovement.get()) {
            targetPos = new Vec3d(
                target.getX() + target.getVelocity().x,
                target.getY() + target.getVelocity().y,
                target.getZ() + target.getVelocity().z
            );
        } else {
            targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        }

        BlockPos targetFeet = new BlockPos((int) targetPos.x, (int) targetPos.y, (int) targetPos.z);
        int radius = (int) Math.ceil(placeRange.get());

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = targetFeet.add(x, y, z);
                    if (!isValidPlacement(pos)) continue;

                    Vec3d posCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    double distToPlayer = mc.player.squaredDistanceTo(posCenter.x, posCenter.y, posCenter.z);
                    if (distToPlayer > placeRange.get() * placeRange.get()) continue;

                    double dmg = estimateDamage(target, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    if (dmg > bestDamage) { bestDamage = dmg; bestPos = pos; }
                }
            }
        }
        return bestPos;
    }

    private boolean isValidPlacement(BlockPos pos) {
        var block = mc.world.getBlockState(pos).getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK && block != Blocks.CRYING_OBSIDIAN) return false;
        BlockPos above = pos.up();
        if (!mc.world.getBlockState(above).isAir()) return false;
        if (!mc.world.getBlockState(pos.up(2)).isAir()) return false;
        return mc.world.getOtherEntities(null, new Box(above)).isEmpty();
    }

    private double estimateDamage(PlayerEntity entity, double x, double y, double z) {
        Vec3d entityCenter = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() / 2.0, entity.getZ());
        double dist = entityCenter.distanceTo(new Vec3d(x, y, z));
        double power = 6.0;
        double damage = (1.0 - Math.min(dist / (power * 2), 1.0)) * power * 7.0;
        return damage * 0.6;
    }

    private int findCrystalSlot() {
        if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL)
            return mc.player.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) return i;
        }
        return -1;
    }
}
