
package si.lifeadmin.lifeadmin.gmail;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bill-senders")
public class BillSenderController {

    private final BillSenderService billSenderService;

    public BillSenderController(BillSenderService billSenderService) {
        this.billSenderService = billSenderService;
    }

    @GetMapping
    public List<BillSender> getAllSenders() {
        return billSenderService.getAllSenders();
    }

    @PostMapping
    public ResponseEntity<?> addSender(
            @Valid @RequestBody CreateBillSenderRequest request) {
        try {
            BillSender sender = billSenderService.addSender(
                    request.senderEmail(),
                    request.taskTitle()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(sender);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSender(@PathVariable Long id) {
        if (!billSenderService.deleteSender(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    public record CreateBillSenderRequest(
            @NotBlank(message = "Sender email is required")
            @Email(message = "Enter a valid email address")
            @Size(max = 254, message = "Email address is too long")
            String senderEmail,

            @NotBlank(message = "Task title is required")
            @Size(max = 100, message = "Task title must be at most 100 characters")
            String taskTitle
    ) {}
}
