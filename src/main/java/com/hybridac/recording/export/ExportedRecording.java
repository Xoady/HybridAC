package com.hybridac.recording.export;

import java.nio.file.Path;

public record ExportedRecording(Path jsonlPath, Path summaryPath) {
}
