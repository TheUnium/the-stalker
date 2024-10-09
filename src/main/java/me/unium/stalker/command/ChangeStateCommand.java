package me.unium.stalker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.unium.stalker.entity.StalkerEntity;
import me.unium.stalker.entity.StalkerState;
import me.unium.stalker.event.StalkerEventHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class ChangeStateCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("stalker-change-state")
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("state", StringArgumentType.word())
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            String stateStr = StringArgumentType.getString(context, "state").toUpperCase();
                            StalkerEntity stalker = StalkerEventHandler.getCurrentStalker();

                            if (stalker != null) {
                                try {
                                    StalkerState newState = StalkerState.valueOf(stateStr);
                                    StalkerState oldState = stalker.getCurrentState();
                                    stalker.setCurrentState(newState);

                                    source.sendFeedback(() -> Text.literal(
                                                    String.format("Changed Stalker state from %s to %s", oldState, newState)),
                                            false);
                                } catch (IllegalArgumentException e) {
                                    source.sendError(Text.literal("Invalid state. Valid states are: IDLE, STALKING, ATTACKING, HIDDEN, TAKING_COVER, STARING"));
                                }
                            } else {
                                source.sendError(Text.literal("No active Stalker found."));
                            }
                            return 1;
                        })));
    }
}