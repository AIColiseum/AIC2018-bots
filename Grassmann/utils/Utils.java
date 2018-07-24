package Grassmann.utils;

import aic2018.*;

public class Utils {
	private DirectionIterator dirIt;
	private UnitController uc;

	public Utils(UnitController uc) {
		this.uc = uc;
		dirIt = new DirectionIterator();
	}

	public Utils(DirectionIterator _dirIt) {
		dirIt = _dirIt;
	}

	public int getRandom(int n) {
		return (int) (Math.random() * n);
	}

	public UnitType getRandomType() {
	    return UnitType.values()[getRandom(UnitType.values().length)];
	}

	public Direction getRandomDir() {
	    return dirIt.directions[getRandom(dirIt.directions.length)];
	}

	public Location getDangerLocation(UnitInfo[] enemies) {
		if (enemies.length == 0) return null;
		float x = 0.0f, y = 0.0f;
		int n = 0;
		for (UnitInfo enemy : enemies) {
			if (enemy.getType().canAttack()) {
				x += enemy.getLocation().x;
				y += enemy.getLocation().y;
				n++;
			}
		}
		return new Location(Math.round(x / n), Math.round(y / n));
	}

	public boolean tryGatherVP(Direction dir) {
		if (uc.canGatherVPs(dir)) {
			uc.gatherVPs(dir);
			return true;
		}
		return false;
	}

	public void tryGatherVP() {
		for (VictoryPointsInfo vp : uc.senseVPs(2)) {
			tryGatherVP(uc.getLocation().directionTo(vp.getLocation()));
		}
	}

	public boolean tryUseActive(Location loc) {
		if (uc.canUseActiveAbility(loc)) {
			uc.useActiveAbility(loc);
			return true;
		}
		return false;
	}

	public boolean allyInLocation(Location loc) {
		UnitInfo unit = uc.senseUnit(loc);
		if (unit == null) return false;
		if (!unit.getTeam().isEqual(uc.getTeam())) return false;
		return true;
	}

	public boolean checkConnectedLine2(Location a, Location b) {
		TreeInfo tree = uc.senseTree(a);
		//if (uc.senseWaterAtLocation(a) || (tree != null && tree.isOak())) return false;
		if (uc.senseWaterAtLocation(a)) return false;
		if (a.isEqual(b)) return true;
		a = a.add(a.directionTo(b));
		return checkConnectedLine(a, b);
	}

	public boolean checkConnectedLine(Location a, Location b) {
		// isObstructed
		if (a.distanceSquared(b) <= 2) return true;
		return checkConnectedLine2(a.add(a.directionTo(b)), b.add(b.directionTo(a)));
	}

	public AccessibleTiles countTiles(Integer radio) {
		Integer r = (int)Math.sqrt(radio + .5);
		Integer onMap = 0, waters = 0, oaks = 0;
		for (int i = -r; i <= r; ++i) {
			for (int j = -r; j <= r; ++j) {
				Location loc = uc.getLocation().add(i, j);
				if (uc.canSenseLocation(loc)) {
					if (!uc.isOutOfMap(loc)) {
						onMap++;
						if (uc.senseWaterAtLocation(loc)) waters++;
						else {
							TreeInfo tree = uc.senseTree(loc);
							if (tree != null && tree.isOak()) oaks++;
						}
					}
				}
			}
		}
		return new AccessibleTiles(onMap - waters - oaks, oaks);
	}

	public Integer getDistanceToClosestEnemyInitialLocation(Location loc, Location[] enemyInitialLocs) {
		Integer closestEnemyDist = 99999;
		for (Location enemyLoc : enemyInitialLocs) {
			Integer dist = loc.distanceSquared(enemyLoc);
			if (dist < closestEnemyDist) closestEnemyDist = dist;
		}
		return closestEnemyDist;
	}

	public UnitInfo[] getFrontLine(Direction dir, Team team) {
		UnitInfo[] units;
		if (dir.length() < 1.1) {
			units = uc.senseUnits(uc.getLocation().add(dir).add(dir), 5, team);
		} else {
			units = uc.senseUnits(uc.getLocation().add(dir).add(dir), 4, team);
		}
		return units;
	}

	public boolean isCombat(UnitType type) {
		return type == UnitType.WARRIOR || type == UnitType.ARCHER || type == UnitType.KNIGHT ||
				type == UnitType.BALLISTA;
	}

	public Double getBalanceFrontLine(Direction dir) {
		Team myTeam = uc.getTeam();
		UnitInfo[] allies = getFrontLine(dir, myTeam);
		UnitInfo[] enemies = getFrontLine(dir, myTeam.getOpponent());
		Double balance = 1.0;
		for (UnitInfo unit : allies) {
			UnitType type = unit.getType();
			if (isCombat(type)) balance++;
		}
		for (UnitInfo unit : enemies) {
			UnitType type = unit.getType();
			if (isCombat(type)) balance--;
		}
		return balance;
	}

	public boolean isSmallChoppable(TreeInfo tree) {
		return tree.getRemainingGrowthTurns() == 0 && tree.getHealth() > GameConstants.SMALL_TREE_CHOPPING_DMG;
	}

	public boolean isSmallChoppableNextTurn(TreeInfo tree) {
		return tree.getRemainingGrowthTurns() <= 1 &&
				tree.getHealth() + GameConstants.TREE_REGENERATION_RATE > GameConstants.SMALL_TREE_CHOPPING_DMG + 0.5;
	}

	public Integer getAvailableResources(int rounds, MessageController rc) {
		//uc.println("Reserved: " + rc.getBookedResources());
		Integer myResources = uc.getResources() - rc.getBookedResources();
		if (uc.getRound() >= 70) myResources -= 100;
		else if (uc.getRound() >= 500) myResources -= 200;
		//uc.println("My resources: " + myResources);
		return myResources;
	}
}
