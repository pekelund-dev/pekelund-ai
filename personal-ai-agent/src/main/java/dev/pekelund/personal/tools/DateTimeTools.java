package dev.pekelund.personal.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateTimeTools {

    @Tool(description = "Get the current date and time. Parameter: timeZone (optional timezone ID like 'Europe/Stockholm', 'America/New_York', defaults to system timezone)")
    public String getCurrentDateTime(String timeZone) {
        ZoneId zone = (timeZone != null && !timeZone.isBlank())
                ? ZoneId.of(timeZone) : ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        return "Current date and time: " + now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' HH:mm:ss z"));
    }
}
