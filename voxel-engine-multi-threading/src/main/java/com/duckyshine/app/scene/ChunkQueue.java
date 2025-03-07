package com.duckyshine.app.scene;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.joml.Vector3i;

public class ChunkQueue {
    private Set<Vector3i> queuedChunks;

    private Deque<Vector3i> chunks;

    public ChunkQueue() {
        this.queuedChunks = new HashSet<>();

        this.chunks = new ArrayDeque<>();
    }

    public boolean isInQueue(Vector3i position) {
        return this.queuedChunks.contains(position);
    }

    public void queue(Vector3i position) {
        if (!this.queuedChunks.contains(position)) {
            this.chunks.add(position);

            this.queuedChunks.add(position);
        }
    }

    public void update() {

        while (!this.chunks.isEmpty()) {
            Vector3i position = this.chunks.poll();

            this.queuedChunks.remove(position);
        }
    }
}
