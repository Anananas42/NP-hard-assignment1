package astar.tests.test_levels;

import java.io.File;

import astar.BoardCustom;
import game.board.oop.Board;

public class TestLevelLoader {
    BoardCustom boardCustom;
    Board board;
    
    public TestLevelLoader(File file) {
        Board b = Board.fromFileSok(file, 1);
        b.validate();

        this.board = b;
        this.boardCustom = new BoardCustom(b.makeBoardCompact());
    }

    public BoardCustom getBoardCustom() {
        return boardCustom;
    }
}
