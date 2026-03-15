package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BetterNametags extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Boolean> showHealth = sgGeneral.add(new BoolSetting.Builder()
        .name("show-health").description("Show health.").defaultValue(true).build()
    );
    private final Setting<Boolean> showPing = sgGeneral.add(new BoolSetting.Builder()
        .name("show-ping").description("Show ping.").defaultValue(true).build()
    );
    private final Setting<Boolean> showHeldItem = sgGeneral.add(new BoolSetting.Builder()
        .name("show-held-item").description("Show held item name.").defaultValue(true).build()
    );
    private final Setting<Boolean> showArmor = sgGeneral.add(new BoolSetting.Builder()
        .name("show-armor").description("Show armor durability.").defaultValue(true).build()
    );
    private final Setting<Boolean> showPopCount = sgGeneral.add(new BoolSetting.Builder()
        .name("show-pop-count").description("Show totem pop count.").defaultValue(true).build()
    );
    private final Setting<Boolean> showShulkerContents = sgGeneral.add(new BoolSetting.Builder()
        .name("show-shulker-contents").description("Show contents of shulker box if held.").defaultValue(true).build()
    );
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale").description("Nametag scale.").defaultValue(1.0).min(0.5).max(3.0).sliderMin(0.5).sliderMax(2.0).build()
    );

    private final Setting<SettingColor> nameColor = sgColors.add(new ColorSetting.Builder()
        .name("name-color").defaultValue(new SettingColor(255, 255, 255, 255)).build()
    );
    private final Setting<SettingColor> healthHighColor = sgColors.add(new ColorSetting.Builder()
        .name("health-high").defaultValue(new SettingColor(0, 255, 0, 255)).build()
    );
    private final Setting<SettingColor> healthMidColor = sgColors.add(new ColorSetting.Builder()
        .name("health-mid").defaultValue(new SettingColor(255, 255, 0, 255)).build()
    );
    private final Setting<SettingColor> healthLowColor = sgColors.add(new ColorSetting.Builder()
        .name("health-low").defaultValue(new SettingColor(255, 0, 0, 255)).build()
    );
    private final Setting<SettingColor> pingColor = sgColors.add(new ColorSetting.Builder()
        .name("ping-color").defaultValue(new SettingColor(100, 200, 255, 255)).build()
    );
    private final Setting<SettingColor> popColor = sgColors.add(new ColorSetting.Builder()
        .name("pop-color").defaultValue(new SettingColor(255, 100, 0, 255)).build()
    );

    public static final Map<UUID, Integer> popCounts = new HashMap<>();

    public BetterNametags() {
        super(AddonTemplate.CATEGORY, "better-nametags", "Enhanced nametags with health, ping, armor, held item and shulker contents.");
    }

    @Override
    public void onActivate() {
        popCounts.clear();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != 35) return;
        if (!(packet.getEntity(mc.world) instanceof PlayerEntity player)) return;
        if (player == mc.player) return;
        popCounts.merge(player.getUuid(), 1, Integer::sum);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null || mc.gameRenderer == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;

            double[] screen = worldToScreen(
                player.getX(),
                player.getY() + player.getHeight() + 0.3,
                player.getZ()
            );
            if (screen == null) continue;

            renderNametag(player, screen[0], screen[1]);
        }
    }

    private void renderNametag(PlayerEntity player, double sx, double sy) {
        TextRenderer text = TextRenderer.get();
        float s = scale.get().floatValue() * 0.5f;
        float lineHeight = 12 * s;
        float currentY = (float) sy;

        // Name
        String name = player.getName().getString();
        text.begin(s, false, true);
        text.render(name, (float)(sx - text.getWidth(name) / 2f), currentY, nameColor.get(), true);
        text.end();
        currentY += lineHeight;

        // Health
        if (showHealth.get()) {
            float health = player.getHealth();
            float maxHealth = player.getMaxHealth();
            float ratio = health / maxHealth;
            Color barColor;
            if (ratio > 0.6f) barColor = healthHighColor.get();
            else if (ratio > 0.3f) barColor = healthMidColor.get();
            else barColor = healthLowColor.get();

            String healthStr = String.format("%.1f HP", health);
            text.begin(s * 0.85f, false, true);
            text.render(healthStr, (float)(sx - text.getWidth(healthStr) / 2f), currentY, barColor, true);
            text.end();
            currentY += lineHeight;
        }

        // Ping
        if (showPing.get()) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) {
                String pingStr = entry.getLatency() + "ms";
                text.begin(s * 0.85f, false, true);
                text.render(pingStr, (float)(sx - text.getWidth(pingStr) / 2f), currentY, pingColor.get(), true);
                text.end();
                currentY += lineHeight;
            }
        }

        // Held item
        if (showHeldItem.get()) {
            ItemStack held = player.getMainHandStack();
            if (!held.isEmpty()) {
                String itemName = held.getName().getString();
                text.begin(s * 0.85f, false, true);
                text.render(itemName, (float)(sx - text.getWidth(itemName) / 2f), currentY, new Color(220, 220, 220, 255), true);
                text.end();
                currentY += lineHeight;

                // Shulker contents
                if (showShulkerContents.get() && isShulkerBox(held)) {
                    ContainerComponent contents = held.get(DataComponentTypes.CONTAINER);
                    if (contents != null) {
                        StringBuilder sb = new StringBuilder("[");
                        int count = 0;
                        for (ItemStack stack : contents.stream().toList()) {
                            if (!stack.isEmpty() && count < 5) {
                                if (count > 0) sb.append(", ");
                                sb.append(stack.getName().getString());
                                if (stack.getCount() > 1) sb.append(" x").append(stack.getCount());
                                count++;
                            }
                        }
                        if (count == 0) sb.append("Empty");
                        sb.append("]");
                        String shulkerStr = sb.toString();
                        text.begin(s * 0.75f, false, true);
                        text.render(shulkerStr, (float)(sx - text.getWidth(shulkerStr) / 2f), currentY, new Color(180, 100, 255, 255), true);
                        text.end();
                        currentY += lineHeight;
                    }
                }
            }
        }

        // Armor
        if (showArmor.get()) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            StringBuilder armorStr = new StringBuilder();
            for (EquipmentSlot slot : slots) {
                ItemStack armor = player.getEquippedStack(slot);
                if (!armor.isEmpty() && armor.isDamageable()) {
                    int dur = armor.getMaxDamage() - armor.getDamage();
                    String n = armor.getName().getString();
                    armorStr.append(n, 0, Math.min(3, n.length())).append(":").append(dur).append(" ");
                }
            }
            if (!armorStr.isEmpty()) {
                String aStr = armorStr.toString().trim();
                text.begin(s * 0.75f, false, true);
                text.render(aStr, (float)(sx - text.getWidth(aStr) / 2f), currentY, new Color(200, 200, 100, 255), true);
                text.end();
                currentY += lineHeight;
            }
        }

        // Pop count
        if (showPopCount.get()) {
            int pops = popCounts.getOrDefault(player.getUuid(), 0);
            if (pops > 0) {
                String popStr = "Pops: " + pops;
                text.begin(s * 0.85f, false, true);
                text.render(popStr, (float)(sx - text.getWidth(popStr) / 2f), currentY, popColor.get(), true);
                text.end();
            }
        }
    }

    private double[] worldToScreen(double x, double y, double z) {
        double cx = mc.gameRenderer.getCamera().getFocusedEntity().getX();
        double cy = mc.gameRenderer.getCamera().getFocusedEntity().getEyeY();
        double cz = mc.gameRenderer.getCamera().getFocusedEntity().getZ();

        double dx = x - cx;
        double dy = y - cy;
        double dz = z - cz;

        float yaw = mc.gameRenderer.getCamera().getYaw();
        float pitch = mc.gameRenderer.getCamera().getPitch();

        double sinYaw = Math.sin(Math.toRadians(yaw));
        double cosYaw = Math.cos(Math.toRadians(yaw));
        double sinPitch = Math.sin(Math.toRadians(pitch));
        double cosPitch = Math.cos(Math.toRadians(pitch));

        double rx = dx * cosYaw + dz * sinYaw;
        double ry = dy * cosPitch - (-dx * sinYaw + dz * cosYaw) * sinPitch;
        double rz = (-dx * sinYaw + dz * cosYaw) * cosPitch + dy * sinPitch;

        if (rz <= 0) return null;

        double fov = mc.options.getFov().getValue();
        double screenW = mc.getWindow().getScaledWidth();
        double screenH = mc.getWindow().getScaledHeight();

        double aspectRatio = screenW / screenH;
        double tanFov = Math.tan(Math.toRadians(fov / 2.0));

        double screenX = screenW / 2.0 + (rx / rz) / (tanFov * aspectRatio) * (screenW / 2.0);
        double screenY = screenH / 2.0 - (ry / rz) / tanFov * (screenH / 2.0);

        return new double[]{screenX, screenY};
    }

    private boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() == Items.SHULKER_BOX ||
            stack.getItem() == Items.WHITE_SHULKER_BOX ||
            stack.getItem() == Items.ORANGE_SHULKER_BOX ||
            stack.getItem() == Items.MAGENTA_SHULKER_BOX ||
            stack.getItem() == Items.LIGHT_BLUE_SHULKER_BOX ||
            stack.getItem() == Items.YELLOW_SHULKER_BOX ||
            stack.getItem() == Items.LIME_SHULKER_BOX ||
            stack.getItem() == Items.PINK_SHULKER_BOX ||
            stack.getItem() == Items.GRAY_SHULKER_BOX ||
            stack.getItem() == Items.LIGHT_GRAY_SHULKER_BOX ||
            stack.getItem() == Items.CYAN_SHULKER_BOX ||
            stack.getItem() == Items.PURPLE_SHULKER_BOX ||
            stack.getItem() == Items.BLUE_SHULKER_BOX ||
            stack.getItem() == Items.BROWN_SHULKER_BOX ||
            stack.getItem() == Items.GREEN_SHULKER_BOX ||
            stack.getItem() == Items.RED_SHULKER_BOX ||
            stack.getItem() == Items.BLACK_SHULKER_BOX;
    }
}
