package Grassmann;

import aic2018.*;
import Grassmann.utils.*;

public class Worker extends Unit {
    private Integer myBookedResources;
    private boolean firstBarracks;
    private boolean noTreeFound;
    private Integer[] chopping;
    int choppingIndex;
    Double ratioChopped;

    private UnitInfo[] enemies;
    private UnitInfo[] allies;
    private TreeInfo[] trees;
    private Integer nTrees;

    private TreeInfo chop() {
        TreeInfo[] trees = uc.senseTrees(myLoc, UnitType.WORKER.attackRangeSquared);
        if (!uc.canAttack()) return null;
        TreeInfo bestTree = null;
        boolean bestTreeSmall = false;
        boolean bestTreeOverAlly = false;
        Double bestTreeHealth = 0.0;
        for (TreeInfo tree : trees) {
            if (!uc.canAttack(tree)) continue;
            if (tree.isSmall() && !utils.isSmallChoppable(tree)) continue;
            if (tree.getHealth() < .1) continue;
            if (bestTree == null) {
                bestTree = tree;
                bestTreeSmall = tree.isSmall();
                bestTreeOverAlly = utils.allyInLocation(tree.getLocation());
                bestTreeHealth = tree.getHealth();
            } else if (tree.isSmall()) {
                if (uc.getResources() > 600 && !bestTreeSmall) continue;
                boolean treeOverAlly = utils.allyInLocation(tree.getLocation());
                if (!bestTreeSmall || bestTreeOverAlly || tree.getHealth() > bestTreeHealth) {
                    bestTree = tree;
                    bestTreeSmall = true;
                    bestTreeOverAlly = treeOverAlly;
                    bestTreeHealth = tree.getHealth();
                    if (!bestTreeOverAlly) break;
                }
            } else {
                if (uc.getResources() <= 600 && bestTreeSmall) continue;
                if (bestTreeSmall || tree.getHealth() < bestTreeHealth) {
                    bestTree = tree;
                    bestTreeSmall = false;
                    bestTreeOverAlly = false;
                    bestTreeHealth = tree.getHealth();
                }
            }
        }
        if (bestTree != null) {
            uc.attack(bestTree);
            return bestTree;
        }
        return null;
    }

    void runPreparation() {
        super.runPreparation();
        myBookedResources = 0;
        if (!rc.getFirstBarracks()) {
            firstBarracks = true;
            rc.setFirstBarracks();
        }
        noTreeFound = true;
        chopping = new Integer[6];
        choppingIndex = 0;
        for (int i = 0; i < chopping.length; i++) chopping[i] = null;
        ratioChopped = null;
    }

    void runTurn() {
        super.runTurn();
        enemies = uc.senseUnits(theirTeam);
        allies = uc.senseUnits(myTeam);
        trees = uc.senseTrees();
        nTrees = trees.length;
        if (trees.length > 15) trees = uc.senseTrees(10);
        // VP
        utils.tryGatherVP();
        // Chop tree
        TreeInfo choppedTree1 = chop();
        // Spawn
        int chopped = 0, total = 0;
        for (Integer ch : chopping) {
            if (ch != null) {
                if (ch == 1) chopped++;
                total++;
            }
        }
        if (total < 6) ratioChopped = null;
        else ratioChopped = (double)chopped / (double)total;
        WorkerBuildController wbc = new WorkerBuildController(uc, utils, allies, enemies, trees, nTrees, rc.getBarracksLocation());
        spawnBarracks(wbc);
        spawnWorkers(wbc);
        spawnSmalls(wbc);
        // Move
        move();
        // Chop tree
        TreeInfo choppedTree2 = chop();
        if (choppedTree1 != null || choppedTree2 != null) chopping[choppingIndex] = 1;
        else chopping[choppingIndex] = 0;
        choppingIndex = (choppingIndex + 1) % chopping.length;
        // VP
        if (!uc.canMove()) utils.tryGatherVP();
        // Attack
        ac.attack(enemies);
        // Unbook if too low health
        if (uc.getInfo().getHealth() <= 10) {
            rc.bookResources(-myBookedResources);
            myBookedResources = 0;
        }
    }

    private void move() {
        //uc.println("Start move: " + uc.getEnergyUsed());
        if (!uc.canMove()) return;
        // Create arrays of options
        Direction[] dirs = Direction.values();
        Movement[] moves = new Movement[dirs.length];
        for (int i = 0; i < dirs.length; i++) moves[i] = new Movement(myLoc.add(dirs[i]));
        if (trees.length < 15) {
            for (int i = 0; i < dirs.length; i++) moves[i].update(enemies, trees);
        } else {
            TreeInfo[] trees2 = uc.senseTrees(8);
            if (trees2.length >= 15) trees2 = uc.senseTrees(4);
            for (int i = 0; i < dirs.length; i++) moves[i].update(enemies, trees2);
        }
        // Choose best option
        Integer bestMoveIndex = null;
        for (int i = dirs.length - 1; i >= 0; i--) {
            if (!uc.canMove(dirs[i])) continue;
            if (bestMoveIndex == null || moves[i].isBetter(moves[bestMoveIndex])) bestMoveIndex = i;
        }
        // Apply
        if (bestMoveIndex == null) return;
        int noneDirection = 8;
        if ((moves[bestMoveIndex].nChoppableTrees > 0) ||
                (bestMoveIndex != noneDirection && moves[bestMoveIndex].nEnemies < moves[noneDirection].nEnemies) ||
                (bestMoveIndex == noneDirection && moves[bestMoveIndex].nEnemies < moves[0].nEnemies)) {
            if (bestMoveIndex != noneDirection) {
                uc.move(dirs[bestMoveIndex]);
                myLoc = uc.getLocation();
            }
        } else {
            if (pc == null || noTreeFound) {
                if (moves[bestMoveIndex].closestTree != null) {
                    pc = new PathController(uc, myLoc, moves[bestMoveIndex].closestTree.getLocation());
                    noTreeFound = false;
                } else if (pc == null){
                    pc = new PathController(uc, myLoc, enemyInitialLocs[0]);
                    noTreeFound = true;
                }
            }
            Direction dir = pc.getMoveDir();
            if (uc.canMove(dir)) {
                uc.move(dir);
                myLoc = uc.getLocation();
                if (myLoc.distanceSquared(pc.goal) <= 2) {
                    pc = null;
                    noTreeFound = true;
                }
            }
        }
        //uc.println("End move: " + uc.getEnergyUsed());
    }

    private class Movement {
        Location loc;
        int nChoppableTrees, nEnemies, distClosestTree;
        TreeInfo closestTree;
        boolean choppableTreeOnLoc;

        Movement(Location loc) {
            this.loc = loc;
            nChoppableTrees = 0;
            nEnemies = 0;
            TreeInfo tree = uc.senseTree(loc);
            choppableTreeOnLoc = (tree != null) && utils.isSmallChoppableNextTurn(tree);
            closestTree = null;
            distClosestTree = 99999;
        }

        boolean isBetter(Movement m) {
            if (nChoppableTrees >= 1 && m.nChoppableTrees < 1) return true;
            if (nChoppableTrees < 1 && m.nChoppableTrees >= 1) return false;
            if (nChoppableTrees < 1 && m.nChoppableTrees < 1) {
                if (nChoppableTrees != m.nChoppableTrees) return nChoppableTrees > m.nChoppableTrees;
            }
            if (nEnemies >= 3 && m.nEnemies < 3) return false;
            if (nEnemies < 3 && m.nEnemies >= 3) return true;
            if (nEnemies < 3 && m.nEnemies < 3) {
                if (nEnemies != m.nEnemies) return nEnemies < m.nEnemies;
            }
            if (!choppableTreeOnLoc && m.choppableTreeOnLoc) return true;
            if (choppableTreeOnLoc && !m.choppableTreeOnLoc) return false;
            if (distClosestTree <= 8 && m.distClosestTree > 8) return true;
            if (distClosestTree > 8 && m.distClosestTree <= 8) return false;
            return myLoc.distanceSquared(loc) < myLoc.distanceSquared(m.loc);
        }

        void update(UnitInfo[] enemies, TreeInfo[] trees) {
            for (UnitInfo enemy : enemies) updateEnemy(enemy);
            for (TreeInfo tree : trees) updateTree(tree);
        }

        void updateTree(TreeInfo tree) {
            if (tree.isSmall() && !utils.isSmallChoppableNextTurn(tree)) return;
            Integer dist = loc.distanceSquared(tree.getLocation());
            if (dist < distClosestTree) {
                closestTree = tree;
                distClosestTree = dist;
            }
            if (dist <= 2) nChoppableTrees++;
        }

        void updateEnemy(UnitInfo unit) {
            UnitType hisType = unit.getType();
            Location hisLoc = unit.getLocation();
            Integer dist = loc.distanceSquared(hisLoc);
            if (dist > hisType.attackRangeSquared || dist < hisType.minAttackRangeSquared) return;
            if (uc.isObstructed(loc, hisLoc)) return;
            nEnemies++;
        }
    }

    private boolean spawnBarracks(WorkerBuildController wbc) {
        //uc.println("Barracks: " + wbc.shouldBuildDefensiveBarracks());
        if (firstBarracks || wbc.shouldBuildDefensiveBarracks()) {
            Direction dir;
            if (enemies.length > 0) dir = myLoc.directionTo(enemies[0].getLocation());
            else dir = utils.getRandomDir();
            Direction spawnedBarracks = sc.spawn(dir, UnitType.BARRACKS);
            if (spawnedBarracks != null) {
                firstBarracks = false;
                rc.bookResources(-myBookedResources);
                myBookedResources = 0;
                rc.setBarracksLocation(myLoc.add(spawnedBarracks));
            } else {
                if (myBookedResources == 0) {
                    rc.bookResources(GameConstants.BARRACKS_COST);
                    myBookedResources += GameConstants.BARRACKS_COST;
                }
            }
            return true;
        } else {
            rc.bookResources(-myBookedResources);
            myBookedResources = 0;
            return false;
        }
    }

    private boolean spawnWorkers(WorkerBuildController wbc) {
        //uc.println("Workers: " + wbc.getNWorkerToBuild());
        if (ratioChopped == null) return false;
        if (ratioChopped < 0.83) return false;
        for (int i = 0; i < wbc.getNWorkerToBuild(); i++) {
            Integer myResources = utils.getAvailableResources(round, rc);
            if (myResources < UnitType.WORKER.cost) break;
            sc.spawn(utils.getRandomDir(), UnitType.WORKER);
        }
        return wbc.getNWorkerToBuild() > 0;
    }

    private boolean spawnSmalls(WorkerBuildController wbc) {
        //uc.println("Smalls: " + wbc.getNSmallToBuild());
        /*if (ratioChopped == null) return false;
        if (wbc.getNSmallToBuild() > 0 || (ratioChopped < 0.83)) {*/
        if (wbc.getNSmallToBuild() > 0 || (wbc.getNWorkerToBuild() == 0 && !wbc.shouldBuildDefensiveBarracks())) {
            Integer myResources = utils.getAvailableResources(round, rc);
            if (myResources >= GameConstants.SMALL_TREE_COST) sc.spawnTree(utils.getRandomDir());
            return true;
        }
        return false;
    }
}
