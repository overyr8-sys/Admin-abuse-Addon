package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AutoLogin extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
        .name("command")
        .description("The login command to send. Use /login, /l, etc.")
        .defaultValue("/login YourPasswordHere")
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks to wait before sending the command after joining.")
        .defaultValue(40)
        .min(0)
        .max(200)
        .sliderMin(0)
        .sliderMax(200)
        .build()
    );

    private boolean shouldLogin = false;
    private int tickTimer = 0;

    public AutoLogin() {
        super(AddonTemplate.CATEGORY, "auto-login", "Automatically sends your login command when joining a server.");
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        shouldLogin = true;
        tickTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!shouldLogin || mc.player == null) return;

        tickTimer++;
        if (tickTimer < delay.get()) return;

        mc.player.networkHandler.sendChatMessage(command.get());
        shouldLogin = false;
        tickTimer = 0;
    }
}
