package com.hybridac.ml.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hybridac.config.MlSettings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class MlRequestExecutor {

    private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
    private static final int READ_TIMEOUT_MILLIS = 3_000;
    private static final int RETRY_COUNT = 3;
    private static final int RETRY_BACKOFF_MILLIS = 250;

    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private volatile MlSettings settings;
    private volatile HttpClient httpClient;

    public MlRequestExecutor(MlSettings settings, ExecutorService executorService) {
        this.objectMapper = createObjectMapper();
        this.executorService = executorService;
        this.settings = settings;
        this.httpClient = buildClient(settings);
    }

    public void reconfigure(MlSettings settings) {
        this.settings = settings;
        this.httpClient = buildClient(settings);
    }

    public <T, R> CompletableFuture<MlHttpResult<R>> postJson(String path, T payload, Class<R> responseType) {
        return CompletableFuture.supplyAsync(() -> executeWithRetry(path, payload, responseType), executorService);
    }

    static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private <T, R> MlHttpResult<R> executeWithRetry(String path, T payload, Class<R> responseType) {
        IllegalStateException failure = null;
        for (int attempt = 0; attempt < RETRY_COUNT; attempt++) {
            try {
                String body = objectMapper.writeValueAsString(payload);
                java.net.URI uri = URI.create(settings.baseUrl() + path);
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofMillis(READ_TIMEOUT_MILLIS))
                        .header("Content-Type", "application/json");
                if (!settings.apiKey().isBlank()) {
                    builder.header("Authorization", "Bearer " + settings.apiKey());
                }
                HttpResponse<String> response = httpClient.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("ML request failed with status " + response.statusCode()
                            + " for " + path + ": " + summarizeBody(response.body()));
                }
                R mapped = response.body().isBlank() ? null : objectMapper.readValue(response.body(), responseType);
                return new MlHttpResult<>(response.statusCode(), mapped);
            } catch (IOException exception) {
                failure = new IllegalStateException("ML request failed", exception);
                sleepBackoff();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                failure = new IllegalStateException("ML request interrupted", exception);
                sleepBackoff();
            }
        }
        throw failure != null ? failure : new IllegalStateException("ML request failed");
    }

    private String summarizeBody(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }
        String normalized = body.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= 320) {
            return normalized;
        }
        return normalized.substring(0, 320) + "...";
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(RETRY_BACKOFF_MILLIS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private HttpClient buildClient(MlSettings ignored) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MILLIS))
                .executor(executorService)
                .build();
    }
}
