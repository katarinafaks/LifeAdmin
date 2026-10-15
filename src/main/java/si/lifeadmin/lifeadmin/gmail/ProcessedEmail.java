package si.lifeadmin.lifeadmin.gmail;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.time.LocalDateTime;

@Entity
public class ProcessedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String gmailMessageId;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public ProcessedEmail() {
    }

    public ProcessedEmail(String gmailMessageId) {
        this.gmailMessageId = gmailMessageId;
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getGmailMessageId() {
        return gmailMessageId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
