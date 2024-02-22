package astar.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import astar.BoardCustom;
import astar.actions.TAction;
import astar.actions.TWalk;
import astar.tests.test_levels.TestLevelLoader;
import game.board.oop.EEntity;

public class BoardCustomTest {

    public static void main(String[] args) {
        File testLevelFile = new File("src/astar/tests/test_levels/test_level3.sok");
        BoardCustom board = new TestLevelLoader(testLevelFile).getBoardCustom();
        System.out.printf("testing level in %s\n\n", testLevelFile.getName());
        board.debugPrint();
        
        testPosition(board);

        Set<Integer> correctWalkableBoxNeighbours = new HashSet<>();
        correctWalkableBoxNeighbours.add(board.getPosition(2, 2));
        correctWalkableBoxNeighbours.add(board.getPosition(4, 2));
        correctWalkableBoxNeighbours.add(board.getPosition(6, 2));
        correctWalkableBoxNeighbours.add(board.getPosition(8, 2));
        correctWalkableBoxNeighbours.add(board.getPosition(4, 3));
        correctWalkableBoxNeighbours.add(board.getPosition(7, 3));
        correctWalkableBoxNeighbours.add(board.getPosition(9, 3));
        correctWalkableBoxNeighbours.add(board.getPosition(5, 3));
        correctWalkableBoxNeighbours.add(board.getPosition(2, 4));
        correctWalkableBoxNeighbours.add(board.getPosition(3, 4));
        correctWalkableBoxNeighbours.add(board.getPosition(2, 6));

        testWalkableBoxNeighbours(board, correctWalkableBoxNeighbours);

        List<List<Integer>> correctPossibleWalks = new ArrayList<>();
        List<Integer> correctWalk = new ArrayList<>();
        correctWalk.add(board.getPosition(8, 1));
        correctWalk.add(board.getPosition(8, 2));
        correctPossibleWalks.add(correctWalk);

        testPossibleWalks(board, correctPossibleWalks);
        
        List<TAction> actions = board.getActions();
        for (TAction a : actions) {
            System.out.println(a.toString());
        }
        TAction lastAction = actions.get(actions.size()-1);
        lastAction.perform(board);

        System.out.println("PLAYER: " + board.playerX + ", " + board.playerY);
        board.debugPrint();

    }

    private static void testPosition(BoardCustom b) {
        if (b.getXFromPosition(b.getPosition(b.width()-1, b.height()-1)) == b.width()-1
         && b.getYFromPosition(b.getPosition(b.width()-1, b.height()-1)) == b.height()-1){
            System.out.println("Position test successful!");
            return;
        }
        
        throw new Error("[FAILED TEST] Position test");
    }

    private static void testWalkableBoxNeighbours(BoardCustom b, Set<Integer> correctAnswer) {
        Set<Integer> walkableBoxNeighbours = b.getBoxNeighbourPositions();
        if (correctAnswer.equals(walkableBoxNeighbours)) {
            System.out.println("Walkable box neighbours test successful!");
            return;
        }

        System.out.println("-- Answer of size " + walkableBoxNeighbours.size() + " --");
        for (Integer p : walkableBoxNeighbours) {
            System.out.println("Box x, y - " + b.getXFromPosition(p) + ", " + b.getYFromPosition(p) + " -- Position: " + p);
        }
        
        System.out.println("\n-- Correct answer of size " + correctAnswer.size() + " --");
        for (Integer p : correctAnswer) {
            System.out.println("Correct Box x, y - " + b.getXFromPosition(p) + ", " + b.getYFromPosition(p) + " -- Position: " + p);
        }
        throw new Error("[FAILED TEST] Walkable box neighbours");
    }

    private static void testPossibleWalks(BoardCustom b, List<List<Integer>> correctAnswer) {
        List<List<Integer>> possibleWalks = b.getPossibleWalks();
        if (correctAnswer.equals(possibleWalks)) {
            System.out.println("Possible walks test successful!");
            return;
        }

        for (int i = 0; i < possibleWalks.size(); i++) {
            List<Integer> w = possibleWalks.get(i);
            System.out.println("-- Walk " + i + " --");
            for (Integer m : w) {
                System.out.println("Move -> " + b.getXFromPosition(m) + ", " + b.getYFromPosition(m) + " -- Pos: " + m);
            }
            System.out.println();
        }

        List<TWalk> walkActions = b.getWalkActions();
        for (TWalk w : walkActions) {
            System.out.println(w.toString());
        }
        throw new Error("[FAILED TEST] Possible walks");
    }
}


