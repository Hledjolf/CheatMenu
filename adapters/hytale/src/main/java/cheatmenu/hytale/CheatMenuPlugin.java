package cheatmenu.hytale;

import cheatmenu.gate.Gate;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

/**
 * Cheat Menu for Hytale. The presets themselves are data ({@code commanddeck/commands.json}, which CommandDeck
 * reads from this jar); the plugin registers the permission and the {@code /cheatmenu check} gate they start with.
 */
public final class CheatMenuPlugin extends JavaPlugin {
    public CheatMenuPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        PermissionsModule.registerPermission(Gate.PERMISSION);
        getCommandRegistry().registerCommand(new CheatMenuCommand());
    }
}
