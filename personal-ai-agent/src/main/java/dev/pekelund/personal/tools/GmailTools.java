package dev.pekelund.personal.tools;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import dev.pekelund.personal.google.GoogleApiService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;

@Component
public class GmailTools {

    private final GoogleApiService googleApiService;

    public GmailTools(GoogleApiService googleApiService) {
        this.googleApiService = googleApiService;
    }

    @Tool(description = "List emails from Gmail. Parameters: maxResults (number of emails, 1-50, default 10), query (optional Gmail search query like 'from:user@example.com subject:meeting has:attachment')")
    public String listEmails(int maxResults, String query) {
        try {
            Gmail gmail = googleApiService.getGmailService();
            Gmail.Users.Messages.List request = gmail.users().messages().list("me")
                    .setMaxResults((long) (maxResults > 0 ? maxResults : 10));
            if (query != null && !query.isBlank()) {
                request.setQ(query);
            }

            ListMessagesResponse response = request.execute();
            if (response.getMessages() == null || response.getMessages().isEmpty()) {
                return "No emails found.";
            }

            StringBuilder result = new StringBuilder("Emails:\n");
            for (Message msg : response.getMessages()) {
                Message full = gmail.users().messages().get("me", msg.getId())
                        .setFormat("metadata")
                        .setMetadataHeaders(List.of("From", "Subject", "Date"))
                        .execute();
                result.append(String.format("- ID: %s | Date: %s | From: %s | Subject: %s | Preview: %s%n",
                        msg.getId(),
                        getHeader(full, "Date"),
                        getHeader(full, "From"),
                        getHeader(full, "Subject"),
                        full.getSnippet()));
            }
            return result.toString();
        } catch (GeneralSecurityException | IOException e) {
            return "Error listing emails: " + e.getMessage();
        }
    }

    @Tool(description = "Read the full content of an email. Parameter: messageId (the Gmail message ID from listEmails)")
    public String readEmail(String messageId) {
        try {
            Gmail gmail = googleApiService.getGmailService();
            Message message = gmail.users().messages().get("me", messageId)
                    .setFormat("full")
                    .execute();

            return String.format("From: %s%nTo: %s%nDate: %s%nSubject: %s%n%nBody:%n%s",
                    getHeader(message, "From"),
                    getHeader(message, "To"),
                    getHeader(message, "Date"),
                    getHeader(message, "Subject"),
                    extractBody(message));
        } catch (GeneralSecurityException | IOException e) {
            return "Error reading email: " + e.getMessage();
        }
    }

    @Tool(description = "Send an email. Parameters: to (recipient email address), subject (email subject), body (plain text email body)")
    public String sendEmail(String to, String subject, String body) {
        try {
            Gmail gmail = googleApiService.getGmailService();
            String raw = String.format("To: %s\r\nSubject: %s\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n%s",
                    to, subject, body);
            Message message = new Message();
            message.setRaw(Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes()));
            Message sent = gmail.users().messages().send("me", message).execute();
            return "Email sent successfully. Message ID: " + sent.getId();
        } catch (GeneralSecurityException | IOException e) {
            return "Error sending email: " + e.getMessage();
        }
    }

    @Tool(description = "Search emails using Gmail query syntax. Parameters: query (Gmail search query, e.g. 'from:boss@company.com newer_than:7d'), maxResults (max results to return, default 10)")
    public String searchEmails(String query, int maxResults) {
        return listEmails(maxResults > 0 ? maxResults : 10, query);
    }

    private String getHeader(Message message, String name) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) return "N/A";
        return message.getPayload().getHeaders().stream()
                .filter(h -> name.equalsIgnoreCase(h.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst().orElse("N/A");
    }

    private String extractBody(Message message) {
        if (message.getPayload() == null) return "No content";
        String body = extractPartBody(message.getPayload(), "text/plain");
        if (body.isEmpty()) body = extractPartBody(message.getPayload(), "text/html");
        if (body.isEmpty() && message.getPayload().getBody() != null
                && message.getPayload().getBody().getData() != null) {
            body = new String(Base64.getUrlDecoder().decode(message.getPayload().getBody().getData()));
        }
        return body.isEmpty() ? "No readable content" : body;
    }

    private String extractPartBody(MessagePart part, String mimeType) {
        if (mimeType.equals(part.getMimeType()) && part.getBody() != null
                && part.getBody().getData() != null) {
            return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
        }
        if (part.getParts() != null) {
            for (MessagePart sub : part.getParts()) {
                String body = extractPartBody(sub, mimeType);
                if (!body.isEmpty()) return body;
            }
        }
        return "";
    }
}
