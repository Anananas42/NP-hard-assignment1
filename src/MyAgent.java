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
		this.board = new BoardCustom(boardCompact);
		searchedNodes = 0;
		long searchStartMillis = System.currentTimeMillis();

		// Execute A*
		AStarProblem problem = new AStarProblem(this.board, boardCompact);
		Solution<BoardCustom, TAction> solution = AStar.search(problem);
		
		List<EDirection> result = new ArrayList<>();
		if (solution == null) {
			throw new Error("[MyAgent] No solution found! Steps: " + searchedNodes);
		}
		for (TAction a : solution.actions) {
			result.addAll(new ArrayList<>(Arrays.asList(a.getDirections())));
		}

		long searchTime = System.currentTimeMillis() - searchStartMillis;

		this.searchedNodes = solution.searchedNodes;
        
        if (verbose) {
            out.println("Nodes visited: " + searchedNodes);
			// out.println("Frozen deadlocks avoided: " + DeadSquareDetector.frozenDeadlockCount);
			// out.println("Frozen deadlock calls: " + DeadSquareDetector.frozenDeadlockCallsCount);
			// out.printf("Frozen deadlock search time: %.1f ms\n", (double)DeadSquareDetector.frozenDeadlockSearchTime);
			// out.println("Bipartite deadlocks avoided: " + DeadSquareDetector.bipartiteDeadlockCount);
			// out.println("Bipartite deadlock calls: " + DeadSquareDetector.bipartiteDeadlockCallsCount);
			// out.printf("Bipartite deadlock search time: %.1f ms\n", (double)DeadSquareDetector.bipartiteDeadlockSearchTime);
            out.printf("Performance: %.1f nodes/sec\n",
                        ((double)searchedNodes / (double)searchTime * 1000));
        }
		
		return result.isEmpty() ? null : result;
	}
}