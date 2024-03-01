package astar.heuristics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import astar.BoardCustom;
import astar.actions.TTile;
import astar.detectors.DeadSquareDetector;
import astar.util.HungarianAlgorithm;

public class MinDistFromTargetsHeuristic implements Heuristic {
    BoardCustom b;

    // Precompute for a level when creating this heuristic
    HashMap<Integer, Integer> minDistanceMap;

    public MinDistFromTargetsHeuristic(BoardCustom board) {
        this.b = board;
        minDistanceMap = new HashMap<>();

        for (int y = 0; y < BoardCustom.height; ++y) {
			for (int x = 0; x < BoardCustom.width; ++x) {
                int min = Integer.MAX_VALUE;
                for (int i = 0; i < BoardCustom.targets.size(); i++) {
                    List<Integer> distances = getShortestPathLength(x, y, false, BoardCustom.targets);
                    if (distances.isEmpty()) continue; // Walls return empty list
                    min = Math.min(min, getShortestPathLength(x, y, false, BoardCustom.targets).get(0));
                }
                minDistanceMap.put(BoardCustom.getPacked(x, y), min);
            }
        }
    }

    // private int manhattanDistance(int x1, int y1, int x2, int y2) {
    //     return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    // }

    // Use BFS to find shortest possible path
    private List<Integer> getShortestPathLength(int x, int y, boolean includeAllPaths, List<Integer> remainingTargets) {
        List<Integer> resultDistances = new ArrayList<>();
        Queue<Integer> q = new ArrayDeque<Integer>();
        HashMap<Integer, Integer> prevPositions = new HashMap<>();

        int initPosition = BoardCustom.getPacked(x, y);
        q.add(initPosition);

        int curr = -1;
        while (!q.isEmpty()) {
            curr = q.poll();

            if (remainingTargets.contains(curr)) {
                int pathLength = 0;
                int prevNode = curr;
                while (prevNode != initPosition) {
                    prevNode = prevPositions.get(prevNode);
                    pathLength++;
                }
                resultDistances.add(pathLength);
                if (!includeAllPaths || (resultDistances.size() == remainingTargets.size())) return resultDistances;
            }

            // Get non-wall neighbours
            for (Integer neighbour : b.getNonWallNeighbours(curr)) {
                if (prevPositions.containsKey(neighbour) || DeadSquareDetector.isSimpleDeadlock[BoardCustom.getX(neighbour)][BoardCustom.getY(neighbour)]) continue; // Already visited
                q.add(neighbour);
                prevPositions.put(neighbour, curr);
            }
        }
        return resultDistances; // Walls return empty list
    }

    private int getMinBipartiteDistanceTotal(BoardCustom board) {
        if (BoardCustom.boxCount == board.boxInPlaceCount) return 0;

        // Each list inside is a list of distances from a box to all targets
        List<Integer> remainingTargets = new ArrayList<>();
        List<Integer> occupiedTargetPackeds = new ArrayList<>();
        for (Integer target : BoardCustom.targets) {
            // Skips targets with a box
            if (TTile.isBox(board.tile(BoardCustom.getX(target), BoardCustom.getY(target)))) {
                occupiedTargetPackeds.add(target);
                continue;
            }
            remainingTargets.add(target);
        }

        int[][] distancesFromTargets = new int[remainingTargets.size()][remainingTargets.size()];
        List<Integer> boxes = board.getBoxes();
        int i = 0;
        for (Integer b : boxes) {
            if (occupiedTargetPackeds.contains(b)) continue; // Skip boxes already on targets
            List<Integer> distances = getShortestPathLength(BoardCustom.getX(b), BoardCustom.getY(b), true, remainingTargets);
            for (int j = 0; j < distances.size(); j++) {
                distancesFromTargets[i][j] = distances.get(j);
            }
            i++;
        }

        return new HungarianAlgorithm(distancesFromTargets).findOptimalAssignment();
    }

    public double estimate(BoardCustom board) {
        // Sum of min distances from closest targets
        double result = 0;
        for (Integer box : board.getBoxes()) {
            result += minDistanceMap.get(box);
        }
        return result;
        
        // Sum of min distances from closest targets without collisions
        // return getMinBipartiteDistanceTotal(board);
    }
}
