
package si.lifeadmin.lifeadmin.gmail;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    private final GmailReaderService gmailReaderService;

    public GmailController(GmailReaderService gmailReaderService) {
        this.gmailReaderService = gmailReaderService;
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scanGmail() {
        try {
            GmailReaderService.ScanResult result =
                    gmailReaderService.scanUnreadBills();

            return ResponseEntity.ok(result);

        } catch (IOException | GeneralSecurityException e) {
            return ResponseEntity.internalServerError()
                    .body("Gmail scan failed: " + e.getMessage());
        }
    }
}
