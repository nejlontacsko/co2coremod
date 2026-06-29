package hu.steradian.co2coremod.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import hu.steradian.co2coremod.smog.SmogHandler;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.chunk.LevelChunk;

public class SmogCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("smog")
                    .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))

                        .then(Commands.literal("show")
                            .executes(ctx -> {
                                LevelChunk chunk = commandChunk(ctx.getSource());
                                int amount = SmogHandler.getChunkAmount(chunk);
                                ctx.getSource().sendSuccess(
                                    () -> Component.literal("Chunk CO₂ = " + amount),
                                    false
                                );
                                return 1;
                            }))

                        .then(Commands.literal("inc")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> modify(
                                    ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "amount")
                                ))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                                    .executes(ctx -> modifyRadius(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount"),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                    ))
                                )
                            ))

                        .then(Commands.literal("dec")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> modify(
                                    ctx.getSource(),
                                    -IntegerArgumentType.getInteger(ctx, "amount")
                                ))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                                    .executes(ctx -> modifyRadius(
                                        ctx.getSource(),
                                        -IntegerArgumentType.getInteger(ctx, "amount"),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                    ))
                                )
                            ))

                        .then(Commands.literal("set")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    LevelChunk chunk = commandChunk(ctx.getSource());
                                    SmogHandler.setChunkAmount(chunk, amount);
                                    ctx.getSource().sendSuccess(
                                        () -> Component.literal("Set CO₂ to " + amount),
                                        true
                                    );
                                    return 1;
                                })
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                                    .executes(ctx -> setRadius(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount"),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                    ))
                                )
                            ))
        ));
    }

    private static int modify(CommandSourceStack source, int amount) throws CommandSyntaxException {
        LevelChunk chunk = commandChunk(source);
        SmogHandler.add(chunk, amount);
        source.sendSuccess(
            () -> Component.literal((amount > 0 ? "Increased" : "Decreased") + " CO₂ by " + Math.abs(amount)),
            true
        );
        return 1;
    }

    private static int modifyRadius(CommandSourceStack source, int amount, int radius) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int centerChunkX = player.blockPosition().getX() >> 4;
        int centerChunkZ = player.blockPosition().getZ() >> 4;
        int changedChunks = 0;

        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                LevelChunk chunk = player.level().getChunk(chunkX, chunkZ);
                SmogHandler.add(chunk, amount);
                changedChunks++;
            }
        }

        final int finalChangedChunks = changedChunks;
        source.sendSuccess(
            () -> Component.literal((amount > 0 ? "Increased" : "Decreased")
                + " CO₂ by " + Math.abs(amount)
                + " in " + finalChangedChunks + " chunks, radius " + radius),
            true
        );
        return changedChunks;
    }

    private static int setRadius(CommandSourceStack source, int amount, int radius) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int centerChunkX = player.blockPosition().getX() >> 4;
        int centerChunkZ = player.blockPosition().getZ() >> 4;
        int changedChunks = 0;

        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                LevelChunk chunk = player.level().getChunk(chunkX, chunkZ);
                SmogHandler.setChunkAmount(chunk, amount);
                changedChunks++;
            }
        }

        final int finalChangedChunks = changedChunks;
        source.sendSuccess(
            () -> Component.literal("Set CO₂ to " + amount
                + " in " + finalChangedChunks + " chunks, radius " + radius),
            true
        );
        return changedChunks;
    }

    private static LevelChunk commandChunk(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return player.level().getChunk(
            player.blockPosition().getX() >> 4,
            player.blockPosition().getZ() >> 4
        );
    }
}