
package si.lifeadmin.lifeadmin.gmail;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BillSenderService {

    private final BillSenderRepository repository;

    public BillSenderService(BillSenderRepository repository) {
        this.repository = repository;
    }

    // Add the existing Energetika configuration if it isn't saved yet.
    @PostConstruct
    public void initializeDefaultSender() {
        String email = "energetika.ljubljana@eracuni.e-jhl.si";

        if (!repository.existsBySenderEmailIgnoreCase(email)) {
            repository.save(new BillSender(
                    email,
                    "Energetika račun (imported from Gmail)"
            ));
        }
    }

    public List<BillSender> getAllSenders() {
        return repository.findAll();
    }

    public List<BillSender> getEnabledSenders() {
        return repository.findByEnabledTrue();
    }

    @Transactional
    public BillSender addSender(String senderEmail, String taskTitle) {
        String normalizedEmail = senderEmail.trim().toLowerCase();
        String normalizedTitle = taskTitle.trim();

        if (repository.existsBySenderEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "This sender email is already configured.");
        }

        return repository.save(
                new BillSender(normalizedEmail, normalizedTitle));
    }

    public boolean deleteSender(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }

        repository.deleteById(id);
        return true;
    }
    
    @Transactional
    public BillSender updateTaskTitle(Long id, String taskTitle) {
        BillSender sender = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bill sender not found."));

        sender.setTaskTitle(taskTitle.trim());

        return repository.save(sender);
    }

}
