package edu.cwru.sepia.agent.minimax;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.minimax.GameState.GameStateUnit;

public class GameStateUtility {

	public static GameState computeGameState(Map<Integer, Action> actionsToApply, GameState previousState) {
		GameState resultState = new GameState(previousState.maxX, previousState.maxY, previousState.getResourceViews());
		HashMap<Integer, GameStateUnit> addedUnits = new HashMap<Integer, GameStateUnit>();
		HashMap<Integer, GameStateUnit> previousUnits = new HashMap<Integer, GameState.GameStateUnit>();
		
		for(GameStateUnit pu : previousState.getPlayerUnits()) {
			previousUnits.put(pu.getOriginalUnit().getID(), pu);
		}
		
		for(GameStateUnit eu : previousState.getEnemyUnits()) {
			previousUnits.put(eu.getOriginalUnit().getID(), eu);
		}
		
		for(GameStateUnit gameStateUnit : previousUnits.values()) {
			int unitId = gameStateUnit.getOriginalUnit().getID();
			Action selectedAction = actionsToApply.get(unitId);
		
			if(selectedAction.getType()==ActionType.PRIMITIVEATTACK) {
				
				TargetedAction targetedAction = (TargetedAction)selectedAction;
			
				if(!addedUnits.containsKey(targetedAction.getTargetId())) {
				}
				
				gameStateUnit.basicAttack(addedUnits.get(targetedAction.getTargetId()));
				resultState.getPlayerUnits().add(gameStateUnit);
				addedUnits.put(gameStateUnit.getOriginalUnit().getID(), new GameStateUnit(gameStateUnit));
			}
			
			//
			// if(selectedAction.getType())
			//
			// if(selectedAction.getType() == ActionType.PRIMITIVEMOVE) {
			//
			// }
		}
		
		return resultState;
	
	}
	
}
