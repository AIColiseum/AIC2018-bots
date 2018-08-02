package Felix;

import aic2018.*;

class Warrior {
  void run(UnitController uc) {
    Comms comms = new Comms(uc);
    Tactics tactics = new Tactics();

    int idleTurns = 0;
    int MAX_IDLE_TURNS = 80;
    for (; true; uc.yield()) {
      ++idleTurns;

      // pick up victory points from the floor
      tactics.gatherVictoryPoints(uc);

      // buy victory points
      int victoryPointsToBuy = tactics.amountOfVictoryPointsToBuy(uc);
      uc.buyVP(victoryPointsToBuy);

      // attack
      UnitInfo target = tactics.highestDpsPerHpAttackableEnemy(uc);
      if (target != null) {
        uc.attack(target);
        idleTurns = 0;
      }

      // use counter
      if (uc.canUseActiveAbility()) {
        UnitInfo[] enemies = uc.senseUnits(uc.getOpponent());
        UnitInfo bestEnemy = null;
        double bestDps = 0.0;
        for (UnitInfo e : enemies) {
          if (uc.canUseActiveAbility(e.getLocation()) && !e.isCountered()) {
            UnitType enemyType = e.getType();
            double dps = (double)enemyType.getAttack(e.getLevel()) / Math.max(1.0, (double)enemyType.getAttackDelay());
            if (bestEnemy == null || dps > bestDps) {
              bestEnemy = e;
              bestDps = dps;
            }
          }
        }
        final double EPS = 1e-6;
        if (bestEnemy != null && bestDps > EPS) {
          uc.useActiveAbility(bestEnemy.getLocation());
          idleTurns = 0;
        }
      }

      // mark enemy location
      UnitInfo nearestEnemy = tactics.nearestEnemy(uc);
      if (nearestEnemy != null) {
        Location nearestEnemyLoc = nearestEnemy.getLocation();
        comms.markEnemyLocation(uc.getRound(), nearestEnemyLoc.y, nearestEnemyLoc.x);
      }

      // move
      if (uc.canMove()) {
        Direction dir = null;
        if (nearestEnemy != null) {
          // move with enemy in sight
          int dist2 = uc.getLocation().distanceSquared(nearestEnemy.getLocation());
          if (uc.canAttack() && dist2 > uc.getType().getAttackRangeSquared()) {
            // move towards nearest enemy if too far away to attack
            dir = tactics.firstStepTowards(uc, nearestEnemy.getLocation());
          } else {
            // move in combat
            dir = tactics.bestCombatMove(uc);
          }
        } else {
          // move towards nearest enemy location marked by team
          Location[] enemyLocs = comms.getEnemyLocationsOfAge1(uc.getRound());
          Location nearestEnemyLoc = tactics.nearestLocation(uc, enemyLocs);
          if (nearestEnemyLoc != null) {
            dir = tactics.firstStepTowards(uc, nearestEnemyLoc);
          }
        }
        if (dir != null) uc.move(dir);

        // move in random direction
        if (dir == null) {
          Direction[] ds = Util.randomShuffle(Util.removeElement(Direction.values(), Direction.ZERO));
          for (Direction d : ds) {
            if (uc.canMove(d)) {
              dir = d;
              break;
            }
          }
          if (dir != null) uc.move(dir);
        }
      }

      // try attacking again after moving
      if (target == null) {
        target = tactics.highestDpsPerHpAttackableEnemy(uc);
        if (target != null) {
          uc.attack(target);
          idleTurns = 0;
        }
      }

      // try using counter again after moving
      if (uc.canUseActiveAbility()) {
        UnitInfo[] enemies = uc.senseUnits(uc.getOpponent());
        UnitInfo bestEnemy = null;
        double bestDps = 0.0;
        for (UnitInfo e : enemies) {
          if (uc.canUseActiveAbility(e.getLocation()) && !e.isCountered()) {
            UnitType enemyType = e.getType();
            double dps = (double)enemyType.getAttack(e.getLevel()) / Math.max(1.0, (double)enemyType.getAttackDelay());
            if (bestEnemy == null || dps > bestDps) {
              bestEnemy = e;
              bestDps = dps;
            }
          }
        }
        final double EPS = 1e-6;
        if (bestEnemy != null && bestDps > EPS) {
          uc.useActiveAbility(bestEnemy.getLocation());
          idleTurns = 0;
        }
      }

      // destroy oaks if there is nothing better to attack
      if (target == null) {
        TreeInfo[] trees = uc.senseTrees();
        TreeInfo oakToDestroy = tactics.getOakToDestroy(uc, trees);
        if (oakToDestroy != null) uc.attack(oakToDestroy);
      }

      // seppuku if idle for too long
      if (idleTurns > MAX_IDLE_TURNS) return;
    }
  }
}
