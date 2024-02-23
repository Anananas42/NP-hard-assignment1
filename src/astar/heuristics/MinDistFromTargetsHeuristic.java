package astar.heuristics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import astar.BoardCustom;
import game.board.oop.EPlace;

public class MinDistFromTargetsHeuristic implements Heuristic {
    BoardCustom b;
    // X and Y coordinates of box targets
    List<Integer> targetX;
    List<Integer> targetY;
    List<Integer> targetPositions; // Single number form
    // Precompute for a level when creating this heuristic
    HashMap<Integer, Integer> minDistanceMap;

    public MinDistFromTargetsHeuristic(BoardCustom board) {
        this.b = board;
        targetX = new ArrayList<>();
        targetY = new ArrayList<>();
        targetPositions = new ArrayList<>();
        minDistanceMap = new HashMap<>();

        for (int y = 0; y < board.height(); ++y) {
			for (int x = 0; x < board.width(); ++x) {
				EPlace place = EPlace.fromSlimFlag(board.tiles[x][y]);
                if (place != null && place.forSomeBox()) {
                    targetX.add(x);
                    targetY.add(y);
                    targetPositions.add(b.getPosition(x, y));
                }
            }
        }

        for (int y = 0; y < board.height(); ++y) {
			for (int x = 0; x < board.width(); ++x) {
                int min = Integer.MAX_VALUE;
                for (int i = 0; i < targetX.size(); i++) {
                    min = Math.min(min, getShortestPathLength(x, y));
                }
                minDistanceMap.put(board.getPosition(x, y), min);
            }
        }
    }

    // private int manhattanDistance(int x1, int y1, int x2, int y2) {
    //     return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    // }

    // Use BFS to find shortest possible path - very brute force but boards are small, so not a problem
    private int getShortestPathLength(int x, int y) {
        Queue<Integer> q = new ArrayDeque<Integer>();
        HashMap<Integer, Integer> prevPositions = new HashMap<>();

        int initPosition = b.getPosition(x, y);
        q.add(initPosition);

        int curr = -1;
        while (!q.isEmpty()) {
            curr = q.poll();

            if (targetPositions.contains(curr)) {
                int result = 0;
                int prevNode = curr;
                while (prevNode != initPosition) {
                    prevNode = prevPositions.get(prevNode);
                    result++;
                }
                return result;
            }

            // Get non-wall neighbours
            for (Integer neighbour : b.getNonWallNeighbours(curr)) {
                if (prevPositions.containsKey(neighbour)) continue; // Already visited
                q.add(neighbour);
                prevPositions.put(neighbour, curr);
            }
        }
        return 0; // Box cannot ever reach a target. The level isn't designed in a way that this box is supposed to move.
    }

    public double estimate(BoardCustom board) {
        double result = 0;
        for (Integer box : board.getBoxes()) {
            result += minDistanceMap.get(box);
        }
        return result;
    }
}
