package dev.pekelund.familiar.fire.model;

import jakarta.persistence.*;

@Entity
@Table(name = "spells")
public class Spell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String element;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int power;

    public Spell() {}

    public Spell(String name, String element, String description, int power) {
        this.name = name;
        this.element = element;
        this.description = description;
        this.power = power;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getElement() { return element; }
    public String getDescription() { return description; }
    public int getPower() { return power; }

    @Override
    public String toString() {
        return "Spell{name='" + name + "', element='" + element + "', description='" + description + "', power=" + power + "}";
    }
}
