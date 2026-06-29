package hu.steradian.co2coremod.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SmogCommands {
    private static final long UNDO_TTL_MILLIS = 10 * 60 * 1000L;
    private static final Map<UUID, JsonObject> undoBuffersByPlayer = new HashMap<>();

    private static final String[] HELP_LINES = {
        "=== CO₂ Smog Commands ===",
        "/smog help  - Show this help text",
        "/smog show  - Show current chunk CO₂ value",
        "/smog inc <amount>  - Increase current chunk CO₂",
        "/smog inc <amount> <radius>  - Increase CO₂ in chunk radius",
        "/smog dec <amount>  - Decrease current chunk CO₂",
        "/smog dec <amount> <radius>  - Decrease CO₂ in chunk radius",
        "/smog set <amount>  - Set current chunk CO₂",
        "/smog set <amount> <radius>  - Set CO₂ in chunk radius",
        "/smog undo  - Undo your last smog command (10 min)",
        "/smog debug  - Display your current undo snapshot",
        "/smog restricted show  - Show current chunk restriction status",
        "/smog restricted add <radius>  - Add restricted chunks around you",
        "/smog restricted remove <radius>  - Remove restricted chunks around you"
    };

    private enum SmogOperation {
        SET, ADD
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                Commands.literal("helpsmog")
                    .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .executes(ctx -> helpSmog(ctx.getSource()))
            );
            dispatcher.register(
                Commands.literal("smog")
                    .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .executes(ctx -> helpSmog(ctx.getSource()))

                        .then(Commands.literal("help")
                            .executes(ctx -> helpSmog(ctx.getSource())))

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

                        .then(Commands.literal("undo")
                            .executes(ctx -> undo(ctx.getSource())))

                        .then(Commands.literal("debug")
                            .executes(ctx -> debugUndo(ctx.getSource())))

                        .then(Commands.literal("restricted")
                            .then(Commands.literal("show")
                                .executes(ctx -> restrictedShow(ctx.getSource())))
                            .then(Commands.literal("add")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                                    .executes(ctx -> restrictedModify(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius"),
                                        true
                                    ))))
                            .then(Commands.literal("remove")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                                    .executes(ctx -> restrictedModify(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius"),
                                        false
                                    )))))

                        .then(Commands.literal("inc")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> modifyChunks(
                                    ctx.getSource(),
                                    SmogOperation.ADD,
                                    IntegerArgumentType.getInteger(ctx, "amount"),
                                    0
                                ))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                                    .executes(ctx -> modifyChunks(
                                        ctx.getSource(),
                                        SmogOperation.ADD,
                                        IntegerArgumentType.getInteger(ctx, "amount"),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                    ))
                                )
                            ))

                        .then(Commands.literal("dec")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> modifyChunks(
                                    ctx.getSource(),
                                    SmogOperation.ADD,
                                    -IntegerArgumentType.getInteger(ctx, "amount"),
                                    0
                                ))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                                    .executes(ctx -> modifyChunks(
                                        ctx.getSource(),
                                        SmogOperation.ADD,
                                        -IntegerArgumentType.getInteger(ctx, "amount"),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                    ))
                                )
                            ))

                        .then(Commands.literal("set")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> modifyChunks(
                                    ctx.getSource(),
                                    SmogOperation.SET,
                                    IntegerArgumentType.getInteger(ctx, "amount"),
                                    0
                                ))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                                    .executes(ctx -> modifyChunks(
                                        ctx.getSource(),
                                        SmogOperation.SET,
                                        IntegerArgumentType.getInteger(ctx, "amount"),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                    ))
                                )
                            ))
            );
        });
    }

    private static int helpSmog(CommandSourceStack source) {
        for (String line : HELP_LINES) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int modifyChunks(CommandSourceStack source, SmogOperation operation, int amount, int radius) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<LevelChunk> chunks = radius == 0 ? List.of(commandChunk(source)) : collectChunks(player, radius);
        String command = commandDescription(operation, amount, radius);
        storeUndoSnapshot(player, command, chunks);

        for (LevelChunk chunk : chunks) {
            if (operation == SmogOperation.SET)
                SmogHandler.setChunkAmount(chunk, amount);
            else
                SmogHandler.add(chunk, amount);
        }

        int changedChunks = chunks.size();
        source.sendSuccess(
            () -> Component.literal(successMessage(operation, amount, radius, changedChunks)),
            true
        );
        return changedChunks;
    }

    private static String commandDescription(SmogOperation operation, int amount, int radius) {
        String base = operation == SmogOperation.SET
            ? "/smog set " + amount
            : "/smog " + (amount > 0 ? "inc " : "dec ") + Math.abs(amount);

        return radius == 0 ? base : base + " " + radius;
    }

    private static String successMessage(SmogOperation operation, int amount, int radius, int changedChunks) {
        String message = operation == SmogOperation.SET
            ? "Set CO₂ to " + amount
            : (amount > 0 ? "Increased" : "Decreased") + " CO₂ by " + Math.abs(amount);

        return radius == 0
            ? message
            : message + " in " + changedChunks + " chunks, radius " + radius;
    }

    private static List<LevelChunk> collectChunks(ServerPlayer player, int radius) {
        int centerChunkX = player.blockPosition().getX() >> 4;
        int centerChunkZ = player.blockPosition().getZ() >> 4;
        List<LevelChunk> chunks = new ArrayList<>();

        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                chunks.add(player.level().getChunk(chunkX, chunkZ));
            }
        }

        return chunks;
    }

    private static void storeUndoSnapshot(ServerPlayer player, String command, List<LevelChunk> chunks) {
        JsonObject root = new JsonObject();
        root.addProperty("player", player.getUUID().toString());
        root.addProperty("command", command);
        root.addProperty("expiresAt", System.currentTimeMillis() + UNDO_TTL_MILLIS);

        JsonArray chunkArray = new JsonArray();
        for (LevelChunk chunk : chunks) {
            JsonObject entry = new JsonObject();
            entry.addProperty("x", chunk.getPos().x);
            entry.addProperty("z", chunk.getPos().z);
            entry.addProperty("amount", SmogHandler.getChunkAmount(chunk));
            chunkArray.add(entry);
        }

        root.add("chunks", chunkArray);
        undoBuffersByPlayer.put(player.getUUID(), root);
    }

    private static JsonObject getValidUndoBufferOrNotify(CommandSourceStack source, ServerPlayer player) {
        JsonObject buffer = undoBuffersByPlayer.get(player.getUUID());

        if (buffer == null) {
            source.sendSuccess(() -> Component.literal("Your smog undo buffer is empty."), false);
            return null;
        }

        if (System.currentTimeMillis() > buffer.get("expiresAt").getAsLong()) {
            undoBuffersByPlayer.remove(player.getUUID());
            String command = buffer.has("command") ? buffer.get("command").getAsString() : "unknown command";
            source.sendSuccess(
                () -> Component.literal("Your smog undo snapshot expired and was cleared. Expired command: " + command),
                false
            );
            return null;
        }

        return buffer;
    }

    private static int undo(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        JsonObject buffer = getValidUndoBufferOrNotify(source, player);

        if (buffer == null)
            return 0;

        JsonArray chunks = buffer.getAsJsonArray("chunks");
        for (int i = 0; i < chunks.size(); i++) {
            JsonObject entry = chunks.get(i).getAsJsonObject();
            int x = entry.get("x").getAsInt();
            int z = entry.get("z").getAsInt();
            int amount = entry.get("amount").getAsInt();

            LevelChunk chunk = player.level().getChunk(x, z);
            SmogHandler.setChunkAmount(chunk, amount);
        }

        String undoneCommand = buffer.get("command").getAsString();
        undoBuffersByPlayer.remove(player.getUUID());

        source.sendSuccess(
            () -> Component.literal("Undid smog command: " + undoneCommand),
            true
        );
        return 1;
    }

    private static int debugUndo(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        JsonObject buffer = getValidUndoBufferOrNotify(source, player);

        if (buffer == null)
            return 0;

        source.sendSuccess(
            () -> Component.literal("Your smog undo buffer: " + buffer),
            false
        );
        return 1;
    }

    private static int restrictedShow(CommandSourceStack source) throws CommandSyntaxException {
        LevelChunk chunk = commandChunk(source);
        boolean restricted = SmogHandler.isRestricted(chunk);
        int amount = SmogHandler.getChunkAmount(chunk);

        source.sendSuccess(
            () -> Component.literal("Current chunk restricted = " + restricted
                + ", CO₂ = " + amount
                + ", custom restricted chunks = " + SmogHandler.getRestrictedChunkCount()),
            false
        );
        return 1;
    }

    private static int restrictedModify(CommandSourceStack source, int radius, boolean add) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<LevelChunk> chunks = collectChunks(player, radius);
        int changed = 0;

        for (LevelChunk chunk : chunks) {
            ChunkPos pos = chunk.getPos();
            boolean changedThisChunk = add
                ? SmogHandler.addRestrictedChunk(pos)
                : SmogHandler.removeRestrictedChunk(pos);

            if (changedThisChunk)
                changed++;

            if (add)
                SmogHandler.setChunkAmount(chunk, 0);
        }

        final int finalChanged = changed;
        final int totalChunks = chunks.size();
        source.sendSuccess(
            () -> Component.literal((add ? "Added" : "Removed")
                + " " + finalChanged + " custom restricted chunks out of " + totalChunks
                + " checked chunks, radius " + radius),
            true
        );
        return changed;
    }

    private static LevelChunk commandChunk(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return player.level().getChunk(
            player.blockPosition().getX() >> 4,
            player.blockPosition().getZ() >> 4
        );
    }
}