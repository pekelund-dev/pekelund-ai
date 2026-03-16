package dev.pekelund.personal.tools;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import dev.pekelund.personal.google.GoogleApiService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CalendarTools {

    private final GoogleApiService googleApiService;

    public CalendarTools(GoogleApiService googleApiService) {
        this.googleApiService = googleApiService;
    }

    @Tool(description = "List upcoming Google Calendar events. Parameters: days (number of days ahead to look, default 7), maxResults (max events to return, default 20)")
    public String listUpcomingEvents(int days, int maxResults) {
        try {
            Calendar calendar = googleApiService.getCalendarService();
            java.util.Calendar now = java.util.Calendar.getInstance();
            DateTime timeMin = new DateTime(now.getTime());
            now.add(java.util.Calendar.DAY_OF_YEAR, days > 0 ? days : 7);
            DateTime timeMax = new DateTime(now.getTime());

            Events events = calendar.events().list("primary")
                    .setMaxResults(maxResults > 0 ? maxResults : 20)
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> items = events.getItems();
            if (items == null || items.isEmpty()) {
                return "No upcoming events found in the next " + (days > 0 ? days : 7) + " days.";
            }

            StringBuilder result = new StringBuilder("Upcoming events:\n");
            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) start = event.getStart().getDate();
                result.append(String.format("- ID: %s | Start: %s | Summary: %s | Location: %s%n",
                        event.getId(),
                        start,
                        event.getSummary(),
                        event.getLocation() != null ? event.getLocation() : "N/A"));
            }
            return result.toString();
        } catch (GeneralSecurityException | IOException e) {
            return "Error listing events: " + e.getMessage();
        }
    }

    @Tool(description = "Create a new Google Calendar event. Parameters: summary (event title), description (optional event description), startDateTime (ISO-8601 datetime like '2024-01-15T10:00:00'), endDateTime (ISO-8601 datetime like '2024-01-15T11:00:00'), timeZone (timezone ID like 'Europe/Stockholm', default 'UTC'), attendeeEmails (comma-separated email addresses, optional)")
    public String createEvent(String summary, String description, String startDateTime,
                              String endDateTime, String timeZone, String attendeeEmails) {
        try {
            Calendar calendar = googleApiService.getCalendarService();
            String tz = timeZone != null && !timeZone.isBlank() ? timeZone : "UTC";

            Event event = new Event().setSummary(summary);
            if (description != null && !description.isBlank()) {
                event.setDescription(description);
            }

            event.setStart(new EventDateTime()
                    .setDateTime(new DateTime(startDateTime))
                    .setTimeZone(tz));
            event.setEnd(new EventDateTime()
                    .setDateTime(new DateTime(endDateTime))
                    .setTimeZone(tz));

            if (attendeeEmails != null && !attendeeEmails.isBlank()) {
                List<EventAttendee> attendees = Arrays.stream(attendeeEmails.split(","))
                        .map(String::trim)
                        .filter(e -> !e.isEmpty())
                        .map(e -> new EventAttendee().setEmail(e))
                        .collect(Collectors.toList());
                event.setAttendees(attendees);
            }

            Event created = calendar.events().insert("primary", event).execute();
            return "Event created successfully. ID: " + created.getId() + ", Link: " + created.getHtmlLink();
        } catch (GeneralSecurityException | IOException e) {
            return "Error creating event: " + e.getMessage();
        }
    }

    @Tool(description = "Get details of a specific Google Calendar event. Parameter: eventId (the calendar event ID from listUpcomingEvents)")
    public String getEvent(String eventId) {
        try {
            Calendar calendar = googleApiService.getCalendarService();
            Event event = calendar.events().get("primary", eventId).execute();

            StringBuilder result = new StringBuilder();
            result.append("ID: ").append(event.getId()).append("\n");
            result.append("Summary: ").append(event.getSummary()).append("\n");
            result.append("Description: ").append(event.getDescription() != null ? event.getDescription() : "N/A").append("\n");
            result.append("Start: ").append(event.getStart().getDateTime() != null
                    ? event.getStart().getDateTime() : event.getStart().getDate()).append("\n");
            result.append("End: ").append(event.getEnd().getDateTime() != null
                    ? event.getEnd().getDateTime() : event.getEnd().getDate()).append("\n");
            result.append("Location: ").append(event.getLocation() != null ? event.getLocation() : "N/A").append("\n");
            if (event.getAttendees() != null) {
                result.append("Attendees: ").append(event.getAttendees().stream()
                        .map(EventAttendee::getEmail).collect(Collectors.joining(", "))).append("\n");
            }
            return result.toString();
        } catch (GeneralSecurityException | IOException e) {
            return "Error getting event: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing Google Calendar event. Parameters: eventId (event ID to update), summary (new title, optional), description (new description, optional), startDateTime (new start in ISO-8601, optional), endDateTime (new end in ISO-8601, optional), timeZone (timezone ID, optional)")
    public String updateEvent(String eventId, String summary, String description,
                              String startDateTime, String endDateTime, String timeZone) {
        try {
            Calendar calendar = googleApiService.getCalendarService();
            Event event = calendar.events().get("primary", eventId).execute();

            if (summary != null && !summary.isBlank()) event.setSummary(summary);
            if (description != null && !description.isBlank()) event.setDescription(description);

            String tz = timeZone != null && !timeZone.isBlank() ? timeZone : "UTC";
            if (startDateTime != null && !startDateTime.isBlank()) {
                event.setStart(new EventDateTime().setDateTime(new DateTime(startDateTime)).setTimeZone(tz));
            }
            if (endDateTime != null && !endDateTime.isBlank()) {
                event.setEnd(new EventDateTime().setDateTime(new DateTime(endDateTime)).setTimeZone(tz));
            }

            Event updated = calendar.events().update("primary", eventId, event).execute();
            return "Event updated successfully. ID: " + updated.getId();
        } catch (GeneralSecurityException | IOException e) {
            return "Error updating event: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a Google Calendar event. Parameter: eventId (the event ID to delete)")
    public String deleteEvent(String eventId) {
        try {
            Calendar calendar = googleApiService.getCalendarService();
            calendar.events().delete("primary", eventId).execute();
            return "Event deleted successfully. ID: " + eventId;
        } catch (GeneralSecurityException | IOException e) {
            return "Error deleting event: " + e.getMessage();
        }
    }
}
