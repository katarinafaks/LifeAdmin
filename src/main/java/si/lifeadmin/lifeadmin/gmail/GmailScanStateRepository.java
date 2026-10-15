package si.lifeadmin.lifeadmin.gmail;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailScanStateRepository
        extends JpaRepository<GmailScanState, String> {
}
