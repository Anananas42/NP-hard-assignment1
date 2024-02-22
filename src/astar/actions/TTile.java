package astar.actions;

import game.board.slim.STile;

public class TTile extends STile {
    /**
	 * Can a player can pass through this tile (a tile containing a player is considered walkable as well)?
	 * @param tileFlag
	 * @return
	 */
	public static boolean isWalkable(byte tileFlag) {
		return isFree(tileFlag) || isPlayer(tileFlag);
	}
    
}
