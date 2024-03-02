package astar;

import java.util.*;

import astar.actions.TAction;
import astar.actions.TMove;
import astar.actions.TPush;
import astar.actions.TTile;
import astar.actions.TWalk;
import astar.actions.TWalkPushSequence;
import astar.detectors.DeadSquareDetector;
import astar.util.ZobristKeys;
import game.actions.EDirection;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;
import game.board.oop.EEntity;
import game.board.oop.EPlace;
import game.board.oop.ESpace;


public class BoardCustom {

    // Level data that doesn't change
    public static int width;
    public static int height;
    public static int boxCount;
    public static byte[][] tiles; // Static tiles (excluding box and player positions)
    public static List<Integer> targets;

    public static final int Y_MASK = (Integer.MAX_VALUE >> 16);
	
    // Changing data representing a state
	/**
	 * PLAYER
	 * [0] = player-x, player-y
	 * 
	 * BOXES (for n>0)
	 * [n] = nth-box-x (the first 16bits), nth-box-y (the second 16bits)
	 */
	public int[] positions;
    public int boxInPlaceCount;
	
	private Integer hash = null;

    public BoardCustom(BoardCompact boardCompact) {
        int elements = 1 + boardCompact.boxCount;
        boxInPlaceCount = boardCompact.boxInPlaceCount;
		positions = new int[elements];
		positions[0] = getPacked(boardCompact.playerX, boardCompact.playerY);
		int index = 1;
        BoardCustom.targets = new ArrayList<>();
		for (int x = 0; x < boardCompact.width(); ++x) {
			for (int y = 0; y < boardCompact.height(); ++y) {
				if (CTile.isSomeBox(boardCompact.tile(x, y))) {
					positions[index] = getPacked(x, y);
					++index;
				}
                if (CTile.forSomeBox(boardCompact.tile(x, y))) {
                    BoardCustom.targets.add(getPacked(x, y));
                }
			}
		}

        BoardCustom.width = boardCompact.width();
        BoardCustom.height = boardCompact.height();
		BoardCustom.boxCount = boardCompact.boxCount;
		
        BoardCustom.tiles = new byte[width][height];
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				BoardCustom.tiles[x][y] = computeCustomStaticTile(boardCompact, x, y);
			}
		}
        ZobristKeys.initializeKeys();
    }

    private BoardCustom(int[] positions, Integer hash, int boxInPlaceCount) {
        this.positions = positions.clone();
        this.hash = hash;
        this.boxInPlaceCount = boxInPlaceCount;
    }

    public int getPlayerX() {
        return BoardCustom.getX(this.positions[0]);
    }

    public int getPlayerY() {
        return BoardCustom.getY(this.positions[0]);
    }

    public List<TAction> getActions(boolean[][] isSimpleDeadlock) {
        List<TAction> result = new ArrayList<>();

        // Add all pushes from the player's current position
        result.addAll(getPushesWithoutDeadlock(getPlayerX(), getPlayerY(), isSimpleDeadlock));
        // Add all WalkPush actions
        result.addAll(getWalkPushActions(isSimpleDeadlock));

        // System.out.println("POSSIBLE ACTIONS");
        // for (TAction a : result) {
        //     System.out.println(a.toString());
        // }
        // debugPrint();

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
            EDirection dir = push.getDirection();
            boolean isPossible = TPush.isPushPossible(this, x, y, dir);
            if (!isPossible                                                             // Illegal move
                 || isSimpleDeadlock[x+dir.dX+dir.dX][y+dir.dY+dir.dY]                  // New box position can't reach any target
                 || DeadSquareDetector.isFreezeDeadlock(dir, x, y, this)                // Freeze deadlocks
                //  || DeadSquareDetector.isBipartiteDeadlockSimple(dir, x, y, this)       // A target won't have any box that could reach it DISABLED (dont occur in aymeric level set)
                //  || DeadSquareDetector.isBipartiteDeadlock(dir, x, y, this)             // Every target can be matched and there are enough boxes to distribute DISABLED (dont occur in aymeric level set)
                 
               ) continue;     
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
                positionsX[i] = BoardCustom.getX(position);
                positionsY[i] = BoardCustom.getY(position);
            }
            // Add a TWalk only if the final position enables pushing a box at least in one of the four directions
            boolean isPushPossible = false;
            for (TPush push : TPush.getActions()) { 
                isPushPossible = TPush.isPushPossible(this, positionsX[length-1], positionsY[length-1], push.getDirection());
                if (isPushPossible) break;
            }
            if (!isPushPossible) continue;
            result.add(TWalk.fromPositions(this, getPlayerX(), getPlayerY(), positionsX, positionsY));
        }

        return result;
    }

    // Agent always needs to go next to a box and start pushing in order to progress the game
    // This method returns all positions next to boxes that the agent can access
    public List<List<Integer>> getPossibleWalks() {
        // Recompute accessible positions
        List<List<Integer>> possibleWalks = new ArrayList<>();
        Set<Integer> destinations = getBoxNeighbourPositions();

        // BFS
        /// HashMap<Integer, Integer> costMap = new HashMap<>(); // Useless now, but later can be used for heuristic (try closer boxes first)
        Queue<Integer> q = new ArrayDeque<Integer>(); // For uniform cost, we can just use queue. (BFS basically)
        HashMap<Integer, Integer> prevMap = new HashMap<>();

        int initPosition = positions[0]; // Player
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

        for (Integer boxPosition : getBoxes()) {
            result.addAll(getWalkableNeighbours(boxPosition));
        }

        return result;
    }

    public boolean isBox(int x, int y) {
        int position = getPacked(x, y);
        return isBox(position);
    }

    public boolean isBox(int position) {
        for (int i = 1; i < positions.length; i++) {
            if (position == positions[i]) return true;
        }
        return false;
    }

    public static boolean isTarget(int x, int y) {
        int position = getPacked(x, y);
        return isTarget(position);
    }

    public static boolean isTarget(int position) {
        return BoardCustom.targets.contains(position);
    }

    public boolean isPlayer(int x, int y) {
        return (x == getPlayerX()) && (y == getPlayerY());
    }

    /**
	 * Fair warning: by moving the player you're invalidating {@link #hashCode()}...
	 * @param sourceTileX
	 * @param sourceTileY
	 * @param targetTileX
	 * @param targetTileY
	 */
	public void movePlayer(int sourceTileX, int sourceTileY, int targetTileX, int targetTileY) {
		// Remove current position from hash
        hash ^= ZobristKeys.playerKEYS[getX(positions[0])][getY(positions[0])];

        positions[0] = getPacked(targetTileX, targetTileY);

        // Add new position to hash
        hash ^= ZobristKeys.playerKEYS[getX(positions[0])][getY(positions[0])];
	}

    public void moveBox(int sourceTileX, int sourceTileY, int targetTileX, int targetTileY) {
        int sourcePosition = getPacked(sourceTileX, sourceTileY);
        int targetPosition = getPacked(targetTileX, targetTileY);
		
		if (BoardCustom.isTarget(targetTileX, targetTileY)) {
			++boxInPlaceCount;
		}

        int index = -1;
        for (int i = 1; i < positions.length; i++) {
            if (positions[i] != sourcePosition) continue;
            index = i;
            break;
        }

        // Remove current position from hash
        hash ^= ZobristKeys.boxKEYS[getX(positions[index])][getY(positions[index])];

        // Overwrite box position
        positions[index] = targetPosition;

        // Add new position to hash
        hash ^= ZobristKeys.boxKEYS[getX(positions[index])][getY(positions[index])];
		
		if (BoardCustom.isTarget(sourceTileX, sourceTileY)) {
			--boxInPlaceCount;
		}
    }

    private List<Integer> getWalkableNeighbours(int position) {
        List<Integer> neighbours = new ArrayList<>();
        int x = getX(position);
        int y = getY(position);
        for (TMove m : TMove.getActions()) { // Get moves in all directions
            int neighbourPosition = m.isPossible(this, x, y); // Check if move in a direction possible
            if (neighbourPosition == -1) continue; // Not possible, ignore this direction
            neighbours.add(neighbourPosition); // Possible, add neighbour position
        }

        return neighbours;
    }

    public List<Integer> getNonWallNeighbours(int position) {
        List<Integer> neighbours = new ArrayList<>();
        int x = getX(position);
        int y = getY(position);
        for (TMove m : TMove.getActions()) { // Get moves in all directions
            EDirection dir = m.getDirection();
            if (TMove.isOnBoard(this, x, y, m.getDirection()) && !TTile.isWall(tile(x+dir.dX, y+dir.dY))) {
                neighbours.add(getPacked(x+dir.dX, y+dir.dY));
            }
        }

        return neighbours;
    }


    @Override
	public BoardCustom clone() {
        BoardCustom result = new BoardCustom(this.positions, this.hashCode(), this.boxInPlaceCount);
        return result;
    }

    public List<Integer> getBoxes() {
        List<Integer> result = new ArrayList<>();
        for (int i = 1; i < positions.length; i++) {
            result.add(positions[i]);
        }
        return result;
    }

    public boolean isVictory() {
		return BoardCustom.boxCount == this.boxInPlaceCount;
	}

    public void debugPrint() {
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
                byte tile = tile(x, y);
				EEntity entity = EEntity.fromSlimFlag(tile);
				EPlace place = EPlace.fromSlimFlag(tile);
				ESpace space = ESpace.fromSlimFlag(tile);
				
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

    private byte computeCustomStaticTile(BoardCompact board, int x, int y) {
        int compact = board.tile(x, y);
		
		byte staticTile = 0;
		
		if (CTile.forSomeBox(compact)) staticTile |= TTile.PLACE_FLAG;		
		if (CTile.isFree(compact)) return staticTile;
		if (CTile.isWall(compact)) {
			staticTile |= TTile.WALL_FLAG;
			return staticTile;
		}
		
		return staticTile;
    }

    public byte tile(int x, int y) {
        byte tile = BoardCustom.tiles[x][y];
		
        if (isBox(x, y)) {
			tile |= TTile.BOX_FLAG;
		}
        else if (isPlayer(x, y)) {
			tile |= TTile.PLAYER_FLAG;
		}
		
		return tile;
    }


    // ------------------------------------ STATE MINIMAL ------------------------------------
	
	/**
	 * Packs [x;y] into single integer.
	 * @param x
	 * @param y
	 * @return
	 */
	public static int getPacked(int x, int y) {
		return x << 16 | y;
	}
	
	/**
	 * Returns X coordinate from packed value.
	 * @param packed
	 * @return
	 */
	public static int getX(int packed) {
		return packed >> 16;
	}
	
	/**
	 * Returns Y coordinate from packed value.
	 * @param packed
	 * @return
	 */
	public static int getY(int packed) {
		return packed & Y_MASK;
	}

    @Override
	public int hashCode() {
		if (hash == null) {
            hash = 0;

            hash ^= ZobristKeys.playerKEYS[getX(positions[0])][getY(positions[0])];

            for (int i = 1; i < positions.length; ++i) {
                int boxPosition = positions[i];
                hash ^= ZobristKeys.boxKEYS[getX(boxPosition)][getY(boxPosition)];
            }
        }
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
        // if (!(obj instanceof BoardCustom)) return false;
        // BoardCustom other = (BoardCustom) obj;
        // if(positions[0] != other.positions[0]) return false;
        // if (positions.length != other.positions.length) return false;
		return obj.hashCode() == hashCode();
	}
	
	@Override
	public String toString() {
		return "BoardCustom[" + hashCode() + "]";
	}
}
