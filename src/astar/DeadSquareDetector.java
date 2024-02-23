package astar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

import astar.actions.TTile;
import game.actions.EDirection;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;

public class DeadSquareDetector {
    public static boolean[][] isSimpleDeadlock;

    // 1. Delete all boxes from the board
    // 2. Place a box at the goal square
    // 3. PULL the box from the goal square to every possible square and mark all reached squares as visited 
    
    public static boolean[][] detect(BoardCompact board) {
        if (isSimpleDeadlock == null) {
            precomputeSimpleDeadlocks(board);

            // DEBUG
            // for (int x = 0; x < board.width(); x++) {
            //     System.out.println();
            //     for (int y = 0; y < board.height(); y++) {
            //         if (CTile.isWall(board.tile(x, y))) {
            //             System.out.print("#");
            //         }else if (isSimpleDeadlock[x][y]){
            //             System.out.print("X");
            //         }else{
            //             System.out.print(" ");
            //         }
            //     }
            // }
        }        

        return isSimpleDeadlock;
    }

    private static void precomputeSimpleDeadlocks(BoardCompact board) {
        // Get targets
        isSimpleDeadlock = new boolean[board.width()][board.height()];
        List<Integer> targetX = new ArrayList<>();
        List<Integer> targetY = new ArrayList<>();
        boolean[][] isWall = new boolean[board.width()][board.height()];

        for (int x = 0; x < board.width(); ++x) {
			for (int y = 0; y < board.height(); ++y) {
                // Initialize the deadlock 2D array to true
                isSimpleDeadlock[x][y] = true;

                int tile = board.tiles[x][y];
                if (CTile.forSomeBox(tile)) {
                    targetX.add(x);
                    targetY.add(y);
                }
                isWall[x][y] = CTile.isWall(tile);
			}			
		}

        // For each target, mark reachable tiles as not deadlocks
        for (int i = 0; i < targetX.size(); i++) {
            Queue<Integer> remainingX = new ArrayDeque<>();
            Queue<Integer> remainingY = new ArrayDeque<>();
            HashSet<Integer> visited = new HashSet<>();

            remainingX.add(targetX.get(i));
            remainingY.add(targetY.get(i));
            visited.add(targetX.get(i) + targetY.get(i) * board.width());

            int x, y;
            while (!remainingX.isEmpty()) {
                x = remainingX.poll();
                y = remainingY.poll();
                isSimpleDeadlock[x][y] = false;

                // Process possible pulls
                for (EDirection dir : EDirection.arrows()) {
                    if (!isPullPossible(x, y, dir, isWall) || visited.contains((x+dir.dX) + (y+dir.dY) * board.width())) continue;

                    int neighbourX = x + dir.dX;
                    int neighbourY = y + dir.dY;

                    remainingX.add(neighbourX);
                    remainingY.add(neighbourY);
                    visited.add(neighbourX + neighbourY * board.width());
                }
            }
        }
    }

    private static boolean isPullPossible(int x, int y, EDirection dir, boolean[][] isWall) {
        return !isWall[x+dir.dX][y+dir.dY] && !isWall[x+dir.dX+dir.dX][y+dir.dY+dir.dY];
    }
}
