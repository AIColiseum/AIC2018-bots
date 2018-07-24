package Grassmann;

import aic2018.*;
import Grassmann.utils.*;

public class Archer extends Unit {
    Location[] basicObjectives;
    int basicObjectivesIndex;

    void runPreparation() {
        super.runPreparation();
        basicObjectives = new Location[enemyInitialLocs.length * 2];
        int k = 0;
        for (Location loc : enemyInitialLocs) basicObjectives[k++] = loc;
        for (Location loc : allyInitialLocs) basicObjectives[k++] = loc;
        basicObjectivesIndex = 0;
    }

    void runTurn() {
        super.runTurn();
        // Get info
        UnitInfo[] enemies = uc.senseUnits(theirTeam);
        UnitInfo[] allies = uc.senseUnits(5, myTeam);
        UnitInfo closestEnemy = null;
        Integer closestEnemyDist = null;
        for (UnitInfo unit : enemies) {
            Integer dist = myLoc.distanceSquared(unit.getLocation());
            if (closestEnemy == null || dist < closestEnemyDist) {
                closestEnemy = unit;
                closestEnemyDist = dist;
            }
        }
        // Do
        if (closestEnemy != null) {
            pc = null;
            if (closestEnemyDist < GameConstants.MIN_ARCHER_ATTACK_RANGE_SQUARED) {
                mc.moveTowards(myLoc.directionTo(closestEnemy.getLocation()).opposite());
                ac.attack();
            } else if (closestEnemyDist > GameConstants.ARCHER_ATTACK_RANGE_SQUARED) {
                mc.moveTowards(myLoc.directionTo(closestEnemy.getLocation()));
                ac.attack();
            } else {
                UnitInfo attackedUnit = ac.attack();
                if (attackedUnit != null) {
                    if (utils.getBalanceFrontLine(myLoc.directionTo(attackedUnit.getLocation())) < 0) {
                        mc.moveTowards(myLoc.directionTo(closestEnemy.getLocation()).opposite());
                    }
                }
            }
        }
        // Report closest enemy and add as objective
        Location closestMilitar = null, closestWorker = null;
        int closestMilitarDist = 999999, closestWorkerDist = 999999;
        for (UnitInfo enemy : enemies) {
            Location loc = enemy.getLocation();
            if (enemy.getType() == UnitType.WORKER) {
                if (closestWorker == null || myLoc.distanceSquared(loc) < closestWorkerDist) closestWorker = loc;
            } else if (utils.isCombat(enemy.getType())) {
                if (closestMilitar == null || myLoc.distanceSquared(loc) < closestMilitarDist) closestMilitar = loc;
            }
        }
        rc.setEnemy(closestWorker, closestMilitar);
        // Objectives
        objectives.handleBasicMilitarObjectives(rc);
        objectives.add(basicObjectives[basicObjectivesIndex], 2);
        // Move towards objective
        Location loc = objectives.get();
        if (pc == null || pc.goal.distanceSquared(loc) > 5) pc = new PathController(uc, myLoc, loc);
        Direction dir = pc.getMoveDir();
        if (mc.moveTowards(dir) == null) {
            TreeInfo tree = uc.senseTree(myLoc.add(dir));
            if (tree != null && tree.isOak() && uc.canAttack(tree)) uc.attack(tree);
        }
        /*else {
            if (goToEnemyBase < enemyInitialLocs.length) {
                if (myLoc.distanceSquared(enemyInitialLocs[goToEnemyBase]) <= 13) {
                    goToEnemyBase++;
                    pc = null;
                }
            }
            if (goToEnemyBase < enemyInitialLocs.length) {
                if (pc == null) {
                    pc = new PathController(uc, myLoc, enemyInitialLocs[goToEnemyBase]);
                }
                Direction dir = pc.getMoveDir();
                if (utils.getFrontLine(dir, myTeam).length > 0) mc.tryMove(dir);
            } else {
                Direction dir = utils.getRandomDir();
                for (Direction d = dir.rotateRight(); !d.isEqual(dir); d.rotateRight()) {
                    Integer frontLineBalance = utils.getFrontLine(d, myTeam).length;
                    if (frontLineBalance > 0) mc.moveTowards(d);
                    else if (frontLineBalance == 0 && d.length() < 1.1) mc.moveTowards(d);
                    else mc.moveRandom();
                }
            }
        }*/
        // Active
        for (UnitInfo enemy : enemies) {
            Location eLoc = enemy.getLocation();
            if (uc.canUseActiveAbility(eLoc)) uc.useActiveAbility(eLoc);
        }
        // Check objectives
        if (myLoc.distanceSquared(basicObjectives[basicObjectivesIndex]) <= 8) {
            basicObjectivesIndex = (basicObjectivesIndex + 1) % basicObjectives.length;
        }
        if (pc != null && myLoc.distanceSquared(pc.goal) <= 8) pc = null;
    }
}
