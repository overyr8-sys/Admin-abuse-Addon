package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.*;

public class AutoGGModule extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onKill = sgGeneral.add(
        new BoolSetting.Builder()
            .name("on-kill")
            .description("Send message when you kill someone.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onPop = sgGeneral.add(
        new BoolSetting.Builder()
            .name("on-pop")
            .description("Send message when someone pops a totem.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onDeath = sgGeneral.add(
        new BoolSetting.Builder()
            .name("on-death")
            .description("Send message when you die.")
            .defaultValue(true)
            .build()
    );

    private final Setting<String> killMessage = sgGeneral.add(
        new StringSetting.Builder()
            .name("kill-message")
            .defaultValue("get noobed <name>")
            .build()
    );

    private final Setting<String> popMessage = sgGeneral.add(
        new StringSetting.Builder()
            .name("pop-message")
            .defaultValue("ez pop <name>")
            .build()
    );

    private final Setting<String> deathMessage = sgGeneral.add(
        new StringSetting.Builder()
            .name("death-message")
            .defaultValue("I only died because i wasn't using crystals <name>")
            .build()
    );

    private final Map<UUID, Integer> popCount = new HashMap<>();
    private final Map<UUID, Long> lastHit = new HashMap<>();
    private final Set<UUID> deadPlayers = new HashSet<>();

    private boolean diedLastTick = false;

    public AutoGGModule() {
        super(
            AddonTemplate.CATEGORY,
            "auto-gg",
            "Sends a message when you do a event"
        );
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (!(event.entity instanceof PlayerEntity player)) return;
        lastHit.put(player.getUuid(), System.currentTimeMillis());
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (!onPop.get()) return;

        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 35) {
                if (!(packet.getEntity(mc.world) instanceof PlayerEntity player)) return;
                if (player == mc.player) return;

                UUID id = player.getUuid();
                int pops = popCount.getOrDefault(id, 0) + 1;
                popCount.put(id, pops);

                sendPop(player.getName().getString());
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            UUID id = player.getUuid();

            if (player.isDead() && !deadPlayers.contains(id)) {
                deadPlayers.add(id);

                Long hitTime = lastHit.get(id);

                if (onKill.get() && hitTime != null) {
                    if (System.currentTimeMillis() - hitTime < 6000) {
                        sendKill(player.getName().getString());
                    }
                }
            }

            if (!player.isDead()) {
                deadPlayers.remove(id);
            }
        }

        if (onDeath.get()) {
            if (mc.player.isDead() && !diedLastTick) {
                PlayerEntity killer = null;

                if (mc.player.getAttacker() instanceof PlayerEntity p) {
                    killer = p;
                }

                if (killer != null) {
                    sendDeath(killer.getName().getString());
                } else {
                    sendDeath("unknown");
                }
            }

            diedLastTick = mc.player.isDead();
        }
    }

    private void sendKill(String name) {
        String msg = killMessage.get().replace("<name>", name);
        mc.player.networkHandler.sendChatMessage(msg);
    }

    private void sendPop(String name) {
        String msg = popMessage.get().replace("<name>", name);
        mc.player.networkHandler.sendChatMessage(msg);
    }

    private void sendDeath(String name) {
        String msg = deathMessage.get().replace("<name>", name);
        mc.player.networkHandler.sendChatMessage(msg);
    }
}

