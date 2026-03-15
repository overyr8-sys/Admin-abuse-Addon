package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class LegitKillAura extends Module {

    private final SettingGroup sgAimAssist = settings.createGroup("Aim Assist");
    private final SettingGroup sgTriggerBot = settings.createGroup("Trigger Bot");

    private final Setting<Boolean> aimAssist = sgAimAssist.add(new BoolSetting.Builder()
        .name("aim-assist")
        .description("Slowly moves your aim towards the nearest player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> aimRange = sgAimAssist.add(new DoubleSetting.Builder()
        .name("aim-range")
        .description("Range to aim at players.")
        .defaultValue(4.0)
        .min(1.0)
        .max(6.0)
        .sliderMin(1.0)
        .sliderMax(6.0)
        .visible(aimAssist::get)
        .build()
    );

    private final Setting<Double> aimSpeed = sgAimAssist.add(new DoubleSetting.Builder()
        .name("aim-speed")
        .description("How fast to move aim towards target. Lower = more legit.")
        .defaultValue(3.0)
        .min(0.5)
        .max(10.0)
        .sliderMin(0.5)
        .sliderMax(10.0)
        .visible(aimAssist::get)
        .build()
    );

    private final Setting<Double> fov = sgAimAssist.add(new DoubleSetting.Builder()
        .name("fov")
        .description("Field of view to check for targets (degrees).")
        .defaultValue(60.0)
        .min(10.0)
        .max(180.0)
        .sliderMin(10.0)
        .sliderMax(180.0)
        .visible(aimAssist::get)
        .build()
    );

    private final Setting<Boolean> triggerBot = sgTriggerBot.add(new BoolSetting.Builder()
        .name("trigger-bot")
        .description("Auto clicks when crosshair is on a player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> triggerDelay = sgTriggerBot.add(new IntSetting.Builder()
        .name("trigger-delay")
        .description("Ticks to wait before clicking. Higher = more legit.")
        .defaultValue(3)
        .min(0)
        .max(10)
        .sliderMin(0)
        .sliderMax(10)
        .visible(triggerBot::get)
        .build()
    );

    private final Setting<Integer> attackCooldown = sgTriggerBot.add(new IntSetting.Builder()
        .name("attack-cooldown")
        .description("Minimum ticks between attacks.")
        .defaultValue(10)
        .min(1)
        .max(20)
        .sliderMin(1)
        .sliderMax(20)
        .visible(triggerBot::get)
        .build()
    );

    private int triggerTimer = 0;
    private int cooldownTimer = 0;

    public LegitKillAura() {
        super(AddonTemplate.CATEGORY, "legit-kill-aura", "Aim assist and trigger bot for legit-looking combat.");
    }

    @Override
    public void onActivate() {
        triggerTimer = 0;
        cooldownTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (cooldownTimer > 0) cooldownTimer--;

        PlayerEntity target = findTarget();

        if (aimAssist.get() && target != null) {
            aimAt(target);
        }

        if (triggerBot.get()) {
            if (isLookingAtPlayer()) {
                triggerTimer++;
                if (triggerTimer >= triggerDelay.get() && cooldownTimer <= 0) {
                    attack();
                    triggerTimer = 0;
                    cooldownTimer = attackCooldown.get();
                }
            } else {
                triggerTimer = 0;
            }
        }
    }

    private void aimAt(PlayerEntity target) {
        Vec3d eyePos = mc.player.getEyePos();
        double tx = target.getX();
        double ty = target.getY() + target.getHeight() * 0.6;
        double tz = target.getZ();

        double dx = tx - eyePos.x;
        double dy = ty - eyePos.y;
        double dz = tz - eyePos.z;

        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float)(Math.toDegrees(Math.atan2(-dx, dz)));
        float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float speed = (float)(aimSpeed.get() * 0.1);

        float newYaw = currentYaw + MathHelper.wrapDegrees(targetYaw - currentYaw) * speed;
        float newPitch = currentPitch + (targetPitch - currentPitch) * speed;

        mc.player.setYaw(newYaw);
        mc.player.setPitch(MathHelper.clamp(newPitch, -90, 90));
    }

    private boolean isLookingAtPlayer() {
        if (mc.crosshairTarget == null) return false;
        if (mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return false;
        EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
        return entityHit.getEntity() instanceof PlayerEntity;
    }

    private void attack() {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
            if (entityHit.getEntity() instanceof PlayerEntity target) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private PlayerEntity findTarget() {
        PlayerEntity closest = null;
        double closestAngle = fov.get() / 2.0;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.isDead()) continue;

            double dist = mc.player.distanceTo(player);
            if (dist > aimRange.get()) continue;

            double angle = getAngleTo(player);
            if (angle < closestAngle) {
                closestAngle = angle;
                closest = player;
            }
        }

        return closest;
    }

    private double getAngleTo(PlayerEntity target) {
        Vec3d eyePos = mc.player.getEyePos();
        double tx = target.getX();
        double ty = target.getY() + target.getHeight() * 0.6;
        double tz = target.getZ();

        double dx = tx - eyePos.x;
        double dy = ty - eyePos.y;
        double dz = tz - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        float dyaw = Math.abs(MathHelper.wrapDegrees(targetYaw - mc.player.getYaw()));
        float dpitch = Math.abs(targetPitch - mc.player.getPitch());

        return Math.sqrt(dyaw * dyaw + dpitch * dpitch);
    }
}
