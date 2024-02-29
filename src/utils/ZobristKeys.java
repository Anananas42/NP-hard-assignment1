package utils;
import astar.BoardCustom;

import java.util.Random;

public class ZobristKeys {
    public static long[][] KEYS;

    static {
        initializeKeys();
    }

    public static void initializeKeys() {
        Random random = new Random(42);

        KEYS = new long[BoardCustom.width][BoardCustom.height];
        for (int i = 0; i < BoardCustom.width; i++) {
            for (int j = 0; j < BoardCustom.height; j++) {
                KEYS[i][j] = random.nextLong();
            }
        }
    }
}
