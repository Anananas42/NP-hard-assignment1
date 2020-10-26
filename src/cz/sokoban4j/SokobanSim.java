package cz.sokoban4j;

import cz.sokoban4j.simulation.SokobanResult;
import cz.sokoban4j.simulation.SokobanResult.SokobanResultType;
import cz.sokoban4j.simulation.actions.EDirection;
import cz.sokoban4j.simulation.actions.oop.IAction;
import cz.sokoban4j.simulation.actions.oop.MoveOrPush;
import cz.sokoban4j.simulation.agent.IAgent;
import cz.sokoban4j.simulation.board.compact.BoardCompact;
import cz.sokoban4j.simulation.board.oop.Board;

public class SokobanSim implements ISokobanGame, Runnable {

	// SETUP
	
	private Board board;
	private IAgent agent;
	private long timeoutMillis;
	
	// THREAD
	
	private Thread gameThread;
	
	// RUNTIME
	
	private SokobanGameState state;
	
	private IAction agentAction;
	
	private boolean observe = true;
	
	private boolean shouldRun = true;
	
	// RESULT
	
	private SokobanResult result = new SokobanResult();
	
	private int steps = 0;
	
	/**
	 * @param id
	 * @param board
	 * @param agent
	 * @param sprites
	 * @param uiBoard
	 * @param view
	 * @param frame
	 * @param timeoutMillis negative number or zero == no time; in milliseconds
	 */
	public SokobanSim(String id, Board board, IAgent agent, long timeoutMillis) {
		// SETUP
		
		if (id == null) id = "SokobanSim";		
		this.board = board;
		this.agent = agent;
		this.timeoutMillis = timeoutMillis;
		
		// RUNTIME
		
		this.state = SokobanGameState.INIT;
		
		// RESULT
		
		result.setId(id);
		result.setAgent(agent);
		result.setLevel(board.level == null ? "N/A" : board.level);
	}
	
	@Override
	public void startGame() {
		if (state != SokobanGameState.INIT) return;
		try { 
			state = SokobanGameState.RUNNING;
			gameThread = new Thread(this, "SokobanVis");
			gameThread.start();
		} catch (Exception e) {
			stopGame();  
			onSimulationException(e);
		}
	}
	
	@Override
	public void stopGame() {
		if (state != SokobanGameState.RUNNING) return;
		try {
			shouldRun = false;
			gameThread.interrupt();
			try {
				if (gameThread.isAlive()) {
					gameThread.join();
				}
			} catch (Exception e) {			
			}
			gameThread = null;
			onTermination();
		} catch (Exception e) {
			onSimulationException(e);
		}
	}
	
	@Override
	public void run() {
		try {
			result.setSimStartMillis(System.currentTimeMillis());
			
			try {
				agent.newLevel();
			} catch (Exception e) {
				onAgentException(e);
				return;
			}
			
			while (shouldRun && !Thread.interrupted()) {

				// TIMEOUT?
				if (timeoutMillis > 0) {
					long now = System.currentTimeMillis();
					long timeLeftMillis = timeoutMillis - (now - result.getSimStartMillis());
					if (timeLeftMillis <= 0) {						
						onTimeout();
						return;
					}					
				}
				
				// VICTORY?
				if (board.isVictory()) {					
					onVictory();				 
					return;
				}
				
				// OTHERWISE QUERY AGENT FOR THE NEXT ACTION
				
				if (observe) {
					// EXTRACT COMPACT VERSION OF THE BOARD FOR AI
					BoardCompact compactBoard = board.makeBoardCompact();
					// PRESENT BOARD TO THE AGENT
					agent.observe(compactBoard);
					observe = false;
				}
									
				// GET AGENT ACTION
				EDirection whereToMove = agent.act();
				
				if (whereToMove == null) continue;
                
                if (whereToMove == EDirection.NONE) {   // agent gave up
                    stopSimulation(SokobanResultType.AGENT_FAILED, SokobanGameState.FAILED);
                    break;
                }

				agentAction = MoveOrPush.getMoveOrPush(whereToMove);
	
				// AGENT ACTION VALID?
				if (agentAction != null && agentAction.isPossible(board)) {
					// PERFORM THE ACTION
					agentAction.perform(board);
					++steps;
					observe = true;
				} else {
                    System.out.println("Agent returned an illegal move!");
                    stopSimulation(SokobanResultType.AGENT_FAILED, SokobanGameState.FAILED);
				}
				
				agentAction = null;
			}
		} catch (Exception e) {
			onSimulationException(e);
		}
	}

    void stopSimulation(SokobanResultType resultType, SokobanGameState endState) {
		result.setSimEndMillis(System.currentTimeMillis());
		result.setResult(resultType);
		try {
			agent.stop();
		} catch (Exception e) {						
		}
		shouldRun = false;
		state = endState;
    }

	private void onSimulationException(Exception e) {
        result.setException(e);
        stopSimulation(SokobanResultType.SIMULATION_EXCEPTION, SokobanGameState.FAILED);
	}

	private void onTermination() {
        stopSimulation(SokobanResultType.TERMINATED, SokobanGameState.TERMINATED);
	}

	private void onVictory() {
		result.setSimEndMillis(System.currentTimeMillis());
		result.setResult(SokobanResultType.VICTORY);
		result.setSteps(steps);
		try {
			agent.victory();
		} catch (Exception e) {
			onAgentException(e);
			return;
		}		
		state = SokobanGameState.FINISHED;
		try {
			agent.stop();
		} catch (Exception e) {						
		}
	}

	private void onTimeout() {
        stopSimulation(SokobanResultType.TIMEOUT, SokobanGameState.FINISHED);
	}

	private void onAgentException(Exception e) {
        result.setException(e);
        stopSimulation(SokobanResultType.AGENT_EXCEPTION, SokobanGameState.FAILED);
	}

	@Override
	public SokobanGameState getGameState() {
		return state;
	}

	@Override
	public SokobanResult getResult() {
		if (state == SokobanGameState.INIT || state == SokobanGameState.RUNNING) return null;
		return result;
	}

	@Override
	public SokobanResult waitFinish() throws InterruptedException {
		switch (state) {
		case INIT:
			return null;
			
		case RUNNING:
			if (gameThread != null && gameThread.isAlive()) this.gameThread.join();
			return getResult();
		
		default:
			return result;
		}
	}
	
}
