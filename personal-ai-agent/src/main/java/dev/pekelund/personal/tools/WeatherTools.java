package dev.pekelund.personal.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class WeatherTools {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WeatherTools() {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    @Tool(description = "Get current weather and 7-day forecast for a location. Parameter: location (city name or 'city, country' like 'Stockholm' or 'London, UK')")
    public String getWeather(String location) {
        try {
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            String geocodeUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                + encodedLocation + "&count=1&language=en&format=json";
            String geocodeResponse = restClient.get().uri(geocodeUrl).retrieve().body(String.class);

            JsonNode geocodeJson = objectMapper.readTree(geocodeResponse);
            if (!geocodeJson.has("results") || geocodeJson.get("results").isEmpty()) {
                return "Location not found: " + location;
            }

            JsonNode place = geocodeJson.get("results").get(0);
            double lat = place.get("latitude").asDouble();
            double lon = place.get("longitude").asDouble();
            String placeName = place.get("name").asText();
            String country = place.has("country") ? place.get("country").asText() : "";

            String weatherUrl = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
                "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weathercode" +
                "&daily=temperature_2m_max,temperature_2m_min,weathercode" +
                "&timezone=auto&forecast_days=7", lat, lon);
            String weatherResponse = restClient.get().uri(weatherUrl).retrieve().body(String.class);

            JsonNode weatherJson = objectMapper.readTree(weatherResponse);
            JsonNode current = weatherJson.get("current");

            StringBuilder result = new StringBuilder();
            result.append(String.format("Weather for %s, %s:%n", placeName, country));
            result.append(String.format("Current: %.1f°C, Humidity: %d%%, Wind: %.1f km/h%n",
                    current.get("temperature_2m").asDouble(),
                    current.get("relative_humidity_2m").asInt(),
                    current.get("wind_speed_10m").asDouble()));

            JsonNode daily = weatherJson.get("daily");
            result.append("\n7-day forecast:\n");
            for (int i = 0; i < 7; i++) {
                result.append(String.format("- %s: High %.1f°C, Low %.1f°C%n",
                        daily.get("time").get(i).asText(),
                        daily.get("temperature_2m_max").get(i).asDouble(),
                        daily.get("temperature_2m_min").get(i).asDouble()));
            }
            return result.toString();
        } catch (Exception e) {
            return "Error getting weather: " + e.getMessage();
        }
    }
}
