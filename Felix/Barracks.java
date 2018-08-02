package Felix;

import aic2018.*;

class Barracks {
  void run(UnitController uc) {
    Comms comms = new Comms(uc);
    Tactics tactics = new Tactics();

    UnitType nextSpawn = null;
    for (; true; uc.yield()) {
      // buy victory points
      int victoryPointsToBuy = tactics.amountOfVictoryPointsToBuy(uc);
      uc.buyVP(victoryPointsToBuy);

      // mark enemy location
      UnitInfo nearestEnemy = tactics.nearestEnemy(uc);
      if (nearestEnemy != null) {
        Location nearestEnemyLoc = nearestEnemy.getLocation();
        comms.markEnemyLocation(uc.getRound(), nearestEnemyLoc.y, nearestEnemyLoc.x);
      }

      // plan next spawn
      if (nextSpawn == null) {
        if (nearestEnemy != null) {
          if (!tactics.straightPathIsWaterfree(uc, nearestEnemy.getLocation())) {
            // match number of enemy units with archers if possibly disconnected by water
            int numAllyCombatUnits = tactics.countCombatUnits(uc.senseUnits(uc.getTeam()));
            int numEnemyUnits = uc.senseUnits(uc.getOpponent()).length;
            if (numAllyCombatUnits < numEnemyUnits) {
              nextSpawn = UnitType.ARCHER;
            }
          } else {
            // build combat units without limit if connected by land
            int r = Util.random(3);
            if (r == 0) {
              nextSpawn = UnitType.WARRIOR;
            } else if (r == 1) {
              nextSpawn = UnitType.ARCHER;
            } else {
              nextSpawn = UnitType.KNIGHT;
            }
          }
        }
      }

      // spawn
      if (nextSpawn != null) {
        if (nearestEnemy != null) {
          // build units away from enemy
          Direction dirToEnemy = uc.getLocation().directionTo(nearestEnemy.getLocation());
          Direction buildDir = tactics.freeDirectionClosestTo(uc, dirToEnemy.opposite());
          if (buildDir != null && uc.canSpawn(buildDir, nextSpawn)) {
            uc.spawn(buildDir, nextSpawn);
            nextSpawn = null;
          }
        } else if (uc.getResources() - nextSpawn.getCost() >= tactics.getBarracksMoneyReserve(uc)) {
          Direction[] ds = Util.randomShuffle(Util.removeElement(Direction.values(), Direction.ZERO));
          for (Direction d : ds) {
            if (uc.canSpawn(d, nextSpawn)) {
              uc.spawn(d, nextSpawn);
              nextSpawn = null;
              break;
            }
          }
        }
      }
    }
  }
}
