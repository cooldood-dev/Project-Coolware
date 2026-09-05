package com.github.cooldood.utils.minecraft;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.WorldUnloadEvent;
import com.github.cooldood.utils.client.C;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;

public class BlockTracker {
    @Getter
    private static final HashMap<Block, ArrayList<BlockPos>> trackedBlocks = new HashMap<>();
    @Getter
    private static final HashMap<Integer, HashMap<Integer, Integer>> loadedChunks = new HashMap<>();

    @SubscribeEvent
    public static void clearTrackedBlocks(WorldUnloadEvent event) {
        for (Block block : trackedBlocks.keySet()) trackedBlocks.get(block).clear();
        loadedChunks.clear();
    }

    public static void loadBlocksFromChunk(int chunkX, int chunkZ, Block searchBlock) {
        if (trackedBlocks.isEmpty()) return;

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos blockPos = new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z);
                    Block block = C.w().getBlockState(blockPos).getBlock();

                    if (searchBlock != null && block != searchBlock) continue;

                    ArrayList<BlockPos> trackedBlocks = BlockTracker.getTrackedBlocks().get(block);
                    if (trackedBlocks != null) trackedBlocks.add(blockPos);
                }
            }
        }
    }

    public static void unloadBlocksFromChunk(int chunkX, int chunkZ) {
        if (trackedBlocks.isEmpty()) return;

        for (ArrayList<BlockPos> positions : BlockTracker.getTrackedBlocks().values()) {
            for (int i = 0; i < positions.size(); i++) {
                BlockPos pos = positions.get(i);
                if (pos.getX() < chunkX * 16 || pos.getX() >= chunkX * 16 + 16 || pos.getZ() < chunkZ * 16 || pos.getZ() >= chunkZ * 16 + 16) continue;

                positions.remove(i);
                i--;
            }
        }
    }

    public static void updateBlock(BlockPos pos, Block block) {
        Block originalBlock = BlockTracker.getTrackedBlockAtPos(pos);

        if (originalBlock == null) {
            ArrayList<BlockPos> positions = BlockTracker.getTrackedBlocks().get(block);
            if (positions == null) return;

            positions.add(pos);
        }
        else if (originalBlock != block) {
            BlockTracker.getTrackedBlocks().get(originalBlock).remove(pos);
        }
    }

    public static void addLoadedChunk(int chunkX, int chunkZ) {
        loadedChunks.putIfAbsent(chunkX, new HashMap<>());
        loadedChunks.get(chunkX).put(chunkZ, 0);

        loadBlocksFromChunk(chunkX, chunkZ, null);
    }

    public static void removeLoadedChunk(int chunkX, int chunkZ) {
        HashMap<Integer, Integer> chunksAtX = loadedChunks.get(chunkX);
        if (chunksAtX == null) return;

        chunksAtX.remove(chunkZ);
        if (chunksAtX.isEmpty()) loadedChunks.remove(chunkX);

        unloadBlocksFromChunk(chunkX, chunkZ);
    }

    public static void beginTracking(Block block) {
        trackedBlocks.put(block, new ArrayList<>());

        for (int chunkX : loadedChunks.keySet()) {
            for (int chunkZ : loadedChunks.get(chunkX).keySet()) {
                loadBlocksFromChunk(chunkX, chunkZ, block);
            }
        }
    }

    public static void stopTracking(Block block) {
        trackedBlocks.remove(block);
    }

    public static ArrayList<BlockPos> getBlockPositions(Block block) {
        return trackedBlocks.get(block);
    }

    public static Block getTrackedBlockAtPos(BlockPos pos) {
        for (Block block : trackedBlocks.keySet()) {
            if (trackedBlocks.get(block).contains(pos)) return block;
        }

        return null;
    }
}