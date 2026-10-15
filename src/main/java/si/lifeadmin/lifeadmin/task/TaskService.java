
package si.lifeadmin.lifeadmin.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import si.lifeadmin.lifeadmin.gmail.ProcessedEmailRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskDocumentRepository taskDocumentRepository;
    private final ProcessedEmailRepository processedEmailRepository;

    public TaskService(
            TaskRepository taskRepository,
            TaskDocumentRepository taskDocumentRepository,
            ProcessedEmailRepository processedEmailRepository) {
        this.taskRepository = taskRepository;
        this.taskDocumentRepository = taskDocumentRepository;
        this.processedEmailRepository = processedEmailRepository;
    }

    public Task createTask(
            String title,
            String description,
            LocalDate dueDate) {

        Task task = new Task(null, title, description, dueDate);
        return taskRepository.save(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public boolean setTaskCompleted(Long id, boolean completed) {
        Optional<Task> result = taskRepository.findById(id);

        if (result.isEmpty()) {
            return false;
        }

        Task task = result.get();
        task.setCompleted(completed);
        taskRepository.save(task);

        return true;
    }

    @Transactional
    public boolean deleteTask(Long id) {
        Optional<Task> result = taskRepository.findById(id);

        if (result.isEmpty()) {
            return false;
        }

        Task task = result.get();

        // Find documents attached to this task.
        List<TaskDocument> documents =
                taskDocumentRepository.findByTaskId(id);

        Path documentsDirectory = Path.of("documents")
                .toAbsolutePath()
                .normalize();

        for (TaskDocument document : documents) {
            Path filePath = documentsDirectory
                    .resolve(document.getStoredFilename())
                    .normalize();

            if (!filePath.startsWith(documentsDirectory)) {
                throw new IllegalStateException(
                        "Invalid document file path.");
            }

            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println(
                        "Could not delete document file: " + filePath);
                e.printStackTrace();
            }
        }

        // Delete the document records.
        taskDocumentRepository.deleteAll(documents);

        // Allow the corresponding Gmail messages to be imported again.
        documents.stream()
                .map(TaskDocument::getGmailMessageId)
                .distinct()
                .forEach(
                        processedEmailRepository::deleteByGmailMessageId
                );

        // Finally, delete the task.
        taskRepository.delete(task);

        return true;
    }
    public boolean setTaskDueDate(Long id, LocalDate dueDate) {
        Optional<Task> result = taskRepository.findById(id);

        if (result.isEmpty()) {
            return false;
        }

        Task task = result.get();
        task.setDueDate(dueDate);
        taskRepository.save(task);

        return true;
    }
}
