package com.hybridac.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Level;

public final class CredentialsStorage {

    private final File file;
    private final ObjectMapper mapper;
    private final JavaPlugin plugin;
    private volatile CredentialsRecord cached;

    public CredentialsStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mapper = new ObjectMapper();
        File dir = new File(plugin.getDataFolder(), "storage");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.file = new File(dir, "credentials.json");
        this.cached = readFromFile().orElse(null);
    }

    public record CredentialsRecord(
            String apiKey,
            String mlBearer,
            String serverName,
            String plan,
            String model,
            long pairedAt
    ) {
    }

    public synchronized Optional<CredentialsRecord> load() {
        if (cached != null) {
            return Optional.of(cached);
        }
        cached = readFromFile().orElse(null);
        return Optional.ofNullable(cached);
    }

    public synchronized void save(CredentialsRecord record) {
        this.cached = record;
        try {
            File tempFile = new File(file.getParentFile(), "credentials.json.tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tempFile, record);
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save credentials to " + file.getAbsolutePath(), e);
        }
    }

    public synchronized void clear() {
        this.cached = null;
        if (file.exists()) {
            file.delete();
        }
    }

    public boolean hasCredentials() {
        return load().map(r -> (r.apiKey() != null && !r.apiKey().isBlank()) || (r.mlBearer() != null && !r.mlBearer().isBlank())).orElse(false);
    }

    public Optional<CredentialsRecord> cached() {
        return Optional.ofNullable(cached);
    }

    private Optional<CredentialsRecord> readFromFile() {
        if (!file.exists() || file.length() == 0) {
            return Optional.empty();
        }
        try {
            CredentialsRecord record = mapper.readValue(file, CredentialsRecord.class);
            return Optional.ofNullable(record);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse credentials from " + file.getAbsolutePath(), e);
            return Optional.empty();
        }
    }
}
