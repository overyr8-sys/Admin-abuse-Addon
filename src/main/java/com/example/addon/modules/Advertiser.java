package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.util.List;

public class Advertiser extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
        .name("messages")
        .description("Messages to advertise. Cycles through them in order.")
        .defaultValue(List.of("Join our clan! /msg me", "Admin Abuse clan recruiting!"))
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay-seconds")
        .description("Seconds between each message.")
        .defaultValue(30)
        .min(1)
        .max(300)
        .sliderMin(1)
        .sliderMax(120)
        .build()
    );

    private final Setting<Boolean> randomOrder = sgGeneral.add(new BoolSetting.Builder()
        .name("random-order")
        .description("Send messages in random order.")
        .defaultValue(false)
        .build()
    );

    private int tickTimer = 0;
    private int messageIndex = 0;

    public Advertiser() {
        super(AddonTemplate.CATEGORY, "advertiser", "Automatically sends advertising messages in chat.");
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
        // Convert seconds to ticks (20 ticks per second)
        if (tickTimer < delay.get() * 20) return;
        tickTimer = 0;

        String message;
        if (randomOrder.get()) {
            message = messages.get().get((int)(Math.random() * messages.get().size()));
        } else {
            message = messages.get().get(messageIndex % messages.get().size());
            messageIndex++;
        }

        mc.player.networkHandler.sendChatMessage(message);
    }
}
