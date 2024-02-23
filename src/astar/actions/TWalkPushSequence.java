package astar.actions;


import astar.BoardCustom;
import game.actions.EDirection;
import game.actions.oop.EActionType;

public class TWalkPushSequence extends TAction {
    
    private TWalk walk;
    private TPush push;

    public TWalkPushSequence(TWalk walk, TPush push) {
        this.walk = walk;
        this.push = push;
    }

    public EActionType getType() {
        return EActionType.WALK_AND_PUSH;
    }
	
	public EDirection getDirection() {
        return walk.getDirection();
    }

	public EDirection[] getDirections() {
        EDirection[] walkDirs = walk.getDirections();
        EDirection[] pushDirs = push.getDirections();

        // Combine the lists
        EDirection[] result = new EDirection[walkDirs.length + pushDirs.length];
        for (int i = 0; i < walkDirs.length; i++) {
            result[i] = walkDirs[i];
        }   
        for (int i = walkDirs.length; i < walkDirs.length+pushDirs.length; i++) {
            result[i] = pushDirs[i-walkDirs.length];
        }   

        return result;
    }

    /**
	 * How many steps the action implements; may return -1 if unknown (i.e., custom teleports).
	 * @return
	 */
	public int getSteps() {
        return walk.getSteps() + push.getSteps();
    }

	public boolean isPossible(BoardCustom board) {
        // Pre-condition when creating this macro action
        return true;
    }
	
	public void perform(BoardCustom board) {
        walk.perform(board);
        push.perform(board);
    }
	
	public void reverse(BoardCustom board) {
        push.reverse(board);
        walk.reverse(board);
    }
}
