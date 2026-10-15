package si.lifeadmin.lifeadmin.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        taskService = new TaskService(
                taskRepository,
                mock(TaskDocumentRepository.class),
                mock(si.lifeadmin.lifeadmin.gmail.ProcessedEmailRepository.class)
        );
    }

    @Test
    void createTaskSavesTaskWithCorrectDetails() {
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Task task = taskService.createTask(
                "Pay electricity bill",
                "Pay the October bill",
                LocalDate.of(2026, 10, 20)
        );

        assertEquals("Pay electricity bill", task.getTitle());
        assertEquals("Pay the October bill", task.getDescription());
        assertEquals(LocalDate.of(2026, 10, 20), task.getDueDate());
        assertFalse(task.isCompleted());

        verify(taskRepository).save(task);
    }

    @Test
    void getAllTasksReturnsTasksFromRepository() {
        Task task = new Task(
                1L, "Submit thesis", "", LocalDate.of(2026, 12, 1)
        );

        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<Task> result = taskService.getAllTasks();

        assertEquals(1, result.size());
        assertEquals("Submit thesis", result.get(0).getTitle());
    }

    @Test
    void completeTaskSavesUpdatedTask() {
        Task task = new Task(
                1L, "Book dentist", "", LocalDate.of(2026, 10, 25)
        );

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        boolean result = taskService.setTaskCompleted(1L, true);

        assertTrue(result);
        assertTrue(task.isCompleted());
        verify(taskRepository).save(task);
    }

    @Test
    void completingNonexistentTaskReturnsFalse() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertFalse(taskService.setTaskCompleted(999L, true));

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void deleteTaskRemovesExistingTask() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        boolean result = taskService.deleteTask(1L);

        assertTrue(result);
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void deletingNonexistentTaskReturnsFalse() {
        when(taskRepository.existsById(999L)).thenReturn(false);

        assertFalse(taskService.deleteTask(999L));

        verify(taskRepository, never()).deleteById(anyLong());
    }
}
