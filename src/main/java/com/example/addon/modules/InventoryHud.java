package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class InventoryHud extends HudElement {

    public static final HudElementInfo<InventoryHud> INFO = new HudElementInfo<>(
        AddonTemplate.HUD_GROUP, "inventory-hud", "Displays your full inventory.", InventoryHud::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the inventory HUD.")
        .defaultValue(2.0)
        .min(0.5)
        .max(4.0)
        .sliderMin(0.5)
        .sliderMax(4.0)
        .build()
    );

    private final Setting<Boolean> showHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("show-hotbar")
        .description("Show the hotbar row.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showArmor = sgGeneral.add(new BoolSetting.Builder()
        .name("show-armor")
        .description("Show armor slots.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showOffhand = sgGeneral.add(new BoolSetting.Builder()
        .name("show-offhand")
        .description("Show offhand slot.")
        .defaultValue(true)
        .build()
    );

    private static final int SLOT_SIZE = 16;
    private static final int GAP = 2;
    private static final int COLS = 9;
    private static final Color SLOT_BG = new Color(0, 0, 0, 136);

    public InventoryHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            setSize(COLS * (SLOT_SIZE + GAP) * scale.get(), 5 * (SLOT_SIZE + GAP) * scale.get());
            return;
        }

        double s = scale.get();
        int slotW = (int)(SLOT_SIZE * s);
        int slotH = (int)(SLOT_SIZE * s);
        int gap = (int)(GAP * s);

        double x = this.x;
        double y = this.y;

        int totalRows = 3;
        if (showHotbar.get()) totalRows++;
        if (showArmor.get()) totalRows++;

        setSize(COLS * (slotW + gap), totalRows * (slotH + gap));

        int currentRow = 0;

        // Armor row
        if (showArmor.get()) {
            EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < 4; i++) {
                ItemStack armor = mc.player.getEquippedStack(armorSlots[i]);
                double sx = x + i * (slotW + gap);
                double sy = y + currentRow * (slotH + gap);
                renderSlot(renderer, armor, sx, sy, slotW, slotH);
            }

            // Offhand
            if (showOffhand.get()) {
                ItemStack offhand = mc.player.getOffHandStack();
                double sx = x + 4 * (slotW + gap);
                double sy = y + currentRow * (slotH + gap);
                renderSlot(renderer, offhand, sx, sy, slotW, slotH);
            }

            currentRow++;
        }

        // Main inventory (slots 9-35)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = 9 + row * COLS + col;
                ItemStack stack = mc.player.getInventory().getStack(slot);
                double sx = x + col * (slotW + gap);
                double sy = y + (currentRow + row) * (slotH + gap);
                renderSlot(renderer, stack, sx, sy, slotW, slotH);
            }
        }
        currentRow += 3;

        // Hotbar (slots 0-8)
        if (showHotbar.get()) {
            for (int col = 0; col < COLS; col++) {
                ItemStack stack = mc.player.getInventory().getStack(col);
                double sx = x + col * (slotW + gap);
                double sy = y + currentRow * (slotH + gap);
                renderSlot(renderer, stack, sx, sy, slotW, slotH);
            }
        }
    }

    private void renderSlot(HudRenderer renderer, ItemStack stack, double x, double y, int w, int h) {
        renderer.quad(x, y, w, h, SLOT_BG);

        if (stack != null && !stack.isEmpty()) {
            renderer.item(stack, (int) x, (int) y, (float)(w / 16.0), true);
        }
    }
}
