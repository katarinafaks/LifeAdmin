package si.lifeadmin.lifeadmin.task;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    @PostMapping
    public ResponseEntity<Task> createTask(
        @Valid @RequestBody CreateTaskRequest  request){
        Task task = taskService.createTask(
            request.title(),
            request.description(),
            request.dueDate()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(task);
            
    }


    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> completeTask(@PathVariable Long id) {
        boolean updated = taskService.setTaskCompleted(id, true);

        if (!updated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/uncomplete")
    public ResponseEntity<Void> uncompleteTask(@PathVariable Long id) {
        boolean updated = taskService.setTaskCompleted(id, false);

        if (!updated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/due-date")
    public ResponseEntity<Void> setTaskDueDate(
            @PathVariable Long id,
            @Valid @RequestBody SetDueDateRequest request) {

        boolean updated = taskService.setTaskDueDate(
                id,
                request.dueDate()
        );

        if (!updated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    public record SetDueDateRequest(
            @NotNull(message = "Due date is required")
            LocalDate dueDate
    ) {}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable long id) {
        boolean deleted = taskService.deleteTask(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        } 
        
        return ResponseEntity.noContent().build();
        
    }

    
     
    public record CreateTaskRequest(

        @NotBlank(message = "Title must not be blank")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        LocalDate dueDate

    ) {}

}
