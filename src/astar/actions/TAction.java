package astar.actions;

import astar.BoardCustom;
import game.actions.EDirection;
import game.actions.compact.CWalk;
import game.actions.compact.CWalkPush;
import game.actions.oop.EActionType;

public abstract class TAction {
	
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

	public abstract boolean isPossible(BoardCustom board);
	
	public abstract void perform(BoardCustom board);
	
	public abstract void reverse(BoardCustom board);

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
	protected boolean onBoard(BoardCustom board, int tileX, int tileY, EDirection dir) {		
		return isOnBoard(board, tileX, tileY, dir);
	}
	
	/**
	 * If we move 1 step in given 'dir', will we still be at board? 
	 * @param tile
	 * @param dir
	 * @param steps
	 * @return
	 */
	public static boolean isOnBoard(BoardCustom board, int tileX, int tileY, EDirection dir) {
		int targetX = tileX + dir.dX;
		if (targetX < 0 || targetX >= BoardCustom.width) return false;
		int targetY = tileY + dir.dY;
		if (targetY < 0 || targetY >= BoardCustom.height) return false;
		return true;
	}

}
