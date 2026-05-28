package uk.gov.hmcts.reform.dev.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;
import uk.gov.hmcts.reform.dev.models.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task createTask(Task task) {
        log.info("Creating task: {}", task.getTitle());
        Task saved = taskRepository.save(task);
        log.info("Task created successfully with id {}", saved.getId());
        return saved;
    }

    public List<Task> getAllTasks() {
        log.info("Fetching all tasks");
        List<Task> tasks = taskRepository.findAllByDeletedAtIsNullOrderByIdAsc();
        log.info("Retrieved {} tasks successfully", tasks.size());
        return tasks;
    }

    public Task getTaskById(Long id) {
        log.info("Fetching task with id {}", id);
        Task task = taskRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> {
                log.warn("Task not found with id {}", id);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
            });
        log.info("Task {} retrieved successfully", id);
        return task;
    }

    public Task updateTaskStatus(Long id, TaskStatus status) {
        Task task = getTaskById(id);
        log.info("Updating status of task {} to {}", id, status);
        task.setStatus(status);
        Task updated = taskRepository.save(task);
        log.info("Task {} status updated successfully to {}", id, status);
        return updated;
    }

    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        log.info("Soft deleting task {}", id);
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
        log.info("Task {} soft deleted successfully", id);
    }
}
