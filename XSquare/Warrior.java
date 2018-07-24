package XSquare;

import aic2018.Location;
import aic2018.UnitController;
import aic2018.UnitInfo;
import aic2018.UnitType;

/**
 * Warrior class. Not used :(.
 */
public class Warrior {

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
            if (uc.getRound() > 2300) target = basicCombatUnit.getEnemyLocation(true);

            basicCombatUnit.tryAttack();

            if (uc.canMove()){
                bugPath.fightMove();
                if (!bugPath.moveTo(target)) {
                    bugPath.safeMove();
                }
            }

            basicCombatUnit.tryAttack();
            basicCombatUnit.tryAttackTree();
            basicCombatUnit.mes.putVisibleTrees();
            basicCombatUnit.mes.putVisibleLocations();
            useActive();

            uc.yield();
        }
    }

    double formula(UnitInfo unit){
        UnitType type = unit.getType();
        return type.attack*type.attackDelay;
    }

    void useActive(){
        UnitInfo bestEnemy = null;
        UnitInfo[] enemies = uc.senseUnits(uc.getType().activeAbilityRange, uc.getOpponent());
        for (int i = 0; i < enemies.length; ++i){
            if (uc.canUseActiveAbility(enemies[i].getLocation())){
                if (!enemies[i].getType().isCombatUnit()) continue;
                if (bestEnemy == null){
                    bestEnemy = enemies[i];
                    continue;
                }
                if (formula(bestEnemy) < formula(enemies[i])) bestEnemy = enemies[i];
            }
        }
        if (bestEnemy != null) uc.useActiveAbility(bestEnemy.getLocation());
    }

}
