package com.hybridac.violation;

import com.hybridac.buffer.DecayingBuffer;
import com.hybridac.buffer.SignalSample;
import com.hybridac.model.MlInferenceVerdict;
import com.hybridac.model.SuspicionSnapshot;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class PlayerSuspicionState {

    private static final long SIGNAL_RETENTION_MILLIS = 30_000L;
    private static final long ML_FADE_MILLIS = 4_000L;

    private final DecayingBuffer codeBuffer;
    private final DecayingBuffer hybridBuffer;
    private final Deque<SignalSample> recentSignals = new ArrayDeque<>();
    private final Deque<Double> mlHistory = new ArrayDeque<>();

    private double lastMlProbability;
    private String lastModelVersion = "unavailable";
    private double smoothedConfidence;
    private long lastPunishMillis;
    private Instant lastMlVerdictAt = Instant.EPOCH;
    private boolean recordConfidenceHistoryPending;

    public PlayerSuspicionState(double decayPerSecond, double codeMax, double botMax, double hybridDecayPerSecond) {
        this.codeBuffer = new DecayingBuffer(decayPerSecond, codeMax);
        this.hybridBuffer = new DecayingBuffer(hybridDecayPerSecond, 1.0D);
    }

    public synchronized void recordCodeSignal(String signalId, double score) {
        codeBuffer.add(score);
        recentSignals.addLast(new SignalSample(signalId, score, Instant.now()));
        trimSignals();
    }

    public synchronized void recordMlVerdict(MlInferenceVerdict verdict) {
        this.lastMlProbability = verdict.probabilityCheat();
        this.lastModelVersion = verdict.modelVersion();
        this.lastMlVerdictAt = verdict.inferredAt();
        this.recordConfidenceHistoryPending = true;
        if (verdict.probabilityCheat() >= 0.60D) {
            recentSignals.addLast(new SignalSample("ml_window", verdict.probabilityCheat(), verdict.inferredAt()));
            trimSignals();
        }
    }

    public synchronized double codeNormalized() {
        return codeBuffer.normalized();
    }

    public synchronized double botNormalized() {
        return 0.0D;
    }

    public synchronized double lastMlProbability() {
        if (lastMlProbability <= 0.0D || lastMlVerdictAt.equals(Instant.EPOCH)) {
            return 0.0D;
        }
        long ageMillis = Math.max(0L, java.time.Duration.between(lastMlVerdictAt, Instant.now()).toMillis());
        if (ageMillis >= ML_FADE_MILLIS) {
            return 0.0D;
        }
        double freshness = 1.0D - (ageMillis / (double) ML_FADE_MILLIS);
        return lastMlProbability * freshness;
    }

    public synchronized String lastModelVersion() {
        return lastModelVersion;
    }

    public synchronized double lastMlProbabilityRaw() {
        return lastMlProbability;
    }

    public synchronized double updateSmoothedConfidence(double candidate, double smoothingFactor) {
        smoothedConfidence = (smoothedConfidence * (1.0D - smoothingFactor)) + (candidate * smoothingFactor);
        hybridBuffer.add(candidate);
        if (recordConfidenceHistoryPending) {
            mlHistory.addLast(Math.max(candidate, smoothedConfidence));
            if (mlHistory.size() > 6) {
                mlHistory.removeFirst();
            }
            recordConfidenceHistoryPending = false;
        }
        return smoothedConfidence;
    }

    public synchronized double smoothedConfidence() {
        return smoothedConfidence;
    }

    public synchronized int evidenceCount() {
        trimSignals();
        return recentSignals.size();
    }

    public synchronized boolean hasFreshEvidence(long maxAgeMillis) {
        trimSignals();
        Instant threshold = Instant.now().minusMillis(maxAgeMillis);
        return recentSignals.stream().anyMatch(sample -> !sample.timestamp().isBefore(threshold));
    }

    public synchronized boolean hasFreshMlVerdict(long maxAgeMillis) {
        return !lastMlVerdictAt.equals(Instant.EPOCH)
                && !lastMlVerdictAt.isBefore(Instant.now().minusMillis(maxAgeMillis));
    }

    public synchronized List<String> recentReasons() {
        trimSignals();
        return recentSignals.stream()
                .map(SignalSample::signalId)
                .distinct()
                .toList();
    }

    public synchronized SuspicionSnapshot snapshot(double rawHybrid) {
        return new SuspicionSnapshot(
                codeNormalized(),
                lastMlProbability,
                0.0D,
                rawHybrid,
                smoothedConfidence,
                evidenceCount(),
                new ArrayList<>(recentReasons())
        );
    }

    public synchronized long lastPunishMillis() {
        return lastPunishMillis;
    }

    public synchronized void markPunished() {
        this.lastPunishMillis = System.currentTimeMillis();
    }

    public synchronized void reset() {
        codeBuffer.reset();
        hybridBuffer.reset();
        recentSignals.clear();
        mlHistory.clear();
        lastMlProbability = 0.0D;
        lastModelVersion = "unavailable";
        smoothedConfidence = 0.0D;
        lastPunishMillis = 0L;
        lastMlVerdictAt = Instant.EPOCH;
        recordConfidenceHistoryPending = false;
    }

    public synchronized List<Double> getMlHistory() {
        return new ArrayList<>(mlHistory);
    }

    private void trimSignals() {
        Instant threshold = Instant.now().minusMillis(SIGNAL_RETENTION_MILLIS);
        while (!recentSignals.isEmpty() && recentSignals.peekFirst().timestamp().isBefore(threshold)) {
            recentSignals.removeFirst();
        }
    }
}
