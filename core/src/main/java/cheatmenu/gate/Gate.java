package cheatmenu.gate;

/**
 * Who may use Cheat Menu's presets. Every preset starts with {@link #STEP}; CommandDeck stops a preset at the
 * first step the game refuses, so a player who doesn't pass the gate never reaches a cheat. It's a curb, not a
 * lock: anyone the game lets type its commands can still type them.
 */
public final class Gate {
    /** The permission node. Operators have it through {@code *}. */
    public static final String PERMISSION = "cheatmenu.use";
    /** The command's name, as registered and as written in the sidecar. */
    public static final String COMMAND = "cheatmenu check";
    /** The first step of every preset, as a player would type it. */
    public static final String STEP = "/" + COMMAND;
    /** Shown to a player who doesn't pass. */
    public static final String REFUSAL = "You don't have permission to use Cheat Menu (" + PERMISSION + ").";

    private Gate() {}

    /**
     * Whether a presser passes: the server console, the owner of a singleplayer world, or a holder of
     * {@link #PERMISSION}.
     */
    public static boolean allows(boolean console, boolean singleplayerOwner, boolean hasPermission) {
        return console || singleplayerOwner || hasPermission;
    }
}
