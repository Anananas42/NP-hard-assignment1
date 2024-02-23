package astar;

import java.util.*;

import astar.actions.TAction;
import astar.actions.TMove;
import astar.actions.TPush;
import astar.actions.TTile;
import astar.actions.TWalk;
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
    // THIS METHOD IS CALLED ONLY ONCE AT START
    public void populateDynamicStateDescriptors() {
        for (int y = 0; y < height(); ++y) {
			for (int x = 0; x < width(); ++x) {
                EEntity entity = EEntity.fromSlimFlag(tiles[x][y]);

                if (entity == null || !entity.isSomeBox()) continue;
                boxes.add(getPosition(x, y));
            }
        }
    }

    public List<TAction> getActions() {
        List<TAction> result = new ArrayList<>();

        result.addAll(getPushSequenceActions()); // Add pushes first to prioritize them
        result.addAll(getWalkActions());
        
        return result;
    }

    public List<TPush> getPushSequenceActions() {
        // For all directions, check if there's a box next to the agent and can be pushed
        List<TPush> possibleDirections = new ArrayList<>();
        for (TPush push : TPush.getActions()) { 
            boolean isPossible = TPush.isPushPossible(this, playerX, playerY, push.getDirection());
            if (!isPossible) continue;
            possibleDirections.add(push); 
        }

        // Compress tunnels or paths along a wall into a single TWalkPush action
        // Actually not worth it since the levels are fairly small
        // List<TPushSequence> result = new ArrayList<>();
        // for (EDirection dir : possibleDirections) {
        //     // TODO

        // }

        return possibleDirections;
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

    // Assumes unchanging position of targets and walls. Computed from box positions and accessible neighbouring tiles to boxes.
    // Optimized for A* to correctly return same hash for states that shouldn't be revisited in the A*
    // DOES NOT GUARANTEE OPTIMAL SOLUTION ANYMORE, but makes finding a solution much faster
	// public int hashCodeAstar() {
    //     int hash = 0;
    //     for (int i = 0; i < boxes.size(); i++) {
    //         hash += 290317 * boxes.get(i);
    //     }

	// 	for (byte x = 0; x < width(); ++x) {
    //         for (byte y = 0; y < height(); ++y) {
    //             hash += (290317 * x + 97 * y) * tiles[x][y];
    //         }
    //     }
	// 	return hash;
	// }
    // Wrong. This would prevent pushing another box.
    // Further improvement. Just return possible box pushes. TWalk does not need to be returned at all actually.
    // Also precompute if pushes are accessible to the player ONLY every time a box is moved.
    // When A* finishes, simply fill in the blanks. Compute the steps, including walking etc., but during search, operate only on these high level actions.
}
