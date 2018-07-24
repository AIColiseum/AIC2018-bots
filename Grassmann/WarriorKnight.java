package Grassmann;

import aic2018.*;
import Grassmann.utils.*;

public class WarriorKnight extends Unit {
    int ATTACK_RANGE_EXTENDED;

    Location[] basicObjectives;
    int basicObjectivesIndex;

    UnitInfo[] enemies, enemiesMicro;

    void runPreparation() {
        super.runPreparation();
        if (uc.getType() == UnitType.WARRIOR) ATTACK_RANGE_EXTENDED = 13;
        else ATTACK_RANGE_EXTENDED = 13;
        basicObjectives = new Location[enemyInitialLocs.length * 2];
        int k = 0;
        for (Location loc : enemyInitialLocs) basicObjectives[k++] = loc;
        for (Location loc : allyInitialLocs) basicObjectives[k++] = loc;
        basicObjectivesIndex = 0;
    }

    void runTurn() {
        super.runTurn();
        // Get info
        enemies = uc.senseUnits(theirTeam);
        enemiesMicro = enemies;
        // Micro mode
        boolean micro = enemies.length > 0;
        if (micro) {
            if (enemiesMicro.length > 15) enemiesMicro = uc.senseUnits(20, theirTeam);
            if (enemiesMicro.length > 15) enemiesMicro = uc.senseUnits(ATTACK_RANGE_EXTENDED, theirTeam);
            if (enemiesMicro.length > 15) enemiesMicro = uc.senseUnits(myType.attackRangeSquared, theirTeam);
            if (enemiesMicro.length > 15) enemiesMicro = uc.senseUnits(4, theirTeam);
            if (attackAndMove()) return;
        }
        // Report closest enemy and add as objective
        Location closestMilitar = null, closestWorker = null;
        int closestMilitarDist = 999999, closestWorkerDist = 999999;
        for (UnitInfo enemy : enemiesMicro) {
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
        //uc.drawPoint(loc, "blue");
        if (pc == null || pc.goal.distanceSquared(loc) > 5) pc = new PathController(uc, myLoc, loc);
        Direction dir = pc.getMoveDir();
        if (mc.moveTowards(dir) == null) {
            //uc.drawPoint(myLoc.add(dir), "yellow");
            TreeInfo tree = uc.senseTree(myLoc.add(dir));
            if (tree != null && tree.isOak() && uc.canAttack(tree)) uc.attack(tree);
        }
        // Attack tree
        if (myType == UnitType.WARRIOR || enemies.length == 0) {
            if (uc.canAttack()) {
                for (TreeInfo tree : uc.senseTrees(myType.attackRangeSquared)) {
                    if (tree.isOak() && uc.canAttack(tree)) {
                        uc.attack(tree);
                        break;
                    }
                }
            }
        }
        // Active
        if (myType == UnitType.WARRIOR) {
            for (UnitInfo enemy : enemiesMicro) {
                Location eLoc = enemy.getLocation();
                if (uc.canUseActiveAbility(eLoc)) uc.useActiveAbility(eLoc);
            }
        }
        // Check objectives
        if (myLoc.distanceSquared(basicObjectives[basicObjectivesIndex]) <= 8) {
            basicObjectivesIndex = (basicObjectivesIndex + 1) % basicObjectives.length;
        }
        if (pc != null && myLoc.distanceSquared(pc.goal) <= 8) pc = null;
    }

    private boolean attackAndMove() {
        if (!uc.canAttack()) return false;
        UnitInfo best = null;
        //Double bestRatio = 99999.0;
        Integer bestRatio = 99999;
        boolean bestCloseCombat = false;
        for (UnitInfo enemy : enemiesMicro) {
            //if (round < 300) uc.drawPoint(enemy.getLocation(), "red");
            if (!uc.canAttack(enemy) && !canAttackIfMove(enemy)) continue;
            //if (round < 300) uc.drawPoint(enemy.getLocation(), "blue");
            UnitType type = enemy.getType();
            Integer health = enemy.getHealth();
            //Double ratio = (health < myType.attack ? 1.0 : (double)health) / (double)type.cost;
            Integer ratio = (health < myType.attack ? 1 : health);
            if (best == null) {
                best = enemy;
                bestRatio = ratio;
                bestCloseCombat = canReachCloseCombat(enemy.getLocation());
                continue;
            }
            boolean canReachCloseCombat = canReachCloseCombat(enemy.getLocation());
            if (!bestCloseCombat && canReachCloseCombat) {
                best = enemy;
                bestRatio = ratio;
                bestCloseCombat = canReachCloseCombat;
            } else if (bestCloseCombat && !canReachCloseCombat) {
            } else if (ratio < bestRatio) {
                best = enemy;
                bestRatio = ratio;
                bestCloseCombat = canReachCloseCombat;
            }
        }
        if (best != null) {
            Location loc = best.getLocation();
            //if (round < 300) uc.drawPoint(loc, "green");
            Integer dist = myLoc.distanceSquared(loc);
            if (uc.canAttack(best) && (dist <= 2 || !bestCloseCombat)) {
                uc.attack(best);
                moveMicro();
            } else {
                moveMicro(loc, bestCloseCombat);
                uc.attack(best);
            }
            return true;
        }
        return false;
    }

    private boolean canReachCloseCombat(Location loc) {
        if (myType != UnitType.WARRIOR) return false;
        if (uc.getLevel() < 1) return false;
        Integer dist = myLoc.distanceSquared(loc);
        if (dist <= 2) return true;
        if (dist > 8) return false;
        if (dist == 8) return uc.canMove(myLoc.directionTo(loc));
        if (dist == 4) return (mc.moveTowards(loc, 3) != null);
        if (dist == 5) {
            Direction dir = myLoc.directionTo(loc);
            if (uc.canMove(dir)) return true;
            Direction dir2 = dir.rotateRight();
            if (myLoc.add(dir2).distanceSquared(loc) <= 2) return uc.canMove(dir2);
            return uc.canMove(dir.rotateLeft());
        }
        // Should never reach that
        return false;
    }

    private boolean canAttackIfMove(UnitInfo enemy) {
        Location loc = enemy.getLocation();
        if (myLoc.distanceSquared(loc) > ATTACK_RANGE_EXTENDED) return false;
        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            if (!uc.canMove(dirs[i])) continue;
            Location newLoc = myLoc.add(dirs[i]);
            if (newLoc.distanceSquared(loc) <= myType.attackRangeSquared &&
                    !uc.isObstructed(newLoc, loc)) {
                return true;
            }
        }
        return false;
    }

    private boolean canAttackFrom(Location loc, Location enemyLoc) {
        if (loc.distanceSquared(enemyLoc) > myType.attackRangeSquared) return false;
        if (uc.isObstructed(loc, enemyLoc)) return false;
        return true;
    }

    private void moveMicro(Location enemyLoc, boolean closeCombat) {
        //uc.println("Start moveMicro: " + uc.getEnergyUsed());
        if (!uc.canMove()) return;
        // Create arrays of options
        Direction[] dirs = Direction.values();
        Movement[] moves = new Movement[dirs.length];
        for (int i = 0; i < dirs.length; i++) moves[i] = new Movement(myLoc.add(dirs[i]));
        for (int i = 0; i < dirs.length; i++) moves[i].update(enemiesMicro);
        // Choose best option
        Integer bestMoveIndex = null;
        for (int i = dirs.length - 1; i >= 0; i--) {
            if (!uc.canMove(dirs[i])) continue;
            if (enemyLoc != null && !canAttackFrom(myLoc.add(dirs[i]), enemyLoc)) continue;
            if (closeCombat && myLoc.add(dirs[i]).distanceSquared(enemyLoc) > 2) continue;
            if (bestMoveIndex == null || moves[i].isBetter(moves[bestMoveIndex])) bestMoveIndex = i;
        }
        // Apply
        if (bestMoveIndex == null || dirs[bestMoveIndex].isEqual(Direction.ZERO)) return;
        uc.move(dirs[bestMoveIndex]);
        myLoc = uc.getLocation();
        //uc.println("End moveMicro: " + uc.getEnergyUsed());
    }

    private void moveMicro() { moveMicro(null, false); }

    private class Movement {
        Location loc;
        int nEnemies;

        Movement(Location loc) {
            this.loc = loc;
            nEnemies = 0;
        }

        boolean isBetter(Movement m) {
            if (nEnemies != m.nEnemies) return nEnemies < m.nEnemies;
            return myLoc.distanceSquared(loc) < myLoc.distanceSquared(m.loc);
        }

        void update(UnitInfo[] enemies) {
            for (UnitInfo enemy : enemies) updateEnemy(enemy);
        }

        void updateEnemy(UnitInfo unit) {
            Location hisLoc = unit.getLocation();
            Integer dist = loc.distanceSquared(hisLoc);
            UnitType hisType = unit.getType();
            if (dist > hisType.attackRangeSquared || dist < hisType.minAttackRangeSquared) return;
            if (uc.isObstructed(loc, hisLoc)) return;
            nEnemies++;
        }
    }
}
