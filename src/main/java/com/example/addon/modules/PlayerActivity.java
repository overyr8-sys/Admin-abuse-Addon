package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerActivity extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> minBlocks = sgGeneral.add(new IntSetting.Builder()
        .name("min-blocks")
        .description("Minimum number of unusual blocks in a chunk to highlight it.")
        .defaultValue(3)
        .min(1)
        .max(50)
        .sliderMin(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> renderHeight = sgGeneral.add(new IntSetting.Builder()
        .name("render-height")
        .description("Y level to render highlights at.")
        .defaultValue(80)
        .min(-64)
        .max(320)
        .sliderMin(-64)
        .sliderMax(320)
        .build()
    );

    private final Setting<Boolean> showCount = sgGeneral.add(new BoolSetting.Builder()
        .name("show-count")
        .description("Show how many unusual blocks were found in the chunk.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> highlightColor = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("Color to highlight activity chunks.")
        .defaultValue(new SettingColor(0, 100, 255, 60))
        .build()
    );

    private final Set<ChunkPos> activityChunks = new HashSet<>();
    private final Map<ChunkPos, Integer> blockCounts = new HashMap<>();

    public PlayerActivity() {
        super(AddonTemplate.CATEGORY, "player-activity", "Scans chunks for unusual player-placed blocks and highlights them.");
    }

    @Override
    public void onActivate() {
        activityChunks.clear();
        blockCounts.clear();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        WorldChunk chunk = event.chunk();
        ChunkPos pos = chunk.getPos();

        int count = countUnusualBlocks(chunk);

        if (count >= minBlocks.get()) {
            activityChunks.add(pos);
            blockCounts.put(pos, count);
        }
    }

    private int countUnusualBlocks(WorldChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int topY = chunk.getHighestNonEmptySectionYOffset() + 16;
        int bottomY = chunk.getBottomY();
        int count = 0;

        for (BlockPos pos : BlockPos.iterate(
            chunkPos.getStartX(), bottomY, chunkPos.getStartZ(),
            chunkPos.getEndX(), topY - 1, chunkPos.getEndZ()
        )) {
            if (isUnusualBlock(chunk.getBlockState(pos).getBlock())) {
                count++;
            }
        }

        return count;
    }

    private boolean isUnusualBlock(Block block) {
        return
            // Storage
            block == Blocks.CHEST ||
                block == Blocks.TRAPPED_CHEST ||
                block == Blocks.BARREL ||
                block == Blocks.ENDER_CHEST ||
                block == Blocks.SHULKER_BOX ||
                block == Blocks.WHITE_SHULKER_BOX ||
                block == Blocks.ORANGE_SHULKER_BOX ||
                block == Blocks.MAGENTA_SHULKER_BOX ||
                block == Blocks.LIGHT_BLUE_SHULKER_BOX ||
                block == Blocks.YELLOW_SHULKER_BOX ||
                block == Blocks.LIME_SHULKER_BOX ||
                block == Blocks.PINK_SHULKER_BOX ||
                block == Blocks.GRAY_SHULKER_BOX ||
                block == Blocks.LIGHT_GRAY_SHULKER_BOX ||
                block == Blocks.CYAN_SHULKER_BOX ||
                block == Blocks.PURPLE_SHULKER_BOX ||
                block == Blocks.BLUE_SHULKER_BOX ||
                block == Blocks.BROWN_SHULKER_BOX ||
                block == Blocks.GREEN_SHULKER_BOX ||
                block == Blocks.RED_SHULKER_BOX ||
                block == Blocks.BLACK_SHULKER_BOX ||
                // Crafting/utility
                block == Blocks.CRAFTING_TABLE ||
                block == Blocks.FURNACE ||
                block == Blocks.BLAST_FURNACE ||
                block == Blocks.SMOKER ||
                block == Blocks.ENCHANTING_TABLE ||
                block == Blocks.ANVIL ||
                block == Blocks.BREWING_STAND ||
                block == Blocks.BEACON ||
                // Building blocks
                block == Blocks.COBBLESTONE ||
                block == Blocks.OAK_PLANKS ||
                block == Blocks.BIRCH_PLANKS ||
                block == Blocks.SPRUCE_PLANKS ||
                block == Blocks.JUNGLE_PLANKS ||
                block == Blocks.ACACIA_PLANKS ||
                block == Blocks.DARK_OAK_PLANKS ||
                block == Blocks.STONE_BRICKS ||
                block == Blocks.OBSIDIAN ||
                block == Blocks.CRYING_OBSIDIAN ||
                block == Blocks.NETHER_BRICKS ||
                // Redstone
                block == Blocks.HOPPER ||
                block == Blocks.DROPPER ||
                block == Blocks.DISPENSER ||
                block == Blocks.PISTON ||
                block == Blocks.STICKY_PISTON ||
                block == Blocks.LEVER ||
                block == Blocks.REPEATER ||
                block == Blocks.COMPARATOR ||
                block == Blocks.OBSERVER ||
                // Lighting
                block == Blocks.TORCH ||
                block == Blocks.WALL_TORCH ||
                block == Blocks.GLOWSTONE ||
                block == Blocks.SEA_LANTERN ||
                block == Blocks.LANTERN ||
                // Beds
                block == Blocks.WHITE_BED ||
                block == Blocks.ORANGE_BED ||
                block == Blocks.MAGENTA_BED ||
                block == Blocks.LIGHT_BLUE_BED ||
                block == Blocks.YELLOW_BED ||
                block == Blocks.LIME_BED ||
                block == Blocks.PINK_BED ||
                block == Blocks.GRAY_BED ||
                block == Blocks.LIGHT_GRAY_BED ||
                block == Blocks.CYAN_BED ||
                block == Blocks.PURPLE_BED ||
                block == Blocks.BLUE_BED ||
                block == Blocks.BROWN_BED ||
                block == Blocks.GREEN_BED ||
                block == Blocks.RED_BED ||
                block == Blocks.BLACK_BED ||
                // Misc
                block == Blocks.LADDER ||
                block == Blocks.TNT ||
                block == Blocks.NETHER_PORTAL ||
                block == Blocks.RESPAWN_ANCHOR ||
                block == Blocks.SPAWNER;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.world == null) return;

        int y = renderHeight.get();

        for (ChunkPos pos : activityChunks) {
            event.renderer.box(
                pos.getStartX(), y, pos.getStartZ(),
                pos.getEndX() + 1, y + 1, pos.getEndZ() + 1,
                highlightColor.get(), highlightColor.get(),
                ShapeMode.Both, 0
            );
        }
    }
}
