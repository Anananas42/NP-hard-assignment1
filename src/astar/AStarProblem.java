package astar;

import java.util.*;

import astar.actions.TAction;
import astar.detectors.DeadSquareDetector;
import astar.heuristics.MinDistFromTargetsHeuristic;
import astar.search.HeuristicProblem;
import game.board.compact.BoardCompact;


public class AStarProblem implements HeuristicProblem<BoardCustom, TAction> {
    BoardCustom initState;

    MinDistFromTargetsHeuristic minDistHeuristic;
    boolean[][] isSimpleDeadlock;
    
    public AStarProblem(BoardCustom initialState, BoardCompact boardCompact) {
        this.initState = initialState;
        this.minDistHeuristic = new MinDistFromTargetsHeuristic(initialState);
        this.isSimpleDeadlock = DeadSquareDetector.detect(boardCompact);
    }

    public BoardCustom initialState() {
        return this.initState;
    }

    public List<TAction> actions(BoardCustom state) {
        return state.getActions(isSimpleDeadlock);
    }

    public BoardCustom result(BoardCustom s, TAction action) {
        BoardCustom newState = s.clone();
        action.perform(newState);
        return newState;
    }

    public boolean isGoal(BoardCustom state) {
        return state.isVictory();
    }

    public double cost(BoardCustom state, TAction action) {
        return action.getSteps();
    }

    public double estimate(BoardCustom state) {

        return this.minDistHeuristic.estimate(state);
    }
}
