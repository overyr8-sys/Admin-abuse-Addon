package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatLogger extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> logToFile = sgGeneral.add(new BoolSetting.Builder()
        .name("log-to-file")
        .description("Save chat messages to a TXT file.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logToWebhook = sgGeneral.add(new BoolSetting.Builder()
        .name("log-to-webhook")
        .description("Send chat messages to a Discord webhook.")
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

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ChatLogger() {
        super(AddonTemplate.CATEGORY, "chat-logger", "Logs all chat messages to a TXT file and/or Discord webhook.");
    }

    @Override
    public void onActivate() {
        info("Chat Logger enabled.");
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        String message = event.getMessage().getString();
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logLine = "[" + timestamp + "] " + message;

        if (logToFile.get()) {
            saveToFile(logLine);
        }

        if (logToWebhook.get()) {
            sendToWebhook(logLine);
        }
    }

    private void saveToFile(String logLine) {
        try {
            Path logDir = Paths.get(System.getProperty("user.home"), ".minecraft", "chat-logs");
            Files.createDirectories(logDir);

            String fileName = "chat-" + LocalDateTime.now().format(FILE_FORMATTER) + ".txt";
            Path logFile = logDir.resolve(fileName);

            Files.writeString(logFile, logLine + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            error("Failed to write to log file: " + e.getMessage());
        }
    }

    private void sendToWebhook(String logLine) {
        String url = webhookUrl.get();
        if (url.contains("YOUR_WEBHOOK_HERE") || url.isEmpty()) {
            error("Please set a valid Discord webhook URL!");
            return;
        }

        String escaped = logLine.replace("\\", "\\\\").replace("\"", "\\\"");
        String json = "{\"content\": \"" + escaped + "\"}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            error("Failed to send to webhook: " + e.getMessage());
        }
    }
}
