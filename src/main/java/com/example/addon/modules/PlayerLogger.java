package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerLogger extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> logToFile = sgGeneral.add(new BoolSetting.Builder()
        .name("log-to-file")
        .description("Save join/leave events to a TXT file.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logToWebhook = sgGeneral.add(new BoolSetting.Builder()
        .name("log-to-webhook")
        .description("Send join/leave events to a Discord webhook.")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> webhookUrl = sgGeneral.add(new StringSetting.Builder()
        .name("webhook-url")
        .description("Your Discord webhook URL.")
        .defaultValue("https://discord.com/api/webhooks/YOUR_WEBHOOK_HERE")
        .visible(logToWebhook::get)
        .build()
    );

    private final Setting<Boolean> chatAlert = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-alert")
        .description("Show join/leave alerts in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showCoords = sgGeneral.add(new BoolSetting.Builder()
        .name("show-coords")
        .description("Log your current coords when a player joins/leaves.")
        .defaultValue(true)
        .build()
    );

    private final Map<UUID, String> onlinePlayers = new HashMap<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public PlayerLogger() {
        super(AddonTemplate.CATEGORY, "player-logger", "Logs when players join and leave the server with timestamps and coords.");
    }

    @Override
    public void onActivate() {
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                onlinePlayers.put(entry.getProfile().id(), entry.getProfile().name());
            }
        }
    }

    @Override
    public void onDeactivate() {
        onlinePlayers.clear();
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        onlinePlayers.clear();
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlayerListS2CPacket packet)) return;
        if (mc.player == null) return;

        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            UUID uuid = entry.profileId();
            String name = entry.profile() != null ? entry.profile().name() : "Unknown";
            if (name == null) name = "Unknown";

            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                if (!onlinePlayers.containsKey(uuid) && !name.equals(mc.player.getName().getString())) {
                    onlinePlayers.put(uuid, name);
                    logEvent(name, true);
                }
            }
        }

        // Check for removed players by comparing current list
        if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) return;

        // Handle removes by checking who left
        java.util.Set<UUID> currentOnline = new java.util.HashSet<>();
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                currentOnline.add(e.getProfile().id());
            }
        }

        java.util.Iterator<Map.Entry<UUID, String>> iter = onlinePlayers.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, String> e = iter.next();
            if (!currentOnline.contains(e.getKey())) {
                logEvent(e.getValue(), false);
                iter.remove();
            }
        }
    }

    private void logEvent(String playerName, boolean joined) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String action = joined ? "joined" : "left";

        String coords = "";
        if (showCoords.get() && mc.player != null) {
            coords = String.format(" | Your coords: %.0f %.0f %.0f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ());
        }

        String message = String.format("[%s] %s %s%s", timestamp, playerName, action, coords);

        if (chatAlert.get()) {
            info(playerName + " " + action);
        }

        if (logToFile.get()) {
            saveToFile(message);
        }

        if (logToWebhook.get()) {
            sendToWebhook(message, joined);
        }
    }

    private void saveToFile(String message) {
        try {
            Path logDir = Paths.get(System.getProperty("user.home"), ".minecraft", "player-logs");
            Files.createDirectories(logDir);
            String fileName = "players-" + LocalDateTime.now().format(FILE_FORMATTER) + ".txt";
            Path logFile = logDir.resolve(fileName);
            Files.writeString(logFile, message + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            error("Failed to write player log: " + e.getMessage());
        }
    }

    private void sendToWebhook(String message, boolean joined) {
        String url = webhookUrl.get();
        if (url.contains("YOUR_WEBHOOK_HERE") || url.isEmpty()) {
            error("Please set a valid Discord webhook URL!");
            return;
        }

        String emoji = joined ? "+" : "-";
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        String json = "{\"content\": \"[" + emoji + "] " + escaped + "\"}";

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
