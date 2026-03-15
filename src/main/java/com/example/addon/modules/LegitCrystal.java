package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class LegitCrystal extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Ticks between placing crystals. Lower = faster.")
        .defaultValue(2)
        .min(0)
        .max(10)
        .sliderMin(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Integer> explodeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("explode-delay")
        .description("Ticks to wait before exploding a placed crystal.")
        .defaultValue(1)
        .min(0)
        .max(10)
        .sliderMin(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Double> explodeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("explode-range")
        .description("Range to explode nearby crystals.")
        .defaultValue(5.0)
        .min(1.0)
        .max(10.0)
        .sliderMin(1.0)
        .sliderMax(10.0)
        .build()
    );

    private final Setting<Boolean> requireRightClick = sgGeneral.add(new BoolSetting.Builder()
        .name("require-right-click")
        .description("Only activate when you are holding right click.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyObsidian = sgGeneral.add(new BoolSetting.Builder()
        .name("only-obsidian")
        .description("Only place on obsidian and bedrock.")
        .defaultValue(true)
        .build()
    );

    private int placeTimer = 0;
    private int explodeTimer = 0;
    private BlockPos lastPlaced = null;

    public LegitCrystal() {
        super(AddonTemplate.CATEGORY, "legit-crystal", "Legit-looking auto crystal that activates on right click.");
    }

    @Override
    public void onActivate() {
        placeTimer = 0;
        explodeTimer = 0;
        lastPlaced = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Must be holding end crystals
        if (!isHoldingCrystals()) return;

        // Must be holding right click if setting is on
        if (requireRightClick.get() && !mc.options.useKey.isPressed()) return;

        // Must be looking at a valid block
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult blockHit = (BlockHitResult) mc.crosshairTarget;
        BlockPos targetPos = blockHit.getBlockPos();

        // Check if block is obsidian/bedrock
        if (onlyObsidian.get()) {
            var block = mc.world.getBlockState(targetPos).getBlock();
            if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK && block != Blocks.CRYING_OBSIDIAN) return;
        }

        // Explode nearby crystals first
        if (explodeTimer <= 0) {
            explodeNearbyCrystals();
            explodeTimer = explodeDelay.get();
        } else {
            explodeTimer--;
        }

        // Place crystal
        if (placeTimer <= 0) {
            placeCrystal(targetPos, blockHit);
            placeTimer = placeDelay.get();
        } else {
            placeTimer--;
        }
    }

    private void placeCrystal(BlockPos blockPos, BlockHitResult hitResult) {
        BlockPos placePos = blockPos.up();

        // Check if spot is clear
        if (!mc.world.getBlockState(placePos).isAir()) return;
        if (!mc.world.getOtherEntities(null, new Box(placePos)).isEmpty()) return;

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastPlaced = placePos;
    }

    private void explodeNearbyCrystals() {
        List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(
            EndCrystalEntity.class,
            mc.player.getBoundingBox().expand(explodeRange.get()),
            e -> true
        );

        for (EndCrystalEntity crystal : crystals) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private boolean isHoldingCrystals() {
        return mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL ||
            mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
    }
}
