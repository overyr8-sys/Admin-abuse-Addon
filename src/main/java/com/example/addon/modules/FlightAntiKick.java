package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class FlightAntiKick extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between each downward movement.")
        .defaultValue(20)
        .sliderRange(0, 60)
        .build()
    );

    private final Setting<Integer> offTime = sgGeneral.add(new IntSetting.Builder()
        .name("off-time")
        .description("Ticks that you are moved down for.")
        .defaultValue(3)
        .sliderRange(0, 200)
        .build()
    );

    private int delayLeft;
    private int offLeft;

    public FlightAntiKick() {
        super(AddonTemplate.CATEGORY, "flight-anti-kick", "Moves you down periodically to prevent being kicked while flying.");
    }

    @Override
    public void onActivate() {
        delayLeft = delay.get();
        offLeft = offTime.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (delayLeft > 0) {
            delayLeft--;
        } else if (delayLeft <= 0 && offLeft > 0) {
            offLeft--;
            BlockPos playerPos = mc.player.getBlockPos();
            BlockPos pos = playerPos.add(new Vec3i(0, -1, 0));
            if (mc.world.getBlockState(pos).isAir() ||
                (!mc.world.getBlockState(pos).isAir() && mc.player.getY() >= pos.getY() + 1.11)) {
                mc.player.move(MovementType.SELF, new Vec3d(0, -0.1, 0));
            }
        } else {
            delayLeft = delay.get();
            offLeft = offTime.get();
        }
    }
}
