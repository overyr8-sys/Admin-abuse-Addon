package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

public class AutoBuff extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown-seconds")
        .description("Seconds to wait before throwing another pot.")
        .defaultValue(10)
        .min(1)
        .max(120)
        .sliderMin(1)
        .sliderMax(60)
        .build()
    );

    private final Setting<Boolean> onlyIfNoEffect = sgGeneral.add(new BoolSetting.Builder()
        .name("only-if-no-effect")
        .description("Only throw pot if you don't already have strength.")
        .defaultValue(true)
        .build()
    );

    private boolean inCombat = false;
    private int combatTimer = 0;
    private int cooldownTimer = 0;
    private static final int COMBAT_TIMEOUT = 100;

    public AutoBuff() {
        super(AddonTemplate.CATEGORY, "auto-buff", "Throws a strength splash pot at your feet when you enter combat.");
    }

    @Override
    public void onActivate() {
        inCombat = false;
        combatTimer = 0;
        cooldownTimer = 0;
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        inCombat = true;
        combatTimer = COMBAT_TIMEOUT;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (combatTimer > 0) combatTimer--;
        else inCombat = false;

        if (cooldownTimer > 0) {
            cooldownTimer--;
            return;
        }

        if (!inCombat) return;

        if (onlyIfNoEffect.get() && mc.player.hasStatusEffect(StatusEffects.STRENGTH)) return;

        int slot = findStrengthPot();
        if (slot == -1) return;

        // Save previous slot using the network handler trick
        int prevSlot = mc.player.getInventory().getSelectedSlot();

        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));

        float prevPitch = mc.player.getPitch();
        mc.player.setPitch(90);

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);

        mc.player.setPitch(prevPitch);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));

        cooldownTimer = cooldown.get() * 20;
    }

    private int findStrengthPot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() != Items.SPLASH_POTION) continue;

            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) continue;

            for (var effect : contents.getEffects()) {
                if (effect.getEffectType().value() == StatusEffects.STRENGTH.value()) {
                    return i;
                }
            }
        }
        return -1;
    }
}
