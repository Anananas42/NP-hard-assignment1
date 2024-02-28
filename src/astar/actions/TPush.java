package astar.actions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import astar.BoardCustom;
import game.actions.EDirection;
import game.actions.oop.EActionType;

public class TPush extends TAction {
	
	private static Map<EDirection, TPush> actions = new HashMap<EDirection, TPush>();
	
	static {
		actions.put(EDirection.DOWN, new TPush(EDirection.DOWN));
		actions.put(EDirection.UP, new TPush(EDirection.UP));
		actions.put(EDirection.LEFT, new TPush(EDirection.LEFT));
		actions.put(EDirection.RIGHT, new TPush(EDirection.RIGHT));
	}
	
	public static Collection<TPush> getActions() {
		return actions.values();
	}
	
	public static TPush getAction(EDirection direction) {
		return actions.get(direction);
	}
	
	private EDirection dir;
	
	private EDirection[] dirs;
	
	public TPush(EDirection dir) {
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
	public boolean isPossible(BoardCustom board) {
		return isPushPossible(board, board.getPlayerX(), board.getPlayerY(), dir);
	}
	
	/**
	 * Is it possible to push the box from [playerX, playerY] in 'pushDirection' ?
	 * @param board
	 * @param playerX
	 * @param playerY
	 * @param pushDirection
	 * @return
	 */
	public static boolean isPushPossible(BoardCustom board, int playerX, int playerY, EDirection pushDirection) {
		// PLAYER ON THE EDGE
		if (!TAction.isOnBoard(board, playerX, playerY, pushDirection)) return false;
		
		// TILE TO THE DIR IS NOT BOX
		if (!TTile.isBox(board.tile(playerX+pushDirection.dX, playerY+pushDirection.dY))) return false;
		
		// BOX IS ON THE EDGE IN THE GIVEN DIR
		if (!TAction.isOnBoard(board, playerX+pushDirection.dX, playerY+pushDirection.dY, pushDirection)) return false;
		
		// TILE TO THE DIR OF THE BOX IS NOT FREE
		if (!TTile.isPlayer(board.tile(playerX+pushDirection.dX+pushDirection.dX, playerY+pushDirection.dY+pushDirection.dY))
         && !TTile.isFree(board.tile(playerX+pushDirection.dX+pushDirection.dX, playerY+pushDirection.dY+pushDirection.dY))) return false;
				
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
	public static boolean isPushPossibleIgnorePlayer(BoardCustom board, int playerX, int playerY, EDirection pushDirection) {
		// PLAYER ON THE EDGE
		if (!TAction.isOnBoard(board, playerX, playerY, pushDirection)) return false;
		
		// TILE TO THE DIR IS NOT BOX
		if (!TTile.isBox(board.tile(playerX+pushDirection.dX, playerY+pushDirection.dY))) return false;
		
		// BOX IS ON THE EDGE IN THE GIVEN DIR
		if (!TAction.isOnBoard(board, playerX+pushDirection.dX, playerY+pushDirection.dY, pushDirection)) return false;
		
		// TILE TO THE DIR OF THE BOX IS NOT FREE
		if (!TTile.isWalkable(board.tile(playerX+pushDirection.dX+pushDirection.dX, playerY+pushDirection.dY+pushDirection.dY))) return false;
				
		// YEP, WE CAN PUSH
		return true;
	}
	
	/**
	 * PERFORM THE PUSH, no validation, call {@link #isPossible(BoardCustom, EDirection)} first!
	 * @param board
	 * @param dir
	 */
	@Override
	public void perform(BoardCustom board) {
		// MOVE THE BOX
		board.moveBox(board.getPlayerX() + dir.dX, board.getPlayerY() + dir.dY, board.getPlayerX() + dir.dX + dir.dX, board.getPlayerY() + dir.dY + dir.dY);
		// MOVE THE PLAYER
		board.movePlayer(board.getPlayerX(), board.getPlayerY(), board.getPlayerX() + dir.dX, board.getPlayerY() + dir.dY);
	}
	
	/**
	 * REVERSE THE ACTION PREVIOUSLY DONE BY {@link #perform(BoardCustom, EDirection)}, no validation.
	 * @param board
	 * @param dir
	 */
	@Override
	public void reverse(BoardCustom board) {
		// MARK PLAYER POSITION
		int playerX = board.getPlayerX();
		int playerY = board.getPlayerY();
		// MOVE THE PLAYER
		board.movePlayer(board.getPlayerX(), board.getPlayerY(), board.getPlayerX() - dir.dX, board.getPlayerY() - dir.dY);
		// MOVE THE BOX
		board.moveBox(playerX + dir.dX, playerY + dir.dY, playerX, playerY);
	}
	
	@Override
	public String toString() {
		return "TPush[" + dir.toString() + "]";
	}

}
