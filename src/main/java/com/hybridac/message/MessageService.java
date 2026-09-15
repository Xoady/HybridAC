package com.hybridac.message;

import com.hybridac.util.ColorUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class MessageService {

    private final JavaPlugin plugin;
    private final File messagesDir;
    private volatile FileConfiguration configuration;
    private String currentLanguage = "ru";

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesDir = new File(plugin.getDataFolder(), "messages");
    }

    public void load() {
        load(currentLanguage);
    }

    public void load(String language) {
        if (language != null && !language.isBlank()) {
            this.currentLanguage = language.toLowerCase().trim();
        }

        if (!messagesDir.exists()) {
            messagesDir.mkdirs();
        }

        saveDefaultLanguageFile("ru.yml");
        saveDefaultLanguageFile("en.yml");

        File langFile = new File(messagesDir, currentLanguage + ".yml");
        if (!langFile.exists()) {

            langFile = new File(messagesDir, "ru.yml");
            if (!langFile.exists()) {
                langFile = new File(messagesDir, "en.yml");
            }
        }

        if (langFile.exists()) {
            this.configuration = YamlConfiguration.loadConfiguration(langFile);
        } else {

            InputStream stream = plugin.getResource("messages/ru.yml");
            if (stream != null) {
                this.configuration = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            } else {
                this.configuration = new YamlConfiguration();
            }
        }
    }

    private void saveDefaultLanguageFile(String fileName) {
        File targetFile = new File(messagesDir, fileName);
        if (!targetFile.exists()) {
            try {
                plugin.saveResource("messages/" + fileName, false);
            } catch (Throwable ignored) {
            }
        }
    }

    public void reload() {
        load(currentLanguage);
    }

    public void reload(String language) {
        load(language);
    }

    public String format(String path) {
        return format(path, Map.of());
    }

    public String format(String path, Map<String, String> placeholders) {
        String template = current().getString(path, path);
        return ColorUtil.colorize(template, placeholders);
    }

    public List<String> formatList(String path) {
        return formatList(path, Map.of());
    }

    public List<String> formatList(String path, Map<String, String> placeholders) {
        return current().getStringList(path).stream()
                .map(line -> ColorUtil.colorize(line, placeholders))
                .toList();
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(format(path, placeholders));
    }

    public void sendList(CommandSender sender, String path, Map<String, String> placeholders) {
        formatList(path, placeholders).forEach(sender::sendMessage);
    }

    public void sendActionBar(Player player, String path, Map<String, String> placeholders) {
        String formatted = format(path, placeholders);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formatted));
    }

    private FileConfiguration current() {
        if (configuration == null) {
            load();
        }
        return configuration;
    }
}
