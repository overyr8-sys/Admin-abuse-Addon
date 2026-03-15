package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayDeque;
import java.util.Deque;

public class Backtrack extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay-ms")
        .description("How many milliseconds to delay position packets.")
        .defaultValue(200)
        .min(50)
        .max(1000)
        .sliderMin(50)
        .sliderMax(500)
        .build()
    );

    private final Setting<Integer> maxPackets = sgGeneral.add(new IntSetting.Builder()
        .name("max-packets")
        .description("Maximum packets to hold before force flushing.")
        .defaultValue(20)
        .min(5)
        .max(50)
        .sliderMin(5)
        .sliderMax(50)
        .build()
    );

    private final Setting<Boolean> onlyInCombat = sgGeneral.add(new BoolSetting.Builder()
        .name("only-in-combat")
        .description("Only activate when a player is nearby.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> combatRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("combat-range")
        .description("Range to detect nearby players for combat mode.")
        .defaultValue(10.0)
        .min(1.0)
        .max(20.0)
        .sliderMin(1.0)
        .sliderMax(20.0)
        .visible(onlyInCombat::get)
        .build()
    );

    private record DelayedPacket(PlayerMoveC2SPacket packet, long timestamp) {}
    private final Deque<DelayedPacket> packetQueue = new ArrayDeque<>();

    public Backtrack() {
        super(AddonTemplate.CATEGORY, "backtrack", "Delays position packets to the server making you appear to teleport.");
    }

    @Override
    public void onActivate() {
        packetQueue.clear();
    }

    @Override
    public void onDeactivate() {
        flush();
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!(event.packet instanceof PlayerMoveC2SPacket packet)) return;

        // Only delay if in combat when setting is on
        if (onlyInCombat.get() && !isPlayerNearby()) return;

        // Cancel the packet and queue it
        event.cancel();
        packetQueue.add(new DelayedPacket(packet, System.currentTimeMillis()));

        // Force flush if too many packets queued
        if (packetQueue.size() >= maxPackets.get()) {
            flush();
            return;
        }

        // Release packets that have waited long enough
        long now = System.currentTimeMillis();
        while (!packetQueue.isEmpty() && now - packetQueue.peek().timestamp() >= delay.get()) {
            PlayerMoveC2SPacket p = packetQueue.poll().packet();
            mc.getNetworkHandler().sendPacket(p);
        }
    }

    private void flush() {
        while (!packetQueue.isEmpty()) {
            mc.getNetworkHandler().sendPacket(packetQueue.poll().packet());
        }
    }

    private boolean isPlayerNearby() {
        if (mc.world == null || mc.player == null) return false;
        return mc.world.getPlayers().stream()
            .anyMatch(p -> p != mc.player && mc.player.distanceTo(p) <= combatRange.get());
    }
}
