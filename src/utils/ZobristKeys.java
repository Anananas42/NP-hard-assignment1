package utils;
import astar.BoardCustom;

import java.util.Random;

public class ZobristKeys {
    public static int[][] playerKEYS;
    public static int[][] boxKEYS;

    static {
        initializeKeys();
    }

    public static void initializeKeys() {
        Random random = new Random(42);

        playerKEYS = new int[BoardCustom.width][BoardCustom.height];
        boxKEYS = new int[BoardCustom.width][BoardCustom.height];
        for (int i = 0; i < BoardCustom.width; i++) {
            for (int j = 0; j < BoardCustom.height; j++){
                playerKEYS[i][j] = random.nextInt();
                boxKEYS[i][j] = random.nextInt();
            }
        }
    }
}
