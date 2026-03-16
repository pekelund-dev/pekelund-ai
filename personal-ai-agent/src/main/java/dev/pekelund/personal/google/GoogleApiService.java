package dev.pekelund.personal.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class GoogleApiService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_SEND,
            GmailScopes.GMAIL_MODIFY,
            CalendarScopes.CALENDAR,
            CalendarScopes.CALENDAR_EVENTS
    );
    private static final String APPLICATION_NAME = "Personal AI Agent";

    @Value("${google.credentials.file:credentials.json}")
    private String credentialsFilePath;

    @Value("${google.tokens.dir:tokens}")
    private String tokensDirectory;

    private volatile Credential credential;
    private volatile NetHttpTransport httpTransport;

    public Gmail getGmailService() throws GeneralSecurityException, IOException {
        return new Gmail.Builder(getHttpTransport(), JSON_FACTORY, getCredential())
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public Calendar getCalendarService() throws GeneralSecurityException, IOException {
        return new Calendar.Builder(getHttpTransport(), JSON_FACTORY, getCredential())
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private NetHttpTransport getHttpTransport() throws GeneralSecurityException, IOException {
        if (httpTransport == null) {
            synchronized (this) {
                if (httpTransport == null) {
                    httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                }
            }
        }
        return httpTransport;
    }

    private Credential getCredential() throws GeneralSecurityException, IOException {
        if (credential == null) {
            synchronized (this) {
                if (credential == null) {
                    credential = authorize();
                }
            }
        }
        return credential;
    }

    private Credential authorize() throws GeneralSecurityException, IOException {
        File credentialsFile = new File(credentialsFilePath);
        if (!credentialsFile.exists()) {
            throw new IOException(
                    "Credentials file not found: " + credentialsFilePath +
                    ". Download credentials.json from Google Cloud Console (OAuth 2.0 Desktop client).");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(new FileInputStream(credentialsFile)));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                getHttpTransport(), JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(tokensDirectory)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}
