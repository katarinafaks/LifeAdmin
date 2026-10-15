

package si.lifeadmin.lifeadmin.gmail;

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
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.MessagePartBody;

import org.springframework.stereotype.Service;

import si.lifeadmin.lifeadmin.task.Task;
import si.lifeadmin.lifeadmin.task.TaskDocument;
import si.lifeadmin.lifeadmin.task.TaskDocumentRepository;
import si.lifeadmin.lifeadmin.task.TaskService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
// import java.time.Instant;
// import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class GmailReaderService {

    private static final String APPLICATION_NAME = "LifeAdmin";

    private static final JsonFactory JSON_FACTORY =
            GsonFactory.getDefaultInstance();

    private final ProcessedEmailRepository processedEmailRepository;
    // private final GmailScanStateRepository scanStateRepository;
    private final TaskDocumentRepository taskDocumentRepository;
    private final TaskService taskService;
    private final BillSenderService billSenderService;

    public GmailReaderService(
            ProcessedEmailRepository processedEmailRepository,
            TaskDocumentRepository taskDocumentRepository,
            TaskService taskService,
            BillSenderService billSenderService
    ) {
        this.processedEmailRepository = processedEmailRepository;
        this.taskDocumentRepository = taskDocumentRepository;
        this.taskService = taskService;
        this.billSenderService = billSenderService;
    }

    private Gmail createGmailClient()
            throws IOException, GeneralSecurityException {

        File credentialsFile = new File("secrets/credentials.json");

        if (!credentialsFile.exists()) {
            throw new IOException(
                    "OAuth credentials not found at secrets/credentials.json"
            );
        }

        NetHttpTransport httpTransport =
                GoogleNetHttpTransport.newTrustedTransport();

        GoogleClientSecrets clientSecrets;

        try (FileInputStream input =
                     new FileInputStream(credentialsFile);
             InputStreamReader reader =
                     new InputStreamReader(input, StandardCharsets.UTF_8)) {

            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
        }

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport,
                        JSON_FACTORY,
                        clientSecrets,
                        List.of(GmailScopes.GMAIL_READONLY)
                )
                .setDataStoreFactory(
                        new FileDataStoreFactory(new File("tokens"))
                )
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver =
                new LocalServerReceiver.Builder()
                        .setPort(8888)
                        .build();

        Credential credential =
                new AuthorizationCodeInstalledApp(flow, receiver)
                        .authorize("user");

        return new Gmail.Builder(
                httpTransport,
                JSON_FACTORY,
                credential
        )
        .setApplicationName(APPLICATION_NAME)
        .build();
    }

    
    
    public ScanResult scanUnreadBills()
            throws IOException, GeneralSecurityException {

        Gmail gmail = createGmailClient();

        int matchingEmails = 0;
        int imported = 0;
        int skipped = 0;

        List<BillSender> senders = billSenderService.getEnabledSenders();

        for (BillSender sender : senders) {
            ScanResult result = scanSender(gmail, sender);

            matchingEmails += result.matchingEmails();
            imported += result.importedTasks();
            skipped += result.skippedEmails();
        }

        String message;

        if (matchingEmails == 0) {
            message = senders.isEmpty()
                    ? "No bill senders are configured."
                    : "No new matching unread emails found.";
        } else {
            message = "Gmail scan completed. Imported "
                    + imported + " bill(s).";
        }

        return new ScanResult(
                matchingEmails,
                imported,
                skipped,
                message
        );
    }

    private ScanResult scanSender(Gmail gmail, BillSender sender)
            throws IOException {

        String query = "is:unread from:" + sender.getSenderEmail()
                + " has:attachment filename:pdf";

        int matchingEmails = 0;
        int imported = 0;
        int skipped = 0;

        String pageToken = null;

        do {
            var request = gmail.users()
                    .messages()
                    .list("me")
                    .setQ(query)
                    .setMaxResults(100L);

            if (pageToken != null) {
                request.setPageToken(pageToken);
            }

            ListMessagesResponse response = request.execute();
            List<Message> messages = response.getMessages();

            if (messages != null) {
                for (Message messageReference : messages) {
                    String messageId = messageReference.getId();

                    // Skip emails that have already been imported.
                    if (processedEmailRepository
                            .existsByGmailMessageId(messageId)) {
                        continue;
                    }

                    matchingEmails++;

                    Message message = gmail.users()
                            .messages()
                            .get("me", messageId)
                            .setFormat("full")
                            .execute();

                    MessagePart pdfPart = findPdfPart(message.getPayload());

                    if (pdfPart == null) {
                        skipped++;
                        continue;
                    }

                    String originalFilename = pdfPart.getFilename();

                    if (originalFilename == null
                            || originalFilename.isBlank()) {
                        originalFilename = "bill.pdf";
                    }

                    byte[] pdfBytes = getAttachmentBytes(
                            gmail, messageId, pdfPart
                    );

                    Path documentsDirectory = Path.of("documents");
                    Files.createDirectories(documentsDirectory);

                    String storedFilename = UUID.randomUUID() + ".pdf";
                    Path storedPath =
                            documentsDirectory.resolve(storedFilename);

                    Files.write(storedPath, pdfBytes);

                    String subject = getHeader(message, "Subject");

                    Task task = taskService.createTask(
                            sender.getTaskTitle(),
                            "Imported from Gmail. Subject: "
                                    + (subject == null
                                    ? "(no subject)" : subject),
                            null
                    );

                    TaskDocument document = new TaskDocument(
                            task,
                            originalFilename,
                            storedFilename,
                            messageId
                    );

                    taskDocumentRepository.save(document);

                    processedEmailRepository.save(
                            new ProcessedEmail(messageId)
                    );

                    imported++;
                }
            }

            pageToken = response.getNextPageToken();

        } while (pageToken != null);

        return new ScanResult(
                matchingEmails,
                imported,
                skipped,
                "Scan completed for " + sender.getSenderEmail()
        );
    }


    private MessagePart findPdfPart(MessagePart part) {
        if (part == null) {
            return null;
        }

        String filename = part.getFilename();

        if (filename != null
                && filename.toLowerCase().endsWith(".pdf")) {
            return part;
        }

        if (part.getParts() != null) {
            for (MessagePart child : part.getParts()) {
                MessagePart result = findPdfPart(child);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private byte[] getAttachmentBytes(
            Gmail gmail,
            String messageId,
            MessagePart part
    ) throws IOException {

        MessagePartBody body = part.getBody();

        if (body == null) {
            throw new IOException("PDF attachment has no content.");
        }

        if (body.getAttachmentId() != null) {
            MessagePartBody attachment = gmail.users()
                    .messages()
                    .attachments()
                    .get("me", messageId, body.getAttachmentId())
                    .execute();

            return Base64.getUrlDecoder().decode(attachment.getData());
        }

        if (body.getData() != null) {
            return Base64.getUrlDecoder().decode(body.getData());
        }

        throw new IOException("Could not read the PDF attachment.");
    }

    private String getHeader(Message message, String headerName) {
        if (message.getPayload() == null
                || message.getPayload().getHeaders() == null) {
            return null;
        }

        for (MessagePartHeader header :
                message.getPayload().getHeaders()) {
            if (headerName.equalsIgnoreCase(header.getName())) {
                return header.getValue();
            }
        }

        return null;
    }

    public record ScanResult(
            int matchingEmails,
            int importedTasks,
            int skippedEmails,
            String message
    ) {}
}
