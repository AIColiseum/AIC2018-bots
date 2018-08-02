package Felix;

import aic2018.*;

class Worker {
  void run(UnitController uc) {
    Comms comms = new Comms(uc);
    Tactics tactics = new Tactics();

    UnitType nextSpawn = null;
    int idleTurns = 0;
    int MAX_IDLE_TURNS = 20;
    for (; true; uc.yield()) {
      ++idleTurns;

      // pick up victory points from the floor
      tactics.gatherVictoryPoints(uc);

      // buy victory points
      int victoryPointsToBuy = tactics.amountOfVictoryPointsToBuy(uc);
      uc.buyVP(victoryPointsToBuy);

      // mark enemy location
      UnitInfo nearestEnemy = tactics.nearestEnemy(uc);
      if (nearestEnemy != null) {
        Location nearestEnemyLoc = nearestEnemy.getLocation();
        comms.markEnemyLocation(uc.getRound(), nearestEnemyLoc.y, nearestEnemyLoc.x);
      }

      // chop tree
      TreeInfo[] trees = uc.senseTrees();
      TreeInfo treeToChop = tactics.getTreeToChop(uc, trees);
      if (treeToChop != null) {
        uc.attack(treeToChop);
        idleTurns = 0;
      }

      // move
      Direction[] ds = Util.randomShuffle(Util.removeElement(Direction.values(), Direction.ZERO));
      if (uc.canMove()) {
        // move away from nearest enemy if any
        Direction dir = null;
        if (nearestEnemy != null) {
          dir = tactics.stepAwayFrom(uc, nearestEnemy.getLocation());
        }
        if (dir != null) uc.move(dir);

        // move towards choppable tree
        if (dir == null) {
          TreeInfo treeToApproach = tactics.getTreeToApproach(uc);
          if (treeToApproach != null) {
            Location myLoc = uc.getLocation();
            Location treeLoc = treeToApproach.getLocation();
            if (myLoc.distanceSquared(treeLoc) > GameConstants.WORKER_ATTACK_RANGE_SQUARED) {
              // if not adjacent move towards tree
              dir = tactics.firstStepTowards(uc, treeLoc);
            } else {
              // while adjacent to tree get away from ally workers
              Location workersBarycenter = tactics.allyWorkersBarycenter(uc);
              dir = Direction.ZERO;
              int bestDist2 = myLoc.distanceSquared(workersBarycenter);
              for (Direction d : ds) {
                if (!uc.canMove(d)) continue;
                Location loc = myLoc.add(d);
                if (loc.distanceSquared(treeLoc) > GameConstants.WORKER_ATTACK_RANGE_SQUARED) continue;
                if (loc.distanceSquared(workersBarycenter) > bestDist2) {
                  dir = d;
                  bestDist2 = loc.distanceSquared(workersBarycenter);
                }
              }
            }
          }
          if (dir != null) uc.move(dir);
        }

        // move in random direction
        if (dir == null) {
          for (Direction d : ds) {
            if (uc.canMove(d)) {
              dir = d;
              break;
            }
          }
          if (dir != null) uc.move(dir);
        }
      }

      // try again to chop tree after possibly having moved
      if (treeToChop == null) {
        trees = uc.senseTrees();
        treeToChop = tactics.getTreeToChop(uc, trees);
        if (treeToChop != null) {
          uc.attack(treeToChop);
          idleTurns = 0;
        }
      }

      // plan next spawn
      UnitInfo[] allies = uc.senseUnits(uc.getTeam());
      if (nextSpawn == UnitType.BARRACKS) {
        // check whether team still needs barracks
        int numBarracks = tactics.countBarracks(allies);
        if (numBarracks > 0) nextSpawn = null;
      }
      if (nearestEnemy == null) {
        // look for enemies again after possibly having moved
        nearestEnemy = tactics.nearestEnemy(uc);
      }
      if (nextSpawn == null || nextSpawn != UnitType.BARRACKS) {
        if (nearestEnemy != null) {
          int numBarracks = tactics.countBarracks(allies);
          if (numBarracks == 0) {
            nextSpawn = UnitType.BARRACKS;
          }
        } else {
          trees = uc.senseTrees();
          int numSmallTrees = tactics.countSmallTrees(trees);
          int reachableOaksHits = 0;
          for (TreeInfo tree : trees) {
            if (tree.isSmall()) continue;
            if (tactics.straightPathIsWaterfree(uc, tree.getLocation())) {
              reachableOaksHits += (tree.getHealth() + GameConstants.OAK_CHOPPING_DMG - 1) / GameConstants.OAK_CHOPPING_DMG;
            }
          }
          // it takes 6 * (180 + 2 * 10 - 40) / (6 - 2) = 240 rounds of chopping oak to pay for 6 small trees
          int foreseeableRounds = 240;
          int oakWorkerSlots = (reachableOaksHits + foreseeableRounds - 1) / foreseeableRounds;
          int numWorkers = 1 + tactics.countWorkers(allies);
          // need more workers if (workers - oak_worker_slots) / small_tree < regen / damage
          if ((numWorkers - oakWorkerSlots) * GameConstants.SMALL_TREE_CHOPPING_DMG < numSmallTrees * GameConstants.TREE_REGENERATION_RATE) {
            nextSpawn = UnitType.WORKER;
          }
        }
      }

      // spawn
      if (nextSpawn == UnitType.BARRACKS) {
        if (nearestEnemy != null) {
          // build barracks away from enemy if it is in sight
          Direction dirToEnemy = uc.getLocation().directionTo(nearestEnemy.getLocation());
          Direction buildDir = tactics.freeDirectionClosestTo(uc, dirToEnemy.opposite());
          if (buildDir != null && uc.canSpawn(buildDir, nextSpawn)) {
            uc.spawn(buildDir, nextSpawn);
            idleTurns = 0;
            nextSpawn = null;
          }
        } else {
          // build barracks in random direction if no enemy in sight
          for (Direction d : ds) {
            if (uc.canSpawn(d, nextSpawn)) {
              uc.spawn(d, nextSpawn);
              idleTurns = 0;
              nextSpawn = null;
              break;
            }
          }
        }
      } else if (nextSpawn != null  && uc.getResources() >= tactics.getWorkerMoneyReserve(uc)) {
        Direction buildDir = null;

        // build worker in random direction
        for (Direction d : ds) {
          if (buildDir != null) break;
          if (uc.canSpawn(d, nextSpawn)) {
            buildDir = d;
          }
        }

        if (buildDir != null) {
          uc.spawn(buildDir, nextSpawn);
          idleTurns = 0;
          nextSpawn = null;
        }
      }

      // plant tree if none available or have extra money
      if (treeToChop == null || uc.getResources() - GameConstants.SMALL_TREE_COST >= tactics.getWorkerMoneyReserve(uc)) {
        Location plantingLoc = tactics.getPlantingLocation(uc);
        if (plantingLoc != null && uc.getResources() >= tactics.getWorkerMoneyReserve(uc)) {
          uc.useActiveAbility(plantingLoc);
          idleTurns = 0;
        }
      }

      // seppuku if idle for too long
      if (idleTurns > MAX_IDLE_TURNS) return;
    }
  }
}
