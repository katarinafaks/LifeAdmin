package si.lifeadmin.lifeadmin.gmail;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class GmailScanState {

    @Id
    private String sender;

    @Column(nullable = false)
    private long startedAfterEpochSeconds;

    public GmailScanState() {
    }

    public GmailScanState(
            String sender,
            long startedAfterEpochSeconds
    ) {
        this.sender = sender;
        this.startedAfterEpochSeconds = startedAfterEpochSeconds;
    }

    public String getSender() {
        return sender;
    }

    public long getStartedAfterEpochSeconds() {
        return startedAfterEpochSeconds;
    }
}
