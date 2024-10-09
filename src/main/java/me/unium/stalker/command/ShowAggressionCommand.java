package me.unium.stalker.command;

import com.mojang.brigadier.CommandDispatcher;
import me.unium.stalker.entity.StalkerEntity;
import me.unium.stalker.event.StalkerEventHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class ShowAggressionCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("stalkeraggression")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    StalkerEntity stalker = StalkerEventHandler.getCurrentStalker();
                    if (stalker != null) {
                        float aggression = stalker.getAggressionMeter();
                        source.sendFeedback(() -> Text.literal("Current Stalker aggression: " + aggression), false);
                    } else {
                        source.sendFeedback(() -> Text.literal("No active Stalker found."), false);
                    }
                    return 1;
                }));
    }
}