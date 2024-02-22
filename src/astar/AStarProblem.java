package astar;

import java.util.*;

import astar.actions.TAction;
import astar.search.HeuristicProblem;


public class AStarProblem implements HeuristicProblem<BoardCustom, TAction> {
    BoardCustom initState;
    
    public AStarProblem(BoardCustom initialState) {
        this.initState = initialState;
    }

    public BoardCustom initialState() {
        return this.initState;
    }

    public List<TAction> actions(BoardCustom state) {
        return state.getActions();
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
        return 0.0;
    }
}