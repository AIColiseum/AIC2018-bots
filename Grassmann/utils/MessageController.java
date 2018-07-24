package Grassmann.utils;

import aic2018.*;
import java.util.*;

public class MessageController {

	//GameConstants.TEAM_ARRAY_SIZE;
	final private int A_BARRACKS_START		= 0; // 0 - 149
	final private int A_BARRACKS_SLOTS		= 50; // 0 - 149
	final private int BOOK_RESOURCES		= 150; // 150
	final private int A_WORKER_BUILDER_START	= 151; // 151 - 170
	final private int A_WORKER_BUILDER_SLOTS	= 10; // 151 - 170
	final private int A_WORKER_DEFENDER_START	= 171; // 171 - 180
	final private int A_WORKER_DEFENDER_SLOTS	= 10; // 171 - 180
	final private int FIRSTBARRACKS				= 181; // 181
	final private int E_WORKER_X				= 182; // 182
	final private int E_WORKER_Y				= 183; // 183
	final private int E_MILITAR_X				= 184; // 184
	final private int E_MILITAR_Y				= 185; // 185

	private UnitController uc;

	public MessageController(UnitController _uc) {
		uc = _uc;
	}


	public void setEnemy(Location locWorker, Location locMilitar)  {
		if (locWorker != null) setEnemyWorker(locWorker);
		if (locMilitar != null) setEnemyMilitar(locMilitar);
	}

	public void clearEnemyMilitar() {
		uc.write(E_MILITAR_X, 0);
		uc.write(E_MILITAR_Y, 0);
	}
	public void setEnemyMilitar(Location loc) {
		uc.write(E_MILITAR_X, loc.x);
		uc.write(E_MILITAR_Y, loc.y);
	}
	public void setEnemyMilitar(UnitInfo unit) { setEnemyMilitar(unit.getLocation()); }
	public Location getEnemyMilitar() {
		int x = uc.read(E_MILITAR_X);
		int y = uc.read(E_MILITAR_Y);
		if (x == 0 && y == 0) return null;
		return new Location(x, y);
	}

	public void clearEnemyWorker() {
		uc.write(E_WORKER_X, 0);
		uc.write(E_WORKER_Y, 0);
	}
	public void setEnemyWorker(Location loc) {
		uc.write(E_WORKER_X, loc.x);
		uc.write(E_WORKER_Y, loc.y);
	}
	public void setEnemyWorker(UnitInfo unit) { setEnemyWorker(unit.getLocation()); }
	public Location getEnemyWorker() {
		int x = uc.read(E_WORKER_X);
		int y = uc.read(E_WORKER_Y);
		if (x == 0 && y == 0) return null;
		return new Location(x, y);
	}

	public void setFirstBarracks() {
		uc.write(FIRSTBARRACKS, 1);
	}
	public boolean getFirstBarracks() { return uc.read(FIRSTBARRACKS) == 1; }

	public Integer setWorkerEnemyDistance(Integer enemyDist) {
		for (int i = 0; i < A_WORKER_DEFENDER_SLOTS; i++) {
			int k = A_WORKER_DEFENDER_START + i;
			if (uc.read(k) == 0) {
				uc.write(k, enemyDist);
				return i;
			}
		}
		return A_WORKER_DEFENDER_SLOTS;
	}

	public ArrayList<Integer> getWorkerEnemyDistance() {
		ArrayList<Integer> enemyDists = new ArrayList<Integer>();
		for (int i = 0; i < A_WORKER_DEFENDER_SLOTS; i++) {
			int k = A_WORKER_DEFENDER_START + i * 2;
			Integer enemyDist = uc.read(k);
			if (enemyDist == 0) break;
			enemyDists.add(enemyDist);
		}
		return enemyDists;
	}

	public Integer setWorkerAccessibleTiles(AccessibleTiles accessibleTiles) {
		for (int i = 0; i < A_WORKER_BUILDER_SLOTS; i++) {
			int k = A_WORKER_BUILDER_START + i * 2;
			if (uc.read(k) == 0) {
				uc.write(k, accessibleTiles.accessible + 1);
				uc.write(k + 1, accessibleTiles.oaks + 1);
				return i;
			}
		}
		return A_WORKER_BUILDER_SLOTS;
	}

	public ArrayList<AccessibleTiles> getWorkerAccessibleTiles() {
		ArrayList<AccessibleTiles> accessibleTiles = new ArrayList<AccessibleTiles>();
		for (int i = 0; i < A_WORKER_BUILDER_SLOTS; i++) {
			int k = A_WORKER_BUILDER_START + i * 2;
			Integer accessible = uc.read(k) - 1;
			if (accessible == -1) break;
			Integer oaks = uc.read(k + 1) - 1;
			accessibleTiles.add(new AccessibleTiles(accessible, oaks));
		}
		return accessibleTiles;
	}

	public void bookResources(Integer book) {
		uc.write(BOOK_RESOURCES, getBookedResources() + book);
	}

	public Integer getBookedResources() {
		return uc.read(BOOK_RESOURCES);
	}

	public void setBarracksLocation(Location loc) {
		Integer round = Math.max(uc.getRound(), 1);
		for (int i = 0; i < A_BARRACKS_SLOTS; ++i) {
			int k = A_BARRACKS_START + i * 3;
			int lastRoundReported = uc.read(k);
			if (lastRoundReported == 0 || lastRoundReported < round - 26) {
				uc.write(k, round);
				uc.write(k + 1, loc.x);
				uc.write(k + 2, loc.y);
				return;
			}
			if (uc.read(k + 1) == loc.x && uc.read(k + 2) == loc.y) {
				uc.write(k, round);
				return;
			}
		}
	}

	public ArrayList<Location> getBarracksLocation() {
		ArrayList<Location> barracksLocs = new ArrayList<Location>();
		Integer round = uc.getRound();
		for (int i = 0; i < A_BARRACKS_SLOTS; ++i) {
			int k = A_BARRACKS_START + i * 3;
			int lastRoundReported = uc.read(k);
			if (lastRoundReported == 0) break;
			if (lastRoundReported >= round - 26) {
				barracksLocs.add(new Location(uc.read(k + 1), uc.read(k + 2)));
			}
		}
		return barracksLocs;
	}
}
