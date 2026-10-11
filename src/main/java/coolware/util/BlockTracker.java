package coolware.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.*;

public class BlockTracker {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Map<Block, Set<BlockPos>> trackedBlocks = new HashMap<Block, Set<BlockPos>>();
    private static final Map<Long, Chunk> loadedChunks = new HashMap<Long, Chunk>();

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    public static synchronized void clearTrackedBlocks() {
        for (Set<BlockPos> set : trackedBlocks.values()) {
            set.clear();
        }
        loadedChunks.clear();
    }

    public static synchronized void addLoadedChunk(Chunk chunk) {
        if (chunk == null) return;
        long key = chunkKey(chunk.xPosition, chunk.zPosition);
        loadedChunks.put(key, chunk);

        if (!trackedBlocks.isEmpty()) {
            scanChunkForTrackedBlocks(chunk, null);
        }
    }

    public static synchronized void removeLoadedChunk(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        loadedChunks.remove(key);

        int minX = chunkX * 16;
        int maxX = minX + 16;
        int minZ = chunkZ * 16;
        int maxZ = minZ + 16;

        for (Set<BlockPos> set : trackedBlocks.values()) {
            Iterator<BlockPos> it = set.iterator();
            while (it.hasNext()) {
                BlockPos pos = it.next();
                if (pos.getX() >= minX && pos.getX() < maxX && pos.getZ() >= minZ && pos.getZ() < maxZ) {
                    it.remove();
                }
            }
        }
    }

    public static synchronized void updateBlock(BlockPos pos, Block block) {
        if (pos == null || block == null) return;

        // Check if any tracked sets contain this pos
        for (Map.Entry<Block, Set<BlockPos>> entry : trackedBlocks.entrySet()) {
            if (entry.getKey() == block) {
                entry.getValue().add(pos);
            } else {
                entry.getValue().remove(pos);
            }
        }
    }

    public static synchronized void beginTracking(Block block) {
        if (block == null) return;
        Set<BlockPos> set = trackedBlocks.get(block);
        if (set == null) {
            set = new HashSet<BlockPos>();
            trackedBlocks.put(block, set);
        }

        // Fast scan all currently loaded chunks using storageArrays directly
        for (Chunk chunk : loadedChunks.values()) {
            scanChunkForTrackedBlocks(chunk, block);
        }
    }

    public static synchronized void stopTracking(Block block) {
        trackedBlocks.remove(block);
    }

    public static synchronized ArrayList<BlockPos> getBlockPositions(Block block) {
        Set<BlockPos> set = trackedBlocks.get(block);
        if (set == null || set.isEmpty()) {
            return new ArrayList<BlockPos>();
        }
        return new ArrayList<BlockPos>(set);
    }

    private static void scanChunkForTrackedBlocks(Chunk chunk, Block specificBlock) {
        if (chunk == null) return;
        ExtendedBlockStorage[] storages = chunk.getBlockStorageArray();
        if (storages == null) return;

        int baseX = chunk.xPosition << 4;
        int baseZ = chunk.zPosition << 4;

        for (int i = 0; i < storages.length; i++) {
            ExtendedBlockStorage ebs = storages[i];
            if (ebs == null || ebs.isEmpty()) continue;

            char[] data = ebs.getData();
            if (data == null) continue;

            int baseY = ebs.getYLocation();

            for (int idx = 0; idx < data.length; idx++) {
                char val = data[idx];
                if (val == 0) continue; // air

                IBlockState state = (IBlockState) Block.BLOCK_STATE_IDS.getByValue(val);
                if (state == null) continue;
                Block b = state.getBlock();

                if (specificBlock != null && b != specificBlock) continue;

                Set<BlockPos> targetSet = trackedBlocks.get(b);
                if (targetSet != null) {
                    // idx = y << 8 | z << 4 | x
                    int y = (idx >> 8) & 0xF;
                    int z = (idx >> 4) & 0xF;
                    int x = idx & 0xF;
                    targetSet.add(new BlockPos(baseX + x, baseY + y, baseZ + z));
                }
            }
        }
    }
}
