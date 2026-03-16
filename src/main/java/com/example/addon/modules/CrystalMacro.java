package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class CrystalMacro extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLegit = settings.createGroup("Legit");

    private final Setting<Double> placeDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-delay")
        .description("Delay in ticks between placing crystals.")
        .defaultValue(1.0)
        .min(0.0)
        .max(20.0)
        .sliderMax(20.0)
        .build()
    );

    private final Setting<Double> breakDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("break-delay")
        .description("Delay in ticks between breaking crystals.")
        .defaultValue(1.0)
        .min(0.0)
        .max(20.0)
        .sliderMax(20.0)
        .build()
    );

    private final Setting<Boolean> stopOnKill = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-on-kill")
        .description("Pauses the macro when a nearby player dies, then resumes after 5 seconds.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> placeObsidian = sgGeneral.add(new BoolSetting.Builder()
        .name("place-obsidian")
        .description("Places obsidian if the target block isn't obsidian or bedrock.")
        .defaultValue(true)
        .build()
    );

    // Legit settings
    private final Setting<Boolean> randomDelay = sgLegit.add(new BoolSetting.Builder()
        .name("random-delay")
        .description("Adds a random extra delay to look more legit.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> randomDelayMax = sgLegit.add(new IntSetting.Builder()
        .name("random-delay-max")
        .description("Maximum extra random delay in ticks.")
        .defaultValue(2)
        .min(0)
        .max(10)
        .sliderMin(0)
        .sliderMax(10)
        .visible(randomDelay::get)
        .build()
    );

    private final Setting<Boolean> onlyWhenHolding = sgLegit.add(new BoolSetting.Builder()
        .name("only-when-holding-crystal")
        .description("Only runs when you are holding end crystals.")
        .defaultValue(true)
        .build()
    );

    private int placeDelayCounter = 0;
    private int breakDelayCounter = 0;
    private final Set<PlayerEntity> deadPlayers = new HashSet<>();
    private boolean paused = false;
    private long resumeTime = 0;
    private final Random random = new Random();

    public CrystalMacro() {
        super(AddonTemplate.CATEGORY, "crystal-macro", "Automatically crystals fast. Hold right click while looking at obsidian/bedrock.");
    }

    @Override
    public void onActivate() {
        placeDelayCounter = 0;
        breakDelayCounter = 0;
        deadPlayers.clear();
        paused = false;
        resumeTime = 0;
    }

    @Override
    public void onDeactivate() {
        placeDelayCounter = 0;
        breakDelayCounter = 0;
        deadPlayers.clear();
        paused = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        // Tick down counters
        if (placeDelayCounter > 0) placeDelayCounter--;
        if (breakDelayCounter > 0) breakDelayCounter--;

        // Handle pause resume
        if (paused && System.currentTimeMillis() >= resumeTime) {
            paused = false;
            info("Resumed after kill pause.");
        }
        if (paused) return;

        // Must be holding crystals if setting is on
        if (onlyWhenHolding.get() && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) return;

        // Must be holding right click
        if (!mc.options.useKey.isPressed()) return;

        if (mc.player.isUsingItem()) return;

        // Stop on kill check
        if (stopOnKill.get() && checkForDeadPlayers()) {
            paused = true;
            resumeTime = System.currentTimeMillis() + 5000;
            info("Paused due to nearby player death. Resuming in 5s.");
            return;
        }

        handleInteraction();
    }

    private void handleInteraction() {
        HitResult target = mc.crosshairTarget;

        if (target instanceof BlockHitResult blockHit) {
            handleBlockInteraction(blockHit);
        } else if (target instanceof EntityHitResult entityHit) {
            handleEntityInteraction(entityHit);
        }
    }

    private void handleBlockInteraction(BlockHitResult blockHit) {
        if (blockHit.getType() != HitResult.Type.BLOCK) return;
        if (placeDelayCounter > 0) return;

        BlockPos pos = blockHit.getBlockPos();
        boolean isValidBase = mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN ||
            mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK ||
            mc.world.getBlockState(pos).getBlock() == Blocks.CRYING_OBSIDIAN;

        if (!isValidBase && placeObsidian.get()) {
            int obsSlot = findInHotbar(Items.OBSIDIAN);
            int crystalSlot = findInHotbar(Items.END_CRYSTAL);
            if (obsSlot == -1 || crystalSlot == -1) return;

            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(obsSlot));
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(crystalSlot));

            setPlaceDelay();
            return;
        }

        if (!isValidBase) return;
        if (!isValidPlacement(pos)) return;

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
        setPlaceDelay();
    }

    private void handleEntityInteraction(EntityHitResult entityHit) {
        if (breakDelayCounter > 0) return;

        Entity entity = entityHit.getEntity();
        if (!(entity instanceof EndCrystalEntity)) return;

        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
        setBreakDelay();
    }

    private boolean isValidPlacement(BlockPos pos) {
        BlockPos up = pos.up();
        if (!mc.world.getBlockState(up).isAir()) return false;
        int x = up.getX(), y = up.getY(), z = up.getZ();
        return mc.world.getOtherEntities(null, new Box(x, y, z, x + 1.0, y + 2.0, z + 1.0)).isEmpty();
    }

    private boolean checkForDeadPlayers() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if ((player.isDead() || player.getHealth() <= 0) && !deadPlayers.contains(player)) {
                deadPlayers.add(player);
                return true;
            }
        }
        deadPlayers.removeIf(p -> !p.isDead() && p.getHealth() > 0);
        return false;
    }

    private void setPlaceDelay() {
        int extra = randomDelay.get() ? random.nextInt(randomDelayMax.get() + 1) : 0;
        placeDelayCounter = placeDelay.get().intValue() + extra;
    }

    private void setBreakDelay() {
        int extra = randomDelay.get() ? random.nextInt(randomDelayMax.get() + 1) : 0;
        breakDelayCounter = breakDelay.get().intValue() + extra;
    }

    private int findInHotbar(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }
}
