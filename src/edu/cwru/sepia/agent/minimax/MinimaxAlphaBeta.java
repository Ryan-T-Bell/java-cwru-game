package edu.cwru.sepia.agent.minimax;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
public class MinimaxAlphaBeta extends Agent {
	private final int numPlys;
	public MinimaxAlphaBeta(int playernum, String[] args)
	{
		super(playernum);
		if(args.length < 1)
		{
			System.err.println("You must specify the number of plys");
			System.exit(1);
		}
		numPlys = Integer.parseInt(args[0]);
	}
	@Override
	public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
		return middleStep(newstate, statehistory);
	}
	@Override
	public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
		GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
				numPlys,
				Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY);
		return bestChild.action;
	}
	@Override
	public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
	}
	@Override
	public void savePlayerData(OutputStream os) {
	}
	@Override
	public void loadPlayerData(InputStream is) {
	}
	/**
	 * You will implement this.
	 *
	 * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
	 * and book for more information.
	 *
	 * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
	 * code into other functions and methods)
	 *
	 * @param node The action and state to search from
	 * @param depth The remaining number of plys under this node
	 * @param alpha The current best value for the maximizing node from this node to the root
	 * @param beta The current best value for the minimizing node from this node to the root
	 * @return The best child of this node with updated values
	 */
	public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
	{
		return alphaBetaMax(node, depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}
	private GameStateChild alphaBetaMax(GameStateChild maxNode, int depth, double alpha, double beta) {
		
		List<GameStateChild> childNodes = orderChildrenWithHeuristics(maxNode.state.getChildren());
		GameStateChild bestNode = null;

		for(GameStateChild childNode : childNodes) {
			double score = 0;
			if(depth==0 || childNode.state.getGameOver()) {
				score = childNode.state.getUtility();
			}
			else {
				GameStateChild estimatedConclusionNode = alphaBetaMin(childNode, depth - 1, alpha, beta);
				if(estimatedConclusionNode == null) {
					continue;
				}
			}
			// If we found a node which has better score than our current alpha, update alpha
			if(score > alpha) {
				bestNode = childNode;
				alpha = score;
			}
			// If we found a node which has a better score than our current beta, prune this node, because we know that min will not allow us to reach maxNode in the first place
			if(score>beta) {
				return null; // prune this entire node
			}
		}
		return bestNode;
	}
	
	
	private GameStateChild alphaBetaMin(GameStateChild minNode, int depth, double alpha, double beta) {
		
		/*if(depth <= 0 || terminalNode(currentNode.getState())) return getHeuristic(currentNode.getState());
		 
        if(currentNode.getState().getCurrentPlayer().equals(selfColor)) {
            
        	for(GameTreeNode child: currentNode.getChildren()) {
                
        		alpha = Math.max(alpha, miniMax(child, depth - 1, alpha, beta));

                if(alpha >= beta) return beta;
            }
        	
            return alpha;
            
        } else {
        	
            for(GameTreeNode child: currentNode.getChildren()) {
                
            	beta = Math.min(beta, miniMax(child, depth - 1, alpha, beta));

                if(alpha >= beta) return alpha;

            }
            
            return beta;
        }
		*/
		return null;
	}
	/**
	 * You will implement this.
	 *
	 * Given a list of children you will order them according to heuristics you make up.
	 * See the assignment description for suggestions on heuristics to use when sorting.
	 *
	 * Use this function inside of your alphaBetaSearch method.
	 *
	 * Include a good comment about what your heuristics are and why you chose them.
	 *
	 * @param children
	 * @return The list of children sorted by your heuristic.
	 */
	public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
	{
		children.sort(comparator);
		return children;
	}
	
	private static final Comparator<GameStateChild> comparator = new Comparator<GameStateChild>() {
		@Override
		public int compare(GameStateChild o1, GameStateChild o2) {
			return Float.compare(heuristic(o1), heuristic(o2));
		}
	};
	
	private static float heuristic(GameStateChild child)
	{
		double result = child.state.getUtility();
		// Add a bonus utility corresponding to the distance of our footsoldiers to the nearest enemy
		for(UnitView sourceView : child.state.state.getAllUnits()) {
			// My Units
			if(sourceView.getTemplateView().getPlayer()==0) {
				// for(UnitView targetView : child.state.state.getAllUnits()) {
				// if(targetView.getTemplateView().getPlayer()!=0) {
				// }
				// }
				// Add a penalizing utility corresponding to enemy archers that are in range
				// result += 1 * getArchersInRangePenalty(sourceView, child);
			}
			// Enemy units
			if(sourceView.getTemplateView().getPlayer()!=0) {
				if(sourceView.getTemplateView().getRange()>1) {
					// Add a bonus utility corresponding to enemy archers that are partially or fully cornered
					result += 3 * getImpededMotionBonus(sourceView, child);
				}
			}
		}
		// Add a bonus utility corresponding to enemy archers that have restricted ranges of motion
		return (float)result;
	}
	// private static int getArchersInRangePenalty(UnitView sourceView, GameStateChild child) {
	// int inRangeCount = 0;
	// for(UnitView enemyUnit : child.state.state.getUnitIds(paramInt))
	// return 0;
	// }
	private static double getImpededMotionBonus(UnitView sourceView, GameStateChild child) {
		int directionsImpeded = 0;
		if(sourceView.getXPosition()==1) directionsImpeded++;
		if(sourceView.getYPosition()==1) directionsImpeded++;
		if(sourceView.getXPosition()==child.state.state.getXExtent()) directionsImpeded++;
		if(sourceView.getYPosition()==child.state.state.getYExtent()) directionsImpeded++;
		return directionsImpeded;
	}
	private static int getNearestEnemy(UnitView sourceUnit, GameStateChild child) {
		return 0;
	}
}