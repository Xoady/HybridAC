package com.hybridac.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConfigLoader {

    public HybridConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        ConfigurationSection pluginSection = config.getConfigurationSection("plugin");
        ConfigurationSection alertSection = config.getConfigurationSection("alerts");
        ConfigurationSection bufferSection = config.getConfigurationSection("buffers");
        ConfigurationSection hybridSection = config.getConfigurationSection("hybrid");
        ConfigurationSection botSection = config.getConfigurationSection("bot");
        ConfigurationSection mlSection = config.getConfigurationSection("ml");
        ConfigurationSection storageSection = config.getConfigurationSection("storage");
        ConfigurationSection punishmentSection = config.getConfigurationSection("punishment");
        ConfigurationSection checkSection = config.getConfigurationSection("checks");

        Map<String, CheckRuntimeConfig> checks = new HashMap<>();
        if (checkSection != null) {
            for (String key : checkSection.getKeys(false)) {
                ConfigurationSection entry = checkSection.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                checks.put(key, new CheckRuntimeConfig(
                        entry.getBoolean("enabled", true),
                        entry.getDouble("weight", 1.0D),
                        entry.getDouble("trigger-threshold", 0.6D)
                ));
            }
        }

        String language = config.getString("language", config.getString("plugin.language", "ru"));

        return new HybridConfig(
                language,
                pluginSection != null && pluginSection.getBoolean("enabled", true),
                pluginSection != null && pluginSection.getBoolean("debug", false),
                pluginSection != null ? pluginSection.getDouble("verbose-threshold", 0.65D) : 0.65D,
                lower(config.getStringList("plugin.allowed-worlds")),
                lower(config.getStringList("plugin.blocked-worlds")),
                new AlertSettings(
                        alertSection != null ? alertSection.getString("format", "%player% %score%") : "%player% %score%",
                        alertSection != null ? alertSection.getString("stream-format", "%player% %score%") : "%player% %score%",
                        alertSection != null ? alertSection.getDouble("preview-threshold", 0.18D) : 0.18D,
                        alertSection != null && alertSection.getBoolean("silent-mode", false),
                        alertSection != null ? alertSection.getInt("yellow-threshold", 40) : 40,
                        alertSection != null ? alertSection.getInt("red-threshold", 70) : 70
                ),
                checks,
                new BufferSettings(
                        bufferSection != null ? bufferSection.getDouble("default-decay-per-second", 0.12D) : 0.12D,
                        bufferSection != null ? bufferSection.getDouble("code-max", 1.0D) : 1.0D,
                        bufferSection != null ? bufferSection.getDouble("bot-max", 8.0D) : 8.0D,
                        bufferSection != null ? bufferSection.getDouble("hybrid-decay-per-second", 0.08D) : 0.08D,
                        bufferSection != null ? bufferSection.getInt("window-size", 40) : 40,
                        bufferSection != null ? bufferSection.getInt("minimum-evidence-count", 6) : 6
                ),
                new HybridSettings(
                        hybridSection != null ? hybridSection.getDouble("ml-weight", 1.0D) : 1.0D,
                        hybridSection != null ? hybridSection.getDouble("alert-threshold", 0.72D) : 0.72D,
                        hybridSection != null ? hybridSection.getDouble("punish-threshold", 0.86D) : 0.86D,
                        hybridSection != null ? hybridSection.getDouble("hysteresis", 0.06D) : 0.06D,
                        hybridSection != null ? hybridSection.getInt("minimum-hit-window", 24) : 24,
                        hybridSection != null ? hybridSection.getDouble("smoothing-factor", 0.35D) : 0.35D
                ),
                new BotSettings(
                        botSection != null && botSection.getBoolean("enabled", true),
                        botSection != null ? botSection.getInt("max-active-bots", 16) : 16,
                        botSection != null ? botSection.getLong("spawn-interval-ticks", 60L) : 60L,
                        botSection != null ? botSection.getLong("follow-update-ticks", 1L) : 1L,
                        botSection != null ? botSection.getLong("tab-hide-delay-ticks", 16L) : 16L,
                        botSection != null ? botSection.getInt("min-combat-hits-before-spawn", 5) : 5,
                        botSection != null ? botSection.getDouble("min-distance", 2.2D) : 2.2D,
                        botSection != null ? botSection.getDouble("max-distance", 3.8D) : 3.8D,
                        botSection != null ? botSection.getDouble("follow-distance", 2.45D) : 2.45D,
                        botSection != null ? botSection.getDouble("strafe-amplitude", 1.15D) : 1.15D,
                        botSection != null ? botSection.getDouble("strafe-speed", 0.22D) : 0.22D,
                        botSection != null ? botSection.getDouble("follow-smoothing", 0.30D) : 0.30D,
                        botSection != null ? botSection.getDouble("vertical-offset", 0.05D) : 0.05D,
                        botSection != null ? botSection.getInt("inactivity-timeout-seconds", 20) : 20,
                        botSection != null ? botSection.getInt("confirmation-hit-count", 2) : 2,
                        botSection != null ? botSection.getLong("first-hit-confirm-window-ticks", 8L) : 8L,
                        botSection != null ? botSection.getDouble("first-hit-score", 1.8D) : 1.8D,
                        botSection != null ? botSection.getDouble("repeat-hit-score", 4.8D) : 4.8D,
                        botSection != null ? botSection.getInt("swing-min-interval-ticks", 6) : 6,
                        botSection != null ? botSection.getInt("swing-max-interval-ticks", 14) : 14,
                        botSection != null && botSection.getBoolean("line-of-sight-required", false),
                        botSection != null ? botSection.getString("nick-prefix", "H") : "H"
                ),
                new MlSettings(
                        mlSection != null && mlSection.getBoolean("enabled", true),
                        mlSection != null ? mlSection.getString("base-url", "https://hybridac.ru") : "https://hybridac.ru",
                        mlSection != null ? mlSection.getString("api-key", "") : "",
                        mlSection != null ? mlSection.getString("model", "lite") : "lite",
                        mlSection == null || mlSection.getBoolean("enforce-plan-model", true)
                ),
                new StorageSettings(
                        storageSection != null ? storageSection.getString("sqlite-path", "plugins/HybridAC/hybridac.db") : "plugins/HybridAC/hybridac.db",
                        storageSection != null ? storageSection.getString("dataset-directory", "plugins/HybridAC/datasets") : "plugins/HybridAC/datasets",
                        storageSection != null ? storageSection.getString("session-directory", "plugins/HybridAC/sessions") : "plugins/HybridAC/sessions"
                ),
                new PunishmentSettings(
                        punishmentSection != null && punishmentSection.getBoolean("alert-only", false),
                        punishmentSection != null ? punishmentSection.getInt("minimum-punish-interval-seconds", 45) : 45,
                        loadDetections(punishmentSection)
                ),
                parseMenuSettings(config.getConfigurationSection("menu"))
        );
    }

    private Map<String, DetectionPunishmentSettings> loadDetections(ConfigurationSection punishmentSection) {
        Map<String, DetectionPunishmentSettings> detections = new HashMap<>();
        if (punishmentSection == null) {
            return detections;
        }
        ConfigurationSection detectionsSection = punishmentSection.getConfigurationSection("detections");
        if (detectionsSection == null) {
            return detections;
        }

        for (String key : detectionsSection.getKeys(false)) {
            ConfigurationSection detectionSection = detectionsSection.getConfigurationSection(key);
            if (detectionSection == null) {
                continue;
            }
            List<PunishmentThresholdSettings> thresholds = new ArrayList<>();
            for (Map<?, ?> thresholdEntry : detectionSection.getMapList("thresholds")) {
                double thresholdValue = doubleValue(thresholdEntry.get("threshold"), 1.0D);
                List<PunishmentActionSettings> actions = new ArrayList<>();
                Object actionsObject = thresholdEntry.get("actions");
                if (actionsObject instanceof List<?> actionEntries) {
                    for (Object actionEntry : actionEntries) {
                        if (!(actionEntry instanceof Map<?, ?> actionMap)) {
                            continue;
                        }
                        actions.add(new PunishmentActionSettings(
                                stringValue(actionMap.get("type"), "COMMAND"),
                                doubleValue(actionMap.get("multiplier"), 1.0D),
                                longValue(actionMap.get("duration-seconds"), 30L),
                                stringList(actionMap.get("commands"))
                        ));
                    }
                }
                thresholds.add(new PunishmentThresholdSettings(thresholdValue, actions));
            }
            detections.put(key.toLowerCase(Locale.ROOT), new DetectionPunishmentSettings(
                    detectionSection.getString("mode", "VL"),
                    detectionSection.getLong("session-window-seconds", 120L),
                    thresholds
            ));
        }
        return detections;
    }

    private Set<String> lower(List<String> worlds) {
        return worlds.stream()
                .map(entry -> entry.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> entries) {
            return entries.stream().map(String::valueOf).toList();
        }
        if (value == null) {
            return List.of();
        }
        return List.of(String.valueOf(value));
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private long longValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private MenuSettings parseMenuSettings(ConfigurationSection menuSection) {
        if (menuSection == null) {
            return new MenuSettings(
                    "&dSuspected Players",
                    54,
                    0.60D,
                    "PLAYER_HEAD",
                    "&c%player%",
                    java.util.List.of("&7Probability: &e%probability%%", "&7Confidence: &f%confidence%%", "&7Bot Hits: &f%bot%", "&cClick to teleport"),
                    "BLACK_STAINED_GLASS_PANE",
                    " ",
                    java.util.List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53)
            );
        }
        ConfigurationSection headSection = menuSection.getConfigurationSection("items.player-head");
        ConfigurationSection borderSection = menuSection.getConfigurationSection("items.border-item");
        return new MenuSettings(
                menuSection.getString("title", "&dSuspected Players"),
                menuSection.getInt("size", 54),
                menuSection.getDouble("min-probability", 0.60D),
                headSection != null ? headSection.getString("material", "PLAYER_HEAD") : "PLAYER_HEAD",
                headSection != null ? headSection.getString("name", "&c%player%") : "&c%player%",
                headSection != null ? headSection.getStringList("lore") : java.util.List.of("&7Probability: &e%probability%%", "&7Confidence: &f%confidence%%", "&7Bot Hits: &f%bot%", "&cClick to teleport"),
                borderSection != null ? borderSection.getString("material", "BLACK_STAINED_GLASS_PANE") : "BLACK_STAINED_GLASS_PANE",
                borderSection != null ? borderSection.getString("name", " ") : " ",
                borderSection != null ? borderSection.getIntegerList("slots") : java.util.List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53)
        );
    }
}
