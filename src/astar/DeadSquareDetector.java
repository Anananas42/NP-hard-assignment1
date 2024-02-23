package astar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

import astar.actions.TTile;
import game.actions.EDirection;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;

public class DeadSquareDetector {
    public static boolean[][] isSimpleDeadlock;
    private static HashMap<Integer, Boolean> visitedFreezeDeadlocks;
    public static int frozenDeadlockCount;

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
        // For analysis
        frozenDeadlockCount = 0;

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

    public static boolean isFreezeDeadlock(EDirection pushDirection, int x, int y, BoardCustom b) {
        visitedFreezeDeadlocks = new HashMap<>();
        // For both axes check:
        // 1. If there is a wall on the left or on the right side of the box then the box is blocked along this axis
        // 2. If there is a simple deadlock square on both sides (left and right) of the box the box is blocked along this axis
        // 3. If there is a box one the left or right side then this box is blocked if the other box is blocked. 

        // Box position before push
        int sourceX = x + pushDirection.dX;
        int sourceY = y + pushDirection.dY;

        // After push, the target position is where the box will be
        int targetX = x + pushDirection.dX + pushDirection.dX;
        int targetY = y + pushDirection.dY + pushDirection.dY;

        // If the position would be pushed to on a target tile, return true only if some neighbours not on target tiles become frozen
        byte tile = b.tile(targetX, targetY);
        if (TTile.forBox(tile)) {
            boolean frozenNonTargetNeighbour = false;
            byte tileRight = b.tile(targetX+1, targetY);
            byte tileLeft = b.tile(targetX-1, targetY);
            byte tileUp = b.tile(targetX, targetY-1);
            byte tileDown = b.tile(targetX, targetY+1);

            frozenNonTargetNeighbour |= !TTile.forBox(tileRight) && TTile.isBox(tileRight) && isFreezeDeadlock(sourceX, sourceY, targetX, targetY, targetX+1, targetY, b); 
            frozenNonTargetNeighbour |= !TTile.forBox(tileLeft) && TTile.isBox(tileLeft) && isFreezeDeadlock(sourceX, sourceY, targetX, targetY, targetX-1, targetY, b); 
            frozenNonTargetNeighbour |= !TTile.forBox(tileUp) && TTile.isBox(tileUp) && isFreezeDeadlock(sourceX, sourceY, targetX, targetY, targetX, targetY-1, b); 
            frozenNonTargetNeighbour |= !TTile.forBox(tileDown) && TTile.isBox(tileDown) && isFreezeDeadlock(sourceX, sourceY, targetX, targetY, targetX, targetY+1, b); 

            if (frozenNonTargetNeighbour) frozenDeadlockCount++;
            return frozenNonTargetNeighbour;
        }

        boolean result = isFreezeDeadlock(sourceX, sourceY, targetX, targetY, targetX, targetY, b);
        if (result) frozenDeadlockCount++;
        return result;
    }

    // Look if box at x and y is freeze deadlocked while taking into account that box from source is supposed to be moved to target
    private static boolean isFreezeDeadlock(int sourceX, int sourceY, int targetX, int targetY, int x, int y, BoardCustom b) {
        if (x <= 0 || x >= b.width() || y <= 0 || y >= b.height()) return true;

        int position = b.getPosition(x, y);
        if (visitedFreezeDeadlocks.containsKey(position)) return visitedFreezeDeadlocks.get(position);
        visitedFreezeDeadlocks.put(position, true);

        // Horizontal freeze
        boolean leftOutOfBound = x-1 < 0;
        boolean rightOutOfBound = x+1 >= b.width();

        boolean horizontalWallBlock = leftOutOfBound || rightOutOfBound || TTile.isWall(b.tile(x-1, y)) || TTile.isWall(b.tile(x+1, y));

        boolean horizontalSimpleDeadlockBlock = horizontalWallBlock
                                              || (leftOutOfBound || isSimpleDeadlock[x-1][y]) && (rightOutOfBound || isSimpleDeadlock[x+1][y]);

        boolean horizontalBoxBlock = horizontalSimpleDeadlockBlock 
                                   || (leftOutOfBound || (!(x-1 == sourceX && y == sourceY) && TTile.isBox(b.tile(x-1, y)) && isFreezeDeadlock(sourceX, sourceY, targetX, targetY, x-1, y, b)))
                                   || (rightOutOfBound || (!(x+1 == sourceX && y == sourceY) && TTile.isBox(b.tile(x+1, y)) && isFreezeDeadlock(sourceX, sourceY, targetX, targetY, x+1, y, b)));

        if (!horizontalBoxBlock) return false;

        // Vertical freeze
        boolean upOutOfBound = y-1 < 0;
        boolean downOutOfBound = y+1 >= b.height();

        boolean verticalWallBlock = upOutOfBound || downOutOfBound || TTile.isWall(b.tile(x, y+1)) || TTile.isWall(b.tile(x, y-1));
        if (verticalWallBlock) return true;

        boolean verticalSimpleDeadlockBlock = (upOutOfBound || isSimpleDeadlock[x][y-1]) && (downOutOfBound || isSimpleDeadlock[x][y+1]);
        if (verticalSimpleDeadlockBlock) return true;

        boolean verticalBoxBlock = (upOutOfBound || (!(x == sourceX && y-1 == sourceY) && TTile.isBox(b.tile(x, y-1)) && isFreezeDeadlock(sourceX, sourceY, targetX, targetY, x, y-1, b)))
                                || (downOutOfBound || (!(x == sourceX && y+1 == sourceY) && TTile.isBox(b.tile(x, y+1)) && isFreezeDeadlock(sourceX, sourceY, targetX, targetY, x, y+1, b)));
        if (verticalBoxBlock) return true;

        return false;
    }
}
