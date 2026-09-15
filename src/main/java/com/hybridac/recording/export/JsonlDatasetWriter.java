package com.hybridac.recording.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hybridac.recording.RecordingSession;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonlDatasetWriter implements DatasetExportService {

    private final Path datasetDirectory;
    private final Path sessionDirectory;
    private final ObjectMapper objectMapper;

    public JsonlDatasetWriter(Path datasetDirectory, Path sessionDirectory) {
        this.datasetDirectory = datasetDirectory;
        this.sessionDirectory = sessionDirectory;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public ExportedRecording export(RecordingSession session) throws IOException {
        Files.createDirectories(datasetDirectory);
        Files.createDirectories(sessionDirectory);

        String baseName = session.label().apiValue() + "_" + session.sessionId();
        Path jsonlPath = datasetDirectory.resolve(baseName + ".jsonl");
        Path summaryPath = sessionDirectory.resolve(baseName + ".json");

        try (BufferedWriter writer = Files.newBufferedWriter(jsonlPath)) {
            for (var hit : session.hits()) {
                writer.write(objectMapper.writeValueAsString(hit));
                writer.newLine();
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("session_id", session.sessionId());
        summary.put("player_uuid", session.playerUuid());
        summary.put("player_name", session.playerName());
        summary.put("label", session.label().apiValue());
        summary.put("world", session.world());
        summary.put("started_at", session.startedAt());
        summary.put("ended_at", session.endedAt());
        summary.put("hit_count", session.hitCount());
        summary.put("dataset_file", jsonlPath.toString());
        Files.writeString(summaryPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary));

        return new ExportedRecording(jsonlPath, summaryPath);
    }
}
