package XSquare;

import aic2018.Location;
import aic2018.UnitController;

/**
 * Ballista class: goes to closest enemy (stops if there is an enemy at dist <= 45, this is done in BasicCombatUnit)
 * attacks a broadcasted target and broadcasts visible oaks // good spots for gardening
 */
public class Ballista {

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

            if (basicCombatUnit.shootBallista != null && uc.canAttack(basicCombatUnit.shootBallista.loc)){
                uc.attack(basicCombatUnit.shootBallista.loc);
                if (uc.canUseActiveAbility(basicCombatUnit.shootBallista.loc)){
                    uc.useActiveAbility(basicCombatUnit.shootBallista.loc);
                }
            }

            if (uc.canMove()){
                bugPath.fightMove();
                if (!basicCombatUnit.closeTarget) {
                    if (!bugPath.moveTo(target)) {
                        bugPath.safeMove();
                    }
                }
            }

            basicCombatUnit.tryAttack();

            basicCombatUnit.mes.putVisibleTrees();
            basicCombatUnit.mes.putVisibleLocations();

            uc.yield();
        }
    }

}
