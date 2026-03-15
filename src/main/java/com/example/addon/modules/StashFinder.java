package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StashFinder extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBlocks = settings.createGroup("Blocks");
    private final SettingGroup sgNotifications = settings.createGroup("Notifications");

    private final Setting<Integer> minCount = sgGeneral.add(new IntSetting.Builder()
        .name("min-count")
        .description("Minimum number of stash blocks in a chunk to trigger an alert.")
        .defaultValue(5)
        .min(1)
        .max(100)
        .sliderMin(1)
        .sliderMax(50)
        .build()
    );

    private final Setting<Boolean> ignoreTrialChambers = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-trial-chambers")
        .description("Ignore chunks that contain trial spawners.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logToFile = sgGeneral.add(new BoolSetting.Builder()
        .name("log-to-file")
        .description("Save stash finds to a TXT file.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> detectChests = sgBlocks.add(new BoolSetting.Builder()
        .name("chests").description("Detect chests.").defaultValue(true).build()
    );

    private final Setting<Boolean> detectShulkers = sgBlocks.add(new BoolSetting.Builder()
        .name("shulker-boxes").description("Detect shulker boxes.").defaultValue(true).build()
    );

    private final Setting<Boolean> detectEnderChests = sgBlocks.add(new BoolSetting.Builder()
        .name("ender-chests").description("Detect ender chests.").defaultValue(true).build()
    );

    private final Setting<Boolean> detectSpawners = sgBlocks.add(new BoolSetting.Builder()
        .name("spawners").description("Detect mob spawners.").defaultValue(true).build()
    );

    private final Setting<Boolean> detectFurnaces = sgBlocks.add(new BoolSetting.Builder()
        .name("furnaces").description("Detect furnaces.").defaultValue(false).build()
    );

    private final Setting<Boolean> detectBarrels = sgBlocks.add(new BoolSetting.Builder()
        .name("barrels").description("Detect barrels.").defaultValue(true).build()
    );

    private final Setting<Boolean> detectHoppers = sgBlocks.add(new BoolSetting.Builder()
        .name("hoppers").description("Detect hoppers.").defaultValue(false).build()
    );

    private final Setting<List<Block>> customBlocks = sgBlocks.add(new BlockListSetting.Builder()
        .name("custom-blocks").description("Additional blocks to search for.").defaultValue().build()
    );

    private final Setting<Boolean> chatAlert = sgNotifications.add(new BoolSetting.Builder()
        .name("chat-alert").description("Show a chat message when a stash is found.").defaultValue(true).build()
    );

    private final Setting<Boolean> sendToWebhook = sgNotifications.add(new BoolSetting.Builder()
        .name("discord-webhook").description("Send stash finds to a Discord webhook.").defaultValue(false).build()
    );

    private final Setting<String> webhookUrl = sgNotifications.add(new StringSetting.Builder()
        .name("webhook-url")
        .description("Your Discord webhook URL.")
        .defaultValue("https://discord.com/api/webhooks/YOUR_WEBHOOK_HERE")
        .visible(sendToWebhook::get)
        .build()
    );

    private final Set<ChunkPos> foundChunks = new HashSet<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public StashFinder() {
        super(AddonTemplate.CATEGORY, "stash-finder", "Scans chunks for stashes with advanced detection and notifications.");
    }

    @Override
    public void onActivate() {
        foundChunks.clear();
        info("Stash Finder enabled. Scanning chunks...");
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        WorldChunk chunk = event.chunk();
        ChunkPos chunkPos = chunk.getPos();

        if (foundChunks.contains(chunkPos)) return;
        if (ignoreTrialChambers.get() && hasTrialChamber(chunk)) return;

        int topY = chunk.getHighestNonEmptySectionYOffset() + 16;
        int bottomY = chunk.getBottomY();

        int count = 0;
        BlockPos firstFound = null;

        for (BlockPos pos : BlockPos.iterate(
            chunkPos.getStartX(), bottomY, chunkPos.getStartZ(),
            chunkPos.getEndX(), topY - 1, chunkPos.getEndZ()
        )) {
            Block block = chunk.getBlockState(pos).getBlock();
            if (isStashBlock(block)) {
                count++;
                if (firstFound == null) firstFound = pos.toImmutable();
            }
        }

        if (count >= minCount.get() && firstFound != null) {
            foundChunks.add(chunkPos);
            onStashFound(firstFound, count);
        }
    }

    private boolean isStashBlock(Block block) {
        if (detectChests.get() && (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST)) return true;
        if (detectShulkers.get() && isShulkerBox(block)) return true;
        if (detectEnderChests.get() && block == Blocks.ENDER_CHEST) return true;
        if (detectSpawners.get() && block == Blocks.SPAWNER) return true;
        if (detectFurnaces.get() && (block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE || block == Blocks.SMOKER)) return true;
        if (detectBarrels.get() && block == Blocks.BARREL) return true;
        if (detectHoppers.get() && block == Blocks.HOPPER) return true;
        if (!customBlocks.get().isEmpty() && customBlocks.get().contains(block)) return true;
        return false;
    }

    private boolean isShulkerBox(Block block) {
        return block == Blocks.SHULKER_BOX || block == Blocks.WHITE_SHULKER_BOX ||
            block == Blocks.ORANGE_SHULKER_BOX || block == Blocks.MAGENTA_SHULKER_BOX ||
            block == Blocks.LIGHT_BLUE_SHULKER_BOX || block == Blocks.YELLOW_SHULKER_BOX ||
            block == Blocks.LIME_SHULKER_BOX || block == Blocks.PINK_SHULKER_BOX ||
            block == Blocks.GRAY_SHULKER_BOX || block == Blocks.LIGHT_GRAY_SHULKER_BOX ||
            block == Blocks.CYAN_SHULKER_BOX || block == Blocks.PURPLE_SHULKER_BOX ||
            block == Blocks.BLUE_SHULKER_BOX || block == Blocks.BROWN_SHULKER_BOX ||
            block == Blocks.GREEN_SHULKER_BOX || block == Blocks.RED_SHULKER_BOX ||
            block == Blocks.BLACK_SHULKER_BOX;
    }

    private boolean hasTrialChamber(WorldChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int topY = chunk.getHighestNonEmptySectionYOffset() + 16;
        int bottomY = chunk.getBottomY();
        for (BlockPos pos : BlockPos.iterate(
            chunkPos.getStartX(), bottomY, chunkPos.getStartZ(),
            chunkPos.getEndX(), topY - 1, chunkPos.getEndZ()
        )) {
            Block block = chunk.getBlockState(pos).getBlock();
            if (block == Blocks.TRIAL_SPAWNER || block == Blocks.VAULT) return true;
        }
        return false;
    }

    private void onStashFound(BlockPos pos, int count) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String coords = "X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ();
        String message = "[Stash Found] " + coords + " | Blocks: " + count + " | " + timestamp;

        if (chatAlert.get()) info("Stash found at " + coords + " (" + count + " blocks)");
        if (logToFile.get()) saveToFile(message);
        if (sendToWebhook.get()) sendToWebhook(message);
    }

    private void saveToFile(String message) {
        try {
            Path logDir = Paths.get(System.getProperty("user.home"), ".minecraft", "stash-logs");
            Files.createDirectories(logDir);
            String fileName = "stashes-" + LocalDateTime.now().format(FILE_FORMATTER) + ".txt";
            Path logFile = logDir.resolve(fileName);
            Files.writeString(logFile, message + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            error("Failed to write stash log: " + e.getMessage());
        }
    }

    private void sendToWebhook(String message) {
        String url = webhookUrl.get();
        if (url.contains("YOUR_WEBHOOK_HERE") || url.isEmpty()) {
            error("Please set a valid Discord webhook URL!");
            return;
        }
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        String json = "{\"content\": \"**STASH FOUND!**\\n" + escaped + "\"}";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            error("Failed to send webhook: " + e.getMessage());
        }
    }
}
