package si.lifeadmin.lifeadmin.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskDocumentRepository
        extends JpaRepository<TaskDocument, Long> {

    List<TaskDocument> findByTaskId(Long taskId);
}