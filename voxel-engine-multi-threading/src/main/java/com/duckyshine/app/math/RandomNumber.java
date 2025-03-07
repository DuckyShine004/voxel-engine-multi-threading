package com.duckyshine.app.math;

import java.util.Random;

public class RandomNumber {
    private static final Random RANDOM = new Random();

    public static int getRandomInteger(int bound) {
        return RandomNumber.RANDOM.nextInt(bound);
    }

    public static long getRandomLong() {
        return RandomNumber.RANDOM.nextLong();
    }

    public static long getRandomLong(long bound) {
        return RandomNumber.RANDOM.nextLong(bound);
    }

    public static float getRandomFloat(float bound) {
        return RandomNumber.RANDOM.nextFloat(bound);
    }

    public static float getChance() {
        return RandomNumber.getRandomFloat(1.0f);
    }
}
