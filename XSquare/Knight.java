package XSquare;

import aic2018.Direction;
import aic2018.Location;
import aic2018.UnitController;

/**
 * Knight class, moves to closest enemy unit, broadcasts oaks and good spots for farming
 * It dashes if it has gotten closer to the closest ranger when moving (which means there is prob no one guarding it?)
 * It DOES dash sometimes, in doublespiral it jumps over!
 */
public class Knight {

    Bugpath bugPath;
    UnitController uc;

    void run(UnitController _uc){
        uc = _uc;
        BasicCombatUnit basicCombatUnit = new BasicCombatUnit(uc);
        bugPath = new Bugpath(uc, basicCombatUnit.mes);
        Location target = null;

        while (true) {
            basicCombatUnit.getBestTarget();
            target = basicCombatUnit.bestTargetLocation;
            if (target == null) target = basicCombatUnit.getEnemyLocation(true);
            basicCombatUnit.tryAttack();

            if (uc.canMove()){
                Location prevLoc = uc.getLocation().add(Direction.ZERO);
                bugPath.fightMove();
                if (!bugPath.moveTo(target)) {
                    bugPath.safeMove();
                }
                if (uc.canUseActiveAbility() && bugPath.closestRanger != null && prevLoc.distanceSquared(bugPath.closestRanger) > uc.getLocation().distanceSquared(bugPath.closestRanger)){
                    tryUseActive(bugPath.closestRanger);
                }
            }

            basicCombatUnit.tryAttack();
            basicCombatUnit.tryAttackTree();
            basicCombatUnit.mes.putVisibleTrees();
            basicCombatUnit.mes.putVisibleLocations();
            uc.yield();
        }
    }

    void tryUseActive(Location target){
        Location[] locs = uc.getVisibleLocations(uc.getType().activeAbilityRange);
        Location bestLoc = uc.getLocation();
        int bestDist = bestLoc.distanceSquared(target);
        for (Location loc : locs){
            if (!uc.canUseActiveAbility(loc)) continue;
            int d = loc.distanceSquared(target);
            if (d < bestDist){
                bestDist = d;
                bestLoc = loc;
            }
        }
        if (!bestLoc.isEqual(uc.getLocation())) uc.useActiveAbility(bestLoc);
    }

}
