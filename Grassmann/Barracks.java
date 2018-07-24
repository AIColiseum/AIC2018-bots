package Grassmann;

import aic2018.*;

import java.util.*;

public class Barracks extends Unit {
    Integer myBookerResources;
    boolean spawnInitialKnight;

    boolean spawn(Direction dir, UnitType type) {
        Direction spawnedUnit = sc.spawn(dir, type);
        if (spawnedUnit != null) {
            rc.bookResources(-myBookerResources);
            myBookerResources = 0;
            return true;
        }
        if (myBookerResources == 0) {
            rc.bookResources(type.cost);
            myBookerResources += type.cost;
        }
        return false;
    }

    void runPreparation() {
        super.runPreparation();
        myBookerResources = 0;
        spawnInitialKnight = uc.getRound() < 300;
    }

    void runTurn() {
        super.runTurn();
        // Initial stuff
        rc.setBarracksLocation(myLoc);
        if (spawnInitialKnight) spawnInitialKnight = !spawn(utils.getRandomDir(), UnitType.KNIGHT);
        // Count units
        Map<UnitType, Double> diffs = new HashMap<UnitType, Double>();
        boolean thereAreEnemies = false;
        for (UnitType type : UnitType.values()) diffs.put(type, 0.0);
        UnitInfo[] units = uc.senseUnits(32);
        Integer dx = 0, dy = 0;
        for (UnitInfo unit : units) {
            Location loc = unit.getLocation();
            UnitType type = unit.getType();
            if (unit.getTeam().isEqual(myTeam)) {
                if (type == UnitType.WORKER) continue;
                diffs.put(type, diffs.get(type) + 1);
                dx += loc.x - myLoc.x;
                dy += loc.y - myLoc.y;
            } else {
                thereAreEnemies = true;
                diffs.put(type, diffs.get(type) - 1);
                dx -= loc.x - myLoc.x;
                dy -= loc.y - myLoc.y;
            }

        }
        Double totalDiff = diffs.get(UnitType.WARRIOR) + diffs.get(UnitType.ARCHER) + diffs.get(UnitType.KNIGHT) +
                diffs.get(UnitType.BALLISTA) + diffs.get(UnitType.WORKER) / 2;
        Direction enemyDir = myLoc.directionTo(new Location(myLoc.x + dx, myLoc.y + dy)).opposite();
        // Build if required
        if (totalDiff < 0 || (totalDiff <= 1 && thereAreEnemies)) {
            if (diffs.get(UnitType.ARCHER) < 0) spawn(enemyDir.opposite(), UnitType.KNIGHT);
            else if (diffs.get(UnitType.KNIGHT) < 0) spawn(enemyDir.opposite(), UnitType.WARRIOR);
            else if (diffs.get(UnitType.WARRIOR) < 0) {
                boolean spawnedArched = false;
                UnitInfo[] nearbyAllies = uc.senseUnits(myLoc, 13, myTeam);
                for (UnitInfo unit : nearbyAllies) {
                    if (unit.getType() == UnitType.WARRIOR || unit.getType() == UnitType.KNIGHT) {
                        spawnedArched = spawn(enemyDir.opposite(), UnitType.ARCHER);
                    }
                }
                if (!spawnedArched) spawn(enemyDir.opposite(), UnitType.WARRIOR);
            }
            else spawn(enemyDir.opposite(), UnitType.WARRIOR);
        }
        // Build if war ongoing
        if (utils.getAvailableResources(round, rc) > 100 + 180) {
            Location loc = rc.getEnemyMilitar();
            if (loc != null) {
                if (Math.random() < 0.5) spawn(myLoc.directionTo(loc), UnitType.WARRIOR);
                else spawn(myLoc.directionTo(loc), UnitType.ARCHER);
            }
            else {
                loc = rc.getEnemyWorker();
                if (loc != null) spawn(myLoc.directionTo(loc), UnitType.KNIGHT);
            }
        }
    }
}

/*
// Count units
Map<UnitType, Integer> allies = new HashMap<UnitType, Integer>();
Map<UnitType, Integer> enemies = new HashMap<UnitType, Integer>();
for (UnitType type : UnitType.values()) {
    allies.put(type, 0);
    enemies.put(type, 0);
}
UnitInfo[] units = uc.senseUnits();
for (UnitInfo unit : units) {
    UnitType type = unit.getType();
    if (unit.getTeam().isEqual(myTeam)) allies.put(type, allies.get(type) + 1);
    else enemies.put(type, allies.get(type) + 1);
}
Integer nAllies = allies.get(UnitType.WARRIOR) + allies.get(UnitType.ARCHER) + allies.get(UnitType.KNIGHT) +
                    allies.get(UnitType.BALLISTA);
Integer nEnemies = enemies.get(UnitType.WARRIOR) + enemies.get(UnitType.ARCHER) + enemies.get(UnitType.KNIGHT) +
                    enemies.get(UnitType.BALLISTA);
*/