package si.lifeadmin.lifeadmin.task;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class TaskDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false, unique = true)
    private String storedFilename;

    @Column(nullable = false, unique = true)
    private String gmailMessageId;

    private LocalDateTime createdAt;

    public TaskDocument() {
    }

    public TaskDocument(
            Task task,
            String originalFilename,
            String storedFilename,
            String gmailMessageId
    ) {
        this.task = task;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.gmailMessageId = gmailMessageId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public String getGmailMessageId() {
        return gmailMessageId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
