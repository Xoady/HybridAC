package com.hybridac.recording.export;

import com.hybridac.model.HitSample;
import com.hybridac.model.HitboxPart;
import com.hybridac.model.RecordingLabel;
import com.hybridac.model.VectorSnapshot;
import com.hybridac.recording.RecordingSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonlDatasetWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void exportsJsonlAndSummary() throws Exception {
        RecordingSession session = new RecordingSession(UUID.randomUUID(), "Tester", RecordingLabel.LEGIT, "world", UUID.randomUUID(), "Recorder");
        session.append(new HitSample(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PLAYER",
                "world",
                System.currentTimeMillis(),
                50,
                8.0D,
                10.0D,
                1.0D,
                2.0D,
                0.5D,
                1.5D,
                0.4D,
                0.1D,
                0.2D,
                3.0D,
                new VectorSnapshot(1.0D, 0.0D, 0.0D),
                new VectorSnapshot(1.0D, 0.0D, 0.0D),
                0.03D,
                HitboxPart.CHEST,
                true,
                true,
                new VectorSnapshot(0.1D, 0.0D, 0.0D),
                0.0D,
                0.2D,
                false,
                false,
                0,
                0.9D,
                true,
                0.99D,
                false,
                false,
                Map.of("rotation_precision", 0.6D)
        ));
        session.close();

        JsonlDatasetWriter writer = new JsonlDatasetWriter(tempDir.resolve("datasets"), tempDir.resolve("sessions"));
        ExportedRecording exported = writer.export(session);

        assertTrue(Files.exists(exported.jsonlPath()));
        assertTrue(Files.exists(exported.summaryPath()));
        assertEquals(1, Files.readAllLines(exported.jsonlPath()).size());
    }
}
