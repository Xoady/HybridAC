package com.hybridac.storage;

import com.hybridac.model.SuspicionSnapshot;
import com.hybridac.recording.RecordingSession;
import com.hybridac.stats.StatsSnapshot;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface StorageService extends Closeable {

    void initialize() throws Exception;

    void incrementCounter(String key, long delta);

    StatsSnapshot loadStatsSnapshot();

    void saveRecordingMetadata(RecordingSession session, String filePath);

    void savePunishment(UUID playerUuid, String playerName, double score, List<String> reasons);

    void saveMlRequest(UUID playerUuid, String type, int payloadSize, int responseCode, String modelVersion);

    void saveSuspicionSummary(UUID playerUuid, SuspicionSnapshot snapshot);

    void saveModelMetadata(String modelVersion, String lastTrainingTime);

    @Override
    void close() throws IOException;
}
