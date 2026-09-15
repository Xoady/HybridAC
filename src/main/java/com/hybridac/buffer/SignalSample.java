package com.hybridac.buffer;

import java.time.Instant;

public record SignalSample(String signalId, double score, Instant timestamp) {
}
