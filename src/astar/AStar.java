package astar;

import java.util.*;

import astar.search.*;

// A* search

public class AStar<S, A> {

  public static <S, A> Solution<S, A> search(HeuristicProblem<S, A> prob) {

    PriorityQueue<Node<S, A>> pq = new PriorityQueue<>();
    int searchedNodes = 0;

    // minimal cost to reach the state
    Map<S, Double> costs = new HashMap<>();

    // add initial node
    S startState = prob.initialState();
    pq.add(new Node<>(startState, null, null, 0, prob.estimate(startState)));
    costs.put(startState, 0.0);

    while (!pq.isEmpty()) {
      searchedNodes++;
      Node<S, A> curr = pq.poll();

      if (prob.isGoal(curr.getState())) {
        // collect actions in the parent nodes in reversed order (from goal state to initial state)
        return getSolution(curr, searchedNodes);
      }

      for (A action : prob.actions(curr.getState())) {
        S nextState = prob.result(curr.getState(), action);
        double nextCost = costs.get(curr.getState()) + prob.cost(curr.getState(), action);

        if (nextCost < costs.getOrDefault(nextState, Double.MAX_VALUE)) {
          costs.put(nextState, nextCost);
          Node<S, A> nextNode = new Node<>(nextState, action, curr, nextCost, prob.estimate(nextState));
          pq.add(nextNode);
        }
      }
    }

    return null;
  }

  private static <S, A> Solution<S, A> getSolution(Node<S, A> goalNode, int searchedNodes) {
    List<A> actions = new ArrayList<>();
    Node<S, A> currNode = goalNode;

    while (currNode != null && currNode.getAction() != null) {
      actions.add(currNode.getAction());
      currNode = currNode.getParent();
    }

    Collections.reverse(actions);
    return new Solution<>(actions, goalNode.state, goalNode.getCost(), searchedNodes);
  }


  private static class Node<S, A> implements Comparable<Node<S, A>> {
    private S state;
    private A action;
    private Node<S, A> parent;
    private double cost;
    private double heuristicEstimate;

    public Node(S state, A action, Node<S, A> parent, double cost, double heuristicEstimate) {
      this.state = state;
      this.action = action;
      this.parent = parent;
      this.cost = cost;
      this.heuristicEstimate = heuristicEstimate;
    }

    public S getState() {
      return state;
    }

    public A getAction() {
      return action;
    }

    public Node<S, A> getParent() {
      return parent;
    }

    public double getCost() {
      return cost;
    }

    @Override
    public int compareTo(Node<S, A> other) {
      return Double.compare(this.cost + this.heuristicEstimate, other.cost + other.heuristicEstimate);
    }
  }
}