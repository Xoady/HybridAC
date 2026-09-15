package com.hybridac.command.subcommands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACSubcommand;
import com.hybridac.storage.CredentialsStorage;
import com.hybridac.util.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class AuthSubcommand implements HybridACSubcommand {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String name() {
        return "auth";
    }

    @Override
    public String permission() {
        return "hybridac.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, CommandContext context) {
        if (args.length < 2) {
            context.messageService().send(sender, "commands.auth.usage");
            return true;
        }

        String authCode = args[1].trim();
        context.messageService().send(sender, "commands.auth.connecting");

        TaskUtil.runAsync(context.plugin(), () -> {
            try {
                String baseUrl = context.config().ml().baseUrl();
                if (baseUrl == null || baseUrl.isBlank()) {
                    baseUrl = "https://hybridac.ru";
                }
                while (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                if (baseUrl.endsWith("/api/v1/ml")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - "/api/v1/ml".length());
                } else if (baseUrl.endsWith("/api/v1")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - "/api/v1".length());
                }

                int port = Bukkit.getPort();
                String hostname = Bukkit.getIp();
                if (hostname == null || hostname.isBlank()) {
                    hostname = "127.0.0.1";
                }

                Map<String, Object> payload = Map.of(
                        "code", authCode,
                        "port", port,
                        "hostname", hostname
                );

                String jsonBody = mapper.writeValueAsString(payload);
                String[] endpoints = new String[]{ "/api/v1/server/auth", "/server/auth", "/auth" };
                HttpResponse<String> response = null;

                for (String ep : endpoints) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + ep))
                            .timeout(Duration.ofSeconds(10))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();
                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 404) {
                        break;
                    }
                }

                if (response != null && response.statusCode() >= 200 && response.statusCode() < 300) {
                    JsonNode root = mapper.readTree(response.body());
                    boolean success = root.path("success").asBoolean(false);
                    if (success) {
                        String apiKey = root.path("api_key").asText("");
                        String mlBearer = root.path("ml_bearer").asText("");
                        String serverName = root.path("server_name").asText("Minecraft Server");
                        String plan = root.path("plan_name").asText("Free");
                        String model = root.path("model").asText("standard");

                        CredentialsStorage storage = context.plugin().credentialsStorage();
                        if (storage != null) {
                            storage.save(new CredentialsStorage.CredentialsRecord(
                                    apiKey,
                                    mlBearer,
                                    serverName,
                                    plan,
                                    model,
                                    System.currentTimeMillis()
                            ));
                        }

                        TaskUtil.runGlobal(context.plugin(), () -> {
                            context.plugin().reloadRuntime();
                            context.messageService().sendList(sender, "commands.auth.success-lines", Map.of(
                                    "server", serverName,
                                    "plan", plan,
                                    "model", model
                            ));
                        });
                        return;
                    }
                }

                String errorMsg = response != null ? "HTTP " + response.statusCode() : "No response";
                if (response != null && response.body() != null) {
                    try {
                        JsonNode errNode = mapper.readTree(response.body());
                        if (errNode.has("message")) {
                            errorMsg = errNode.get("message").asText();
                        }
                    } catch (Exception ignored) {
                    }
                }

                String finalErr = errorMsg;
                TaskUtil.runGlobal(context.plugin(), () -> {
                    context.messageService().send(sender, "commands.auth.failed", Map.of("error", finalErr));
                });

            } catch (Exception e) {
                TaskUtil.runGlobal(context.plugin(), () -> {
                    context.messageService().send(sender, "commands.auth.connection-error", Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
                });
            }
        });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, CommandContext context) {
        if (args.length == 2) {
            return List.of("<auth_code>");
        }
        return List.of();
    }
}
