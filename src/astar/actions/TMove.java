package astar.actions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import astar.BoardCustom;
import game.actions.EDirection;
import game.actions.oop.EActionType;

public class TMove extends TAction {
	
	private static Map<EDirection, TMove> actions = new HashMap<EDirection, TMove>();
	
	static {
		actions.put(EDirection.DOWN, new TMove(EDirection.DOWN));
		actions.put(EDirection.UP, new TMove(EDirection.UP));
		actions.put(EDirection.LEFT, new TMove(EDirection.LEFT));
		actions.put(EDirection.RIGHT, new TMove(EDirection.RIGHT));
	}
	
	public static Collection<TMove> getActions() {
		return actions.values();
	}
	
	public static TMove getAction(EDirection direction) {
		return actions.get(direction);
	}
	
	private EDirection dir;
	
	private EDirection[] dirs;
	
	public TMove(EDirection dir) {
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
	public boolean isPossible(BoardCustom board) {
		// PLAYER ON THE EDGE
		if (!isOnBoard(board, board.playerX, board.playerY, dir)) return false;
		
		// TILE TO THE DIR IS FREE
		if (TTile.isFree(board.tile(board.playerX+dir.dX, board.playerY+dir.dY))) return true;
				
		// TILE WE WISH TO MOVE TO IS NOT FREE
		return false;
	}

    // Custom signature that can work with move from a position that is passed as an argument
	public int isPossible(BoardCustom board, byte x, byte y) {
		// PLAYER ON THE EDGE
		if (!isOnBoard(board, x, y, dir)) return -1;
		
		// TILE TO THE DIR IS FREE
		if (TTile.isFree(board.tile(x+dir.dX, y+dir.dY))) return (x+dir.dX)*board.width() + (y+dir.dY);
				
		// TILE WE WISH TO MOVE TO IS NOT FREE
		return -1;
	}
		
	/**
	 * PERFORM THE MOVE, no validation, call {@link #isPossible(BoardCustom, EDirection)} first!
	 * @param board
	 * @param dir
	 */
	@Override
	public void perform(BoardCustom board) {
		// MOVE THE PLAYER
		board.movePlayer(board.playerX, board.playerY, (byte)(board.playerX + dir.dX), (byte)(board.playerY + dir.dY));
	}
	
	/**
	 * REVERSE THE MOVE PRVIOUSLY DONE BY {@link #perform(BoardCustom, EDirection)}, no validation.
	 * @param board
	 * @param dir
	 */
	@Override
	public void reverse(BoardCustom board) {
		// REVERSE THE PLAYER
		board.movePlayer(board.playerX, board.playerY, (byte)(board.playerX - dir.dX), (byte)(board.playerY - dir.dY));
	}
	
	@Override
	public String toString() {
		return "TMove[" + dir.toString() + "]";
	}

}
