package si.lifeadmin.lifeadmin.gmail;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEmailRepository
        extends JpaRepository<ProcessedEmail, Long> {

    boolean existsByGmailMessageId(String gmailMessageId);

    void deleteByGmailMessageId(String gmailMessageId);
}
