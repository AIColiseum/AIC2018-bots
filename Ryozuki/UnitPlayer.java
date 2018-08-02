package ryoplayer;

import aic2018.*;

public class UnitPlayer {
    private Team opponent;
    private Direction[] dirs = Direction.values();
    private int EnemyBaseIndex = 0;
    private int BaseIndex = 0;
    private UnitController uc;
    private boolean ReachedAllEnemyBases = false;
    private boolean SurroundedByTrees = false;
    private final int WorkerIndexOffset = 20;
    private int WorkerIndex = -1;
    private final int BARRACKS_AMOUNT = 0;
    private final int WORKER_AMOUNT = 1;
    private final int UNIT_FOUND = 2;
    private Location[] enemyInitLocations;
    private Location[] baseInitLocations;

    public void run(UnitController uc) {
        this.uc = uc;

        opponent = uc.getOpponent();
        enemyInitLocations = opponent.getInitialLocations();
        baseInitLocations = uc.getTeam().getInitialLocations();

        while (true) {
            /*
            TODO: Fix some worker getting out of energy?
            TODO: Check if spawnrate is ok, more units = easier to win
            TODO: Create barracks if enemy is found?
            TODO: Implement method to find path to enemy base.
            TODO: Maybe add a 50% to go rotateleft or rotateright before all, when using the cheap path finder.
            So units dont get stuck that much
             */

            if (uc.getType() == UnitType.WORKER) {
                WorkerLogic();
            }
            else if (uc.getType() == UnitType.BARRACKS){
                BarracksLogic();
            }
            else {
                OffensiveUnitLogic();
            }

            uc.yield();
        }
    }

    private void WorkerLogic() {
        int barrackNum = uc.read(BARRACKS_AMOUNT);
        int workerNum = uc.read(WORKER_AMOUNT);
        if(workerNum == 0)
            workerNum = 1;
        int maxWorkerNum = uc.getRound() / 40;

        if(WorkerIndex == -1)
            WorkerIndex = workerNum - 1;

        // buy vp if we have lot of wood.
        if(uc.getResources() > 10000) {
            float cost = 50 + uc.getRound() / 20;
            int amount = (int)Math.floor(uc.getResources() / cost) ;
            if(uc.canBuyVP(amount))
                uc.buyVP(amount);
        }

        if(uc.getRound() < 40) {
            for(int i = 0; i < 8; i++) {
                if(!uc.isAccessible(uc.getLocation().add(dirs[i]))) {
                    move(dirs[i].opposite());
                }
            }
        }

        // Attack nearby units
        UnitInfo[] enemyUnitsNear = uc.senseUnits(uc.getType().attackRangeSquared, opponent);
        for (UnitInfo enemyUnit : enemyUnitsNear) {
            if(enemyUnit.getType() != UnitType.WORKER && enemyUnit.getType() != UnitType.BARRACKS)
                uc.write(UNIT_FOUND, enemyUnit.getType().ordinal());
            if (uc.canAttack(enemyUnit)) {
                uc.attack(enemyUnit);
            }
        }

        // Move away from ally workers,
        // TODO: Optimize this, e.g it goes opposite direction, but the dir can be obstructed or outofmap
        UnitInfo[] units = uc.senseUnits(uc.getType().attackRangeSquared + 2, uc.getTeam());
        for (UnitInfo unit : units) {
            if (unit.getType() == UnitType.WORKER && unit.getID() != uc.getInfo().getID()) {
                Direction movDir = uc.getLocation().directionTo(unit.getLocation()).opposite();
                if (move(movDir)) {
                    break;
                } else {
                    for (int j = 0; j < 8; j++) {
                        move(dirs[j]);
                    }
                }
            }
        }

        // Spawn barracks
        if(uc.getResources() >= 500 && barrackNum < (uc.getRound() / 300) + 2) {
            barrackNum++;
            uc.write(BARRACKS_AMOUNT, barrackNum);
            spawnNear(UnitType.BARRACKS);
        }

        // If we got enough resources to keep producing more workers go ahead.
        if(uc.getResources() > 1000) {
            maxWorkerNum = 9999;
        }
        if((barrackNum > 0 || workerNum < 2) && uc.getResources() > 100
                && workerNum < maxWorkerNum && AllSurrounded(workerNum)) {
            for (int i = 0; i < 8; ++i) {
                if(uc.canSpawn(dirs[i], UnitType.WORKER)) {
                    workerNum++;
                    uc.write(WORKER_AMOUNT, workerNum);
                    uc.spawn(dirs[i], UnitType.WORKER);
                    break;
                }
            }
        }

        // Plant tree if you can
        if(uc.canUseActiveAbility()) {
            for (int i = 0; i < 8; ++i) {
                Location loc = uc.getLocation();
                loc.x += dirs[i].dx;
                loc.y += dirs[i].dy;
                tryPlantTree(uc.getResources(), loc);
            }
        }

        if(!SurroundedByTrees) {
            SurroundedByTrees = true;
            for(int i = 0; i < 8; i++) {
                Location loc = uc.getLocation().add(dirs[i]);
                if(!uc.isAccessible(loc) || uc.isOutOfMap(loc))
                    continue;
                boolean treeExists = uc.senseTree(uc.getLocation().add(dirs[i])) != null;
                if(!treeExists) {
                    SurroundedByTrees = false;
                    break;
                }
            }

            if(SurroundedByTrees)
                uc.write(WorkerIndexOffset + WorkerIndex, 1);
        }

        // Look for trees around and chop them.
        TreeInfo[] trees = uc.senseTrees();
        for (TreeInfo tree : trees) {
            if (uc.canAttack(tree) && tree.getHealth() > 12 && !tree.stillGrowing()
                    && uc.getLocation() != tree.getLocation()) {
                UnitInfo inf = uc.senseUnit(tree.getLocation());
                if (inf != null && inf.getTeam().isEqual(uc.getTeam())) continue;
                uc.attack(tree);
            }
        }

        if(uc.getRound() < 40)
            return;

        // If we didn't chop a tree, it means we don't have one near, so get closer.
        TreeInfo tree = nearestTree(trees);
        if(tree != null && uc.canAttack() && !isObstructed(uc.getLocation(), tree.getLocation())) {
            Direction dir = uc.getLocation().directionTo(tree.getLocation());
            move(dir);
            // TODO: Search for a unobstructed tree and save it on a variable
        }

        // If we still can move and don't have a tree nearby
        // go to enemy base, thus expanding trees.
        // TODO: Improve path finding.
        if(uc.canMove() && trees.length < 1) {
            Direction dirOp = uc.getLocation().directionTo(opponent.getInitialLocations()[0]);
            boolean moved = false;
            for(int i = 0; i < 8; i++) {
                if(!move(dirOp))
                    dirOp = dirOp.rotateRight();
                else {
                    moved = true;
                    break;
                }
            }

            // We cant move, dig under us if there is a tree or just suicide
            if(!uc.canMove() && uc.canAttack() && !moved)
                uc.attack(uc.getLocation());
        }

        // If points are nearby, collect them.
        VictoryPointsInfo[] points = uc.senseVPs();
        for (VictoryPointsInfo point : points) {
            Direction direP = uc.getLocation().directionTo(point.getLocation());
            if (uc.canGatherVPs(direP)) {
                uc.gatherVPs(direP);
            }
        }
    }

    private void BarracksLogic() {
        if((uc.getResources() > 500 || Math.random()*100 < 20)) {
            int ordinal = uc.read(UNIT_FOUND);
            if(ordinal == 0)
                ordinal = UnitType.KNIGHT.ordinal();
            UnitType type = UnitType.values()[ordinal];
            UnitType spawnType = UnitType.WARRIOR;

            // TODO: These counters are good?
            /*
                Warriors < Arquers
                Arquers < Cavallers
                Cavallers < Warriors
             */
            if(type == UnitType.ARCHER)
                spawnType = UnitType.KNIGHT;
            else if(type == UnitType.KNIGHT)
                spawnType = UnitType.WARRIOR;
            else if(type == UnitType.WARRIOR)
                spawnType = UnitType.ARCHER;
            else if(type == UnitType.BALLISTA)
                spawnType = UnitType.WARRIOR;
            for (int i = 0; i < 8; ++i) if (uc.canSpawn(dirs[i], spawnType)){
                uc.spawn(dirs[i], spawnType);
            }
        }
    }

    private void OffensiveUnitLogic() {
        UnitInfo[] enemies = uc.senseUnits(opponent);
        UnitInfo enemy = nearestEnemy(enemies);

        // Focus workers
        for (UnitInfo en: enemies) {
            if(en.getType() == UnitType.WORKER) {
                if(uc.canAttack(enemy))
                    enemy = en;
            } else {
                if(en.getType() != UnitType.BARRACKS)
                    uc.write(UNIT_FOUND, en.getType().ordinal());
                else // Focus barracks if no workers
                    enemy = en;
            }
        }

        if(enemy != null && (uc.getType() == UnitType.ARCHER)) {
            // If enemy is to close that we cant attack it, go away
            if(uc.getLocation().distanceSquared(enemy.getLocation()) < uc.getType().minAttackRangeSquared) {
                move(uc.getLocation().directionTo(enemy.getLocation()).opposite());
            }
        }

        if(enemy != null && uc.canAttack(enemy)) {
            // Use ability if you can.
            if(uc.canUseActiveAbility() && uc.canUseActiveAbility(enemy.getLocation()))
                uc.useActiveAbility(enemy.getLocation());

            if(uc.canAttack(enemy))
                uc.attack(enemy);
        }
        else {
            // TODO: Better path finding, and consider breaking or avoiding trees.
            TreeInfo[] trees = uc.senseTrees();
            for (TreeInfo tree : trees) {
                if (uc.canAttack(tree) && tree.isOak()) {
                    UnitInfo inf = uc.senseUnit(tree.getLocation());
                    if (inf != null && inf.getTeam().isEqual(uc.getTeam())) continue;
                    uc.attack(tree);
                }
            }

            if(!ReachedAllEnemyBases) {
                Direction dirOp = uc.getLocation().directionTo(enemyInitLocations[EnemyBaseIndex]);

                for(int i = 0; i < 8; i++) {
                    if(!move(dirOp))
                        dirOp = dirOp.rotateRight();
                    else
                        break;
                }

                if(uc.getLocation().distanceSquared(enemyInitLocations[EnemyBaseIndex]) <= 1) {
                    if(EnemyBaseIndex + 1 < enemyInitLocations.length)
                        EnemyBaseIndex++;
                    else {
                        ReachedAllEnemyBases = true;
                        EnemyBaseIndex = 0;
                    }
                }
            } else {
                // Go to all ally bases, cirular.
                Direction dirOp = uc.getLocation().directionTo(baseInitLocations[BaseIndex]);

                for(int i = 0; i < 8; i++) {
                    if(!move(dirOp))
                        dirOp = dirOp.rotateRight();
                    else
                        break;
                }

                if(uc.getLocation().distanceSquared(baseInitLocations[BaseIndex]) <= 1) {
                    if(BaseIndex + 1 < baseInitLocations.length)
                        BaseIndex++;
                    else {
                        BaseIndex = 0;
                        EnemyBaseIndex = 0;
                        ReachedAllEnemyBases = false;
                    }
                }
            }

            VictoryPointsInfo[] points = uc.senseVPs();
            VictoryPointsInfo point = nearestVp(points);
            if(point != null) {
                Direction dirVp = uc.getLocation().directionTo(point.location);
                if(ReachedAllEnemyBases)
                    uc.gatherVPs(dirVp);
                if(uc.getLocation().distanceSquared(point.location) <= 2 && uc.canGatherVPs(dirVp))
                    uc.gatherVPs(dirVp);
            } else {
                int dirIndex = (int) (Math.random() * 8);
                move(dirs[dirIndex]);
            }

        }
    }

    private boolean AllSurrounded(int workerNum) {
        for(int i = 0; i < workerNum; i++) {
            if(uc.read(WorkerIndexOffset + i) < 1)
                return false;
        }
        return  true;
    }

    private boolean isObstructed(Location loc1, Location loc2) {
        Direction dir = loc1.directionTo(loc2);
        while (!loc1.isEqual(loc2)) {
            loc1 = loc1.add(dir);
            if(uc.canSenseLocation(loc1) && !uc.isAccessible(loc1)) {
                return true;
            }
        }
        return false;
    }

    public boolean move(Direction dir) {
        if(uc.canMove(dir)) {
            uc.move(dir);
            return  true;
        }
        return false;
    }

    private int tryPlantTree(int resources, Location loc) {
        if(resources > 180 && uc.canUseActiveAbility() && uc.canUseActiveAbility(loc)) {
            uc.useActiveAbility(loc);
            return resources-180;
        }
        return resources;
    }

    private void spawnNear(UnitType type) {
        Direction[] dirs = Direction.values();
        for (int i = 0; i < 8; ++i) {
            if (uc.canSpawn(dirs[i], type)) {
                uc.spawn(dirs[i], type);
                return;
            }
        }
    }

    private TreeInfo nearestTree(TreeInfo[] trees) {
        if(trees.length < 1) return  null;
        Location loc = uc.getLocation();
        int nearestDist = -1;
        int index = -1;
        for (int i = 0; i < trees.length; i++) {

            int dist = loc.distanceSquared(trees[i].getLocation());

            if(trees[i].stillGrowing() || trees[i].health <= 12 || trees[i].getLocation() == uc.getLocation()) continue;

            if(nearestDist == -1 || dist < nearestDist) {
                nearestDist = dist;
                index = i;
            }
        }

        if(index != -1)
            return trees[index];
        else
            return null;
    }

    private VictoryPointsInfo nearestVp(VictoryPointsInfo[] points) {
        if(points.length < 1) return  null;
        Location loc = uc.getLocation();
        int nearestDist = -1;
        int index = -1;
        for (int i = 0; i < points.length; i++) {
            int dist = loc.distanceSquared(points[0].getLocation());

            if(nearestDist == -1 || dist < nearestDist) {
                nearestDist = dist;
                index = i;
            }
        }

        if(index != -1)
            return points[index];
        else
            return null;
    }

    private UnitInfo nearestEnemy(UnitInfo[] enemies) {
        if(enemies.length < 1) return null;
        Location loc = uc.getLocation();
        int nearestDist = -1;
        int index = -1;
        for (int i = 0; i < enemies.length; i++) {
            int dist = loc.distanceSquared(enemies[0].getLocation());

            if(nearestDist == -1 || dist < nearestDist) {
                nearestDist = dist;
                index = i;
            }
        }

        if(index != -1)
            return enemies[index];
        else
            return null;
    }
}
