package dev.pekelund.coach.repository;

import dev.pekelund.coach.domain.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingListItem, Long> {

    List<ShoppingListItem> findByUserIdAndCheckedFalseOrderByCategory(Long userId);

    List<ShoppingListItem> findByUserIdOrderByCategoryAscItemNameAsc(Long userId);
}
