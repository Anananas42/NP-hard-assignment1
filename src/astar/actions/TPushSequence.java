package astar.actions;

import astar.BoardCustom;
import game.actions.EDirection;
import game.actions.oop.EActionType;

public class TPushSequence extends TAction {
	
	private EDirection dir;
	
	private EDirection[] dirs;
	
	public TPushSequence(EDirection dir, int seqLength) {
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
	public boolean isPossible(BoardCustom board) {
        boolean isInitialPushPossible = isInitialPushPossible(board, board.playerX, board.playerY, dir);
        if (!isInitialPushPossible) return false;

        int x = board.playerX + dir.dX;
        int y = board.playerY + dir.dY;
        for (int i = 1; i < this.dirs.length; i++) {
            if (!TTile.isWalkable(board.tile(x, y))) return false;
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
	public static boolean isInitialPushPossible(BoardCustom board, int playerX, int playerY, EDirection pushDirection) {
		// PLAYER ON THE EDGE
		if (!TAction.isOnBoard(board, playerX, playerY, pushDirection)) return false;
		
		// TILE TO THE DIR IS NOT BOX
		if (!TTile.isBox(board.tile(playerX+pushDirection.dX, playerY+pushDirection.dY))) return false;
		
		// BOX IS ON THE EDGE IN THE GIVEN DIR
		if (!TAction.isOnBoard(board, playerX+pushDirection.dX, playerY+pushDirection.dY, pushDirection)) return false;
		
		// TILE TO THE DIR OF THE BOX IS NOT FREE
		if (!TTile.isFree(board.tile(playerX+pushDirection.dX+pushDirection.dX, playerY+pushDirection.dY+pushDirection.dY))) return false;
				
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
	public static boolean isInitialPushPossibleIgnorePlayer(BoardCustom board, int playerX, int playerY, EDirection pushDirection) {
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
        for (EDirection d : this.dirs) {
            // MOVE THE BOX
            board.moveBox(board.playerX + d.dX, board.playerY + d.dY, board.playerX + d.dX + d.dX, board.playerY + d.dY + d.dY);
            // MOVE THE PLAYER
            board.movePlayer(board.playerX, board.playerY, board.playerX + d.dX, board.playerY + d.dY);
        }
	}
	
	/**
	 * REVERSE THE ACTION PREVIOUSLY DONE BY {@link #perform(BoardCustom, EDirection)}, no validation.
	 * @param board
	 * @param dir
	 */
	@Override
	public void reverse(BoardCustom board) {
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
		return "TPushSequence[" + dir.toString() + "]";
	}

}
