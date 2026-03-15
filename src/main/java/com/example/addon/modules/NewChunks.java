package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public class NewChunks extends Module {

    public enum DetectionMode {
        MODE_1_12_2,
        MODE_1_19_PLUS
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<DetectionMode> mode = sgGeneral.add(new EnumSetting.Builder<DetectionMode>()
        .name("mode")
        .description("Detection mode. Use 1.12.2 for old anarchy servers, 1.19+ for newer servers.")
        .defaultValue(DetectionMode.MODE_1_12_2)
        .build()
    );

    private final Setting<Boolean> showNew = sgGeneral.add(new BoolSetting.Builder()
        .name("show-new")
        .description("Highlight new chunks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showOld = sgGeneral.add(new BoolSetting.Builder()
        .name("show-old")
        .description("Highlight old/already generated chunks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> renderHeight = sgGeneral.add(new IntSetting.Builder()
        .name("render-height")
        .description("Y level to render the chunk highlights at.")
        .defaultValue(0)
        .min(-64)
        .max(320)
        .sliderMin(-64)
        .sliderMax(320)
        .build()
    );

    private final Setting<SettingColor> newColor = sgRender.add(new ColorSetting.Builder()
        .name("new-color")
        .description("Color for new chunks.")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .build()
    );

    private final Setting<SettingColor> oldColor = sgRender.add(new ColorSetting.Builder()
        .name("old-color")
        .description("Color for old chunks.")
        .defaultValue(new SettingColor(0, 255, 0, 50))
        .build()
    );

    private final Set<ChunkPos> newChunks = new HashSet<>();
    private final Set<ChunkPos> oldChunks = new HashSet<>();

    public NewChunks() {
        super(AddonTemplate.CATEGORY, "new-chunks", "Highlights newly generated chunks vs already generated chunks.");
    }

    @Override
    public void onActivate() {
        newChunks.clear();
        oldChunks.clear();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        WorldChunk chunk = event.chunk();
        ChunkPos pos = chunk.getPos();

        boolean isNew;

        if (mode.get() == DetectionMode.MODE_1_12_2) {
            isNew = detect1122(chunk);
        } else {
            isNew = detect119(chunk);
        }

        if (isNew) {
            newChunks.add(pos);
            oldChunks.remove(pos);
        } else {
            oldChunks.add(pos);
            newChunks.remove(pos);
        }
    }

    // 1.12.2 method: checks for missing liquids below sea level
    // On 1.12.2 servers, new chunks wont have water/lava placed yet
    // in sections below y=64 that should naturally have liquid
    private boolean detect1122(WorldChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();

        // Check sections below sea level (y 0-64) for liquid
        for (BlockPos pos : BlockPos.iterate(
            chunkPos.getStartX(), 1, chunkPos.getStartZ(),
            chunkPos.getEndX(), 63, chunkPos.getEndZ()
        )) {
            var block = chunk.getBlockState(pos).getBlock();
            // If we find water or lava the chunk has been around long enough to have liquid
            if (block == Blocks.WATER || block == Blocks.LAVA) {
                return false; // old chunk
            }
        }
        return true; // no liquid found = new chunk
    }

    // 1.19+ method: checks if chunk sections are completely empty
    // On 1.19+ servers new chunks get sent with empty sections
    private boolean detect119(WorldChunk chunk) {
        ChunkSection[] sections = chunk.getSectionArray();
        int emptySections = 0;

        for (ChunkSection section : sections) {
            if (section == null || section.isEmpty()) {
                emptySections++;
            }
        }

        // If more than half the sections are empty its a new chunk
        return emptySections > sections.length / 2;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.world == null) return;

        int y = renderHeight.get();

        if (showNew.get()) {
            for (ChunkPos pos : newChunks) {
                event.renderer.box(
                    pos.getStartX(), y, pos.getStartZ(),
                    pos.getEndX() + 1, y + 1, pos.getEndZ() + 1,
                    newColor.get(), newColor.get(),
                    ShapeMode.Both, 0
                );
            }
        }

        if (showOld.get()) {
            for (ChunkPos pos : oldChunks) {
                event.renderer.box(
                    pos.getStartX(), y, pos.getStartZ(),
                    pos.getEndX() + 1, y + 1, pos.getEndZ() + 1,
                    oldColor.get(), oldColor.get(),
                    ShapeMode.Both, 0
                );
            }
        }
    }
}
