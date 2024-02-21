package astar;

import java.util.*;

import astar.search.HeuristicProblem;
import game.actions.compact.CAction;
import game.actions.compact.CMove;
import game.actions.compact.CPush;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;

public class AStarProblem implements HeuristicProblem<BoardCompact, CAction> {
    BoardCompact initState;
    
    public AStarProblem(BoardCompact initialState) {
        this.initState = initialState;
    }

    public BoardCompact initialState() {
        return this.initState;
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
        return 0.0;
    }
}
