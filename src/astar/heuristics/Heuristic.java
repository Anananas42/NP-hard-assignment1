package astar.heuristics;

import astar.BoardCustom;

public interface Heuristic {
    public double estimate(BoardCustom state);  // optimistic estimate of cost from state to goal
}
