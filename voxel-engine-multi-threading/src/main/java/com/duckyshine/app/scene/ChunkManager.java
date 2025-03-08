package com.duckyshine.app.scene;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import com.duckyshine.app.math.Vector2;
import com.duckyshine.app.math.Voxel;

import com.duckyshine.app.model.Mesh;
import com.duckyshine.app.model.Block;
import com.duckyshine.app.model.Chunk;
import com.duckyshine.app.model.BlockType;
import com.duckyshine.app.physics.controller.Player;
import com.duckyshine.app.physics.ray.RayResult;
import com.duckyshine.app.camera.Camera;
import com.duckyshine.app.debug.Debug;

// MUST MULTITHREAD, MESH GENERATION AND NOISE IS SUPER SLOW
//
// also MUST test correctness of threading algorithm
// Could also use async
public class ChunkManager {
    public final int CHUNK_WIDTH = 16;
    public final int CHUNK_DEPTH = 16;
    public final int CHUNK_HEIGHT = 16;

    private ConcurrentMap<Vector3i, Chunk> chunks;

    private ConcurrentMap<Vector2i, HeightMap> heightMaps;

    private ConcurrentLinkedDeque<Vector3i> chunkQueue;

    private Set<Vector3i> queuedChunks;

    private Deque<Vector3i> loadedChunks;

    // Can be changed using some sort of config or cli
    private byte cores = 8;

    private ExecutorService threadPool;

    public ChunkManager() {
        this.chunks = new ConcurrentHashMap<>();

        this.heightMaps = new ConcurrentHashMap<>();

        this.chunkQueue = new ConcurrentLinkedDeque<>();

        this.queuedChunks = Collections.newSetFromMap(new ConcurrentHashMap<Vector3i, Boolean>());

        this.loadedChunks = new ArrayDeque<>();

        this.threadPool = Executors.newFixedThreadPool(cores);
    }

    // Dynamically generate based on player's position
    public void initialise() {
        this.queueChunk(0, 0, 0);
    }

    public boolean isHeightMapGenerated(Vector3i position) {
        Vector2i heightMapPosition = Vector2.getXZInteger(position);

        return this.heightMaps.containsKey(heightMapPosition);
    }

    public boolean isChunkActive(int x, int y, int z) {
        Vector3i position = new Vector3i(x, y, z);

        return this.chunks.containsKey(position);
    }

    public boolean isChunkActive(Vector3i position) {
        return this.chunks.containsKey(position);
    }

    public boolean isBlockActiveAtGlobalPosition(float x, float y, float z) {
        Vector3f position = new Vector3f(x, y, z);

        return this.isBlockActiveAtGlobalPosition(position);
    }

    public boolean isBlockActiveAtGlobalPosition(Vector3f position) {
        Chunk chunk = this.getChunkFromGlobalPosition(position);

        if (chunk == null) {
            return false;
        }

        Vector3i blockPosition = Voxel.getBlockPositionFromGlobalPosition(position);

        return chunk.isBlockActive(blockPosition);
    }

    public Chunk getChunk(int x, int y, int z) {
        Vector3i position = new Vector3i(x, y, z);

        return this.getChunk(position);
    }

    public Chunk getChunk(Vector3i position) {
        return this.chunks.get(position);
    }

    public Chunk getChunkFromGlobalPosition(float x, float y, float z) {
        Vector3f position = new Vector3f(x, y, z);

        return this.getChunkFromGlobalPosition(position);
    }

    public Chunk getChunkFromGlobalPosition(Vector3f position) {
        Vector3i chunkPosition = Voxel.getChunkPositionFromGlobalPosition(position);

        if (!this.isChunkActive(chunkPosition)) {
            return null;
        }

        return this.getChunk(chunkPosition);
    }

    public Block getBlockFromGlobalPosition(float x, float y, float z) {
        Vector3f position = new Vector3f(x, y, z);

        return this.getBlockFromGlobalPosition(position);
    }

    public Block getBlockFromGlobalPosition(Vector3f position) {
        Vector3i chunkPosition = Voxel.getChunkPositionFromGlobalPosition(position);

        if (!this.isChunkActive(chunkPosition)) {
            return null;
        }

        Chunk chunk = this.getChunk(chunkPosition);

        Vector3i blockPosition = Voxel.getBlockPositionFromGlobalPosition(position);

        return chunk.getBlock(blockPosition);
    }

    public void addBlock(RayResult rayResult) {
        Vector3f axes = rayResult.getAxes();
        Vector3f position = rayResult.getPosition();
        Vector3f delta = position.add(axes, new Vector3f());

        Chunk chunk = this.getChunkFromGlobalPosition(delta);

        if (chunk == null) {
            return;
        }

        Vector3i blockPosition = Voxel.getBlockPositionFromGlobalPosition(delta);

        Debug.debug(chunk.getPosition(), blockPosition, delta);

        chunk.addBlock(blockPosition, BlockType.GRASS);

        chunk.setIsUpdate(true);

        this.queueChunk(chunk.getPosition());
    }

    public void addHeightMap(Vector3i position) {
        HeightMap heightMap = new HeightMap(this.CHUNK_WIDTH, this.CHUNK_HEIGHT);

        heightMap.generate(position);

        this.heightMaps.put(Vector2.getXZInteger(position), heightMap);
    }

    public HeightMap getHeightMap(Vector3i position) {
        return this.heightMaps.get(Vector2.getXZInteger(position));
    }

    public void removeBlock(Vector3f position) {
        Chunk chunk = this.getChunkFromGlobalPosition(position);

        if (chunk == null) {
            return;
        }

        Vector3i blockPosition = Voxel.getBlockPositionFromGlobalPosition(position);

        chunk.removeBlock(blockPosition);

        chunk.setIsUpdate(true);

        this.queueChunk(chunk.getPosition());
    }

    public void queueChunk(int x, int y, int z) {
        Vector3i position = new Vector3i(x, y, z);

        this.queueChunk(position);
    }

    public void queueChunk(Vector3i position) {
        if (!this.queuedChunks.contains(position)) {
            this.chunkQueue.add(position);

            this.queuedChunks.add(position);
        }
    }

    public void addChunk(Vector3i position) {
        Chunk chunk = new Chunk(position);

        HeightMap heightMap = this.getHeightMap(position);

        chunk.generate(heightMap);

        this.chunks.put(position, chunk);
    }

    public void updateChunk(Vector3i position) {
        Chunk chunk = this.getChunk(position);

        // are we actually updating the chunk
        if (chunk.getIsUpdate()) {
            chunk.update();

            chunk.setIsUpdate(false);
        }
    }

    public void addSurroundingChunks(Player player) {
        int renderDistance = player.getRenderDistance();

        Vector3i chunkPosition = Voxel.getChunkPositionFromGlobalPosition(player.getPosition());

        int startX = chunkPosition.x - (this.CHUNK_WIDTH * renderDistance);
        int startY = chunkPosition.y - (this.CHUNK_HEIGHT * renderDistance);
        int startZ = chunkPosition.z - (this.CHUNK_DEPTH * renderDistance);

        int endX = chunkPosition.x + (this.CHUNK_WIDTH * renderDistance);
        int endY = chunkPosition.y + (this.CHUNK_HEIGHT * renderDistance);
        int endZ = chunkPosition.z + (this.CHUNK_DEPTH * renderDistance);

        for (int x = startX; x <= endX; x += this.CHUNK_WIDTH) {
            for (int y = startY; y <= endY; y += this.CHUNK_HEIGHT) {
                for (int z = startZ; z <= endZ; z += this.CHUNK_DEPTH) {
                    Vector3i position = new Vector3i(x, y, z);

                    this.queueChunk(position);
                }
            }
        }
    }

    public void update(Player player) {
        List<Future<?>> tasks = new ArrayList<>();

        this.addSurroundingChunks(player);

        while (!this.chunkQueue.isEmpty()) {
            // Cannot change
            final Vector3i chunkPosition = this.chunkQueue.poll();

            Future<?> task = this.threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    if (!isHeightMapGenerated(chunkPosition)) {
                        addHeightMap(chunkPosition);
                    }

                    if (!isChunkActive(chunkPosition)) {
                        addChunk(chunkPosition);
                    } else {
                        updateChunk(chunkPosition);
                    }
                }
            });

            this.queuedChunks.remove(chunkPosition);

            this.loadedChunks.push(chunkPosition);

            tasks.add(task);

            this.waitForAllTasksToComplete(tasks);
        }
    }

    // Join worker threads
    public void waitForAllTasksToComplete(List<Future<?>> tasks) {
        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    public void render(Camera camera) {
        while (!this.loadedChunks.isEmpty()) {
            Vector3i chunkPosition = this.loadedChunks.poll();

            if (this.isChunkActive(chunkPosition)) {
                Chunk chunk = this.getChunk(chunkPosition);

                chunk.render(camera);
            }
        }
    }

    public void cleanup() {
        for (Chunk chunk : this.chunks.values()) {
            Mesh mesh = chunk.getMesh();

            mesh.cleanup();
        }

        this.threadPool.shutdown();
    }
}
