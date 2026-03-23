package com.expensetracker.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.io.IOException;
import java.util.Base64;

@Service
public class MistralService {

    @Value("${mistral.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, String> extractExpenseFromImage(MultipartFile file) throws IOException {
        // Convert image to base64
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = file.getContentType();

        // Build Mistral API request
        Map<String, Object> imageUrl = new HashMap<>();
        imageUrl.put("type", "image_url");
        Map<String, String> urlObj = new HashMap<>();
        urlObj.put("url", "data:" + mimeType + ";base64," + base64Image);
        imageUrl.put("image_url", urlObj);

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text",
            "This is a payment screenshot. Extract the following details and respond ONLY in this exact JSON format:\n" +
            "{\n" +
            "  \"amount\": \"numeric amount only no symbols\",\n" +
            "  \"description\": \"merchant or payment description\",\n" +
            "  \"date\": \"YYYY-MM-DD format if found else today\",\n" +
            "  \"category\": \"one of: Food, Transport, Shopping, Bills, Entertainment, Health, Education, Other\"\n" +
            "}\n" +
            "Rules:\n" +
            "- amount must be numbers only like 500 or 1200.50\n" +
            "- category must match exactly one of the given options\n" +
            "- if date not found use " + java.time.LocalDate.now().toString() + "\n" +
            "- respond with JSON only no extra text"
        );

        List<Object> contents = new ArrayList<>();
        contents.add(textContent);
        contents.add(imageUrl);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", contents);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "pixtral-12b-2409");
        requestBody.put("messages", List.of(message));
        requestBody.put("max_tokens", 300);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            "https://api.mistral.ai/v1/chat/completions",
            HttpMethod.POST,
            request,
            Map.class
        );

        // Parse response
        Map responseBody = response.getBody();
        List choices = (List) responseBody.get("choices");
        Map choice = (Map) choices.get(0);
        Map messageResponse = (Map) choice.get("message");
        String content = (String) messageResponse.get("content");
        System.out.println("=== MISTRAL RESPONSE ===");
        System.out.println(content);
        System.out.println("========================");

        // Clean JSON response
        content = content.trim();
        if (content.startsWith("```")) {
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
        }

     // Parse JSON properly - handles both string and number values
        Map<String, String> result = new HashMap<>();
        try {
            content = content.replaceAll("\\s+", " ").trim();

            // Match string values: "key": "value"
            java.util.regex.Pattern stringPattern = java.util.regex.Pattern.compile("\"(\\w+)\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher stringMatcher = stringPattern.matcher(content);
            while (stringMatcher.find()) {
                result.put(stringMatcher.group(1), stringMatcher.group(2));
            }

            // Match number values: "key": 123
            java.util.regex.Pattern numberPattern = java.util.regex.Pattern.compile("\"(\\w+)\"\\s*:\\s*([0-9]+\\.?[0-9]*)");
            java.util.regex.Matcher numberMatcher = numberPattern.matcher(content);
            while (numberMatcher.find()) {
                result.put(numberMatcher.group(1), numberMatcher.group(2));
            }

        } catch (Exception e) {
            result.put("error", "Could not parse response");
        }

        return result;
    }
}