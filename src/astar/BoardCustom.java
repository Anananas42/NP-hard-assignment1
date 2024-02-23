package astar;

import java.util.*;

import astar.actions.TAction;
import astar.actions.TMove;
import astar.actions.TPush;
import astar.actions.TTile;
import astar.actions.TWalk;
import astar.actions.TWalkPushSequence;
import game.actions.EDirection;
import game.board.oop.EEntity;
import game.board.oop.EPlace;
import game.board.oop.ESpace;
import game.board.slim.BoardSlim;



public class BoardCustom extends BoardSlim {
    // List with box positions calculated as x * width + y (List is efficient since number of boxes is small)
    List<Integer> boxes;

    // List of possible sequences of positions to get from current player's position next to accessible boxes
    List<List<Integer>> possibleWalks;

    public BoardCustom(byte width, byte height) {
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

    public List<TAction> getActions(boolean[][] isSimpleDeadlock) {
        List<TAction> result = new ArrayList<>();

        // Add all pushes from the player's current position
        result.addAll(getPushesWithoutDeadlock(playerX, playerY, isSimpleDeadlock));
        // Add all WalkPush actions
        result.addAll(getWalkPushActions(isSimpleDeadlock));
        
        return result;
    }

    public List<TWalkPushSequence> getWalkPushActions(boolean[][] isSimpleDeadlock) {
        List<TWalkPushSequence> result = new ArrayList<>();
        List<TWalk> walkActions = getWalkActions();
        for (TWalk walk : walkActions) {
            for (TPush push : getPushesWithoutDeadlock(walk.getDestinationX(), walk.getDestinationY(), isSimpleDeadlock)) {
                result.add(new TWalkPushSequence(walk, push));
            }
        }
        return result;
    }

    // Get a list of pushes from a position that do not result in a deadlock
    private List<TPush> getPushesWithoutDeadlock(int x, int y, boolean[][] isSimpleDeadlock) {
        List<TPush> result = new ArrayList<>();
        for (TPush push : TPush.getActions()) { 
            boolean isPossible = TPush.isPushPossible(this, x, y, push.getDirection());
            EDirection dir = push.getDirection();
            if (!isPossible || isSimpleDeadlock[x+dir.dX+dir.dX][y+dir.dY+dir.dY]) continue;
            result.add(push);
        }

        return result;
    }

    public List<TWalk> getWalkActions() {
        List<TWalk> result = new ArrayList<>();
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
            // Add a TWalk only if the final position enables pushing a box at least in one of the four directions
            boolean isPushPossible = false;
            for (TPush push : TPush.getActions()) { 
                isPushPossible = TPush.isPushPossible(this, positionsX[length-1], positionsY[length-1], push.getDirection());
                if (isPushPossible) break;
            }
            if (!isPushPossible) continue;
            result.add(TWalk.fromPositions(this, playerX, playerY, positionsX, positionsY));
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
        for (TMove m : TMove.getActions()) { // Get moves in all directions
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
        for (TMove m : TMove.getActions()) { // Get moves in all directions
            EDirection dir = m.getDirection();
            if (TMove.isOnBoard(this, (byte)x, (byte)y, m.getDirection()) && !TTile.isWall(tile(x+dir.dX, y+dir.dY))) {
                neighbours.add(getPosition(x+dir.dX, y+dir.dY));
            }
        }

        return neighbours;
    }


    @Override
	public BoardCustom clone() {
        BoardCustom result = new BoardCustom(width(), height());
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
