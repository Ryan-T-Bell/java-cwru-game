package edu.cwru.sepia.agent.minimax;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.DistanceMetrics;
/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 */
public class GameState {
	
	private static final double PLAYER_HITPOINTS_WEIGHT = 0;
	private static final double ENEMY_HITPOINTS_WEIGHT = 0;
	private static final double FRIENDLIES_ALIVE_WEIGHT = 0;
	private static final double ENEMIES_ALIVE_WEIGHT = 0;
	private static final double ENEMY_CORNERED_WEIGHT = 0;
	
	Map<Integer, GameStateUnit> playerUnitsByID = new HashMap<Integer, GameState.GameStateUnit>();
	Map<Integer, GameStateUnit> enemyUnitsByID = new HashMap<Integer, GameState.GameStateUnit>();
	
	List<ResourceView> resourceViews;
	boolean playersTurn;
	int maxX;
	int maxY;
	private GameState previousState;
	private Map<Integer, Action> previousActions;
	
	/**
	 * You will implement this constructor. It will
	 * extract all of the needed state information from the built in
	 * SEPIA state view.
	 *
	 * You may find the following state methods useful:
	 *
	 * state.getXExtent() and state.getYExtent(): get the map dimensions
	 * state.getAllResourceIDs(): returns all of the obstacles in the map
	 * state.getResourceNode(Integer resourceID): Return a ResourceView for the given ID
	 *
	 * For a given ResourceView you can query the position using
	 * resource.getXPosition() and resource.getYPosition()
	 *
	 * For a given unit you will need to find the attack damage, range and max HP
	 * unitView.getTemplateView().getRange(): This gives you the attack range
	 * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit deals
	 * unitView.getTemplateView().getBaseHealth(): The maximum amount of health of this unit
	 *
	 * @param state Current state of the episode
	 */
	public GameState(State.StateView state) {
		// Since this is the first constructor, we know that it is our turn.
		playersTurn = true;
		List<UnitView> playerUnitViews = state.getUnits(0); // 0 = the player ID, should be a constant declared somewhere...
		for(UnitView unitView : playerUnitViews) {
			playerUnitsByID.put(unitView.getID(), new GameStateUnit(unitView));
		}
		List<UnitView> allUnits = state.getAllUnits();
		for(UnitView unitView : allUnits) {
			if(!playerUnitsByID.containsKey(unitView.getID())) {
				enemyUnitsByID.put(unitView.getID(), new GameStateUnit(unitView));
			}
		}
		resourceViews = state.getAllResourceNodes();
		maxX = state.getXExtent()-1;
		maxY = state.getYExtent()-1;
	}
	
	public GameState(
			int maxX, int maxY, List<ResourceView> resources,
			Map<Integer, GameStateUnit> newPlayerUnits, Map<Integer, GameStateUnit> newEnemyUnits,
			boolean isPlayersTurn,
			GameState previousGameState, Map<Integer, Action> previousActionsTaken) {
		this.resourceViews = resources;
		this.maxX = maxX;
		this.maxY = maxY;
		this.playersTurn = isPlayersTurn;
		this.playerUnitsByID = newPlayerUnits;
		this.enemyUnitsByID = newEnemyUnits;
		this.previousActions = previousActionsTaken;
		this.previousState = previousGameState;
	}
	/**
	 * You will implement this function.
	 *
	 * You should use weighted linear combination of features.
	 * The features may be primitives from the state (such as hp of a unit)
	 * or they may be higher level summaries of information from the state such
	 * as distance to a specific location. Come up with whatever features you think
	 * are useful and weight them appropriately.
	 *
	 * It is recommended that you start simple until you have your algorithm working. Then watch
	 * your agent play and try to add features that correct mistakes it makes. However, remember that
	 * your features should be as fast as possible to compute. If the features are slow then you will be
	 * able to do less plys in a turn.
	 *
	 * Add a good comment about what is in your utility and why you chose those features.
	 *
	 * @return The weighted linear combination of the features
	 */
	public double getUtility() {
		double utility = 0;
		double totalPlayerHitpoints = 0;
		double totalEnemyHitpoints = 0;
		double enemiesAlive = 0;
		double friendliesAlive = 0;
		double enemyCorneredMetric = 0;
		for(GameStateUnit gameStateUnit : playerUnitsByID.values()) {
			totalPlayerHitpoints += gameStateUnit.getHitpoints();
			if(!gameStateUnit.isDead()) {
				friendliesAlive++;
			}
		}
		for(GameStateUnit gameStateUnit : enemyUnitsByID.values()) {
			totalEnemyHitpoints += gameStateUnit.getHitpoints();
			enemyCorneredMetric += Math.pow(getBlockedDirections(gameStateUnit),2);
			if(!gameStateUnit.isDead()) {
				enemiesAlive++;
			}
		}
		utility =
				PLAYER_HITPOINTS_WEIGHT * totalPlayerHitpoints
				+	ENEMY_HITPOINTS_WEIGHT * totalEnemyHitpoints
				+	FRIENDLIES_ALIVE_WEIGHT * friendliesAlive
				+	ENEMIES_ALIVE_WEIGHT * enemiesAlive
				+	ENEMY_CORNERED_WEIGHT * enemyCorneredMetric;
		return utility;
	}
	private int getBlockedDirections(GameStateUnit gameStateUnit) {
		if(gameStateUnit.isDead()) {
			return 0;
		}
		int blockedDirections = 0;
		for(Direction potentialDirection : allowedDirections) {
			// compute the ending x,y coordinate
			int x = gameStateUnit.getxLoc()+potentialDirection.xComponent();
			int y = gameStateUnit.getyLoc()+potentialDirection.yComponent();
			if(!mapLocationIsOpen(x, y)) {
				blockedDirections++;
			}
		}
		return blockedDirections;
	}
	private boolean mapLocationIsOpen(int x, int y) {
		for(GameStateUnit otherUnit : playerUnitsByID.values()) {
			if(otherUnit.getxLoc() == x && otherUnit.getyLoc() == y) {
				return false;
			}
		}
		for(GameStateUnit otherUnit : enemyUnitsByID.values()) {
			if(otherUnit.getxLoc() == x && otherUnit.getyLoc() == y) {
				return false;
			}
		}
		for(ResourceView resource : resourceViews) {
			if(resource.getXPosition() == x && resource.getYPosition() == y) {
				return false;
			}
		}
		return true;
	}
	private Map<Integer, GameStateUnit> getActiveUnitsForTurn() {
		return playersTurn ? playerUnitsByID : enemyUnitsByID;
	}
	/**
	 * You will implement this function.
	 *
	 * This will return a list of GameStateChild objects. You will generate all of the possible
	 * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
	 *
	 * You may find it useful to iterate over all the different directions in SEPIA.
	 *
	 * for(Direction direction : Directions.values())
	 *
	 * To get the resulting position from a move in that direction you can do the following
	 * x += direction.xComponent()
	 * y += direction.yComponent()
	 *
	 * @return All possible actions and their associated resulting game state
	 */
	public List<GameStateChild> getChildren() {
		return filterInvalidStates(generateStates(generateActions(getActiveUnitsForTurn())));
	}
	private boolean isValidGameState() {
		// Make sure no grid space contains more than one unit.
		HashSet<Point> pointSet = new HashSet<Point>();
		for(ResourceView resource : resourceViews) {
			Point point = new Point();
			point.x = resource.getXPosition();
			point.y = resource.getYPosition();
			if(point.x < 0 || point.y < 0 || point.x > maxX || point.y > maxY || pointSet.contains(point)) {
				return false;
			}
			pointSet.add(point);
		}
		for(GameStateUnit unit : playerUnitsByID.values()) {
			Point point = new Point();
			point.x = unit.getxLoc();
			point.y = unit.getyLoc();
			if(point.x < 0 || point.y < 0 || point.x > maxX || point.y > maxY || pointSet.contains(point)) {
				return false;
			}
			pointSet.add(point);
		}
		for(GameStateUnit unit : enemyUnitsByID.values()) {
			Point point = new Point();
			point.x = unit.getxLoc();
			point.y = unit.getyLoc();
			if(point.x < 0 || point.y < 0 || point.x > maxX || point.y > maxY || pointSet.contains(point)) {
				return false;
			}
			pointSet.add(point);
		}
		return true;
	}
	private List<GameStateChild> filterInvalidStates(
			List<GameState> generatedStates) {
		List<GameState> invalidStates = new ArrayList<GameState>();
		for(GameState generatedState : generatedStates) {
			if(!generatedState.isValidGameState()) {
				invalidStates.add(generatedState);
			}
		}
		generatedStates.removeAll(invalidStates);
		List<GameStateChild> result = new ArrayList<GameStateChild>();
		for(GameState validState : generatedStates) {
			result.add(new GameStateChild(validState.previousActions, validState.previousState));
		}
		return result;
	}
	private static class Point {
		int x;
		int y;
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Point other = (Point) obj;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}
	}
	private List<GameState> generateStates(List<Map<Integer, Action>> actionsForTurn) {
		List<GameState> result = new ArrayList<GameState>();
		for(Map<Integer, Action> actionsTaken : actionsForTurn) {
			result.add(generateState(actionsTaken));
		}
		return result;
	}
	private GameState generateState(Map<Integer, Action> actionsTaken) {
		Map<Integer, GameStateUnit> newActiveSideUnits = new HashMap<Integer, GameState.GameStateUnit>();
		Map<Integer, GameStateUnit> newIdleSideUnits = new HashMap<Integer, GameState.GameStateUnit>();
		Map<Integer, GameStateUnit> currentActiveSideUnits = playersTurn ? playerUnitsByID : enemyUnitsByID;
		Map<Integer, GameStateUnit> currentIdleSideUnits = !playersTurn ? playerUnitsByID : enemyUnitsByID;
		// Validate that no idle side units are trying to move
		for(Action action : actionsTaken.values()) {
			if(currentIdleSideUnits.containsKey(action.getUnitId())) {
				throw new IllegalStateException("The idle player is trying to perform actions...");
			}
		}
		for(GameStateUnit currentIdleUnit : currentActiveSideUnits.values()) {
			newIdleSideUnits.put(currentIdleUnit.getOriginalUnit().getID(), new GameStateUnit(currentIdleUnit));
		}
		List<TargetedAction> attackActions = new ArrayList<TargetedAction>();
		for(Integer unitId : currentActiveSideUnits.keySet()) {
			Action action = actionsTaken.get(unitId);
			GameStateUnit activeUnit = currentActiveSideUnits.get(unitId);
			if(action == null) {
				newActiveSideUnits.put(unitId, new GameStateUnit(activeUnit));
			}
			else if(action.getType()==ActionType.PRIMITIVEATTACK) {
				// The unit is attacking, and cannot move
				attackActions.add((TargetedAction)action);
				newActiveSideUnits.put(unitId, new GameStateUnit(activeUnit));
			}
			else if(action.getType()==ActionType.PRIMITIVEMOVE) {
				// The unit should move
				DirectedAction directedAction = (DirectedAction)action;
				newActiveSideUnits.put(unitId, activeUnit.move(directedAction.getDirection())); // Returns a new object
			}
		}
		// The location of all units is now determined, so it's time to calculate damage from attacks.
		for(TargetedAction attack : attackActions) {
			GameStateUnit attackingUnit = newActiveSideUnits.get(attack.getUnitId());
			GameStateUnit receivingUnit = newIdleSideUnits.get((attack).getTargetId());
			if(receivingUnit == null) {
				// Unlikely, but hey, flexibility is nice
				receivingUnit = newActiveSideUnits.get(attack.getTargetId());
			}
			newActiveSideUnits.get(attack.getUnitId()).basicAttack(receivingUnit);
		}
		Map<Integer, GameStateUnit> newPlayerUnits = (playersTurn ? newActiveSideUnits : newIdleSideUnits);
		Map<Integer, GameStateUnit> newEnemyUnits = (!playersTurn ? newActiveSideUnits : newIdleSideUnits);
		GameState result = new GameState(maxX, maxY, resourceViews, newPlayerUnits, newEnemyUnits, !playersTurn, this, actionsTaken);
		return result;
	}
	private List<Map<Integer, Action>> generateActions(Map<Integer, GameStateUnit> activeUnitsForTurn) {
		// All combinations of (unit->action) mappings
		List<Map<Integer, Action>> allUnitActionMaps = new ArrayList<Map<Integer,Action>>();
		for(GameStateUnit gameStateUnit : activeUnitsForTurn.values()) {
			List<Map<Integer, Action>> derivedUnitActionMaps = new ArrayList<Map<Integer,Action>>();
			for(Map<Integer, Action> existingActionMap : allUnitActionMaps) {
				// We need to make multiple variations of every existing action map
				for(Action possibleAction : getPossibleActions(gameStateUnit)) {
					// Copy the existing map into a new object
					Map<Integer, Action> newActionMap = new HashMap<Integer, Action>();
					for(Entry<Integer, Action> entry : existingActionMap.entrySet()) {
						newActionMap.put(entry.getKey(), entry.getValue());
					}
					// Modify the new object with the action selected for the new unit -> action map.
					newActionMap.put(gameStateUnit.getOriginalUnit().getID(), possibleAction);
					// Store the new unit->action map;
					derivedUnitActionMaps.add(newActionMap);
				}
			}
			allUnitActionMaps = derivedUnitActionMaps;
		}
		return allUnitActionMaps;
	}
	private static Direction[] allowedDirections = new Direction[] {
		Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
	};
	private List<Action> getPossibleActions(GameStateUnit gameStateUnit) {
		List<Action> allPossibleActions = new ArrayList<Action>();
		Integer unitId = gameStateUnit.getOriginalUnit().getID();
		int xBase = gameStateUnit.getxLoc();
		int yBase = gameStateUnit.getyLoc();
		for(Direction direction : allowedDirections) {
			int x = xBase + direction.xComponent();
			int y = yBase + direction.yComponent();
			// Don't try to move to occupied locations
			if(mapLocationIsOpen(x, y)) {
				allPossibleActions.add(Action.createPrimitiveMove(unitId, direction));
			}
		}
		// Only consider attacking enemy units.
		// Only consider attacking units within range.
		Collection<GameStateUnit> unitsToCheck = (gameStateUnit.getOriginalUnit().getTemplateView().getPlayer()==0) ? enemyUnitsByID.values() : playerUnitsByID.values();
		for(GameStateUnit unitToCheck : unitsToCheck) {
			if(DistanceMetrics.euclideanDistance(
					gameStateUnit.getxLoc(),
					gameStateUnit.getyLoc(),
					unitToCheck.getxLoc(),
					unitToCheck.getyLoc()
					) <= gameStateUnit.getOriginalUnit().getTemplateView().getRange())
			{
				allPossibleActions.add(Action.createPrimitiveAttack(unitId, unitToCheck.getOriginalUnit().getID()));
			}
		}
		return allPossibleActions;
	}
	public boolean getGameOver() {
		if(playerUnitsByID.isEmpty() || enemyUnitsByID.isEmpty()) {
			return true;
		}
		boolean existsAlivePlayerUnit = false;
		boolean existsAliveEnemyUnit = false;
		for(GameStateUnit playerUnit : playerUnitsByID.values()) {
			if(!playerUnit.isDead()) {
				existsAlivePlayerUnit = true;
				break;
			}
		}
		if(!existsAlivePlayerUnit) {
			return true;
		}
		for(GameStateUnit enemyUnit : enemyUnitsByID.values()) {
			if(!enemyUnit.isDead()) {
				existsAliveEnemyUnit = true;
				break;
			}
		}
		if(!existsAliveEnemyUnit) {
			return true;
		}
		return false;
	}
	public class GameStateUnit{
		private UnitView originalUnit;
		private int xLoc;
		private int yLoc;
		private float hitpoints;
		public GameStateUnit(UnitView copyFrom) {
			originalUnit = copyFrom;
			xLoc = originalUnit.getXPosition();
			yLoc = originalUnit.getYPosition();
			hitpoints = originalUnit.getHP();
		}
		public GameStateUnit(GameStateUnit copyFrom) {
			originalUnit = copyFrom.getOriginalUnit();
			xLoc = copyFrom.getxLoc();
			yLoc = copyFrom.getyLoc();
			hitpoints = copyFrom.getHitpoints();
		}
		public UnitView getOriginalUnit() {
			return originalUnit;
		}
		public int getxLoc() {
			return xLoc;
		}
		public int getyLoc() {
			return yLoc;
		}
		public float getHitpoints() {
			return hitpoints;
		}
		public GameStateUnit move(Direction d) {
			GameStateUnit result = new GameStateUnit(this);
			// Validate that the cell is open
			int newX = xLoc+= d.xComponent();
			int newY = yLoc+= d.yComponent();
			if(mapLocationIsOpen(newX, newY)) {
				result.xLoc = newX;
				result.yLoc = newY;
			}
			return result;
		}
		public GameStateUnit takeDamage(float delta) {
			float armor = originalUnit.getTemplateView().getArmor();
			// predict armor effects
			if(delta<0) {
				if(-delta < armor) {
					delta = 0;
				}
				else {
					delta += armor;
				}
			}
			GameStateUnit result = new GameStateUnit(this);
			result.hitpoints += delta;
			return result;
		}
		public void basicAttack(GameStateUnit target) {
			// compute distance
			if(DistanceMetrics.euclideanDistance(xLoc, yLoc, target.xLoc, target.yLoc) <= target.getOriginalUnit().getTemplateView().getRange()) {
				target.takeDamage(originalUnit.getTemplateView().getBasicAttack() + originalUnit.getTemplateView().getPiercingAttack());
			}
		}
		public boolean isDead() {
			return hitpoints <= 0;
		}
	}
}