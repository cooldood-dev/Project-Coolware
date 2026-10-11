package coolware.config;

/**
 * Animation modes for item rendering ported from OpenMyau
 */
public enum AnimationMode {
    VANILLA,
    EXHIBITION,
    ETB,
    SIGMA,
    DORTWARE,
    PLAIN,
    SPIN,
    AVATAR,
    SWONG,
    SWANG,
    SWANK,
    STYLES,
    NUDGE,
    PUNCH,
    JIGSAW,
    SLIDE,
    SWING,
    OLD,
    PUSH,
    DASH,
    SLASH,
    SCALE,
    SWONK,
    STELLA,
    SMALL,
    EDIT,
    RHYS,
    STAB,
    FLOAT,
    REMIX,
    XIV,
    WINTER,
    YAMATO,
    SLIDE_SWING,
    SMALL_PUSH,
    REVERSE,
    INVENT,
    LEAKED,
    AQUA,
    ASTRO,
    FADEAWAY,
    ASTOLFO,
    ASTOLFO_SPIN,
    MOON,
    MOON_PUSH,
    SMOOTH,
    TAP1,
    TAP2,
    SIGMA3,
    SIGMA4,
    MYAU_1_8,
    MYAU_SLIDE,
    MYAU_SWANK,
    MYAU_SWANG,
    MYAU_AVATAR,
    MYAU_JIGSAW;

    public static AnimationMode fromString(String value) {
        if (value == null) return VANILLA;
        String normalized = value.trim().replace(" ", "_").replace("-", "_").toUpperCase();
        if (normalized.equals("1.8")) return MYAU_1_8;
        if (normalized.equals("SLIDESWING")) return SLIDE_SWING;
        if (normalized.equals("SMALLPUSH")) return SMALL_PUSH;
        if (normalized.equals("MOONPUSH")) return MOON_PUSH;
        if (normalized.equals("ASTOLFOSPIN")) return ASTOLFO_SPIN;
        try {
            return valueOf(normalized);
        } catch (Exception e) {
            return VANILLA;
        }
    }
}
