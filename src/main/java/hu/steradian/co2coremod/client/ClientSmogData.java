package hu.steradian.co2coremod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClientSmogData {
    private static final Map<ChunkPos, Integer> CHUNK_SMOG_MAP = new HashMap<>();
    private static final int MAX_TRACK_DISTANCE = 10;

    public static void setSmogAmount(ChunkPos pos, int amount) {
        CHUNK_SMOG_MAP.put(pos, amount);
    }

    public static int getSmogAmount(ChunkPos pos) {
        return CHUNK_SMOG_MAP.getOrDefault(pos, 0);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        ChunkPos playerPos = new ChunkPos(mc.player.getOnPos());

        Iterator<Map.Entry<ChunkPos, Integer>> it = CHUNK_SMOG_MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkPos, Integer> entry = it.next();
            ChunkPos pos = entry.getKey();

            int dx = Math.abs(pos.x - playerPos.x);
            int dz = Math.abs(pos.z - playerPos.z);

            if (dx > MAX_TRACK_DISTANCE || dz > MAX_TRACK_DISTANCE)
                it.remove();
        }
    }
}