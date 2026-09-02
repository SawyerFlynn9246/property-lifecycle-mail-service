package dev.infrai.propertymail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public final class InfraiEmailClient {
    private static final int MAX_ATTEMPTS = 4;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json;
    private final PropertyMailConfig config;
    private final String apiKey;

    public InfraiEmailClient(ObjectMapper json, PropertyMailConfig config) {
        this.json = json;
        this.config = config;
        this.apiKey = requireApiKey(System.getenv("INFRAI_API_KEY"));
    }

    public SendResult send(String to, PropertyTemplateCatalog.RenderedMail mail, String eventId) {
        byte[] body;
        try {
            body = json.writeValueAsBytes(Map.of("to", to, "subject", mail.subject(), "html", mail.html()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not encode email request", e);
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.infraiBaseUrl() + "/v1/email/send"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", "property-mail-" + eventId)
                    .method("POST", HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (!envelope.path("ok").asBoolean()) {
                    if (response.statusCode() == 429 && attempt + 1 < MAX_ATTEMPTS) {
                        pause(retryDelayMillis(response, attempt));
                        continue;
                    }
                    JsonNode error = envelope.path("error");
                    throw new InfraiException(error.path("code").asText("EMAIL_REJECTED"),
                            error.path("message").asText("Email request was rejected"), response.statusCode());
                }
                return new SendResult(envelope.path("data").path("message_id").asText(), envelope.path("metadata"));
            } catch (IOException e) {
                throw new IllegalStateException("Could not decode email response", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Email request interrupted", e);
            }
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> Long.parseLong(value) * 1_000L)
                .orElse(250L * (1L << attempt));
    }

    private static void pause(long millis) throws InterruptedException { Thread.sleep(millis); }

    private static String requireApiKey(String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        return value;
    }

    public record SendResult(String messageId, JsonNode metadata) {}
    public static final class InfraiException extends RuntimeException {
        private final String code;
        private final int status;
        public InfraiException(String code, String message, int status) { super(message); this.code = code; this.status = status; }
        public String code() { return code; }
        public int status() { return status; }
    }
}
