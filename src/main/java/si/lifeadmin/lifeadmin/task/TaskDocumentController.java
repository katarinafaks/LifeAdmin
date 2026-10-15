
package si.lifeadmin.lifeadmin.task;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskDocumentController {

    private final TaskDocumentRepository taskDocumentRepository;
    private final TaskService taskService;

    public TaskDocumentController(
            TaskDocumentRepository taskDocumentRepository,
            TaskService taskService
    ) {
        this.taskDocumentRepository = taskDocumentRepository;
        this.taskService = taskService;
    }

    @GetMapping("/tasks/{taskId}/documents")
    public ResponseEntity<List<DocumentResponse>> getTaskDocuments(
            @PathVariable Long taskId
    ) {
        boolean taskExists = taskService.getAllTasks().stream()
                .anyMatch(task -> task.getId().equals(taskId));

        if (!taskExists) {
            return ResponseEntity.notFound().build();
        }

        List<DocumentResponse> documents =
                taskDocumentRepository.findByTaskId(taskId)
                        .stream()
                        .map(document -> new DocumentResponse(
                                document.getId(),
                                document.getOriginalFilename(),
                                "/api/documents/" + document.getId()
                        ))
                        .toList();

        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<Resource> openDocument(
            @PathVariable Long documentId
    ) throws IOException {

        TaskDocument document = taskDocumentRepository
                .findById(documentId)
                .orElse(null);

        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        Path documentsDirectory = Path.of("documents")
                .toAbsolutePath()
                .normalize();

        Path filePath = documentsDirectory
                .resolve(document.getStoredFilename())
                .normalize();

        // Only allow files inside the documents directory.
        if (!filePath.startsWith(documentsDirectory)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"bill.pdf\""
                )
                .body(resource);
    }

    public record DocumentResponse(
            Long id,
            String filename,
            String url
    ) {}
}
