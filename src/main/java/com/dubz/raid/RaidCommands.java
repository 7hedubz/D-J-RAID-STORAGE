package com.dubz.raid;

import com.dubz.raid.storage.RaidMath;
import com.dubz.raid.storage.RaidSettings;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class RaidCommands {
    private RaidCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("raidadmin")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(RaidCommands::query)
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.000001))
                        .executes(ctx -> set(ctx, "B"))
                        .then(Commands.argument("unit", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RaidMath.COMMAND_UNITS, builder))
                                .executes(ctx -> set(ctx, StringArgumentType.getString(ctx, "unit"))))));
    }

    private static int query(CommandContext<CommandSourceStack> context) {
        long bytes = RaidSettings.adminBytes();
        context.getSource().sendSuccess(
                () -> Component.translatable("command.raid.admin.query", bytes, RaidMath.formatBytes(bytes)),
                false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context, String unit) {
        double amount = DoubleArgumentType.getDouble(context, "amount");
        long bytes;
        try {
            bytes = RaidMath.bytesFromAmount(amount, unit);
        } catch (IllegalArgumentException ignored) {
            context.getSource().sendFailure(Component.translatable("command.raid.admin.bad_unit"));
            return 0;
        }
        RaidSettings.setAdminBytes(context.getSource().getServer(), bytes);
        context.getSource().sendSuccess(
                () -> Component.translatable("command.raid.admin.set", bytes, RaidMath.formatBytes(bytes)),
                true);
        return 1;
    }
}
