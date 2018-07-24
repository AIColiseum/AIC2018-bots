package Grassmann.utils;

import aic2018.*;

public class SpawnController {
	UnitController uc;

	public SpawnController(UnitController _uc) {
		uc = _uc;
	}

	public Direction spawn(DirectionIterator dirIt, UnitType unitType) {
		for (int i = 0; i < 8; ++i) {
			Direction dir = dirIt.next();
			if (trySpawn(dir, unitType)) return dir;
		}
		return null;
	}

	public Direction spawn(UnitType unitType) {
		return spawn(new DirectionIterator(), unitType);
	}

	public Direction spawn(Direction dir, UnitType unitType) {
		return spawn(new DirectionIterator(dir), unitType);
	}

	public Direction spawnTree(DirectionIterator dirIt) {
		for (int i = 0; i < 8; ++i) {
			Direction dir = dirIt.next();
			Location loc = uc.getLocation().add(dir);
			if (uc.canUseActiveAbility(loc)) {
				uc.useActiveAbility(loc);
				return dir;
			}
		}
		Location loc = uc.getLocation();
		if (uc.canUseActiveAbility(loc)) {
			uc.useActiveAbility(loc);
			return Direction.ZERO;
		}
		return null;
	}

	public Direction spawnTree() {
		return spawnTree(new DirectionIterator());
	}

	public Direction spawnTree(Direction dir) {
		return spawnTree(new DirectionIterator(dir));
	}

	public boolean trySpawn(Direction dir, UnitType unitType) {
		if (uc.canSpawn(dir, unitType)) {
			uc.spawn(dir, unitType);
			return true;
		}
		return false;
	}
}
