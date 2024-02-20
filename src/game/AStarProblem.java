package game;

import java.util.*;

import game.actions.compact.CAction;
import game.actions.compact.CMove;
import game.actions.compact.CPush;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;
import search.HeuristicProblem;

public class AStarProblem implements HeuristicProblem<BoardCompact, CAction> {
    BoardCompact initial;
    List<Position> boxTargets;

    private class Position {
        int tileNum, x, y;

        public Position(int tileNum, int x, int y) {
            this.tileNum = tileNum;
            this.x = x;
            this.y = y;
        }
    }

	public AStarProblem(BoardCompact initial) { 
        this.initial = initial;
        this.boxTargets = getBoxTargets();
    }

    private List<Position> getBoxTargets() {
        List<Position> boxTargets = new ArrayList<>();

        // Iterate over all positions and add box targets
        for (int targetX = 0; targetX < this.initial.width(); targetX++) {
            for (int targetY = 0; targetY < this.initial.height(); targetY++) {
                int tileNum = this.initial.tile(targetX, targetY);
                
                if (!CTile.forSomeBox(tileNum)) continue;
                boxTargets.add(new Position(tileNum, targetX, targetY));
            }
        }

        return boxTargets;
    }

    private int getClosestTargetDistance(List<Position> boxes, Position boxPosition) {
        int minDist = Integer.MAX_VALUE;
        for (Position target : boxTargets) {
            boolean isOccupied = false;
            for (Position box : boxes) {
                if (box.x != target.x || box.y != target.y) continue;
                isOccupied = true;
                break;
            }
            if (isOccupied) continue;
            minDist = Math.min(minDist, this.getManhattanDistance(target.x, target.y, boxPosition.x, boxPosition.y));
        }
        return minDist;
    }

    private int getManhattanDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    }

    private List<Position> getBoxPositions(BoardCompact state) {
        List<Position> boxes = new ArrayList<>();
        for (int x = 0; x < state.width(); x++) {
            for (int y = 0; y < state.height(); y++) {
                int tileNum = state.tile(x, y);
                
                if (!CTile.isSomeBox(tileNum)) continue;
                boxes.add(new Position(tileNum, x, y));
            }
        }
        return boxes;
    }

    public BoardCompact initialState() {
        return initial;
    }

    public List<CAction> actions(BoardCompact state) {
        List<CAction> actions = new ArrayList<CAction>(4);
		
		for (CMove move : CMove.getActions()) {
			if (move.isPossible(state)) {
				actions.add(move);
			}
		}
		for (CPush push : CPush.getActions()) {
			if (push.isPossible(state)) {
				actions.add(push);
			}
		}
        return actions;
    }

    public BoardCompact result(BoardCompact s, CAction action) {
        BoardCompact newState = s.clone();
        action.perform(newState);
        return newState;
    }

    public boolean isGoal(BoardCompact state) {
        return state.isVictory();
    }

    public double cost(BoardCompact state, CAction action) {
        return 1;
    }

    public double estimate(BoardCompact state) {

        List<Position> boxes = getBoxPositions(state);
        int totalBoxDistances = 0;
        for (Position box : boxes) {
            totalBoxDistances += getClosestTargetDistance(boxes, box);
        }

        return totalBoxDistances;
    }
}
