package cheatmenu.hytale;

import cheatmenu.gate.Gate;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.exceptions.GeneralCommandException;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.modules.singleplayer.SingleplayerModule;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import java.util.UUID;

/** {@code /cheatmenu}: for now only {@code check}, the gate every preset starts with. */
final class CheatMenuCommand extends AbstractCommandCollection {
    CheatMenuCommand() {
        super("cheatmenu", "Cheat Menu: its presets are in /deck, on the Macros tab");
        requireNoPermission();
        addSubCommand(new Check());
    }

    /**
     * {@code /cheatmenu check}: passes silently for whoever {@link Gate#allows} lets through, and is refused for
     * everyone else, which stops the preset. The game's own node check is off, because the singleplayer owner
     * passes without holding {@link Gate#PERMISSION}.
     */
    static final class Check extends CommandBase {
        Check() {
            super("check", "Passes if you may use Cheat Menu's presets; every preset runs it first");
            requireNoPermission();
        }

        @Override
        protected void executeSync(CommandContext context) {
            UUID id = context.sender().getUuid();
            boolean console = ConsoleSender.INSTANCE.getUuid().equals(id);
            boolean owner = id != null && Constants.SINGLEPLAYER && SingleplayerModule.isOwner(id);
            PermissionsModule perms = PermissionsModule.get();
            boolean has = id != null && perms != null && perms.hasPermission(id, Gate.PERMISSION);
            if (!Gate.allows(console, owner, has)) throw new GeneralCommandException(Message.raw(Gate.REFUSAL));
        }
    }
}
