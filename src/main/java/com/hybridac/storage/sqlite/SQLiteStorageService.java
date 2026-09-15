package com.hybridac.storage.sqlite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hybridac.model.SuspicionSnapshot;
import com.hybridac.recording.RecordingSession;
import com.hybridac.stats.StatsSnapshot;
import com.hybridac.storage.StorageService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class SQLiteStorageService implements StorageService {

    private final Path databasePath;
    private final ObjectMapper objectMapper;
    private final ExecutorService dbWriteExecutor;
    private HikariDataSource dataSource;

    public SQLiteStorageService(Path databasePath) {
        this.databasePath = databasePath;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.dbWriteExecutor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "HybridAC-DB-Writer"));
    }

    @Override
    public void initialize() throws Exception {
        Files.createDirectories(databasePath.getParent());
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databasePath);
        config.setMaximumPoolSize(4);
        config.setPoolName("HybridAC-SQLite");
        config.setConnectionTestQuery("SELECT 1");
        this.dataSource = new HikariDataSource(config);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String ddl : SqliteSchema.statements()) {
                statement.executeUpdate(ddl);
            }
        }
    }

    @Override
    public void incrementCounter(String key, long delta) {
        dbWriteExecutor.submit(() -> {
            String sql = """
                    INSERT INTO stats(key, value) VALUES (?, ?)
                    ON CONFLICT(key) DO UPDATE SET value = value + excluded.value
                    """;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key);
                statement.setLong(2, delta);
                statement.executeUpdate();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public StatsSnapshot loadStatsSnapshot() {
        Map<String, Long> statMap = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT key, value FROM stats");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                statMap.put(resultSet.getString("key"), resultSet.getLong("value"));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load stats snapshot", exception);
        }

        long legit = statMap.getOrDefault("total_legit_hits", 0L);
        long cheat = statMap.getOrDefault("total_cheat_hits", 0L);
        long recordings = statMap.getOrDefault("total_recordings", 0L);
        long punishments = statMap.getOrDefault("total_punishments", 0L);
        long codeDetections = statMap.getOrDefault("total_code_detections", 0L);
        long mlInferences = statMap.getOrDefault("total_ml_inferences", 0L);
        long botEvidence = statMap.getOrDefault("total_bot_hit_evidences", 0L);

        String modelVersion = "unavailable";
        String trainingTime = "never";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT last_model_version, last_training_time FROM model_metadata WHERE id = 1");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                modelVersion = resultSet.getString("last_model_version");
                trainingTime = resultSet.getString("last_training_time");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load model metadata", exception);
        }

        return new StatsSnapshot(legit, cheat, recordings, punishments, codeDetections, mlInferences, botEvidence, modelVersion, trainingTime);
    }

    @Override
    public void saveRecordingMetadata(RecordingSession session, String filePath) {
        dbWriteExecutor.submit(() -> {
            String sql = """
                    INSERT OR REPLACE INTO recording_metadata(session_id, player_uuid, player_name, label, world, started_at, ended_at, hit_count, file_path)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, session.sessionId().toString());
                statement.setString(2, session.playerUuid().toString());
                statement.setString(3, session.playerName());
                statement.setString(4, session.label().apiValue());
                statement.setString(5, session.world());
                statement.setLong(6, session.startedAt());
                statement.setLong(7, session.endedAt());
                statement.setInt(8, session.hitCount());
                statement.setString(9, filePath);
                statement.executeUpdate();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void savePunishment(UUID playerUuid, String playerName, double score, List<String> reasons) {
        dbWriteExecutor.submit(() -> {
            String sql = "INSERT INTO punish_logs(player_uuid, player_name, score, reasons, created_at) VALUES (?, ?, ?, ?, ?)";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, playerName);
                statement.setDouble(3, score);
                statement.setString(4, objectMapper.writeValueAsString(reasons));
                statement.setLong(5, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void saveMlRequest(UUID playerUuid, String type, int payloadSize, int responseCode, String modelVersion) {
        final String safeModel = (modelVersion != null && !modelVersion.isBlank()) ? modelVersion : "standard";
        dbWriteExecutor.submit(() -> {
            String sql = """
                    INSERT INTO ml_request_history(player_uuid, request_type, payload_size, response_code, model_version, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, type);
                statement.setInt(3, payloadSize);
                statement.setInt(4, responseCode);
                statement.setString(5, safeModel);
                statement.setLong(6, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void saveSuspicionSummary(UUID playerUuid, SuspicionSnapshot snapshot) {
        dbWriteExecutor.submit(() -> {
            String sql = """
                    INSERT INTO suspicion_summaries(player_uuid, code_score, ml_score, bot_score, hybrid_score, evidence_count, reasons, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(player_uuid) DO UPDATE SET
                        code_score = excluded.code_score,
                        ml_score = excluded.ml_score,
                        bot_score = excluded.bot_score,
                        hybrid_score = excluded.hybrid_score,
                        evidence_count = excluded.evidence_count,
                        reasons = excluded.reasons,
                        updated_at = excluded.updated_at
                    """;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUuid.toString());
                statement.setDouble(2, snapshot.codeScore());
                statement.setDouble(3, snapshot.mlScore());
                statement.setDouble(4, snapshot.botScore());
                statement.setDouble(5, snapshot.hybridScore());
                statement.setInt(6, snapshot.evidenceCount());
                statement.setString(7, objectMapper.writeValueAsString(snapshot.reasons()));
                statement.setLong(8, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void saveModelMetadata(String modelVersion, String lastTrainingTime) {
        final String safeModel = (modelVersion != null && !modelVersion.isBlank()) ? modelVersion : "standard";
        final String safeTime = (lastTrainingTime != null && !lastTrainingTime.isBlank()) ? lastTrainingTime : "never";
        dbWriteExecutor.submit(() -> {
            String sql = """
                    INSERT INTO model_metadata(id, last_model_version, last_training_time)
                    VALUES (1, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        last_model_version = excluded.last_model_version,
                        last_training_time = excluded.last_training_time
                    """;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, safeModel);
                statement.setString(2, safeTime);
                statement.executeUpdate();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void close() throws IOException {
        if (dbWriteExecutor != null) {
            dbWriteExecutor.shutdown();
            try {
                dbWriteExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
