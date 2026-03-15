package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FastEat extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder()
        .name("speed")
        .description("How many extra eat ticks to send per tick. Higher = faster.")
        .defaultValue(3)
        .min(1)
        .max(10)
        .sliderMin(1)
        .sliderMax(10)
        .build()
    );

    public FastEat() {
        super(AddonTemplate.CATEGORY, "fast-eat", "Eat food faster than normal.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isUsingItem()) return;

        ItemStack using = mc.player.getActiveItem();
        if (using.isEmpty()) return;

        UseAction action = using.getUseAction();
        if (action != UseAction.EAT && action != UseAction.DRINK) return;

        // Send multiple use item packets to speed up eating
        for (int i = 0; i < speed.get(); i++) {
            mc.getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    Direction.DOWN,
                    0
                )
            );
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
    }
}
