package astar.detectors;

import astar.BoardCustom;

public class PICorralDetector {
    // Tiles containing only walls and targets.
    private Byte[][] tiles;

    public void constructTiles(Byte[][] boardTiles) {
        for (int i = 0; i < boardTiles.length; i++) {
            for (int j = 0; j < boardTiles[0].length; j++) {
                tiles[i][j] = boardTiles[i][j];

            }
        }
    }

    public void detect(BoardCustom board) {
        // int[] positions = board.positions;

        // // Construct boxes if they are not in the target places.
        // for(int i = 1; i < positions.length; i++) {
        //     int x = board.getX(positions[i]);
        //     int y = board.getY(positions[i]);
        //     if(tiles[x][y]!=TTile.PLACE_FLAG) {
        //         tiles[x][y] = TTile.BOX_FLAG;
        //     }
        // }


        // Queue<Integer> queue = new LinkedList<>();


        // // Erase boxes from tiles.
        // for(int i = 1; i < positions.length; i++) {
        //     int x = board.getX(positions[i]);
        //     int y = board.getY(positions[i]);
        //     if(tiles[x][y]==TTile.BOX_FLAG) {
        //         tiles[x][y] = TTile.NONE_FLAG;
        //     }
        // }
    }

}
