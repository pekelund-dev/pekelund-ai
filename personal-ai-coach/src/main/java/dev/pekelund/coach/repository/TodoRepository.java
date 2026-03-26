package dev.pekelund.coach.repository;

import dev.pekelund.coach.domain.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<TodoItem, Long> {

    List<TodoItem> findByUserIdAndStatusOrderByPriorityDescDueDateAsc(
            Long userId, TodoItem.TodoStatus status);

    List<TodoItem> findByUserIdOrderByPriorityDescCreatedAtDesc(Long userId);

    List<TodoItem> findByUserIdAndCategoryOrderByPriorityDesc(
            Long userId, TodoItem.TodoCategory category);

    List<TodoItem> findByUserIdAndDueDateBeforeAndStatusNot(
            Long userId, LocalDate date, TodoItem.TodoStatus status);
}
