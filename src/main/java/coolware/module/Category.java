package coolware.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    VISUALS("Visuals"),
    CLIENT("Client");

    // Compatibility aliases for any older references
    public static final Category GHOST = COMBAT;
    public static final Category VISUAL = VISUALS;
    public static final Category UTILITY = PLAYER;
    public static final Category FLIGHT = MOVEMENT;
    public static final Category PROFILES = CLIENT;
    public static final Category SETTINGS = CLIENT;

    private final String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}



