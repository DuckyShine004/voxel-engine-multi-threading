package com.duckyshine.app.model;

import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Vector3i;

import com.duckyshine.app.math.Axis;
import com.duckyshine.app.math.RandomNumber;
import com.duckyshine.app.math.Range;
import com.duckyshine.app.math.Voxel;
import com.duckyshine.app.math.noise.Noise;
import com.duckyshine.app.scene.ChunkManager;
import com.duckyshine.app.scene.HeightMap;
import com.duckyshine.app.debug.Debug;

public class Chunk {
    private final int WIDTH = 16;
    private final int DEPTH = 16;
    private final int HEIGHT = 16;

    private final Vector3i position;

    private boolean isUpdate;
    private boolean isRender;

    private Block[][][] blocks;

    private Mesh mesh;

    public Chunk(Vector3i position) {
        this.position = position;

        this.initialise();
    }

    public Chunk(int x, int y, int z) {
        this.position = new Vector3i(x, y, z);

        this.initialise();
    }

    public void initialise() {
        this.isUpdate = false;
        this.isRender = false;

        this.blocks = new Block[WIDTH][HEIGHT][DEPTH];

        this.mesh = new Mesh();
    }

    public boolean isValidHeight(int height) {
        return Range.isInRange1D(height, this.position.y, this.position.y + this.HEIGHT);
    }

    public void generate(HeightMap heightMap) {
        int[][] heights = heightMap.getHeights();

        this.generateSurface(heights);
        this.generateUnderground(heights);
        this.generateEnvironment(heights);

        this.update();
    }

    public int getHeightDifference(int y, int height) {
        return this.position.y + y - height;
    }

    public void generateSurface(int[][] heights) {
        for (int z = 0; z < this.DEPTH; z++) {
            for (int x = 0; x < this.WIDTH; x++) {
                int height = heights[z][x];

                if (this.isValidHeight(height)) {
                    int y = Math.floorMod(height, this.HEIGHT);

                    this.addBlock(x, y, z, BlockType.GRASS);
                }
            }
        }
    }

    // theoretical value for now
    public boolean isOutOfBound(int point, Axis axis) {
        if (axis == Axis.Y && (this.position.y + point) < -(1 << 3)) {
            return true;
        }

        return false;
    }

    // Trees and grass for now, customised structures in later iterations
    public void generateEnvironment(int[][] heights) {
        for (int z = 0; z < this.DEPTH; z++) {
            for (int x = 0; x < this.WIDTH; x++) {
                int height = heights[z][x];

                for (int y = 0; y < this.HEIGHT; y++) {
                    int heightDifference = this.getHeightDifference(y, height);

                    if (heightDifference == 1) {
                        if (canGenerateTree(x, y, z)) {
                            this.addTree(x, y, z);
                        }
                    }
                }
            }
        }
    }

    public boolean isTreeInChunk(int x, int y, int z) {
        return y + 6 < this.HEIGHT && x - 2 >= 0 && x + 2 < this.WIDTH && z - 2 >= 0 && z + 2 < this.DEPTH;
    }

    public boolean canGenerateTree(int x, int y, int z) {
        return isTreeInChunk(x, y, z) && RandomNumber.getChance() < 0.05f;
    }

    public void addTree(int x, int y, int z) {
        for (int dy = 0; dy < 3; dy++) {
            this.addBlock(x, y + dy, z, BlockType.OAK_LOG);
        }

        for (int dz = -2; dz < 3; dz++) {
            for (int dx = -2; dx < 3; dx++) {
                for (int dy = 3; dy < 5; dy++) {
                    this.addBlock(x + dx, y + dy, z + dz, BlockType.OAK_LEAVES);
                }
            }
        }

        int[] crossX = { -1, 0, 0, 0, 1 };
        int[] crossZ = { 0, -1, 0, 1, 0 };

        for (int i = 0; i < crossX.length; i++) {
            int dx = crossX[i];
            int dz = crossZ[i];

            for (int dy = 5; dy < 7; dy++) {
                this.addBlock(x + dx, y + dy, z + dz, BlockType.OAK_LEAVES);
            }
        }
    }

    // Use 3d noise
    public void generateUnderground(int[][] heights) {
        for (int z = 0; z < this.DEPTH; z++) {
            for (int x = 0; x < this.WIDTH; x++) {
                int height = heights[z][x];

                for (int y = 0; y < this.HEIGHT; y++) {
                    if (this.isOutOfBound(y, Axis.Y)) {
                        continue;
                    }

                    int heightDifference = this.getHeightDifference(y, height);

                    if (heightDifference <= -1 && heightDifference >= -3) {
                        this.addBlock(x, y, z, BlockType.DIRT);
                    }

                    if (heightDifference < -3) {
                        this.addBlock(x, y, z, BlockType.STONE);
                    }
                }
            }
        }
    }

    public void update() {
        this.mesh.update(this);
    }

    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }

    public Mesh getMesh() {
        return this.mesh;
    }

    public boolean isBlockActive(Vector3i position) {
        return this.isBlockActive(position.x, position.y, position.z);
    }

    public boolean isBlockActive(int x, int y, int z) {
        Vector3i position = new Vector3i(x, y, z);

        if (!Range.isInRange3D(position, this.WIDTH, this.HEIGHT, this.DEPTH)) {
            return false;
        }

        return this.blocks[x][y][z] != null;
    }

    // Assumes args are valid
    public void addBlock(Vector3i position, BlockType blockType) {
        this.addBlock(position.x, position.y, position.z, blockType);
    }

    public void addBlock(int x, int y, int z, BlockType blockType) {
        Block block = new Block(x, y, z, blockType);

        block.setGlobalPosition(this.position.x + x, this.position.y + y, this.position.z + z);

        this.blocks[x][y][z] = block;
    }

    public void removeBlock(Vector3i position) {
        this.removeBlock(position.x, position.y, position.z);
    }

    public void removeBlock(int x, int y, int z) {
        this.blocks[x][y][z] = null;
    }

    public Block getBlock(int x, int y, int z) {
        return this.blocks[x][y][z];
    }

    public Block getBlock(Vector3i position) {
        return this.getBlock(position.x, position.y, position.z);
    }

    public Vector3i getPosition() {
        return this.position;
    }

    public void setIsUpdate(boolean isUpdate) {
        this.isUpdate = isUpdate;
    }

    public boolean getIsUpdate() {
        return this.isUpdate;
    }

    public void setIsRender(boolean isRender) {
        this.isRender = isRender;
    }

    public boolean getIsRender() {
        return this.isRender;
    }

    public int getWidth() {
        return this.WIDTH;
    }

    public int getHeight() {
        return this.HEIGHT;
    }

    public int getDepth() {
        return this.DEPTH;
    }
}
