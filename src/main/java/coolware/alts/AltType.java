package coolware.alts;

public enum AltType {
    CRACKED("Cracked", "\u00a7e", "Offline/Cracked username"),
    SESSION("Session Token", "\u00a7c", "Minecraft bearer access token"),
    REFRESH_TOKEN("Refresh Token", "\u00a7b", "OAuth 2.0 refresh token"),
    MICROSOFT("Microsoft Login", "\u00a7a", "Browser OAuth2 authentication"),
    COOKIE("Cookie Login", "\u00a7d", "XboxLive/sisu cookie login");

    private final String displayName;
    private final String colorCode;
    private final String description;

    AltType(String displayName, String colorCode, String description) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getDescription() {
        return description;
    }
}
