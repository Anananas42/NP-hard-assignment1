package astar;

import game.actions.EDirection;
import game.actions.compact.CWalk;
import game.actions.compact.CWalkPush;
import game.actions.oop.EActionType;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;
import game.board.oop.EEntity;
import game.board.oop.EPlace;
import game.board.oop.ESpace;
import game.board.slim.BoardSlim;
import game.board.slim.STile;
import java.util.*;

class AStarProblemNonWeblab implements HeuristicProblemNonWeblab<BoardCustomNonWeblab, TActionNonWeblab> {
    BoardCustomNonWeblab initState;

    MinDistFromTargetsHeuristicNonWeblab minDistHeuristic;
    boolean[][] isSimpleDeadlock;
    
    public AStarProblemNonWeblab(BoardCustomNonWeblab initialState, BoardCompact boardCompact) {
        this.initState = initialState;
        this.minDistHeuristic = new MinDistFromTargetsHeuristicNonWeblab(initialState);
        this.isSimpleDeadlock = DeadSquareDetectorNonWeblab.detect(boardCompact);
    }

    public BoardCustomNonWeblab initialState() {
        return this.initState;
    }

    public List<TActionNonWeblab> actions(BoardCustomNonWeblab state) {
        return state.getActions(isSimpleDeadlock);
    }

    public BoardCustomNonWeblab result(BoardCustomNonWeblab s, TActionNonWeblab action) {
        BoardCustomNonWeblab newState = s.clone();
        action.perform(newState);
        return newState;
    }

    public boolean isGoal(BoardCustomNonWeblab state) {
        return state.isVictory();
    }

    public double cost(BoardCustomNonWeblab state, TActionNonWeblab action) {
        return action.getSteps();
    }

    public double estimate(BoardCustomNonWeblab state) {

        return this.minDistHeuristic.estimate(state);
    }
}

class BoardCustomNonWeblab extends BoardSlim {
    // List with box positions calculated as x * width + y (List is efficient since number of boxes is small)
    List<Integer> boxes;

    // List of possible sequences of positions to get from current player's position next to accessible boxes
    List<List<Integer>> possibleWalks;

    public BoardCustomNonWeblab(byte width, byte height) {
        super(width, height);
        this.boxes = new ArrayList<>();
    }

    // Initialize information about the state that will change as agent performs actions
    // but would be costly to recompute from scratch every time
    // THIS METHOD IS CALLED ONLY ONCE WHEN CREATING THE BOARD 
    // (then this precomputed result is copied every time and only slightly changed with actions)
    public void populateDynamicStateDescriptors() {
        for (int y = 0; y < height(); ++y) {
			for (int x = 0; x < width(); ++x) {
                EEntity entity = EEntity.fromSlimFlag(tiles[x][y]);

                if (entity == null || !entity.isSomeBox()) continue;
                boxes.add(getPosition(x, y));
            }
        }
    }

    public List<TActionNonWeblab> getActions(boolean[][] isSimpleDeadlock) {
        List<TActionNonWeblab> result = new ArrayList<>();

        // Add all pushes from the player's current position
        result.addAll(getPushesWithoutDeadlock(playerX, playerY, isSimpleDeadlock));
        // Add all WalkPush actions
        result.addAll(getWalkPushActions(isSimpleDeadlock));
        
        return result;
    }

    public List<TWalkPushSequenceNonWeblab> getWalkPushActions(boolean[][] isSimpleDeadlock) {
        List<TWalkPushSequenceNonWeblab> result = new ArrayList<>();
        List<TWalkNonWeblab> walkActions = getWalkActions();
        for (TWalkNonWeblab walk : walkActions) {
            for (TPushNonWeblab push : getPushesWithoutDeadlock(walk.getDestinationX(), walk.getDestinationY(), isSimpleDeadlock)) {
                result.add(new TWalkPushSequenceNonWeblab(walk, push));
            }
        }
        return result;
    }

    // Get a list of pushes from a position that do not result in a deadlock
    private List<TPushNonWeblab> getPushesWithoutDeadlock(int x, int y, boolean[][] isSimpleDeadlock) {
        List<TPushNonWeblab> result = new ArrayList<>();
        for (TPushNonWeblab push : TPushNonWeblab.getActions()) { 
            boolean isPossible = TPushNonWeblab.isPushPossible(this, x, y, push.getDirection());
            EDirection dir = push.getDirection();
            if (!isPossible                                                             // Illegal move
                 || isSimpleDeadlock[x+dir.dX+dir.dX][y+dir.dY+dir.dY]                  // New box position can't reach any target
                //  || DeadSquareDetectorNonWeblab.isBipartiteDeadlockSimple(dir, x, y, this)       // A target won't have any box that could reach it DISABLED (dont occur in aymeric level set)
                //  || DeadSquareDetectorNonWeblab.isBipartiteDeadlock(dir, x, y, this)             // Every target can be matched and there are enough boxes to distribute DISABLED (dont occur in aymeric level set)
                 || DeadSquareDetectorNonWeblab.isFreezeDeadlock(dir, x, y, this)) continue;     // Freeze deadlocks
            result.add(push);
        }
        
        return result;
    }

    public List<TWalkNonWeblab> getWalkActions() {
        List<TWalkNonWeblab> result = new ArrayList<>();
        List<List<Integer>> possibleWalks = getPossibleWalks();
        for (List<Integer> positionSequence : possibleWalks) {
            int length = positionSequence.size();
            int[] positionsX = new int[length];
            int[] positionsY = new int[length];
            for (int i = 0; i < positionSequence.size(); i++) {
                int position = positionSequence.get(i);
                positionsX[i] = getXFromPosition(position);
                positionsY[i] = getYFromPosition(position);
            }
            // Add a TWalkNonWeblab only if the final position enables pushing a box at least in one of the four directions
            boolean isPushPossible = false;
            for (TPushNonWeblab push : TPushNonWeblab.getActions()) { 
                isPushPossible = TPushNonWeblab.isPushPossible(this, positionsX[length-1], positionsY[length-1], push.getDirection());
                if (isPushPossible) break;
            }
            if (!isPushPossible) continue;
            result.add(TWalkNonWeblab.fromPositions(this, playerX, playerY, positionsX, positionsY));
        }

        return result;
    }

    // Agent always needs to go next to a box and start pushing in order to progress the game
    // This method returns all positions next to boxes that the agent can access
    public List<List<Integer>> getPossibleWalks() {
        // Recompute accessible positions
        possibleWalks = new ArrayList<>();
        Set<Integer> destinations = getBoxNeighbourPositions();

        // BFS
        /// HashMap<Integer, Integer> costMap = new HashMap<>(); // Useless now, but later can be used for heuristic (try closer boxes first)
        Queue<Integer> q = new ArrayDeque<Integer>(); // For uniform cost, we can just use queue. (BFS basically)
        HashMap<Integer, Integer> prevMap = new HashMap<>();

        int initPosition = getPosition(playerX, playerY);
        // costMap.put(initPosition, 0);
        q.add(initPosition);

        int curr = -1;
        // int cost;
        while (!q.isEmpty()) {
            curr = q.poll();

            // Retrieve result
            if (curr != initPosition && destinations.contains(curr)) {
                List<Integer> result = new ArrayList<>();
                int prevNode = curr;
                while (prevNode != initPosition) {
                    result.add(0, prevNode);
                    prevNode = prevMap.get(prevNode); // Correctly doesn't add the initial position.
                }
                possibleWalks.add(result);
            }

            // cost = costMap.get(curr);
            for (Integer neighbour : getWalkableNeighbours(curr)) {
                if (prevMap.containsKey(neighbour)) continue; // Already visited
                q.add(neighbour);
                // costMap.put(neighbour, cost+1);
                prevMap.put(neighbour, curr);
            }
        }
        return possibleWalks;
    }

    // Get all walkable positions next to boxes
    public Set<Integer> getBoxNeighbourPositions() {
        Set<Integer> result = new HashSet<>();

        for (Integer boxPosition : boxes) {
            result.addAll(getWalkableNeighbours(boxPosition));
        }

        return result;
    }

    @Override
    public void moveBox(byte sourceTileX, byte sourceTileY, byte targetTileX, byte targetTileY) {
        super.moveBox(sourceTileX, sourceTileY, targetTileX, targetTileY);

        // Update position of the moved box
        boxes.remove((Integer)getPosition(sourceTileX, sourceTileY));
        boxes.add(getPosition(targetTileX, targetTileY));
    }

    public void moveBox(int sourceTileX, int sourceTileY, int targetTileX, int targetTileY) {
        moveBox((byte)sourceTileX, (byte)sourceTileY, (byte)targetTileX, (byte)targetTileY);
    }

    /**
	 * Fair warning: by moving the player you're invalidating {@link #hashCode()}...
	 * @param sourceTileX
	 * @param sourceTileY
	 * @param targetTileX
	 * @param targetTileY
	 */
	public void movePlayer(byte sourceTileX, byte sourceTileY, byte targetTileX, byte targetTileY) {
		super.movePlayer(sourceTileX, sourceTileY, targetTileX, targetTileY);
        this.nullHash();
	}

    public void movePlayer(int sourceTileX, int sourceTileY, int targetTileX, int targetTileY) {
        movePlayer((byte)sourceTileX, (byte)sourceTileY, (byte)targetTileX, (byte)targetTileY);
    }

    private List<Integer> getWalkableNeighbours(int position) {
        List<Integer> neighbours = new ArrayList<>();
        int x = getXFromPosition(position);
        int y = getYFromPosition(position);
        for (TMoveNonWeblab m : TMoveNonWeblab.getActions()) { // Get moves in all directions
            int neighbourPosition = m.isPossible(this, (byte)x, (byte)y); // Check if move in a direction possible
            if (neighbourPosition == -1) continue; // Not possible, ignore this direction
            neighbours.add(neighbourPosition); // Possible, add neighbour position
        }

        return neighbours;
    }

    public List<Integer> getNonWallNeighbours(int position) {
        List<Integer> neighbours = new ArrayList<>();
        int x = getXFromPosition(position);
        int y = getYFromPosition(position);
        for (TMoveNonWeblab m : TMoveNonWeblab.getActions()) { // Get moves in all directions
            EDirection dir = m.getDirection();
            if (TMoveNonWeblab.isOnBoard(this, (byte)x, (byte)y, m.getDirection()) && !TTileNonWeblab.isWall(tile(x+dir.dX, y+dir.dY))) {
                neighbours.add(getPosition(x+dir.dX, y+dir.dY));
            }
        }

        return neighbours;
    }


    @Override
	public BoardCustomNonWeblab clone() {
        BoardCustomNonWeblab result = new BoardCustomNonWeblab(width(), height());
		result.tiles = new byte[width()][height()];
		for (int x = 0; x < width(); ++x) {
			for (int y = 0; y < height(); ++y) {
				result.tiles[x][y] = tiles[x][y];
			}			
		}
		result.playerX = playerX;
		result.playerY = playerY;
		result.boxCount = boxCount;
		result.boxInPlaceCount = boxInPlaceCount;

        if (this.boxes != null) {
            result.boxes = new ArrayList<>(this.boxes);
        }

        return result;
    }

    public int getPosition(int x, int y) {
        return x + y * width();
    }

    public int getXFromPosition(int position) {
        return position % width();
    }

    public int getYFromPosition(int position) {
        return position / width();
    }

    public List<Integer> getBoxes() {
        return boxes;
    }

    @Override
    public void debugPrint() {
		for (int y = 0; y < height(); ++y) {
			for (int x = 0; x < width(); ++x) {
				EEntity entity = EEntity.fromSlimFlag(tiles[x][y]);
				EPlace place = EPlace.fromSlimFlag(tiles[x][y]);
				ESpace space = ESpace.fromSlimFlag(tiles[x][y]);
				
				if (entity != null && entity != EEntity.NONE) {
					System.out.print(entity.getSymbol());
				} else
				if (place != null && place != EPlace.NONE) {
					System.out.print(place.getSymbol());
				} else
				if (space != null) {
					System.out.print(space.getSymbol());
				} else {
					System.out.print("?");
				}
			}
			System.out.println();
		}
	}

}

// A* search

class AStarNonWeblab<S, A> {

  public static <S, A> SolutionNonWeblab<S, A> search(HeuristicProblemNonWeblab<S, A> prob) {

    PriorityQueue<NodeNonWeblab<S, A>> pq = new PriorityQueue<>();
    int searchedNodes = 0;

    // minimal cost to reach the state
    Map<S, Double> costs = new HashMap<>();

    // add initial node
    S startState = prob.initialState();
    pq.add(new NodeNonWeblab<>(startState, null, null, 0, prob.estimate(startState)));
    costs.put(startState, 0.0);

    while (!pq.isEmpty()) {
      searchedNodes++;
      NodeNonWeblab<S, A> curr = pq.poll();

      if (prob.isGoal(curr.getState())) {
        // collect actions in the parent nodes in reversed order (from goal state to initial state)
        return getSolution(curr, searchedNodes);
      }

      for (A action : prob.actions(curr.getState())) {
        S nextState = prob.result(curr.getState(), action);
        double nextCost = costs.get(curr.getState()) + prob.cost(curr.getState(), action);

        if (nextCost < costs.getOrDefault(nextState, Double.MAX_VALUE)) {
          costs.put(nextState, nextCost);
          NodeNonWeblab<S, A> nextNode = new NodeNonWeblab<>(nextState, action, curr, nextCost, prob.estimate(nextState));
          pq.add(nextNode);
        }
      }
    }

    return null;
  }

  private static <S, A> SolutionNonWeblab<S, A> getSolution(NodeNonWeblab<S, A> goalNode, int searchedNodes) {
    List<A> actions = new ArrayList<>();
    NodeNonWeblab<S, A> currNode = goalNode;

    while (currNode != null && currNode.getAction() != null) {
      actions.add(currNode.getAction());
      currNode = currNode.getParent();
    }

    Collections.reverse(actions);
    return new SolutionNonWeblab<>(actions, goalNode.state, goalNode.getCost(), searchedNodes);
  }


  static  class NodeNonWeblab<S, A> implements Comparable<NodeNonWeblab<S, A>> {
    private S state;
    private A action;
    private NodeNonWeblab<S, A> parent;
    private double cost;
    private double heuristicEstimate;

    public NodeNonWeblab(S state, A action, NodeNonWeblab<S, A> parent, double cost, double heuristicEstimate) {
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

    public NodeNonWeblab<S, A> getParent() {
      return parent;
    }

    public double getCost() {
      return cost;
    }

    @Override
    public int compareTo(NodeNonWeblab<S, A> other) {
      return Double.compare(this.cost + this.heuristicEstimate, other.cost + other.heuristicEstimate);
    }
  }
}

interface HeuristicNonWeblab {
    public double estimate(BoardCustomNonWeblab state);  // optimistic estimate of cost from state to goal
}

class MinDistFromTargetsHeuristicNonWeblab implements HeuristicNonWeblab {
    BoardCustomNonWeblab b;
    // X and Y coordinates of box targets
    List<Integer> targetX;
    List<Integer> targetY;
    List<Integer> targetPositions; // Single number form
    // Precompute for a level when creating this heuristic
    HashMap<Integer, Integer> minDistanceMap;

    public MinDistFromTargetsHeuristicNonWeblab(BoardCustomNonWeblab board) {
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
                    List<Integer> distances = getShortestPathLength(x, y, false, targetPositions);
                    if (distances.isEmpty()) continue; // Walls return empty list
                    min = Math.min(min, getShortestPathLength(x, y, false, targetPositions).get(0));
                }
                minDistanceMap.put(board.getPosition(x, y), min);
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

        int initPosition = b.getPosition(x, y);
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
                if (prevPositions.containsKey(neighbour)) continue; // Already visited
                q.add(neighbour);
                prevPositions.put(neighbour, curr);
            }
        }
        return resultDistances; // Walls return empty list
    }

    private int getMinBipartiteDistanceTotal(BoardCustomNonWeblab board) {
        if (board.boxCount == board.boxInPlaceCount) return 0;

        // Each list inside is a list of distances from a box to all targets
        List<Integer> remainingTargets = new ArrayList<>();
        List<Integer> occupiedTargetPositions = new ArrayList<>();
        for (Integer target : targetPositions) {
            // Skips targets with a box
            if (TTileNonWeblab.isBox(board.tile(board.getXFromPosition(target), board.getYFromPosition(target)))) {
                occupiedTargetPositions.add(target);
                continue;
            }
            remainingTargets.add(target);
        }

        int[][] distancesFromTargets = new int[remainingTargets.size()][remainingTargets.size()];
        List<Integer> boxes = board.getBoxes();
        int i = 0;
        for (Integer b : boxes) {
            if (occupiedTargetPositions.contains(b)) continue; // Skip boxes already on targets
            List<Integer> distances = getShortestPathLength(board.getXFromPosition(b), board.getYFromPosition(b), true, remainingTargets);
            for (int j = 0; j < distances.size(); j++) {
                distancesFromTargets[i][j] = distances.get(j);
            }
            i++;
        }

        return new HungarianAlgorithmNonWeblab(distancesFromTargets).findOptimalAssignment();
    }

    public double estimate(BoardCustomNonWeblab board) {
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

class TWalkPushSequenceNonWeblab extends TActionNonWeblab {
    
    private TWalkNonWeblab walk;
    private TPushNonWeblab push;

    public TWalkPushSequenceNonWeblab(TWalkNonWeblab walk, TPushNonWeblab push) {
        this.walk = walk;
        this.push = push;
    }

    public EActionType getType() {
        return EActionType.WALK_AND_PUSH;
    }
	
	public EDirection getDirection() {
        return walk.getDirection();
    }

	public EDirection[] getDirections() {
        EDirection[] walkDirs = walk.getDirections();
        EDirection[] pushDirs = push.getDirections();

        // Combine the lists
        EDirection[] result = new EDirection[walkDirs.length + pushDirs.length];
        for (int i = 0; i < walkDirs.length; i++) {
            result[i] = walkDirs[i];
        }   
        for (int i = walkDirs.length; i < walkDirs.length+pushDirs.length; i++) {
            result[i] = pushDirs[i-walkDirs.length];
        }   

        return result;
    }

    /**
	 * How many steps the action implements; may return -1 if unknown (i.e., custom teleports).
	 * @return
	 */
	public int getSteps() {
        return walk.getSteps() + push.getSteps();
    }

	public boolean isPossible(BoardCustomNonWeblab board) {
        // Pre-condition when creating this macro action
        return true;
    }
	
	public void perform(BoardCustomNonWeblab board) {
        walk.perform(board);
        push.perform(board);
    }
	
	public void reverse(BoardCustomNonWeblab board) {
        push.reverse(board);
        walk.reverse(board);
    }
}

class TTileNonWeblab extends STile {
    /**
	 * Can a player can pass through this tile (a tile containing a player is considered walkable as well)?
	 * @param tileFlag
	 * @return
	 */
	public static boolean isWalkable(byte tileFlag) {
		return isFree(tileFlag) || isPlayer(tileFlag);
	}
    
}

abstract  class TActionNonWeblab {
	
	public abstract EActionType getType();
	
	public abstract EDirection getDirection();

    /**
	 * Provides "all movements" in case of macro actions (e.g. {@link CWalk}, {@link CWalkPush}).
	 * @return
	 */
	public abstract EDirection[] getDirections();

    /**
	 * How many steps the action implements; may return -1 if unknown (i.e., custom teleports).
	 * @return
	 */
	public abstract int getSteps();

	public abstract boolean isPossible(BoardCustomNonWeblab board);
	
	public abstract void perform(BoardCustomNonWeblab board);
	
	public abstract void reverse(BoardCustomNonWeblab board);

    public static EDirection getDirectionFromPosition(int x1, int y1, int x2, int y2) {
        if (y1 == y2) {
            switch (x2 - x1) {
                case 1:
                    return EDirection.RIGHT;
                case -1:
                    return EDirection.LEFT;
            }
        }else if (x1 == x2) {
            switch (y2 - y1) {
                case 1:
                    return EDirection.DOWN;
                case -1:
                    return EDirection.UP;
            }
        }

        return null;
    }

    /**
	 * If we move 1 step in given 'dir', will we still be at board? 
	 * @param tile
	 * @param dir
	 * @param steps
	 * @return
	 */
	protected boolean onBoard(BoardCustomNonWeblab board, int tileX, int tileY, EDirection dir) {		
		return isOnBoard(board, tileX, tileY, dir);
	}
	
	/**
	 * If we move 1 step in given 'dir', will we still be at board? 
	 * @param tile
	 * @param dir
	 * @param steps
	 * @return
	 */
	public static boolean isOnBoard(BoardCustomNonWeblab board, byte tileX, byte tileY, EDirection dir) {
		int targetX = tileX + dir.dX;
		if (targetX < 0 || targetX >= board.width()) return false;
		int targetY = tileY + dir.dY;
		if (targetY < 0 || targetY >= board.height()) return false;
		return true;
	}

    public static boolean isOnBoard(BoardCustomNonWeblab board, int tileX, int tileY, EDirection dir) {
		return isOnBoard(board, (byte)tileX, (byte)tileY, dir);
	}
}

class TWalkNonWeblab extends TActionNonWeblab {

	// Final position
	private int x;
	private int y;
	
	private int fromX = -1;
	private int fromY = -1;
	
	private EDirection[] path;

	public TWalkNonWeblab(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public TWalkNonWeblab(int x, int y, EDirection[] path) {
		this.x = x;
		this.y = y;
		this.path = path;
	}

	@Override
	public EActionType getType() {
		return EActionType.WALK;
	}

	@Override
	public EDirection getDirection() {
		return path == null ? null : path[0];
	}
	
	@Override
	public EDirection[] getDirections() {
		return path;
	}
	
	/**
	 * How many steps do you need in order to perform the walk; defined only if directions are provided during construction using {@link TWalkNonWeblab#TWalkNonWeblab(int, int, EDirection[])}.
	 * @return
	 */
	public int getSteps() {
		return path == null ? -1 : path.length;
	}

	@Override
	public boolean isPossible(BoardCustomNonWeblab board) {
		return TTileNonWeblab.isWalkable(board.tile(x, y));
	}

	@Override
	public void perform(BoardCustomNonWeblab board) {
		this.fromX = board.playerX;
		this.fromY = board.playerY;
		if (fromX != x || fromY != y) {
			board.movePlayer(board.playerX, board.playerY, x, y);
		}
	}

	@Override
	public void reverse(BoardCustomNonWeblab board) {
		if (fromX != x || fromY != y) {
			board.movePlayer(x, y, fromX, fromY);
		}
	}

	public static TWalkNonWeblab fromPositions(BoardCustomNonWeblab board, int startX, int startY, int[] positionsX, int[] positionsY) {
		EDirection[] directions = new EDirection[positionsX.length];

		int prevX = startX;
		int prevY = startY;
		int currX, currY;
		for (int i = 0; i < positionsX.length; i++) {
			currX = positionsX[i];
			currY = positionsY[i];
			directions[i] = TActionNonWeblab.getDirectionFromPosition(prevX, prevY, currX, currY);
			prevX = currX;
			prevY = currY;
		}

		return new TWalkNonWeblab(prevX, prevY, directions);
	}

	public int getDestinationX() {
		return x;
	}

	public int getDestinationY() {
		return y;
	}

	// Object
	// ======
	
	@Override
	public String toString() {
		if (fromX < 0) {
			return "TWalkNonWeblab[->" + x + "," + y + "]";
		} else {
			return "TWalkNonWeblab[" + fromX + "," + fromY + "->" + x + "," + y + "]";
		}
	}
	
}

class TPushNonWeblab extends TActionNonWeblab {
	
	private static Map<EDirection, TPushNonWeblab> actions = new HashMap<EDirection, TPushNonWeblab>();
	
	static {
		actions.put(EDirection.DOWN, new TPushNonWeblab(EDirection.DOWN));
		actions.put(EDirection.UP, new TPushNonWeblab(EDirection.UP));
		actions.put(EDirection.LEFT, new TPushNonWeblab(EDirection.LEFT));
		actions.put(EDirection.RIGHT, new TPushNonWeblab(EDirection.RIGHT));
	}
	
	public static Collection<TPushNonWeblab> getActions() {
		return actions.values();
	}
	
	public static TPushNonWeblab getAction(EDirection direction) {
		return actions.get(direction);
	}
	
	private EDirection dir;
	
	private EDirection[] dirs;
	
	public TPushNonWeblab(EDirection dir) {
		this.dir = dir;
		this.dirs = new EDirection[]{ dir };
	}
	
	@Override
	public EActionType getType() {
		return EActionType.PUSH;
	}

	@Override
	public EDirection getDirection() {
		return dir;
	}
	
	@Override
	public EDirection[] getDirections() {
		return dirs;
	}
	
	@Override
	public int getSteps() {
		return 1;
	}
	
	@Override
	public boolean isPossible(BoardCustomNonWeblab board) {
		return isPushPossible(board, board.playerX, board.playerY, dir);
	}
	
	/**
	 * Is it possible to push the box from [playerX, playerY] in 'pushDirection' ?
	 * @param board
	 * @param playerX
	 * @param playerY
	 * @param pushDirection
	 * @return
	 */
	public static boolean isPushPossible(BoardCustomNonWeblab board, int playerX, int playerY, EDirection pushDirection) {
		// PLAYER ON THE EDGE
		if (!TActionNonWeblab.isOnBoard(board, playerX, playerY, pushDirection)) return false;
		
		// TILE TO THE DIR IS NOT BOX
		if (!TTileNonWeblab.isBox(board.tile(playerX+pushDirection.dX, playerY+pushDirection.dY))) return false;
		
		// BOX IS ON THE EDGE IN THE GIVEN DIR
		if (!TActionNonWeblab.isOnBoard(board, playerX+pushDirection.dX, playerY+pushDirection.dY, pushDirection)) return false;
		
		// TILE TO THE DIR OF THE BOX IS NOT FREE
		if (!TTileNonWeblab.isPlayer(board.tile(playerX+pushDirection.dX+pushDirection.dX, playerY+pushDirection.dY+pushDirection.dY))
         && !TTileNonWeblab.isFree(board.tile(playerX+pushDirection.dX+pushDirection.dX, playerY+pushDirection.dY+pushDirection.dY))) return false;
				
		// YEP, WE CAN PUSH
		return true;
	}
	
	/**
	 * Is it possible to push the box from [playerX, playerY] in 'pushDirection' ? 
	 * 
	 * This deem the box pushable even if there is a player in that direction.
	 * 
	 * @param board
	 * @param playerX
	 * @param playerY
	 * @param pushDirection
	 * @return
	 */
	public static boolean isPushPossibleIgnorePlayer(BoardCustomNonWeblab board, int playerX, int playerY, EDirection pushDirection) {
		// PLAYER ON THE EDGE
		if (!TActionNonWeblab.isOnBoard(board, playerX, playerY, pushDirection)) return false;
		
		// TILE TO THE DIR IS NOT BOX
		if (!TTileNonWeblab.isBox(board.tile(playerX+pushDirection.dX, playerY+pushDirection.dY))) return false;
		
		// BOX IS ON THE EDGE IN THE GIVEN DIR
		if (!TActionNonWeblab.isOnBoard(board, playerX+pushDirection.dX, playerY+pushDirection.dY, pushDirection)) return false;
		
		// TILE TO THE DIR OF THE BOX IS NOT FREE
		if (!TTileNonWeblab.isWalkable(board.tile(playerX+pushDirection.dX+pushDirection.dX, playerY+pushDirection.dY+pushDirection.dY))) return false;
				
		// YEP, WE CAN PUSH
		return true;
	}
	
	/**
	 * PERFORM THE PUSH, no validation, call {@link #isPossible(BoardCustomNonWeblab, EDirection)} first!
	 * @param board
	 * @param dir
	 */
	@Override
	public void perform(BoardCustomNonWeblab board) {
		// MOVE THE BOX
		board.moveBox(board.playerX + dir.dX, board.playerY + dir.dY, board.playerX + dir.dX + dir.dX, board.playerY + dir.dY + dir.dY);
		// MOVE THE PLAYER
		board.movePlayer(board.playerX, board.playerY, board.playerX + dir.dX, board.playerY + dir.dY);
	}
	
	/**
	 * REVERSE THE ACTION PREVIOUSLY DONE BY {@link #perform(BoardCustomNonWeblab, EDirection)}, no validation.
	 * @param board
	 * @param dir
	 */
	@Override
	public void reverse(BoardCustomNonWeblab board) {
		// MARK PLAYER POSITION
		int playerX = board.playerX;
		int playerY = board.playerY;
		// MOVE THE PLAYER
		board.movePlayer(board.playerX, board.playerY, board.playerX - dir.dX, board.playerY - dir.dY);
		// MOVE THE BOX
		board.moveBox(playerX + dir.dX, playerY + dir.dY, playerX, playerY);
	}
	
	@Override
	public String toString() {
		return "TPushNonWeblab[" + dir.toString() + "]";
	}

}

class TMoveNonWeblab extends TActionNonWeblab {
	
	private static Map<EDirection, TMoveNonWeblab> actions = new HashMap<EDirection, TMoveNonWeblab>();
	
	static {
		actions.put(EDirection.DOWN, new TMoveNonWeblab(EDirection.DOWN));
		actions.put(EDirection.UP, new TMoveNonWeblab(EDirection.UP));
		actions.put(EDirection.LEFT, new TMoveNonWeblab(EDirection.LEFT));
		actions.put(EDirection.RIGHT, new TMoveNonWeblab(EDirection.RIGHT));
	}
	
	public static Collection<TMoveNonWeblab> getActions() {
		return actions.values();
	}
	
	public static TMoveNonWeblab getAction(EDirection direction) {
		return actions.get(direction);
	}
	
	private EDirection dir;
	
	private EDirection[] dirs;
	
	public TMoveNonWeblab(EDirection dir) {
		this.dir = dir;
		this.dirs = new EDirection[]{ dir };
	}
	
	@Override
	public EActionType getType() {
		return EActionType.MOVE;
	}

	@Override
	public EDirection getDirection() {
		return dir;
	}
	
	@Override
	public EDirection[] getDirections() {
		return dirs;
	}
	
	@Override
	public int getSteps() {
		return 1;
	}
	
	@Override
	public boolean isPossible(BoardCustomNonWeblab board) {
		// PLAYER ON THE EDGE
		if (!isOnBoard(board, board.playerX, board.playerY, dir)) return false;
		
		// TILE TO THE DIR IS FREE
		if (TTileNonWeblab.isFree(board.tile(board.playerX+dir.dX, board.playerY+dir.dY))) return true;
				
		// TILE WE WISH TO MOVE TO IS NOT FREE
		return false;
	}

    // Custom signature that can work with move from a position that is passed as an argument
	public int isPossible(BoardCustomNonWeblab board, byte x, byte y) {
		// PLAYER ON THE EDGE
		if (!isOnBoard(board, x, y, dir)) return -1;
		
		// TILE TO THE DIR IS FREE
		if (TTileNonWeblab.isFree(board.tile(x+dir.dX, y+dir.dY))) return board.getPosition(x+dir.dX, y+dir.dY);
				
		// TILE WE WISH TO MOVE TO IS NOT FREE
		return -1;
	}
		
	/**
	 * PERFORM THE MOVE, no validation, call {@link #isPossible(BoardCustomNonWeblab, EDirection)} first!
	 * @param board
	 * @param dir
	 */
	@Override
	public void perform(BoardCustomNonWeblab board) {
		// MOVE THE PLAYER
		board.movePlayer(board.playerX, board.playerY, (byte)(board.playerX + dir.dX), (byte)(board.playerY + dir.dY));
	}
	
	/**
	 * REVERSE THE MOVE PRVIOUSLY DONE BY {@link #perform(BoardCustomNonWeblab, EDirection)}, no validation.
	 * @param board
	 * @param dir
	 */
	@Override
	public void reverse(BoardCustomNonWeblab board) {
		// REVERSE THE PLAYER
		board.movePlayer(board.playerX, board.playerY, (byte)(board.playerX - dir.dX), (byte)(board.playerY - dir.dY));
	}
	
	@Override
	public String toString() {
		return "TMoveNonWeblab[" + dir.toString() + "]";
	}

}

class TPushSequenceNonWeblab extends TActionNonWeblab {
	
	private EDirection dir;
	
	private EDirection[] dirs;
	
	public TPushSequenceNonWeblab(EDirection dir, int seqLength) {
		this.dir = dir;
        this.dirs = new EDirection[seqLength];

        for (int i = 0; i < seqLength; i++) {
            // Single push sequence is always in the same direction
            dirs[i] = dir;
        }
	}
	
	@Override
	public EActionType getType() {
		return EActionType.PUSH;
	}

	@Override
	public EDirection getDirection() {
		return this.dir;
	}
	
	@Override
	public EDirection[] getDirections() {
		return dirs;
	}
	
	@Override
	public int getSteps() {
		return this.dirs.length;
	}
	
	@Override
	public boolean isPossible(BoardCustomNonWeblab board) {
        boolean isInitialPushPossible = isInitialPushPossible(board, board.playerX, board.playerY, dir);
        if (!isInitialPushPossible) return false;

        int x = board.playerX + dir.dX;
        int y = board.playerY + dir.dY;
        for (int i = 1; i < this.dirs.length; i++) {
            if (!TTileNonWeblab.isWalkable(board.tile(x, y))) return false;
            x += this.dirs[i].dX;
            y += this.dirs[i].dY;
        }
		return true;
	}
	
	/**
	 * Is it possible to push the box from [playerX, playerY] in 'pushDirection' ?
	 * @param board
	 * @param playerX
	 * @param playerY
	 * @param pushDirection
	 * @return
	 */
	public static boolean isInitialPushPossible(BoardCustomNonWeblab board, int playerX, int playerY, EDirection pushDirection) {
		// PLAYER ON THE EDGE
		if (!TActionNonWeblab.isOnBoard(board, playerX, playerY, pushDirection)) return false;
		
		// TILE TO THE DIR IS NOT BOX
		if (!TTileNonWeblab.isBox(board.tile(playerX+pushDirection.dX, playerY+pushDirection.dY))) return false;
		
		// BOX IS ON THE EDGE IN THE GIVEN DIR
		if (!TActionNonWeblab.isOnBoard(board, playerX+pushDirection.dX, playerY+pushDirection.dY, pushDirection)) return false;
		
		// TILE TO THE DIR OF THE BOX IS NOT FREE
		if (!TTileNonWeblab.isFree(board.tile(playerX+pushDirection.dX+pushDirection.dX, playerY+pushDirection.dY+pushDirection.dY))) return false;
				
		// YEP, WE CAN PUSH
		return true;
	}
	
	/**
	 * Is it possible to push the box from [playerX, playerY] in 'pushDirection' ? 
	 * 
	 * This deem the box pushable even if there is a player in that direction.
	 * 
	 * @param board
	 * @param playerX
	 * @param playerY
	 * @param pushDirection
	 * @return
	 */
	public static boolean isInitialPushPossibleIgnorePlayer(BoardCustomNonWeblab board, int playerX, int playerY, EDirection pushDirection) {
		// PLAYER ON THE EDGE
		if (!TActionNonWeblab.isOnBoard(board, playerX, playerY, pushDirection)) return false;
		
		// TILE TO THE DIR IS NOT BOX
		if (!TTileNonWeblab.isBox(board.tile(playerX+pushDirection.dX, playerY+pushDirection.dY))) return false;
		
		// BOX IS ON THE EDGE IN THE GIVEN DIR
		if (!TActionNonWeblab.isOnBoard(board, playerX+pushDirection.dX, playerY+pushDirection.dY, pushDirection)) return false;
		
		// TILE TO THE DIR OF THE BOX IS NOT FREE
		if (!TTileNonWeblab.isWalkable(board.tile(playerX+pushDirection.dX+pushDirection.dX, playerY+pushDirection.dY+pushDirection.dY))) return false;
				
		// YEP, WE CAN PUSH
		return true;
	}
	
	/**
	 * PERFORM THE PUSH, no validation, call {@link #isPossible(BoardCustomNonWeblab, EDirection)} first!
	 * @param board
	 * @param dir
	 */
	@Override
	public void perform(BoardCustomNonWeblab board) {
        for (EDirection d : this.dirs) {
            // MOVE THE BOX
            board.moveBox(board.playerX + d.dX, board.playerY + d.dY, board.playerX + d.dX + d.dX, board.playerY + d.dY + d.dY);
            // MOVE THE PLAYER
            board.movePlayer(board.playerX, board.playerY, board.playerX + d.dX, board.playerY + d.dY);
        }
	}
	
	/**
	 * REVERSE THE ACTION PREVIOUSLY DONE BY {@link #perform(BoardCustomNonWeblab, EDirection)}, no validation.
	 * @param board
	 * @param dir
	 */
	@Override
	public void reverse(BoardCustomNonWeblab board) {
        for (EDirection d : this.dirs) {
            // MARK PLAYER POSITION
            int playerX = board.playerX;
            int playerY = board.playerY;
            // MOVE THE PLAYER
            board.movePlayer(board.playerX, board.playerY, board.playerX - d.dX, board.playerY - d.dY);
            // MOVE THE BOX
            board.moveBox(playerX + d.dX, playerY + d.dY, playerX, playerY);
        }
	}
	
	@Override
	public String toString() {
		return "TPushSequenceNonWeblab[" + dir.toString() + "]";
	}

}

/**
 * An implemetation of the Kuhn–Munkres assignment algorithm of the year 1957.
 * https://en.wikipedia.org/wiki/Hungarian_algorithm
 *
 *
 * @author https://github.com/aalmi | march 2014
 * @version 1.0
 */
class HungarianAlgorithmNonWeblab {

    int[][] initCostMatrix;
    int[][] matrix; // initial matrix (cost matrix)

    // markers in the matrix
    int[] squareInRow, squareInCol, rowIsCovered, colIsCovered, staredZeroesInRow;

    public HungarianAlgorithmNonWeblab(int[][] matrix) {
        if (matrix.length != matrix[0].length) {
            try {
                throw new IllegalAccessException("The matrix is not square!");
            } catch (IllegalAccessException ex) {
                System.err.println(ex);
                System.exit(1);
            }
        }
        this.initCostMatrix = new int[matrix.length][matrix.length];
        for (int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < matrix.length; y++) {
                initCostMatrix[x][y] = matrix[x][y];
            }
        }

        this.matrix = matrix;
        squareInRow = new int[matrix.length];       // squareInRow & squareInCol indicate the position
        squareInCol = new int[matrix[0].length];    // of the marked zeroes

        rowIsCovered = new int[matrix.length];      // indicates whether a row is covered
        colIsCovered = new int[matrix[0].length];   // indicates whether a column is covered
        staredZeroesInRow = new int[matrix.length]; // storage for the 0*
        Arrays.fill(staredZeroesInRow, -1);
        Arrays.fill(squareInRow, -1);
        Arrays.fill(squareInCol, -1);
    }

    /**
     * find an optimal assignment
     *
     * @return optimal assignment
     */
    public int findOptimalAssignment() {
        step1();    // reduce matrix
        step2();    // mark independent zeroes
        step3();    // cover columns which contain a marked zero

        while (!allColumnsAreCovered()) {
            int[] mainZero = step4();
            while (mainZero == null) {      // while no zero found in step4
                step7();
                mainZero = step4();
            }
            if (squareInRow[mainZero[0]] == -1) {
                // there is no square mark in the mainZero line
                step6(mainZero);
                step3();    // cover columns which contain a marked zero
            } else {
                // there is square mark in the mainZero line
                // step 5
                rowIsCovered[mainZero[0]] = 1;  // cover row of mainZero
                colIsCovered[squareInRow[mainZero[0]]] = 0;  // uncover column of mainZero
                step7();
            }
        }


        int optimalAssignment = 0;
        for (int i = 0; i < squareInCol.length; i++) {
            optimalAssignment += initCostMatrix[i][squareInRow[i]];
        }
        return optimalAssignment;
    }

    /**
     * Check if all columns are covered. If that's the case then the
     * optimal solution is found
     *
     * @return true or false
     */
    private boolean allColumnsAreCovered() {
        for (int i : colIsCovered) {
            if (i == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Step 1:
     * Reduce the matrix so that in each row and column at least one zero exists:
     * 1. subtract each row minima from each element of the row
     * 2. subtract each column minima from each element of the column
     */
    private void step1() {
        // rows
        for (int i = 0; i < matrix.length; i++) {
            // find the min value of the current row
            int currentRowMin = Integer.MAX_VALUE;
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] < currentRowMin) {
                    currentRowMin = matrix[i][j];
                }
            }
            // subtract min value from each element of the current row
            for (int k = 0; k < matrix[i].length; k++) {
                matrix[i][k] -= currentRowMin;
            }
        }

        // cols
        for (int i = 0; i < matrix[0].length; i++) {
            // find the min value of the current column
            int currentColMin = Integer.MAX_VALUE;
            for (int j = 0; j < matrix.length; j++) {
                if (matrix[j][i] < currentColMin) {
                    currentColMin = matrix[j][i];
                }
            }
            // subtract min value from each element of the current column
            for (int k = 0; k < matrix.length; k++) {
                matrix[k][i] -= currentColMin;
            }
        }
    }

    /**
     * Step 2:
     * mark each 0 with a "square", if there are no other marked zeroes in the same row or column
     */
    private void step2() {
        int[] rowHasSquare = new int[matrix.length];
        int[] colHasSquare = new int[matrix[0].length];

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                // mark if current value == 0 & there are no other marked zeroes in the same row or column
                if (matrix[i][j] == 0 && rowHasSquare[i] == 0 && colHasSquare[j] == 0) {
                    rowHasSquare[i] = 1;
                    colHasSquare[j] = 1;
                    squareInRow[i] = j; // save the row-position of the zero
                    squareInCol[j] = i; // save the column-position of the zero
                    continue; // jump to next row
                }
            }
        }
    }

    /**
     * Step 3:
     * Cover all columns which are marked with a "square"
     */
    private void step3() {
        for (int i = 0; i < squareInCol.length; i++) {
            colIsCovered[i] = squareInCol[i] != -1 ? 1 : 0;
        }
    }

    /**
     * Step 7:
     * 1. Find the smallest uncovered value in the matrix.
     * 2. Subtract it from all uncovered values
     * 3. Add it to all twice-covered values
     */
    private void step7() {
        // Find the smallest uncovered value in the matrix
        int minUncoveredValue = Integer.MAX_VALUE;
        for (int i = 0; i < matrix.length; i++) {
            if (rowIsCovered[i] == 1) {
                continue;
            }
            for (int j = 0; j < matrix[0].length; j++) {
                if (colIsCovered[j] == 0 && matrix[i][j] < minUncoveredValue) {
                    minUncoveredValue = matrix[i][j];
                }
            }
        }

        if (minUncoveredValue > 0) {
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[0].length; j++) {
                    if (rowIsCovered[i] == 1 && colIsCovered[j] == 1) {
                        // Add min to all twice-covered values
                        matrix[i][j] += minUncoveredValue;
                    } else if (rowIsCovered[i] == 0 && colIsCovered[j] == 0) {
                        // Subtract min from all uncovered values
                        matrix[i][j] -= minUncoveredValue;
                    }
                }
            }
        }
    }

    /**
     * Step 4:
     * Find zero value Z_0 and mark it as "0*".
     *
     * @return position of Z_0 in the matrix
     */
    private int[] step4() {
        for (int i = 0; i < matrix.length; i++) {
            if (rowIsCovered[i] == 0) {
                for (int j = 0; j < matrix[i].length; j++) {
                    if (matrix[i][j] == 0 && colIsCovered[j] == 0) {
                        staredZeroesInRow[i] = j; // mark as 0*
                        return new int[]{i, j};
                    }
                }
            }
        }
        return null;
    }

    /**
     * Step 6:
     * Create a chain K of alternating "squares" and "0*"
     *
     * @param mainZero => Z_0 of Step 4
     */
    private void step6(int[] mainZero) {
        int i = mainZero[0];
        int j = mainZero[1];

        Set<int[]> K = new LinkedHashSet<>();
        //(a)
        // add Z_0 to K
        K.add(mainZero);
        boolean found = false;
        do {
            // (b)
            // add Z_1 to K if
            // there is a zero Z_1 which is marked with a "square " in the column of Z_0
            if (squareInCol[j] != -1) {
                K.add(new int[]{squareInCol[j], j});
                found = true;
            } else {
                found = false;
            }

            // if no zero element Z_1 marked with "square" exists in the column of Z_0, then cancel the loop
            if (!found) {
                break;
            }

            // (c)
            // replace Z_0 with the 0* in the row of Z_1
            i = squareInCol[j];
            j = staredZeroesInRow[i];
            // add the new Z_0 to K
            if (j != -1) {
                K.add(new int[]{i, j});
                found = true;
            } else {
                found = false;
            }

        } while (found); // (d) as long as no new "square" marks are found

        // (e)
        for (int[] zero : K) {
            // remove all "square" marks in K
            if (squareInCol[zero[1]] == zero[0]) {
                squareInCol[zero[1]] = -1;
                squareInRow[zero[0]] = -1;
            }
            // replace the 0* marks in K with "square" marks
            if (staredZeroesInRow[zero[0]] == zero[1]) {
                squareInRow[zero[0]] = zero[1];
                squareInCol[zero[1]] = zero[0];
            }
        }

        // (f)
        // remove all marks
        Arrays.fill(staredZeroesInRow, -1);
        Arrays.fill(rowIsCovered, 0);
        Arrays.fill(colIsCovered, 0);
    }

    public static void main(String[] args) {
        int[][] costMatrix = {
            {4, 1, 5, 2},
            {2, 3, 6, 4},
            {3, 4, 1, 4},
            {5, 5, 5, 5}
        };
        
        int result = new HungarianAlgorithmNonWeblab(costMatrix).findOptimalAssignment();
        System.out.println("Minimum Cost Matching: " + result);
    }
}

// Used for bipartite deadlock detection
class BipartiteMatcherNonWeblab {
    private final List<List<Integer>> edges;
    private final Map<Integer, Integer> pairV = new HashMap<>();
    private final int[] pairU;
    private int[] dist;

    public BipartiteMatcherNonWeblab(int n, List<Integer> V, List<List<Integer>> edges) {
        this.edges = edges;
        this.pairU = new int[n]; // Initialize pairings for U vertices
        Arrays.fill(pairU, -1); // Initialize all U vertices as unmatched

        V.forEach(v -> pairV.put(v, -1)); // Initialize pairings for V vertices as unmatched
    }

    private boolean bfs() {
        Queue<Integer> queue = new LinkedList<>();
        dist = new int[pairU.length];
        Arrays.fill(dist, Integer.MAX_VALUE); // Initialize distances as infinite

        for (int u = 0; u < pairU.length; u++) {
            if (pairU[u] == -1) {
                dist[u] = 0;
                queue.add(u);
            }
        }

        boolean isPath = false;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : edges.get(u)) {
                if (!pairV.containsKey(v) || pairV.get(v) == -1) {
                    isPath = true;
                } else if (dist[pairV.get(v)] == Integer.MAX_VALUE) {
                    dist[pairV.get(v)] = dist[u] + 1;
                    queue.add(pairV.get(v));
                }
            }
        }
        return isPath;
    }

    private boolean dfs(int u) {
        for (int v : edges.get(u)) {
            if (!pairV.containsKey(v) || pairV.get(v) == -1 || (dist[pairV.get(v)] == dist[u] + 1 && dfs(pairV.get(v)))) {
                pairV.put(v, u);
                pairU[u] = v;
                return true;
            }
        }
        return false;
    }

    public int hopcroftKarp() {
        int matching = 0;
        while (bfs()) {
            for (int u = 0; u < pairU.length; u++) {
                if (pairU[u] == -1 && dfs(u)) {
                    matching++;
                }
            }
        }
        return matching;
    }

    public static void main(String[] args) {
        // Test Case 1: Perfect Matching Exists
        List<Integer> V1 = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> edges1 = Arrays.asList(
            Arrays.asList(1, 2),    
            Arrays.asList(2, 5),
            Arrays.asList(3),
            Arrays.asList(5),
            Arrays.asList(3, 4)
        );
    
        BipartiteMatcherNonWeblab bm1 = new BipartiteMatcherNonWeblab(edges1.size(), V1, edges1);
        int matchingSize1 = bm1.hopcroftKarp();
        System.out.println("Test Case 1 - Maximum Matching Size: " + matchingSize1);
    
        if (edges1.size() == V1.size() && matchingSize1 == edges1.size()) {
            System.out.println("Test Case 1: Perfect matching exists.");
        } else {
            System.out.println("Test Case 1: Perfect matching does not exist.");
        }

        List<List<Integer>> edges2 = Arrays.asList(
            Arrays.asList(1, 2),    
            Arrays.asList(2, 5),
            Arrays.asList(3),
            Arrays.asList(4),
            Arrays.asList(3, 4)
        );

        BipartiteMatcherNonWeblab bm2 = new BipartiteMatcherNonWeblab(edges2.size(), V1, edges2);
        int matchingSize2 = bm2.hopcroftKarp();
        System.out.println("Test Case 2 - Maximum Matching Size: " + matchingSize2);
    
        if (edges2.size() == V1.size() && matchingSize2 == edges2.size()) {
            System.out.println("Test Case 2: Perfect matching exists.");
        } else {
            System.out.println("Test Case 2: Perfect matching does not exist.");
        }
    }
}

class DeadSquareDetectorNonWeblab {
    public static boolean[][] isSimpleDeadlock;
    // Mask for each target to compute bipartite deadlocks
    private static boolean[][][] isSimpleDeadlockByTarget;
    private static List<Integer> targetX;
    private static List<Integer> targetY;

    private static HashMap<Integer, Boolean> visitedFreezeDeadlocks;
    public static int frozenDeadlockCount;
    public static int frozenDeadlockCallsCount;
    public static long frozenDeadlockSearchTime;

    public static int bipartiteDeadlockCount;
    public static int bipartiteDeadlockCallsCount;
    public static long bipartiteDeadlockSearchTime;

    // 1. Delete all boxes from the board
    // 2. Place a box at the goal square
    // 3. PULL the box from the goal square to every possible square and mark all reached squares as visited 
    
    public static boolean[][] detect(BoardCompact board) {
//        if (isSimpleDeadlock == null) {
          precomputeSimpleDeadlocks(board);

            // DEBUG
            // for (int x = 0; x < board.width(); x++) {
            //     System.out.println();
            //     for (int y = 0; y < board.height(); y++) {
            //         if (CTile.isWall(board.tile(x, y))) {
            //             System.out.print("#");
            //         }else if (isSimpleDeadlock[x][y]){
            //             System.out.print("X");
            //         }else{
            //             System.out.print(" ");
            //         }
            //     }
            // }
//        }

        return isSimpleDeadlock;
    }

    private static void precomputeSimpleDeadlocks(BoardCompact board) {
        // For analysis
        // frozenDeadlockCount = 0;
        // frozenDeadlockCallsCount = 0;
        // frozenDeadlockSearchTime = 0;

        // bipartiteDeadlockCount = 0;
        // bipartiteDeadlockCallsCount = 0;
        // bipartiteDeadlockSearchTime = 0;

        // Get targets
        isSimpleDeadlock = new boolean[board.width()][board.height()];
        targetX = new ArrayList<>();
        targetY = new ArrayList<>();
        boolean[][] isWall = new boolean[board.width()][board.height()];

        for (int x = 0; x < board.width(); ++x) {
			for (int y = 0; y < board.height(); ++y) {
                // Initialize the deadlock 2D array to true
                isSimpleDeadlock[x][y] = true;

                int tile = board.tiles[x][y];
                if (CTile.forSomeBox(tile)) {
                    targetX.add(x);
                    targetY.add(y);
                }
                isWall[x][y] = CTile.isWall(tile);
			}			
		}

        isSimpleDeadlockByTarget = new boolean[board.width()][board.height()][targetX.size()];
        for (int x = 0; x < board.width(); ++x) {
			for (int y = 0; y < board.height(); ++y) {
                for (int i = 0; i < targetX.size(); i++) {
                    isSimpleDeadlockByTarget[x][y][i] = true;
                }
            }
        }

        // For each target, mark reachable tiles as not deadlocks
        for (int i = 0; i < targetX.size(); i++) {
            Queue<Integer> remainingX = new ArrayDeque<>();
            Queue<Integer> remainingY = new ArrayDeque<>();
            HashSet<Integer> visited = new HashSet<>();

            remainingX.add(targetX.get(i));
            remainingY.add(targetY.get(i));
            visited.add(targetX.get(i) + targetY.get(i) * board.width());

            int x, y;
            while (!remainingX.isEmpty()) {
                x = remainingX.poll();
                y = remainingY.poll();
                isSimpleDeadlock[x][y] = false;
                isSimpleDeadlockByTarget[x][y][i] = false;

                // Process possible pulls
                for (EDirection dir : EDirection.arrows()) {
                    if (!isPullPossible(x, y, dir, isWall)  || visited.contains((x+dir.dX) + (y+dir.dY) * board.width())
                    ) continue;

                    int neighbourX = x + dir.dX;
                    int neighbourY = y + dir.dY;

                    remainingX.add(neighbourX);
                    remainingY.add(neighbourY);
                }

                visited.add(x + y * board.width());
            }
        }
    }

    private static boolean isPullPossible(int x, int y, EDirection dir, boolean[][] isWall) {
        return !isWall[x+dir.dX][y+dir.dY] && !isWall[x+dir.dX+dir.dX][y+dir.dY+dir.dY];
    }

    // Checks only whether a target becomes unreachable for all boxes
    public static boolean isBipartiteDeadlockSimple(EDirection pushDirection, int x, int y, BoardCustomNonWeblab b) {
        // Check if there exists a target that no box can reach
        // Box position before push
        int startX = x + pushDirection.dX;
        int startY = y + pushDirection.dY;

        // After push, the target position is where the box will be
        int endX = x + pushDirection.dX + pushDirection.dX;
        int endY = y + pushDirection.dY + pushDirection.dY;

        // First get all targets that can be reached by the current box before push, but won't be reachable after push
        List<Integer> reachableTargetIndices = new ArrayList<>();
        for (int t = 0; t < targetX.size(); t++) {
            if (isSimpleDeadlockByTarget[startX][startY][t]) continue;
            reachableTargetIndices.add(t);
        }

        // For these targets, check if there's still some other box that can reach them
        for (Integer reachableTarget : reachableTargetIndices) {
            // Check new place of the moved box
            if (!isSimpleDeadlockByTarget[endX][endY][reachableTarget]) continue;
            boolean isUnreachable = true;
            for (Integer boxPosition : b.getBoxes()) {
                int bx = b.getXFromPosition(boxPosition);
                int by = b.getYFromPosition(boxPosition);
                // The moved box won't be at its current place anymore
                if (bx == startX && by == startY) continue;
                // At least one box can reach this target
                if (!isSimpleDeadlockByTarget[bx][by][reachableTarget]) {
                    isUnreachable = false;
                    break;
                }
            }
            if (isUnreachable) return true;
        }

        return false;
    }

    // Checks not only that every target has a box that can reach it, but also that there are enough boxes to distribute to the targets
    public static boolean isBipartiteDeadlock(EDirection pushDirection, int x, int y, BoardCustomNonWeblab b) {
        // bipartiteDeadlockCallsCount++;
        // long searchStartMillis = System.currentTimeMillis();

        // Check if there exists a target that no box can reach
        // Box position before push
        int startX = x + pushDirection.dX;
        int startY = y + pushDirection.dY;

        // After push, the target position is where the box will be
        int endX = x + pushDirection.dX + pushDirection.dX;
        int endY = y + pushDirection.dY + pushDirection.dY;

        // For these targets, check if there's still some other box that can reach them
        List<List<Integer>> reachableBoxesByTarget = new ArrayList<>();
        for (int target = 0; target < targetX.size(); target++) {
            List<Integer> reachableBoxes = new ArrayList<>();
            // Check new place of the moved box
            if (!isSimpleDeadlockByTarget[endX][endY][target]) {
                reachableBoxes.add(b.getPosition(endX, endY));
            }
            boolean isUnreachable = true;
            for (Integer boxPosition : b.getBoxes()) {
                int bx = b.getXFromPosition(boxPosition);
                int by = b.getYFromPosition(boxPosition);
                // The moved box won't be at its current place anymore
                if (bx == startX && by == startY) continue;
                // At least one box can reach this target
                if (!isSimpleDeadlockByTarget[bx][by][target]) {
                    isUnreachable = false;
                    reachableBoxes.add(boxPosition);
                }
            }
            reachableBoxesByTarget.add(reachableBoxes);
            // if (isUnreachable) bipartiteDeadlockCount++;
            // bipartiteDeadlockSearchTime += System.currentTimeMillis() - searchStartMillis;
            if (isUnreachable) return true;
        }

        // ??? Use Ford-Fulkerson max flow algorithm to decide whether there is a matching
        // Targets are connected to source
        // Edges indicate that a target can be reached by a box
        // Boxes are connected to sink
        // Flow at the sink must be same as number of targets
        BipartiteMatcherNonWeblab bpmatcher = new BipartiteMatcherNonWeblab(targetX.size(), b.getBoxes(), reachableBoxesByTarget);
        int matchingSize = bpmatcher.hopcroftKarp();

        // if (matchingSize != targetX.size()) {
        //     System.out.println(x + ", " + y + ", " + pushDirection.toString());
        //     b.debugPrint();
        // }
        // if (matchingSize != targetX.size()) bipartiteDeadlockCount++;
        // bipartiteDeadlockSearchTime += System.currentTimeMillis() - searchStartMillis;
        return matchingSize != targetX.size();
    }

    public static boolean isFreezeDeadlock(EDirection pushDirection, int x, int y, BoardCustomNonWeblab b) {
        // frozenDeadlockCallsCount++;
        // long searchStartMillis = System.currentTimeMillis();

        visitedFreezeDeadlocks = new HashMap<>();
        // For both axes check:
        // 1. If there is a wall on the left or on the right side of the box then the box is blocked along this axis
        // 2. If there is a simple deadlock square on both sides (left and right) of the box the box is blocked along this axis
        // 3. If there is a box one the left or right side then this box is blocked if the other box is blocked. 

        // Box position before push
        int startX = x + pushDirection.dX;
        int startY = y + pushDirection.dY;

        // After push, the target position is where the box will be
        int endX = x + pushDirection.dX + pushDirection.dX;
        int endY = y + pushDirection.dY + pushDirection.dY;

        // If the position would be pushed to on a target tile, return true only if some neighbours not on target tiles become frozen
        byte tile = b.tile(endX, endY);
        if (TTileNonWeblab.forBox(tile)) {
            boolean frozenNonTargetNeighbour = false;
            byte tileRight = b.tile(endX+1, endY);
            byte tileLeft = b.tile(endX-1, endY);
            byte tileUp = b.tile(endX, endY-1);
            byte tileDown = b.tile(endX, endY+1);

            frozenNonTargetNeighbour |= !TTileNonWeblab.forBox(tileRight) && TTileNonWeblab.isBox(tileRight) && isFreezeDeadlock(startX, startY, endX, endY, endX+1, endY, b); 
            frozenNonTargetNeighbour |= !TTileNonWeblab.forBox(tileLeft) && TTileNonWeblab.isBox(tileLeft) && isFreezeDeadlock(startX, startY, endX, endY, endX-1, endY, b); 
            frozenNonTargetNeighbour |= !TTileNonWeblab.forBox(tileUp) && TTileNonWeblab.isBox(tileUp) && isFreezeDeadlock(startX, startY, endX, endY, endX, endY-1, b); 
            frozenNonTargetNeighbour |= !TTileNonWeblab.forBox(tileDown) && TTileNonWeblab.isBox(tileDown) && isFreezeDeadlock(startX, startY, endX, endY, endX, endY+1, b); 

            // if (frozenNonTargetNeighbour) frozenDeadlockCount++;
            // frozenDeadlockSearchTime += System.currentTimeMillis() - searchStartMillis;
            return frozenNonTargetNeighbour;
        }

        boolean result = isFreezeDeadlock(startX, startY, endX, endY, endX, endY, b);
        // if (result) frozenDeadlockCount++;
        // frozenDeadlockSearchTime += System.currentTimeMillis() - searchStartMillis;
        return result;
    }

    // Look if box at x and y is freeze deadlocked while taking into account that box from source is supposed to be moved to target
    private static boolean isFreezeDeadlock(int startX, int startY, int endX, int endY, int x, int y, BoardCustomNonWeblab b) {
        if (x <= 0 || x >= b.width() || y <= 0 || y >= b.height()) return true;

        int position = b.getPosition(x, y);
        if (visitedFreezeDeadlocks.containsKey(position)) return visitedFreezeDeadlocks.get(position);
        visitedFreezeDeadlocks.put(position, true);

        // Horizontal freeze
        boolean leftOutOfBound = x-1 < 0;
        boolean rightOutOfBound = x+1 >= b.width();

        boolean horizontalWallBlock = leftOutOfBound || rightOutOfBound || TTileNonWeblab.isWall(b.tile(x-1, y)) || TTileNonWeblab.isWall(b.tile(x+1, y));

        boolean horizontalSimpleDeadlockBlock = horizontalWallBlock
                                              || (leftOutOfBound || isSimpleDeadlock[x-1][y]) && (rightOutOfBound || isSimpleDeadlock[x+1][y]);

        boolean horizontalBoxBlock = horizontalSimpleDeadlockBlock 
                                   || (leftOutOfBound || (!(x-1 == startX && y == startY) && TTileNonWeblab.isBox(b.tile(x-1, y)) && isFreezeDeadlock(startX, startY, endX, endY, x-1, y, b)))
                                   || (rightOutOfBound || (!(x+1 == startX && y == startY) && TTileNonWeblab.isBox(b.tile(x+1, y)) && isFreezeDeadlock(startX, startY, endX, endY, x+1, y, b)));

        if (!horizontalBoxBlock) return false;

        // Vertical freeze
        boolean upOutOfBound = y-1 < 0;
        boolean downOutOfBound = y+1 >= b.height();

        boolean verticalWallBlock = upOutOfBound || downOutOfBound || TTileNonWeblab.isWall(b.tile(x, y+1)) || TTileNonWeblab.isWall(b.tile(x, y-1));
        if (verticalWallBlock) return true;

        boolean verticalSimpleDeadlockBlock = (upOutOfBound || isSimpleDeadlock[x][y-1]) && (downOutOfBound || isSimpleDeadlock[x][y+1]);
        if (verticalSimpleDeadlockBlock) return true;

        boolean verticalBoxBlock = (upOutOfBound || (!(x == startX && y-1 == startY) && TTileNonWeblab.isBox(b.tile(x, y-1)) && isFreezeDeadlock(startX, startY, endX, endY, x, y-1, b)))
                                || (downOutOfBound || (!(x == startX && y+1 == startY) && TTileNonWeblab.isBox(b.tile(x, y+1)) && isFreezeDeadlock(startX, startY, endX, endY, x, y+1, b)));
        if (verticalBoxBlock) return true;

        return false;
    }
}

class PICorralDetectorNonWeblab {
    
}

class SolutionNonWeblab<S, A> {
  public List<A> actions;  // series of actions from start state to goal state
  public S goalState;      // goal state that was reached
  public double pathCost;  // total cost from start state to goal
  public int searchedNodes;

  public SolutionNonWeblab(List<A> actions, S goalState, double pathCost, int searchedNodes) {
    this.actions = actions; this.goalState = goalState; this.pathCost = pathCost; this.searchedNodes = searchedNodes;
  }

  // Return true if this is a valid solution to the given problem.
  public boolean isValid(ProblemNonWeblab<S, A> prob) {
    S state = prob.initialState();
    double cost = 0.0;

    // Check that the actions actually lead from the problem's initial state to the goal.
    for (A action : actions) {
      cost += prob.cost(state, action);
      state = prob.result(state, action);
    }
    
    return state.equals(goalState) && prob.isGoal(goalState) && pathCost == cost;
  }

  // Describe a solution.
  public static <S, A> boolean report(SolutionNonWeblab<S, A> solution, ProblemNonWeblab<S, A> prob) {
    if (solution == null) {
      System.out.println("no solution found");
      return false;
    } else if (!solution.isValid(prob)) {
      System.out.println("solution is invalid!");
      return false;
    } else {
      System.out.println("solution is valid");
      System.out.format("total cost is %.1f\n", solution.pathCost);
      return true;
    }
  }
}

// S = state type, A = action type
interface ProblemNonWeblab<S, A> {
  S initialState();
  List<A> actions(S state);
  S result(S state, A action);
  boolean isGoal(S state);
  double cost(S state, A action);        
}

// S = state type, A = action type
interface HeuristicProblemNonWeblab<S, A> extends ProblemNonWeblab<S, A> {
    double estimate(S state);  // optimistic estimate of cost from state to goal
}