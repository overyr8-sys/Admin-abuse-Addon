package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.List;

public class MsgAura extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMessages = settings.createGroup("Messages");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Range to detect players.")
        .defaultValue(10.0)
        .min(1.0)
        .max(100.0)
        .sliderMin(1.0)
        .sliderMax(100.0)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between each message.")
        .defaultValue(40)
        .min(1)
        .max(200)
        .sliderMin(1)
        .sliderMax(200)
        .build()
    );

    private final Setting<Boolean> spam = sgGeneral.add(new BoolSetting.Builder()
        .name("spam")
        .description("Keep sending messages repeatedly.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> randomOrder = sgMessages.add(new BoolSetting.Builder()
        .name("random-order")
        .description("Send messages in random order instead of sequential.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> perPlayer = sgGeneral.add(new BoolSetting.Builder()
        .name("per-player")
        .description("Send messages to each player individually using /msg.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<String>> messages = sgMessages.add(new StringListSetting.Builder()
        .name("messages")
        .description("List of messages to send.")
        .defaultValue(List.of("hello!", "get rekt", "ez", "you're bad"))
        .build()
    );

    private int tickTimer = 0;
    private int messageIndex = 0;

    public MsgAura() {
        super(AddonTemplate.CATEGORY, "advanced-msg-aura", "Sends messages to nearby players automatically.");
    }

    @Override
    public void onActivate() {
        tickTimer = 0;
        messageIndex = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (messages.get().isEmpty()) return;

        tickTimer++;
        if (tickTimer < delay.get()) return;
        tickTimer = 0;

        List<AbstractClientPlayerEntity> nearby = mc.world.getPlayers().stream()
            .filter(p -> p != mc.player)
            .filter(p -> mc.player.distanceTo(p) <= range.get())
            .toList();

        if (nearby.isEmpty()) return;

        String message;
        if (randomOrder.get()) {
            message = messages.get().get((int)(Math.random() * messages.get().size()));
        } else {
            message = messages.get().get(messageIndex % messages.get().size());
            messageIndex++;

            if (!spam.get() && messageIndex >= messages.get().size()) {
                toggle();
                return;
            }
        }

        if (perPlayer.get()) {
            for (AbstractClientPlayerEntity player : nearby) {
                mc.player.networkHandler.sendChatMessage("/msg " + player.getName().getString() + " " + message);
            }
        } else {
            mc.player.networkHandler.sendChatMessage(message);
        }
    }
}
