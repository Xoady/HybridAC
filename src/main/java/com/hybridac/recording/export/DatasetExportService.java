package com.hybridac.recording.export;

import com.hybridac.recording.RecordingSession;

import java.io.IOException;

public interface DatasetExportService {

    ExportedRecording export(RecordingSession session) throws IOException;
}
