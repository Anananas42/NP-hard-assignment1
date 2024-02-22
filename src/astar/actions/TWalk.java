package astar.actions;

import java.util.List;

import astar.BoardCustom;
import game.actions.EDirection;
import game.actions.oop.EActionType;

public class TWalk extends TAction {

	private int x;
	private int y;
	
	private int fromX = -1;
	private int fromY = -1;
	
	private EDirection[] path;

	public TWalk(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public TWalk(int x, int y, EDirection[] path) {
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
	 * How many steps do you need in order to perform the walk; defined only if directions are provided during construction using {@link TWalk#TWalk(int, int, EDirection[])}.
	 * @return
	 */
	public int getSteps() {
		return path == null ? -1 : path.length;
	}

	@Override
	public boolean isPossible(BoardCustom board) {
		return TTile.isWalkable(board.tile(x, y));
	}

	@Override
	public void perform(BoardCustom board) {
		this.fromX = board.playerX;
		this.fromY = board.playerY;
		if (fromX != x || fromY != y) {
			board.movePlayer(board.playerX, board.playerY, x, y);
		}
	}

	@Override
	public void reverse(BoardCustom board) {
		if (fromX != x || fromY != y) {
			board.movePlayer(x, y, fromX, fromY);
		}
	}

	public static TWalk fromPositions(BoardCustom board, int startX, int startY, int[] positionsX, int[] positionsY) {
		EDirection[] directions = new EDirection[positionsX.length];

		int prevX = startX;
		int prevY = startY;
		int currX, currY;
		for (int i = 0; i < positionsX.length; i++) {
			currX = positionsX[i];
			currY = positionsY[i];
			directions[i] = TAction.getDirectionFromPosition(prevX, prevY, currX, currY);
			prevX = currX;
			prevY = currY;
		}

		return new TWalk(prevX, prevY, directions);
	}

	// Object
	// ======
	
	@Override
	public String toString() {
		if (fromX < 0) {
			return "TWalk[->" + x + "," + y + "]";
		} else {
			return "TWalk[" + fromX + "," + fromY + "->" + x + "," + y + "]";
		}
	}
	
}
