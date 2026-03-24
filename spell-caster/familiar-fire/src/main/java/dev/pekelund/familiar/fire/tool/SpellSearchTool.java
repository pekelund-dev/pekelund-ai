package dev.pekelund.familiar.fire.tool;

import dev.pekelund.familiar.fire.model.Spell;
import dev.pekelund.familiar.fire.repository.SpellRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Tool that searches the Librarium database for spells.
 * Represents the "Declarative Database Connection" in the tool ecosystem.
 */
@Component
public class SpellSearchTool {

    private final SpellRepository spellRepository;

    public SpellSearchTool(SpellRepository spellRepository) {
        this.spellRepository = spellRepository;
    }

    /**
     * Scouts the Librarium for the most powerful spell of a given element.
     */
    public String scoutSpell(String element) {
        Optional<Spell> spell = spellRepository.findMostPowerfulByElement(element);
        return spell.map(s -> "Scout found: " + s.getName() + " (power=" + s.getPower() + ") - " + s.getDescription())
                    .orElse("No " + element + " spell found in the Librarium.");
    }

    /**
     * Searches for spells by keyword in the Librarium.
     */
    public String searchByKeyword(String keyword) {
        List<Spell> spells = spellRepository.findByNameContainingIgnoreCase(keyword);
        if (spells.isEmpty()) {
            return "No spells matching '" + keyword + "' found in the Librarium.";
        }
        StringBuilder sb = new StringBuilder("Found " + spells.size() + " spell(s):\n");
        spells.forEach(s -> sb.append("  - ").append(s.getName()).append(" [").append(s.getElement()).append("] power=").append(s.getPower()).append("\n"));
        return sb.toString();
    }
}
