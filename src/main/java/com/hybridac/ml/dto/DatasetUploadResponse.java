package com.hybridac.ml.dto;

public final class DatasetUploadResponse {
    private boolean accepted;
    private String storedPath;
    private int totalHits;

    public DatasetUploadResponse() {}

    public DatasetUploadResponse(boolean accepted, String storedPath, int totalHits) {
        this.accepted = accepted;
        this.storedPath = storedPath;
        this.totalHits = totalHits;
    }

    public boolean accepted() {
        return accepted;
    }

    public String storedPath() {
        return storedPath;
    }

    public int totalHits() {
        return totalHits;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public void setStoredPath(String storedPath) {
        this.storedPath = storedPath;
    }

    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }
}
