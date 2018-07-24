package Grassmann.utils;

import aic2018.*;

public class PathController {
	UnitController uc;
	public Location goal;
	Location start;
	Location obstacle;
	boolean directionRight;

	public PathController(UnitController _uc, Location _start, Location _goal) {
		uc = _uc;
		start = _start;
		goal = _goal;
		obstacle = null;
		directionRight = Math.random() < .5 ? true : false;
	}

	public Direction getMoveDir() {
		if (!uc.canMove()) return Direction.ZERO;
		if (obstacle == null) return headTowardGoal();
		return followObstacle();
	}

	Direction headTowardGoal() {
		Direction nextDir = uc.getLocation().directionTo(goal);
		if (uc.canMove(nextDir)) {
			return nextDir;
		}
		Location potObstacle = uc.getLocation().add(nextDir);
		UnitInfo unit = uc.senseUnit(potObstacle);
		if (uc.isOutOfMap(potObstacle) ||
				uc.hasObstacle(potObstacle) ||
				(unit != null && unit.getType() == UnitType.BARRACKS)) {
			obstacle = potObstacle;
			start = uc.getLocation();
			return followObstacle();
		}
		return Direction.ZERO;
	}

	Direction followObstacle() {
		Direction obstacleDir = uc.getLocation().directionTo(obstacle);
		Direction dir;
		if (directionRight) dir = obstacleDir.rotateRight();
		else dir = obstacleDir.rotateLeft();
		while (!dir.isEqual(obstacleDir) && !uc.canMove(dir)) {
			Location potObstacle = uc.getLocation().add(dir);
			UnitInfo unit = uc.senseUnit(potObstacle);
			if (uc.isOutOfMap(potObstacle) ||
					uc.hasObstacle(potObstacle) ||
					(unit != null && unit.getType() == UnitType.BARRACKS)) obstacle = potObstacle;
			if (directionRight) dir = dir.rotateRight();
			else dir = dir.rotateLeft();
		}
		if (dir.isEqual(obstacleDir)) return Direction.ZERO;
		if (hasFoundLine(dir)) {
			obstacle = null;
			directionRight = !directionRight;
			return headTowardGoal();
		}
		return dir;
	}

	boolean hasFoundLine(Direction dir) {
		Location currLoc = uc.getLocation();
		if (start.isEqual(currLoc)) return false;
		Location nextLoc = uc.getLocation().add(dir);
		int vecProd1 = (currLoc.x - start.x) * (goal.y - start.y) - (currLoc.y - start.y) * (goal.x - start.x);
		int vecProd2 = (nextLoc.x - start.x) * (goal.y - start.y) - (nextLoc.y - start.y) * (goal.x - start.x);
		return vecProd1 * vecProd2 <= 0;
	}
}
