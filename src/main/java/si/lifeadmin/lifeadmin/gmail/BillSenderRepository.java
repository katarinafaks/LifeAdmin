
package si.lifeadmin.lifeadmin.gmail;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillSenderRepository
        extends JpaRepository<BillSender, Long> {

    List<BillSender> findByEnabledTrue();

    boolean existsBySenderEmailIgnoreCase(String senderEmail);

    Optional<BillSender> findBySenderEmailIgnoreCase(String senderEmail);
}
