package com.harsh.context_broker.contextBroker.controller;

import tools.jackson.databind.JsonNode;
import com.harsh.context_broker.contextBroker.dto.AlertResponse;
import com.harsh.context_broker.contextBroker.dto.UpdateResponse;
import com.harsh.context_broker.contextBroker.service.IncidentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/incoming")
public class SlackController {

    private static final Pattern INCIDENT_KEY_PATTERN =
            Pattern.compile("(?i)(?:^|[\\s,;])incidentKey\\s*[:=]\\s*([A-Za-z0-9_-]+)");
    private static final Pattern MESSAGE_PATTERN =
            Pattern.compile("(?i)(?:^|[\\s,;])message\\s*[:=]\\s*(.+)$");

    private final IncidentService incidentService;

    public SlackController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping(
            value = "/slack",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> receiveSlack(@RequestBody JsonNode request) {
        String type = request.path("type").asText();

        if ("url_verification".equals(type)) {
            String challenge = request.path("challenge").asText();
            if (challenge.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(challenge);
        }

        if (!"event_callback".equals(type)) {
            return ResponseEntity.ok().build();
        }

        JsonNode event = request.path("event");
        if (event.isMissingNode() || !event.isObject()) {
            return ResponseEntity.ok().build();
        }

        if (!"message".equals(event.path("type").asText())) {
            return ResponseEntity.ok().build();
        }

        if (event.has("bot_id") && !event.path("bot_id").isNull() && !event.path("bot_id").asText().isBlank()) {
            return ResponseEntity.ok().build();
        }
        if (event.has("subtype") && "bot_message".equals(event.path("subtype").asText())) {
            return ResponseEntity.ok().build();
        }

        String text = event.path("text").asText();
        ParsedSlackMessage parsed = parseSlackMessage(text);
        if (parsed == null) {
            return ResponseEntity.ok().build();
        }

        AlertResponse alert = incidentService.handleUpdate(parsed.incidentKey(), parsed.message());

        UpdateResponse response = new UpdateResponse();
        response.setIncidentKey(parsed.incidentKey());
        response.setAlert(alert);

        return ResponseEntity.ok(response);
    }

    private ParsedSlackMessage parseSlackMessage(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher incidentKeyMatcher = INCIDENT_KEY_PATTERN.matcher(text);
        if (!incidentKeyMatcher.find()) {
            return null;
        }

        Matcher messageMatcher = MESSAGE_PATTERN.matcher(text);
        if (!messageMatcher.find()) {
            return null;
        }

        String incidentKey = incidentKeyMatcher.group(1).trim();
        String message = messageMatcher.group(1).trim();

        if (incidentKey.isBlank() || message.isBlank()) {
            return null;
        }

        return new ParsedSlackMessage(incidentKey, message);
    }

    private record ParsedSlackMessage(String incidentKey, String message) {
    }
}
