package dev.pekelund.coach.domain;

/**
 * Defines the different agent types available in the system.
 * Each agent specializes in a specific functional area.
 */
public enum AgentType {

    /** Kalenderöversikt — Calendar overview with multiple calendars. */
    CALENDAR("Kalender", "📅", "Hantera och översikt av dina kalendrar"),

    /** E-postöversikt — Email overview with filtering and action highlighting. */
    EMAIL("E-post", "📧", "Översikt och hantering av e-post"),

    /** Ekonomiöversikt — Financial overview with stocks, savings, spendings. */
    FINANCE("Ekonomi", "💰", "Aktier, sparande och utgifter"),

    /** Menyplanering — Weekly menu planning with shopping list. */
    MENU("Menyplanering", "🍽️", "Veckomeny och inköpslista"),

    /** Semesterplanering — Vacation planning with flights, hotels, prices. */
    VACATION("Semester", "✈️", "Planera resor och semester"),

    /** Todo-hantering — Todo management with automatic creation. */
    TODO("Att göra", "✅", "Hantera uppgifter och att-göra-listor"),

    /** Personlig coaching — Personal coaching and motivation. */
    COACHING("Coaching", "🎯", "Personlig coaching och motivation"),

    /** Generell assistent — General chat assistant. */
    GENERAL("Assistent", "🤖", "Generell hjälp och frågor");

    private final String displayName;
    private final String icon;
    private final String description;

    AgentType(String displayName, String icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
}
