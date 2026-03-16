package dev.pekelund.personal.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class NotesTools {

    private final ObjectMapper objectMapper;

    @Value("${personal.notes.file:notes.json}")
    private String notesFile;

    public NotesTools(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Save a personal note. Parameters: title (short title for the note), content (the note content)")
    public String saveNote(String title, String content) {
        try {
            Map<String, Map<String, String>> notes = loadNotes();
            String id = UUID.randomUUID().toString().substring(0, 8);
            Map<String, String> note = new LinkedHashMap<>();
            note.put("id", id);
            note.put("title", title);
            note.put("content", content);
            note.put("created", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            notes.put(id, note);
            saveNotes(notes);
            return "Note saved successfully. ID: " + id + ", Title: " + title;
        } catch (IOException e) {
            return "Error saving note: " + e.getMessage();
        }
    }

    @Tool(description = "List all saved notes with their IDs and titles")
    public String listNotes() {
        try {
            Map<String, Map<String, String>> notes = loadNotes();
            if (notes.isEmpty()) return "No notes found.";
            StringBuilder result = new StringBuilder("Your notes:\n");
            for (Map<String, String> note : notes.values()) {
                result.append(String.format("- ID: %s | Created: %s | Title: %s%n",
                        note.get("id"), note.get("created"), note.get("title")));
            }
            return result.toString();
        } catch (IOException e) {
            return "Error listing notes: " + e.getMessage();
        }
    }

    @Tool(description = "Get the full content of a note. Parameter: noteId (the note ID from listNotes)")
    public String getNote(String noteId) {
        try {
            Map<String, Map<String, String>> notes = loadNotes();
            Map<String, String> note = notes.get(noteId);
            if (note == null) return "Note not found: " + noteId;
            return String.format("Title: %s%nCreated: %s%n%nContent:%n%s",
                    note.get("title"), note.get("created"), note.get("content"));
        } catch (IOException e) {
            return "Error getting note: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a note. Parameter: noteId (the note ID to delete)")
    public String deleteNote(String noteId) {
        try {
            Map<String, Map<String, String>> notes = loadNotes();
            if (notes.remove(noteId) == null) return "Note not found: " + noteId;
            saveNotes(notes);
            return "Note deleted successfully. ID: " + noteId;
        } catch (IOException e) {
            return "Error deleting note: " + e.getMessage();
        }
    }

    @Tool(description = "Search notes by keyword. Parameter: keyword (search term to find in note titles and content)")
    public String searchNotes(String keyword) {
        try {
            Map<String, Map<String, String>> notes = loadNotes();
            StringBuilder result = new StringBuilder("Notes matching '" + keyword + "':\n");
            boolean found = false;
            for (Map<String, String> note : notes.values()) {
                if (note.get("title").toLowerCase().contains(keyword.toLowerCase()) ||
                    note.get("content").toLowerCase().contains(keyword.toLowerCase())) {
                    result.append(String.format("- ID: %s | Title: %s%n", note.get("id"), note.get("title")));
                    found = true;
                }
            }
            return found ? result.toString() : "No notes found matching: " + keyword;
        } catch (IOException e) {
            return "Error searching notes: " + e.getMessage();
        }
    }

    private Map<String, Map<String, String>> loadNotes() throws IOException {
        File file = new File(notesFile);
        if (!file.exists()) return new LinkedHashMap<>();
        return objectMapper.readValue(file, new TypeReference<>() {});
    }

    private void saveNotes(Map<String, Map<String, String>> notes) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(notesFile), notes);
    }
}
