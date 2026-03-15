package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

public class XCarry extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> preventClose = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-close")
        .description("Prevents the crafting grid from being cleared when opening containers.")
        .defaultValue(true)
        .build()
    );

    public XCarry() {
        super(AddonTemplate.CATEGORY, "x-carry", "Allows you to store items in your 2x2 crafting slots as extra inventory space.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!preventClose.get()) return;

        if (event.packet instanceof CloseHandledScreenC2SPacket packet) {
            // Screen ID 0 is the player inventory - cancel closing it to keep crafting slots
            if (packet.getSyncId() == 0) {
                event.cancel();
            }
        }
    }
}
