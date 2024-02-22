package astar;

import java.util.*;

import astar.actions.TAction;
import astar.actions.TMove;
import astar.actions.TPush;
import astar.actions.TPushSequence;
import astar.actions.TWalk;
import game.actions.EDirection;
import game.board.oop.EEntity;
import game.board.slim.BoardSlim;

@SuppressWarnings("unused")
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
                boxes.add(x * width() + y);
            }
        }
    }

    public List<TAction> getActions() {
        List<TAction> result = new ArrayList<>();

        result.addAll(getWalkActions());
        result.addAll(getPushSequenceActions());
        
        return result;
    }

    private List<TPushSequence> getPushSequenceActions() {
        // For all directions, check if there's a box next to the agent and can be pushed
        List<EDirection> possibleDirections = new ArrayList<>();
        for (TPush push : TPush.getActions()) { 
            boolean isPossible = TPush.isPushPossible(this, playerX, playerY, push.getDirection());
            if (!isPossible) continue;
            possibleDirections.add(push.getDirection()); 
        }

        // Compress tunnels or paths along a wall into a single TWalkPush action
        List<TPushSequence> result = new ArrayList<>();
        for (EDirection dir : possibleDirections) {
            // TODO

        }

        return result;
    }

    private List<TWalk> getWalkActions() {
        List<TWalk> result = new ArrayList<>();
        List<List<Integer>> possibleWalks = getPossibleWalks();
        for (List<Integer> positionSequence : possibleWalks) {
            result.add(TWalk.fromPositions(this, playerX, playerY, positionSequence));
        }

        return result;
    }

    // Agent always needs to go next to a box and start pushing in order to progress the game
    // This method returns all positions next to boxes that the agent can access
    private List<List<Integer>> getPossibleWalks() {
        if (possibleWalks == null) {

            // Recompute accessible positions
            possibleWalks = new ArrayList<>();
            Set<Integer> destinations = getBoxNeighbourPositions();

            // BFS
            HashMap<Integer, Integer> costMap = new HashMap<>(); // Useless now, but later can be used for heuristic (try closer boxes first)
            Queue<Integer> q = new ArrayDeque<Integer>(); // For uniform cost, we can just use queue. (BFS basically)
            HashMap<Integer, Integer> prevMap = new HashMap<>();

            int initPosition = playerX * width() + playerY;
            costMap.put(initPosition, 0);
            q.add(initPosition);

            int curr = -1;
            int cost;
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

                cost = costMap.get(curr);
                for (Integer neighbour : getWalkableNeighbours(curr)) {
                    if (costMap.containsKey(neighbour)) continue; // Already visited
                    q.add(neighbour);
                    costMap.put(neighbour, cost+1);
                }
            }
        }
        return possibleWalks;
    }

    // Get all walkable positions next to boxes
    private Set<Integer> getBoxNeighbourPositions() {
        Set<Integer> result = new HashSet<>();

        for (Integer boxPosition : boxes) {
            result.addAll(getWalkableNeighbours(boxPosition));
        }

        return result;
    }

    @Override
    public void moveBox(byte sourceTileX, byte sourceTileY, byte targetTileX, byte targetTileY) {
        // Update position of the moved box
        boxes.remove((int)sourceTileX * width() + sourceTileY);
        boxes.add((int)targetTileX * width() + targetTileY);
        
        // Possible walks need updating
        possibleWalks = null;

        super.moveBox(sourceTileX, sourceTileY, targetTileX, targetTileY);
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
		int entity = tiles[sourceTileX][sourceTileY] & EEntity.SOME_ENTITY_FLAG;
		
		tiles[targetTileX][targetTileY] &= EEntity.NULLIFY_ENTITY_FLAG;
		tiles[targetTileX][targetTileY] |= entity;
		
		tiles[sourceTileX][sourceTileY] &= EEntity.NULLIFY_ENTITY_FLAG;
		tiles[sourceTileX][sourceTileY] |= EEntity.NONE.getFlag();	
		
		playerX = targetTileX;
		playerY = targetTileY;

        this.nullHash();
	}

    public void movePlayer(int sourceTileX, int sourceTileY, int targetTileX, int targetTileY) {
        movePlayer((byte)sourceTileX, (byte)sourceTileY, (byte)targetTileX, (byte)targetTileY);
    }
    
    @Override
	public BoardCustom clone() {
        BoardCustom result = (BoardCustom)super.clone();

        if (this.boxes != null) {
            result.boxes = new ArrayList<>(this.boxes);
        }

        return result;
    }

    private List<Integer> getWalkableNeighbours(int position) {
        List<Integer> neighbours = new ArrayList<>();
        int y = position % width();
        int x = position / width();
        for (TMove m : TMove.getActions()) { // Get moves in all directions
            int neighbourPosition = m.isPossible(this, (byte)x, (byte)y); // Check if move in a direction possible
            if (neighbourPosition == -1) continue; // Not possible, ignore this direction
            neighbours.add(neighbourPosition); // Possible, add neighbour position
        }

        return neighbours;
    }
}
