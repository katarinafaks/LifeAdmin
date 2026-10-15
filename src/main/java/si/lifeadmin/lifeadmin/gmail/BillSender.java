
package si.lifeadmin.lifeadmin.gmail;

import jakarta.persistence.*;

@Entity
@Table(
    name = "bill_senders",
    uniqueConstraints = @UniqueConstraint(columnNames = "sender_email")
)
public class BillSender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(name = "task_title", nullable = false)
    private String taskTitle;

    @Column(nullable = false)
    private boolean enabled = true;

    public BillSender() {
    }

    public BillSender(String senderEmail, String taskTitle) {
        this.senderEmail = senderEmail;
        this.taskTitle = taskTitle;
        this.enabled = true;
    }

    public Long getId() {
        return id;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
