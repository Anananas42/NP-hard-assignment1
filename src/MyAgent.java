import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import agents.ArtificialAgent;
import astar.AStar;
import astar.AStarProblem;
import astar.BoardCustom;
import astar.actions.TAction;
import astar.search.Solution;
import game.actions.EDirection;
import game.board.compact.BoardCompact;


public class MyAgent extends ArtificialAgent {
	protected BoardCustom board;
	protected int searchedNodes;

	@Override
	protected List<EDirection> think(BoardCompact boardCompact) {
		this.board = boardCompact.makeBoardCustom();
		searchedNodes = 0;
		long searchStartMillis = System.currentTimeMillis();

		// Execute A*
		AStarProblem problem = new AStarProblem(this.board, boardCompact);
		Solution<BoardCustom, TAction> solution = AStar.search(problem);
		
		List<EDirection> result = new ArrayList<>();
		if (solution == null) {
			throw new Error("[MyAgent] No solution found!");
		}
		for (TAction a : solution.actions) {
			result.addAll(new ArrayList<>(Arrays.asList(a.getDirections())));
		}

		long searchTime = System.currentTimeMillis() - searchStartMillis;

		this.searchedNodes = solution.searchedNodes;
        
        if (verbose) {
            out.println("Nodes visited: " + searchedNodes);
            out.printf("Performance: %.1f nodes/sec\n",
                        ((double)searchedNodes / (double)searchTime * 1000));
        }
		
		return result.isEmpty() ? null : result;
	}
}

/**
 * The simplest Tree-DFS agent.
 * @author Jimmy
 */
// public class MyAgent extends ArtificialAgent {
// 	protected BoardCompact board;
// 	protected int searchedNodes;
	
// 	@Override
// 	protected List<EDirection> think(BoardCompact board) {
// 		this.board = board;
// 		searchedNodes = 0;
// 		long searchStartMillis = System.currentTimeMillis();
		
// 		List<EDirection> result = new ArrayList<EDirection>();
// 		dfs(5, result); // the number marks how deep we will search (the longest plan we will consider)

// 		long searchTime = System.currentTimeMillis() - searchStartMillis;
        
//         if (verbose) {
//             out.println("Nodes visited: " + searchedNodes);
//             out.printf("Performance: %.1f nodes/sec\n",
//                         ((double)searchedNodes / (double)searchTime * 1000));
//         }
		
// 		return result.isEmpty() ? null : result;
// 	}

// 	private boolean dfs(int level, List<EDirection> result) {
// 		if (level <= 0) return false; // DEPTH-LIMITED
		
// 		++searchedNodes;
		
// 		// COLLECT POSSIBLE ACTIONS
		
// 		List<CAction> actions = new ArrayList<CAction>(4);
		
// 		for (CMove move : CMove.getActions()) {
// 			if (move.isPossible(board)) {
// 				actions.add(move);
// 			}
// 		}
// 		for (CPush push : CPush.getActions()) {
// 			if (push.isPossible(board)) {
// 				actions.add(push);
// 			}
// 		}
		
// 		// TRY ACTIONS
// 		for (CAction action : actions) {
// 			// PERFORM THE ACTION
// 			result.add(action.getDirection());
// 			action.perform(board);
			
// 			// CHECK VICTORY
// 			if (board.isVictory()) {
// 				// SOLUTION FOUND!
// 				return true;
// 			}
			
// 			// CONTINUE THE SEARCH
// 			if (dfs(level - 1, result)) {
// 				// SOLUTION FOUND!
// 				return true;
// 			}
			
// 			// REVERSE ACTION
// 			result.remove(result.size()-1);
// 			action.reverse(board);
// 		}
		
// 		return false;
// 	}
// }