package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LoginCommand extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in milliseconds before sending commands after joining.")
        .defaultValue(1000)
        .min(0)
        .max(10000)
        .sliderMax(5000)
        .build()
    );

    private final Setting<List<String>> commands = sgGeneral.add(new StringListSetting.Builder()
        .name("commands")
        .description("Commands to send on join. Add as many as you want.")
        .defaultValue(List.of("/login YourPassword"))
        .build()
    );

    private final Timer timer = new Timer();

    public LoginCommand() {
        super(AddonTemplate.CATEGORY, "login-command", "Sends commands when joining a server with a delay.");
        this.runInMainMenu = true;
    }

    @EventHandler
    public void onJoin(GameJoinedEvent event) {
        if (!this.isActive()) return;
        if (commands.get().isEmpty()) return;

        long currentDelay = delay.get();

        for (String command : commands.get()) {
            final String cmd = command;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (mc.player != null) {
                        mc.player.networkHandler.sendChatMessage(cmd);
                    }
                }
            }, currentDelay);
            // Stagger each command by the delay so they don't all fire at once
            currentDelay += delay.get();
        }
    }
}
