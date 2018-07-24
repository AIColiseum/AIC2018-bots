package Grassmann.utils;

import aic2018.*;

public class MovementController {

	private UnitController uc;
	private Utils utils;

	public MovementController(UnitController uc) {
		this.uc = uc;
		utils = new Utils(uc);
	}

	public Direction moveTowards(Direction dirTo, int maxIters) {
		DirectionIterator dirIt = new DirectionIterator(dirTo);
		for (int i = 0; i < maxIters; ++i) {
			Direction dir = dirIt.next();
			if (tryMove(dir)) return dir;
		}
		return null;
	}
	public Direction moveTowards(Direction dirTo) { return moveTowards(dirTo, 8); }
	public Direction moveTowards(Location loc, int maxIters) { return moveTowards(uc.getLocation().directionTo(loc), maxIters); }
	public Direction moveTowards(Location loc) { return moveTowards(uc.getLocation().directionTo(loc)); }
	public Direction moveAwayFrom(Direction dir, int maxIters) { return moveTowards(dir.opposite(), maxIters); }
	public Direction moveAwayFrom(Direction dir) { return moveTowards(dir.opposite()); }
	public Direction moveAwayFrom(Location loc, int maxIters) { return moveAwayFrom(uc.getLocation().directionTo(loc), maxIters); }
	public Direction moveAwayFrom(Location loc) { return moveAwayFrom(uc.getLocation().directionTo(loc)); }

	public Direction moveRandom() {
		for (int x = 0; x < 10 && uc.canMove(); ++x) {
			Direction dir = utils.getRandomDir();
			if (tryMove(dir)) return dir;
		}
		return null;
	}

	public boolean tryMove(Direction dir) {
		if (uc.canMove(dir)) {
			uc.move(dir);
			return true;
		}
		return false;
	}
}
