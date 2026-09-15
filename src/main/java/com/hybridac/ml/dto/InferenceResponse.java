package com.hybridac.ml.dto;

public final class InferenceResponse {
    private double probabilityCheat;
    private String label;
    private String modelVersion;
    private String featureVersion;

    public InferenceResponse() {}

    public InferenceResponse(double probabilityCheat, String label, String modelVersion, String featureVersion) {
        this.probabilityCheat = probabilityCheat;
        this.label = label;
        this.modelVersion = modelVersion;
        this.featureVersion = featureVersion;
    }

    public double probabilityCheat() {
        return probabilityCheat;
    }

    public String label() {
        return label;
    }

    public String modelVersion() {
        return modelVersion;
    }

    public String featureVersion() {
        return featureVersion;
    }

    public void setProbabilityCheat(double probabilityCheat) {
        this.probabilityCheat = probabilityCheat;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public void setFeatureVersion(String featureVersion) {
        this.featureVersion = featureVersion;
    }
}
