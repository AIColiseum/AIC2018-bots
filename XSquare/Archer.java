package XSquare;

import aic2018.GameConstants;
import aic2018.Location;
import aic2018.UnitController;
import aic2018.UnitInfo;

/**
 * Archer class, moves to closest enemy uses active if possible and broadcasts oaks and good spots for gardening
 */
public class Archer {

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

            useActive();

            if (uc.canMove()){
                bugPath.fightMove();
                if (!bugPath.moveTo(target)) {
                    bugPath.safeMove();
                }
            }

            basicCombatUnit.tryAttack();

            useActive();

            basicCombatUnit.mes.putVisibleTrees();
            basicCombatUnit.mes.putVisibleLocations();

            uc.yield();
        }
    }


    double formula(UnitInfo unit){
        return unit.getHealth();
    }

    void useActive(){
        if (!uc.canUseActiveAbility()) return;
        UnitInfo bestEnemy = null;
        UnitInfo[] enemies = uc.senseUnits(uc.getType().activeAbilityRange, uc.getOpponent());
        for (int i = 0; i < enemies.length; ++i){
            if (uc.canUseActiveAbility(enemies[i].getLocation())){
                if (!enemies[i].getType().isCombatUnit()) continue;
                if (enemies[i].getPoisonTurnsRemaining() > 0) continue;
                if (bestEnemy == null){
                    bestEnemy = enemies[i];
                    continue;
                }
                if (formula(bestEnemy) < formula(enemies[i])) bestEnemy = enemies[i];
            }
        }
        if (bestEnemy != null){
            uc.useActiveAbility(bestEnemy.getLocation());
        }
    }

}
