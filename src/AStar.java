import java.util.*;

import search.*;

// A* search

public class AStar<S, A> {
  public static <S, A> Solution<S, A> search(HeuristicProblem<S, A> prob) {
    S initState = prob.initialState();

    // Initialize priority queue with initial state and value 0
    HashMap<S, Double> valueMap = new HashMap<S, Double>();
    HashSet<S> visited = new HashSet<S>();
    PriorityQueue<S> remaining = new PriorityQueue<S>(new Comparator<S>() {
      public int compare(S s1, S s2) {
        if (!valueMap.containsKey(s1)) {
          return 1;
        }else if (!valueMap.containsKey(s2)) {
          return -1;
        }
        return Double.compare(valueMap.get(s1), valueMap.get(s2));
      }
    });
    HashMap<S, S> prevStatesMap = new HashMap<S, S>();
    HashMap<S, A> prevActionMap = new HashMap<S, A>();

    valueMap.put(initState, prob.estimate(initState));
    remaining.add(initState);

    // -- Main loop --
    S currentState = null;
    Double currentValue;
    while (!remaining.isEmpty()) {
      // Pick next element from queue and mark it visited
      currentState = remaining.poll();

      if (prob.isGoal(currentState)) break;

      visited.add(currentState);

      // Get current value
      currentValue = valueMap.get(currentState);

      // Get its neighbours and filter out visited ones
      List<A> actions = prob.actions(currentState);

      // -- iterate over unvisited neighbours --
      for (A a : actions) {
        S neighbourState = prob.result(currentState, a);
        if (visited.contains(neighbourState)) continue;

        double cost = prob.cost(currentState, a);

        double newValue = (currentValue - prob.estimate(currentState)) + cost + prob.estimate(neighbourState);
        
        // Check if current value + edge cost + heuristic is smaller than neighbour's value and update it (reorders PQ)
        if (!valueMap.containsKey(neighbourState) || newValue < valueMap.get(neighbourState)) {
          valueMap.put(neighbourState, newValue);
          remaining.add(neighbourState); // Duplicate will always be lower so the higher ones will be skipped since they'll already be marked as visited

          // Keep track of previous nodes
          prevStatesMap.put(neighbourState, currentState);
          prevActionMap.put(neighbourState, a);
        }
      }
    }

    // Goal reached, reconstruct solution
    if (prob.isGoal(currentState)) {
      S goalState = currentState;
      List<A> actions = new ArrayList<A>();
      double pathCost = 0;

      A prevAction;
      S prevState = currentState;
      while (prevState != initState) {
        prevAction = prevActionMap.get(prevState);
        prevState = prevStatesMap.get(prevState);
        actions.add(0, prevAction);
        pathCost += prob.cost(prevState, prevAction);
      }

      return new Solution<S, A>(actions, goalState, pathCost);
    }

    // Goal not reached, return null
    return null;
  }     
}
