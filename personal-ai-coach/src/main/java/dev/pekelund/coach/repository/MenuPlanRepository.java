package dev.pekelund.coach.repository;

import dev.pekelund.coach.domain.MenuPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuPlanRepository extends JpaRepository<MenuPlan, Long> {

    List<MenuPlan> findByUserIdAndWeekYearOrderByDayOfWeek(Long userId, String weekYear);

    void deleteByUserIdAndWeekYear(Long userId, String weekYear);
}
