package astar.heuristics;

// S = state type
public interface Heuristic<S> {
    double estimate(S state);  // optimistic estimate of cost from state to goal
}
