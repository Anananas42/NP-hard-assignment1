package astar;

import java.util.*;

import game.board.oop.EEntity;
import game.board.slim.BoardSlim;

public class BoardCustom extends BoardSlim {
    // List with box positions calculated as x * width + y
    List<Integer> boxes; // List is efficient since there aren't many boxes

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

    @Override
    public void moveBox(byte sourceTileX, byte sourceTileY, byte targetTileX, byte targetTileY) {
        // Update position of the moved box
        boxes.remove((int)sourceTileX * width() + sourceTileY);
        boxes.add((int)targetTileX * width() + targetTileY);
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
}
