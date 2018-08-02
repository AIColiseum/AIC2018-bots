package Felix;

import java.util.Arrays;
import aic2018.*;

class Tactics {
  final int TURNS_BEFORE_BARRACKS = 50;
  final int THRESHOLD_FOR_BUYING_VP = 1500;

  int getWorkerMoneyReserve(UnitController uc) {
    if (uc.getRound() < TURNS_BEFORE_BARRACKS) return GameConstants.SMALL_TREE_COST;
    return GameConstants.BARRACKS_COST + GameConstants.SMALL_TREE_COST;
  }

  int getBarracksMoneyReserve(UnitController uc) {
    return GameConstants.BALLISTA_COST;
  }

  void gatherVictoryPoints(UnitController uc) {
    for (Direction d : Direction.values()) {
      if (uc.canGatherVPs(d)) {
        uc.gatherVPs(d);
      }
    }
  }

  UnitInfo weakestAttackableEnemy(UnitController uc) {
    if (!uc.canAttack()) return null;
    UnitInfo[] enemies = uc.senseUnits(uc.getOpponent());
    UnitInfo weakestTarget = null;
    int weakestTargetHealth = -1;
    for (UnitInfo enemy : enemies) {
      if (!uc.canAttack(enemy)) continue;
      if (weakestTarget == null || enemy.getHealth() < weakestTargetHealth) {
        weakestTarget = enemy;
        weakestTargetHealth = enemy.getHealth();
      }
    }
    return weakestTarget;
  }

  UnitInfo highestDpsPerHpAttackableEnemy(UnitController uc) {
    if (!uc.canAttack()) return null;
    UnitInfo[] enemies = uc.senseUnits(uc.getOpponent());
    UnitInfo highestDpsPerHpTarget = null;
    double highestDpsPerHp = -1.0;
    for (UnitInfo enemy : enemies) {
      if (!uc.canAttack(enemy)) continue;
      UnitType enemyType = enemy.getType();
      double dps = (double)enemyType.getAttack(enemy.getLevel()) / Math.max(1.0, (double)enemyType.getAttackDelay());
      double dpsPerHp = dps / Math.max(1.0, enemy.getHealth());
      if (highestDpsPerHpTarget == null || dpsPerHp > highestDpsPerHp) {
        highestDpsPerHpTarget = enemy;
        highestDpsPerHp = dpsPerHp;
      }
    }
    return highestDpsPerHpTarget;
  }

  UnitInfo nearestEnemy(UnitController uc) {
    UnitInfo[] enemies = uc.senseUnits(uc.getOpponent());
    UnitInfo nearestEnemy = null;
    int nearestEnemyDist2 = -1;
    Location here = uc.getLocation();
    for (UnitInfo enemy : enemies) {
      int dist2 = here.distanceSquared(enemy.getLocation());
      if (nearestEnemy == null || dist2 < nearestEnemyDist2) {
        nearestEnemy = enemy;
        nearestEnemyDist2 = dist2;
      }
    }
    return nearestEnemy;
  }

  TreeInfo getTreeToApproach(UnitController uc) {
    if (uc.getType() != UnitType.WORKER) {
      uc.println("non-worker looking for a tree to approach");
      return null;
    }
    TreeInfo[] trees = Util.randomShuffle(uc.senseTrees());
    TreeInfo nearestTree = null;
    int nearestTreeDist2 = -1;
    Location here = uc.getLocation();
    for (TreeInfo tree : trees) {
      int turnsUntilChoppable = 0;
      if (tree.stillGrowing()) {
        turnsUntilChoppable = tree.getRemainingGrowthTurns();
      } else if (tree.isSmall()) {
        // turns to get health above chopping damage
        turnsUntilChoppable = Math.max(0, ((GameConstants.SMALL_TREE_CHOPPING_DMG + 1) - (int)tree.getHealth() + (GameConstants.TREE_REGENERATION_RATE - 1)) / GameConstants.TREE_REGENERATION_RATE);
      }
      if (turnsUntilChoppable > 0) continue;
      int dist2 = here.distanceSquared(tree.getLocation());
      if (dist2 == 0) continue;
      if ((nearestTree == null || dist2 < nearestTreeDist2) &&
          (tree.isSmall() || straightPathIsWaterfree(uc, tree.getLocation()))) {
        nearestTree = tree;
        nearestTreeDist2 = dist2;
      }
    }
    return nearestTree;
  }

  TreeInfo getTreeToChop(UnitController uc, TreeInfo[] trees) {
    if (uc.getType() != UnitType.WORKER) {
      uc.println("non-worker looking for a tree to chop");
      return null;
    }
    if (!uc.canAttack()) return null;
    int myAttack = uc.getType().getAttack(uc.getInfo().getLevel());
    TreeInfo bestTree = null;
    int bestProfit = 0;
    for (TreeInfo tree : trees) {
      if (!uc.canAttack(tree)) continue;
      if (tree.stillGrowing()) continue;
      if (tree.isSmall() && tree.getHealth() <= GameConstants.SMALL_TREE_CHOPPING_DMG) continue;
      int profit = 0;
      if (tree.isSmall()) {
        profit = GameConstants.SMALL_TREE_CHOPPING_WOOD;
      } else {
        // bonus multiplier for oak because clearing obstacles is valuable early
        profit = GameConstants.OAK_CHOPPING_WOOD * (3 * GameConstants.MAX_TURNS - 2 * uc.getRound()) / GameConstants.MAX_TURNS;
      }
      UnitInfo target = uc.senseUnit(tree.getLocation());
      if (target != null) {
        UnitType targetType = target.getType();
        int valueDestroyed = targetType.getCost() * myAttack / targetType.getMaxHealth(0);
        if (target.getTeam() == uc.getTeam()) profit -= valueDestroyed;
        else profit += valueDestroyed;
      }
      if (bestTree == null || profit > bestProfit) {
        bestTree = tree;
        bestProfit = profit;
      } else if (profit == bestProfit) {
        if (tree.isSmall()) {
          if (tree.getHealth() > bestTree.getHealth()) {
            bestTree = tree;
            bestProfit = profit;
          }
        } else {
          if (tree.getHealth() == tree.getMaxHealth() ||
              (bestTree.getHealth() < bestTree.getMaxHealth() && tree.getHealth() < bestTree.getHealth())) {
            bestTree = tree;
            bestProfit = profit;
          }
        }
      }
    }
    return bestTree;
  }

  TreeInfo getOakToDestroy(UnitController uc, TreeInfo[] trees) {
    if (!uc.canAttack()) return null;
    TreeInfo weakestOak = null;
    for (TreeInfo tree : trees) {
      if (!uc.canAttack(tree)) continue;
      if (tree.isSmall()) continue;
      if (weakestOak == null || tree.getHealth() < weakestOak.getHealth()) {
        weakestOak = tree;
      }
    }
    return weakestOak;
  }

  Location getPlantingLocation(UnitController uc) {
    if (uc.getType() != UnitType.WORKER) {
      uc.println("non-worker looking for a planting location");
      return null;
    }
    if (!uc.canUseActiveAbility()) return null;
    Direction[] ds0 = Util.randomShuffle(Direction.values());
    Location here = uc.getLocation();
    for (Direction d : ds0) {
      Location loc = here.add(d);
      if (uc.canUseActiveAbility(loc)) {
        return loc;
      }
    }
    return null;
  }

  int amountOfVictoryPointsToBuy(UnitController uc) {
    int victoryPointsToWin = GameConstants.VICTORY_POINTS_MILESTONE - uc.getTeam().getVictoryPoints();
    if (uc.canBuyVP(victoryPointsToWin)) {
      return victoryPointsToWin;
    }
    // spend everything on VP just before the game ends
    if (uc.getRound() == GameConstants.MAX_TURNS) {
      return uc.getResources() / uc.getVPCost();
    }
    // buy VP just before the price is about to go up
    if ((uc.getRound() + 1) % GameConstants.VICTORY_POINTS_INFLATION_ROUNDS == 0) {
      return Math.max(0, (uc.getResources() - THRESHOLD_FOR_BUYING_VP)) / uc.getVPCost();
    }
    return 0;
  }

  Location nearestLocation(UnitController uc, Location[] locs) {
    Location here = uc.getLocation();
    Location nearestLoc = null;
    int nearestLocDist2 = -1;
    for (Location loc : locs) {
      if (nearestLoc == null || here.distanceSquared(loc) < nearestLocDist2) {
        nearestLoc = loc;
        nearestLocDist2 = here.distanceSquared(loc);
      }
    }
    return nearestLoc;
  }

  int countCombatUnits(UnitInfo[] units) {
    int count = 0;
    for (UnitInfo unit : units) {
      if (unit.getType() != UnitType.WORKER && unit.getType() != UnitType.BARRACKS) ++count;
    }
    return count;
  }

  int countBarracks(UnitInfo[] units) {
    int count = 0;
    for (UnitInfo unit : units) {
      if (unit.getType() == UnitType.BARRACKS) ++count;
    }
    return count;
  }

  int countWorkers(UnitInfo[] units) {
    int count = 0;
    for (UnitInfo unit : units) {
      if (unit.getType() == UnitType.WORKER) ++count;
    }
    return count;
  }

  int countSmallTrees(TreeInfo[] trees) {
    int count = 0;
    for (TreeInfo tree : trees) {
      if (tree.isSmall()) ++count;
    }
    return count;
  }

  Direction freeDirectionClosestTo(UnitController uc, Direction dir) {
    Location here = uc.getLocation();
    if (uc.isAccessible(here.add(dir))) return dir;
    Direction left = dir;
    Direction right = dir;
    for (int i = 0; i < 4; ++i) {
      left = left.rotateLeft();
      right = right.rotateRight();
      if (uc.isAccessible(here.add(left))) return left;
      if (uc.isAccessible(here.add(right))) return right;
    }
    return null;
  }

  Direction firstStepTowards(UnitController uc, Location loc) {
    if (loc == null) return null;
    Direction dir = freeDirectionClosestTo(uc, uc.getLocation().directionTo(loc));
    if (dir == null || (!uc.canMove(dir) && uc.canMove())) return null;
    return dir;
  }

  Direction stepAwayFrom(UnitController uc, Location loc) {
    if (loc == null) return null;
    Direction dir = freeDirectionClosestTo(uc, uc.getLocation().directionTo(loc).opposite());
    if (dir == null || (!uc.canMove(dir) && uc.canMove())) return null;
    return dir;
  }

  Location allyWorkersBarycenter(UnitController uc) {
    UnitInfo[] allies = uc.senseUnits(uc.getTeam());
    int ySum = uc.getLocation().y;
    int xSum = uc.getLocation().x;
    int n = 1;
    for (UnitInfo ally : allies) {
      if (ally.getType() == UnitType.WORKER) {
        ySum += ally.getLocation().y;
        xSum += ally.getLocation().x;
        ++n;
      }
    }
    return new Location((xSum + n / 2) / n, (ySum + n / 2) / n);
  }

  boolean straightPathIsWaterfree(UnitController uc, Location goal) {
    if (goal == null || !uc.canSenseLocation(goal)) return false;
    Location loc = uc.getLocation();
    while (!loc.isEqual(goal)) {
      loc = loc.add(loc.directionTo(goal));
      if (uc.senseWaterAtLocation(loc)) return false;
    }
    return true;
  }

  // consumes ~18 energy per unit
  UnitInfo[] combatUnits(UnitInfo[] units) {
    UnitInfo[] combatUnits = new UnitInfo[units.length];
    int end = 0;
    for (UnitInfo unit : units) {
      if (unit.getType() != UnitType.WORKER && unit.getType() != UnitType.BARRACKS) {
        combatUnits[end] = unit;
        ++end;
      }
    }
    return Arrays.copyOf(combatUnits, end);
  }

  Direction bestCombatMove(UnitController uc) {
    Location myLoc = uc.getLocation();
    int myAttackRange2 = uc.getType().getAttackRangeSquared();
    int myMinAttackRange2 = uc.getType().getMinAttackRangeSquared();
    UnitInfo[] enemies = uc.senseUnits(uc.getOpponent());
    UnitInfo[] combatEnemies = combatUnits(enemies);
    Direction[] ds = Util.randomShuffle(Direction.values());
    Direction bestDir = null;
    double bestDpsReceived = 0.0;
    int bestDist2Sum = 0;
    boolean bestHasTarget = false;
    for (Direction d : ds) {
      if (!uc.canMove(d)) continue;
      Location loc = myLoc.add(d);
      boolean hasTarget = false;
      if (uc.getInfo().getAttackCooldown() <= 2.0) {
        for (UnitInfo enemy : enemies) {
          int dist2 = loc.distanceSquared(enemy.getLocation());
          if (dist2 <= myAttackRange2 && dist2 >= myMinAttackRange2 && !uc.isObstructed(loc, enemy.getLocation())) {
            hasTarget = true;
            break;
          }
        }
      }
      double dpsReceived = 0.0;
      int dist2Sum = 0;
      for (UnitInfo enemy : combatEnemies) {
        int dist2 = loc.distanceSquared(enemy.getLocation());
        UnitType enemyType = enemy.getType();
        if (dist2 <= enemyType.getAttackRangeSquared() && dist2 >= enemyType.getMinAttackRangeSquared() && !uc.isObstructed(enemy.getLocation(), loc)) {
          dpsReceived += (double)enemyType.getAttack(enemy.getLevel()) / Math.max(1.0, (double)enemyType.getAttackDelay());
        }
        dist2Sum += dist2;
      }
      if (bestDir == null || hasTarget && !bestHasTarget ||
          (hasTarget == bestHasTarget && dpsReceived < bestDpsReceived) ||
          (hasTarget == bestHasTarget && dpsReceived == bestDpsReceived && dist2Sum > bestDist2Sum)) {
        bestDir = d;
        bestDpsReceived = dpsReceived;
        bestHasTarget = hasTarget;
        bestDist2Sum = dist2Sum;
      }
    }
    return bestDir;
  }
}
