package Grassmann.utils;

import aic2018.*;

public class DirectionIterator {
	Direction orig;

	boolean lastRight = false;
	Direction currentLeft;
	Direction currentRight;

	Utils utils;

	Direction[] directions = {
		Direction.NORTH, Direction.NORTHWEST, Direction.WEST, Direction.SOUTHWEST,
		Direction.SOUTH, Direction.SOUTHEAST, Direction.EAST, Direction.NORTHEAST
	};

	DirectionIterator(Direction dir) {
		utils = new Utils(this);
		if (dir.isEqual(Direction.ZERO)) {
			dir = utils.getRandomDir();
		}
		orig = dir;
		currentLeft = orig;
		currentRight = orig.rotateLeft();
	}

	DirectionIterator() {
		this(Direction.ZERO);
	}

	public Direction next() {
		Direction ret = null;
		if (lastRight) {
			currentLeft = currentLeft.rotateLeft();
			ret = currentLeft;
		} else {
			currentRight = currentRight.rotateRight();
			ret = currentRight;
		}
		lastRight = !lastRight;
		return ret;
	}
}
