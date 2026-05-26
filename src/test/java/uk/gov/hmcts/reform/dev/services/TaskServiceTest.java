package uk.gov.hmcts.reform.dev.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskStatus;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = new Task(1L, "Test task", "A description", TaskStatus.TODO,
            LocalDateTime.of(2026, 6, 1, 10, 0), null);
    }

    @Test
    void createTaskSavesAndReturnsTask() {
        when(taskRepository.save(sampleTask)).thenReturn(sampleTask);

        Task result = taskService.createTask(sampleTask);

        assertEquals(sampleTask, result);
        verify(taskRepository).save(sampleTask);
    }

    @Test
    void getAllTasksReturnsOnlyNonDeletedTasks() {
        List<Task> activeTasks = List.of(sampleTask);
        when(taskRepository.findAllByDeletedAtIsNull()).thenReturn(activeTasks);

        List<Task> result = taskService.getAllTasks();

        assertEquals(activeTasks, result);
        verify(taskRepository).findAllByDeletedAtIsNull();
    }

    @Test
    void getAllTasksReturnsEmptyListWhenNoActiveTasks() {
        when(taskRepository.findAllByDeletedAtIsNull()).thenReturn(List.of());

        List<Task> result = taskService.getAllTasks();

        assertTrue(result.isEmpty());
    }

    @Test
    void getTaskByIdReturnsTaskWhenFound() {
        when(taskRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(sampleTask));

        Task result = taskService.getTaskById(1L);

        assertEquals(sampleTask, result);
    }

    @Test
    void getTaskByIdThrowsNotFoundWhenMissing() {
        when(taskRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> taskService.getTaskById(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateTaskStatusUpdatesAndSavesWhenFound() {
        when(taskRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(sampleTask)).thenReturn(sampleTask);

        Task result = taskService.updateTaskStatus(1L, TaskStatus.IN_PROGRESS);

        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
        verify(taskRepository).save(sampleTask);
    }

    @Test
    void updateTaskStatusThrowsNotFoundWhenMissing() {
        when(taskRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> taskService.updateTaskStatus(99L, TaskStatus.DONE));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void deleteTaskSetsDeletedAtAndDoesNotCallDeleteById() {
        when(taskRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(sampleTask));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        when(taskRepository.save(captor.capture())).thenReturn(sampleTask);

        taskService.deleteTask(1L);

        Task saved = captor.getValue();
        assertNotNull(saved.getDeletedAt());
        assertFalse(saved.getDeletedAt().isAfter(LocalDateTime.now()));
        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    void deleteTaskThrowsNotFoundWhenMissing() {
        when(taskRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> taskService.deleteTask(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
