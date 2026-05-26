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
        return taskRepository.save(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAllByDeletedAtIsNull();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> {
                log.warn("Task not found: {}", id);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
            });
    }

    public Task updateTaskStatus(Long id, TaskStatus status) {
        Task task = getTaskById(id);
        log.info("Updating task {} status to {}", id, status);
        task.setStatus(status);
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        log.info("Soft deleting task {}", id);
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }
}
