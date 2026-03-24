package dev.pekelund.familiar.fire.repository;

import dev.pekelund.familiar.fire.model.Spell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpellRepository extends JpaRepository<Spell, Long> {

    List<Spell> findByElementIgnoreCase(String element);

    @Query("SELECT s FROM Spell s WHERE s.element = :element ORDER BY s.power DESC")
    Optional<Spell> findMostPowerfulByElement(String element);

    List<Spell> findByNameContainingIgnoreCase(String keyword);
}
