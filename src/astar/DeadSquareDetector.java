package astar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

import astar.actions.TTile;
import astar.util.BipartiteMatcher;
import game.actions.EDirection;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;

public class DeadSquareDetector {
    public static boolean[][] isSimpleDeadlock;
    // Mask for each target to compute bipartite deadlocks
    private static boolean[][][] isSimpleDeadlockByTarget;
    private static List<Integer> targetX;
    private static List<Integer> targetY;

    private static HashMap<Integer, Boolean> visitedFreezeDeadlocks;
    public static int frozenDeadlockCount;
    public static int frozenDeadlockCallsCount;
    public static long frozenDeadlockSearchTime;

    public static int bipartiteDeadlockCount;
    public static int bipartiteDeadlockCallsCount;
    public static long bipartiteDeadlockSearchTime;

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
        frozenDeadlockCallsCount = 0;
        frozenDeadlockSearchTime = 0;

        bipartiteDeadlockCount = 0;
        bipartiteDeadlockCallsCount = 0;
        bipartiteDeadlockSearchTime = 0;

        // Get targets
        isSimpleDeadlock = new boolean[board.width()][board.height()];
        targetX = new ArrayList<>();
        targetY = new ArrayList<>();
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

        isSimpleDeadlockByTarget = new boolean[board.width()][board.height()][targetX.size()];
        for (int x = 0; x < board.width(); ++x) {
			for (int y = 0; y < board.height(); ++y) {
                for (int i = 0; i < targetX.size(); i++) {
                    isSimpleDeadlockByTarget[x][y][i] = true;
                }
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
                isSimpleDeadlockByTarget[x][y][i] = false;

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

    // Checks only whether a target becomes unreachable for all boxes
    public static boolean isBipartiteDeadlockSimple(EDirection pushDirection, int x, int y, BoardCustom b) {
        // Check if there exists a target that no box can reach
        // Box position before push
        int startX = x + pushDirection.dX;
        int startY = y + pushDirection.dY;

        // After push, the target position is where the box will be
        int endX = x + pushDirection.dX + pushDirection.dX;
        int endY = y + pushDirection.dY + pushDirection.dY;

        // First get all targets that can be reached by the current box before push, but won't be reachable after push
        List<Integer> reachableTargetIndices = new ArrayList<>();
        for (int t = 0; t < targetX.size(); t++) {
            if (isSimpleDeadlockByTarget[startX][startY][t]) continue;
            reachableTargetIndices.add(t);
        }

        // For these targets, check if there's still some other box that can reach them
        for (Integer reachableTarget : reachableTargetIndices) {
            // Check new place of the moved box
            if (!isSimpleDeadlockByTarget[endX][endY][reachableTarget]) continue;
            boolean isUnreachable = true;
            for (Integer boxPosition : b.getBoxes()) {
                int bx = b.getXFromPosition(boxPosition);
                int by = b.getYFromPosition(boxPosition);
                // The moved box won't be at its current place anymore
                if (bx == startX && by == startY) continue;
                // At least one box can reach this target
                if (!isSimpleDeadlockByTarget[bx][by][reachableTarget]) {
                    isUnreachable = false;
                    break;
                }
            }
            if (isUnreachable) return true;
        }

        return false;
    }

    // Checks not only that every target has a box that can reach it, but also that there are enough boxes to distribute to the targets
    public static boolean isBipartiteDeadlock(EDirection pushDirection, int x, int y, BoardCustom b) {
        bipartiteDeadlockCallsCount++;
        long searchStartMillis = System.currentTimeMillis();

        // Check if there exists a target that no box can reach
        // Box position before push
        int startX = x + pushDirection.dX;
        int startY = y + pushDirection.dY;

        // After push, the target position is where the box will be
        int endX = x + pushDirection.dX + pushDirection.dX;
        int endY = y + pushDirection.dY + pushDirection.dY;

        // For these targets, check if there's still some other box that can reach them
        List<List<Integer>> reachableBoxesByTarget = new ArrayList<>();
        for (int target = 0; target < targetX.size(); target++) {
            List<Integer> reachableBoxes = new ArrayList<>();
            // Check new place of the moved box
            if (!isSimpleDeadlockByTarget[endX][endY][target]) {
                reachableBoxes.add(b.getPosition(endX, endY));
            }
            boolean isUnreachable = true;
            for (Integer boxPosition : b.getBoxes()) {
                int bx = b.getXFromPosition(boxPosition);
                int by = b.getYFromPosition(boxPosition);
                // The moved box won't be at its current place anymore
                if (bx == startX && by == startY) continue;
                // At least one box can reach this target
                if (!isSimpleDeadlockByTarget[bx][by][target]) {
                    isUnreachable = false;
                    reachableBoxes.add(boxPosition);
                }
            }
            reachableBoxesByTarget.add(reachableBoxes);
            if (isUnreachable) bipartiteDeadlockCount++;
            bipartiteDeadlockSearchTime += System.currentTimeMillis() - searchStartMillis;
            if (isUnreachable) return true;
        }

        // ??? Use Ford-Fulkerson max flow algorithm to decide whether there is a matching
        // Targets are connected to source
        // Edges indicate that a target can be reached by a box
        // Boxes are connected to sink
        // Flow at the sink must be same as number of targets
        BipartiteMatcher bpmatcher = new BipartiteMatcher(targetX.size(), b.getBoxes(), reachableBoxesByTarget);
        int matchingSize = bpmatcher.hopcroftKarp();

        // if (matchingSize != targetX.size()) {
        //     System.out.println(x + ", " + y + ", " + pushDirection.toString());
        //     b.debugPrint();
        // }
        if (matchingSize != targetX.size()) bipartiteDeadlockCount++;
        bipartiteDeadlockSearchTime += System.currentTimeMillis() - searchStartMillis;
        return matchingSize != targetX.size();
    }

    public static boolean isFreezeDeadlock(EDirection pushDirection, int x, int y, BoardCustom b) {
        frozenDeadlockCallsCount++;
        long searchStartMillis = System.currentTimeMillis();

        visitedFreezeDeadlocks = new HashMap<>();
        // For both axes check:
        // 1. If there is a wall on the left or on the right side of the box then the box is blocked along this axis
        // 2. If there is a simple deadlock square on both sides (left and right) of the box the box is blocked along this axis
        // 3. If there is a box one the left or right side then this box is blocked if the other box is blocked. 

        // Box position before push
        int startX = x + pushDirection.dX;
        int startY = y + pushDirection.dY;

        // After push, the target position is where the box will be
        int endX = x + pushDirection.dX + pushDirection.dX;
        int endY = y + pushDirection.dY + pushDirection.dY;

        // If the position would be pushed to on a target tile, return true only if some neighbours not on target tiles become frozen
        byte tile = b.tile(endX, endY);
        if (TTile.forBox(tile)) {
            boolean frozenNonTargetNeighbour = false;
            byte tileRight = b.tile(endX+1, endY);
            byte tileLeft = b.tile(endX-1, endY);
            byte tileUp = b.tile(endX, endY-1);
            byte tileDown = b.tile(endX, endY+1);

            frozenNonTargetNeighbour |= !TTile.forBox(tileRight) && TTile.isBox(tileRight) && isFreezeDeadlock(startX, startY, endX, endY, endX+1, endY, b); 
            frozenNonTargetNeighbour |= !TTile.forBox(tileLeft) && TTile.isBox(tileLeft) && isFreezeDeadlock(startX, startY, endX, endY, endX-1, endY, b); 
            frozenNonTargetNeighbour |= !TTile.forBox(tileUp) && TTile.isBox(tileUp) && isFreezeDeadlock(startX, startY, endX, endY, endX, endY-1, b); 
            frozenNonTargetNeighbour |= !TTile.forBox(tileDown) && TTile.isBox(tileDown) && isFreezeDeadlock(startX, startY, endX, endY, endX, endY+1, b); 

            if (frozenNonTargetNeighbour) frozenDeadlockCount++;
            frozenDeadlockSearchTime += System.currentTimeMillis() - searchStartMillis;
            return frozenNonTargetNeighbour;
        }

        boolean result = isFreezeDeadlock(startX, startY, endX, endY, endX, endY, b);
        if (result) frozenDeadlockCount++;
        frozenDeadlockSearchTime += System.currentTimeMillis() - searchStartMillis;
        return result;
    }

    // Look if box at x and y is freeze deadlocked while taking into account that box from source is supposed to be moved to target
    private static boolean isFreezeDeadlock(int startX, int startY, int endX, int endY, int x, int y, BoardCustom b) {
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
                                   || (leftOutOfBound || (!(x-1 == startX && y == startY) && TTile.isBox(b.tile(x-1, y)) && isFreezeDeadlock(startX, startY, endX, endY, x-1, y, b)))
                                   || (rightOutOfBound || (!(x+1 == startX && y == startY) && TTile.isBox(b.tile(x+1, y)) && isFreezeDeadlock(startX, startY, endX, endY, x+1, y, b)));

        if (!horizontalBoxBlock) return false;

        // Vertical freeze
        boolean upOutOfBound = y-1 < 0;
        boolean downOutOfBound = y+1 >= b.height();

        boolean verticalWallBlock = upOutOfBound || downOutOfBound || TTile.isWall(b.tile(x, y+1)) || TTile.isWall(b.tile(x, y-1));
        if (verticalWallBlock) return true;

        boolean verticalSimpleDeadlockBlock = (upOutOfBound || isSimpleDeadlock[x][y-1]) && (downOutOfBound || isSimpleDeadlock[x][y+1]);
        if (verticalSimpleDeadlockBlock) return true;

        boolean verticalBoxBlock = (upOutOfBound || (!(x == startX && y-1 == startY) && TTile.isBox(b.tile(x, y-1)) && isFreezeDeadlock(startX, startY, endX, endY, x, y-1, b)))
                                || (downOutOfBound || (!(x == startX && y+1 == startY) && TTile.isBox(b.tile(x, y+1)) && isFreezeDeadlock(startX, startY, endX, endY, x, y+1, b)));
        if (verticalBoxBlock) return true;

        return false;
    }
}
