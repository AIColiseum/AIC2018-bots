package caterpie;

import aic2018.*;

public class Worker extends Attacker {
    ZoneLocation myZone = null;
    boolean cuttingOaks = false;

    int[] spiralX = new int[]{0, -1, 1, 0, 0, -1, -1, 1, 1, -2, 2, -2, -2, 2, 2, -3, 0, 0, 3, -1, -1, 1, 1, -3, -3, 3, 3, -2, -2, 2, 2, -4, 4, -3, -3, 3, 3, -4, -4, 4, 4, 0, 0, -1, -1, 1, 1, -2, -2, 2, 2, -5, -4, -4, 4, 4, 5, -5, -5, 5, 5, -3, -3, 3, 3, -5, -5, 5, 5, -6, 0, 0, 6, -4, -4, 4, 4, -1, -1, 1, 1, -6, -6, 6, 6, -2, -2, 2, 2, -6, -6, -3, -3, 3, 3, 6, 6, -5, -5, 5, 5, -7, 7, -7, -7, 7, 7, -4, -4, 4, 4, -6, -6, 0, 0, 6, 6, -1, -1, 1, 1, -7, -7, 7, 7, -2, -2, 2, 2, -5, -5, 5, 5, -8, 8, -3, -3, 3, 3, -8, -8, 8, 8, -7, -7, 7, 7, -6, -6, 6, 6, -4, -4, 4, 4, -8, -8, 8, 8};
    int[] spiralY = new int[]{0, 0, 0, -1, 1, -1, 1, -1, 1, 0, 0, -1, 1, -1, 1, 0, -2, 2, 0, -2, 2, -2, 2, -1, 1, -1, 1, -2, 2, -2, 2, 0, 0, -2, 2, -2, 2, -1, 1, -1, 1, -3, 3, -3, 3, -3, 3, -3, 3, -3, 3, 0, -2, 2, -2, 2, 0, -1, 1, -1, 1, -3, 3, -3, 3, -2, 2, -2, 2, 0, -4, 4, 0, -3, 3, -3, 3, -4, 4, -4, 4, -1, 1, -1, 1, -4, 4, -4, 4, -2, 2, -4, 4, -4, 4, -2, 2, -3, 3, -3, 3, 0, 0, -1, 1, -1, 1, -4, 4, -4, 4, -3, 3, -5, 5, -3, 3, -5, 5, -5, 5, -2, 2, -2, 2, -5, 5, -5, 5, -4, 4, -4, 4, 0, 0, -5, 5, -5, 5, -1, 1, -1, 1, -3, 3, -3, 3, -4, 4, -4, 4, -5, 5, -5, 5, -2, 2, -2, 2};

    @Override
    protected void InitGame(UnitController _uc) {
        super.InitGame(_uc);
        myZone = null;
        myLoc = uc.getLocation();
        if (xBase == 0 && yBase == 0) {
            //I'm the first unit to move in the game. I initialize the base coordinates
            uc.write(comm.BASE_X_CHANNEL, myLoc.x);
            uc.write(comm.BASE_Y_CHANNEL, myLoc.y);
            uc.write(comm.MIN_X_CHANNEL, Integer.MIN_VALUE);
            uc.write(comm.MAX_X_CHANNEL, Integer.MAX_VALUE);
            uc.write(comm.MIN_Y_CHANNEL, Integer.MIN_VALUE);
            uc.write(comm.MAX_Y_CHANNEL, Integer.MAX_VALUE);
            xBase = myLoc.x;
            yBase = myLoc.y;
            comm.InitGame(uc, myLoc.x, myLoc.y);
            uc.println("Base location: " + Utils.PrintLoc(myLoc));
        }
    }

    @Override
    protected void InitTurn() {
        comm.Increment(comm.WORKERS_COUNT_CHANNEL);
        super.InitTurn();
    }

    ////////////////////////////////////////////////////

    @Override
    protected void SendMessages() {
        super.SendMessages();
        if (myZone != null) {
            comm.SendZoneAssignedMessage(myZone, uc.getRound());
        }
    }

    ////////////////////////////////////////////////////

    @Override
    void UpdateMoveTarget() {
        if (myZone != null) {
            Location center = myZone.GetCenter();
            if (comm.IsOutOfMap(center) || comm.ReadZoneMessage(myZone).type.isInaccessible()) {
//                uc.println("Reset zone because it's inaccessible");
                myZone = null;
            }
            if (uc.canSenseLocation(center) && uc.senseWaterAtLocation(center)){
//                uc.println("Reset zone because there's water in the center");
                comm.sendZoneInaccessibleMessage(myZone);
                myZone = null;
            }
        }
        if (!IsAtZone()) {
//            uc.println("===================== " + Utils.PrintLoc(myLoc) + " tries searching for a free zone.");
            ZoneLocation currentZone = ZoneLocation.fromLocation(myLoc, xBase, yBase);
//            uc.println("CurrentZone is " + Utils.PrintLoc(currentZone));
            int initBytecode = uc.getEnergyUsed();
            for (int i = 0; i < spiralX.length; i++) {
                if (uc.getEnergyUsed() > initBytecode + 5000) break;
                int dx = spiralX[i];
                int dy = spiralY[i];
                ZoneLocation newZone = new ZoneLocation(currentZone.x + dx, currentZone.y + dy, xBase, yBase);
//                uc.println("Tries zone " + Utils.PrintLoc(newZone) + " with center on " + Utils.PrintLoc(newZone.GetCenter()));
                if (comm.IsOutOfMap(newZone.GetCenter())) {
//                    uc.println("Zone " + Utils.PrintLoc(newZone) + " is out of map.");
                    continue;
                }
                if (comm.isZoneFree(newZone)) {
                    //I found a free zone, I assign it
                    myZone = newZone;
                    uc.println("Assigning zone " + Utils.PrintLoc(newZone));
                    return;
                }
//                else uc.println("Zone already assigned");
            }
        }
    }

    ////////////////////////////////////////////////////
    private boolean IsAtZone() {
        if (myZone == null) return false;
        Location center = myZone.GetCenter();
        return myLoc.distanceSquared(center) < 3;
    }

    private Location GetLocationWithNoTree() {
        if (myZone == null) return null;
        Location center = myZone.GetCenter();
        if (!uc.canSenseLocation(center)) return null;
        Location[] zoneTiles = myZone.GetZoneTiles();
        for (Location tile: zoneTiles) {
            if (uc.senseTree(tile) != null) continue;
            if (uc.senseWaterAtLocation(tile)) continue;
            if (uc.isOutOfMap(tile)) continue;
            UnitInfo unit = uc.senseUnit(tile);
            if (unit != null && unit.getType() == UnitType.BARRACKS) continue;
            return tile;
        }
        return null;
    }

    private void Plant() {
        if (!uc.canUseActiveAbility()) return;
        if (!IsAtZone()) return;
        if (cuttingOaks) return;
        Location plantLoc = GetLocationWithNoTree();
        if (plantLoc == null || !uc.canUseActiveAbility(plantLoc)) return;
        uc.useActiveAbility(plantLoc);
    }

    ////////////////////////////////////////////////////

    private Direction adjacentFreeZone() {
        if (myZone == null) return Direction.ZERO;
        for (Direction dir: new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (comm.isZoneFree(myZone.add(dir))) return dir;
        }
        return Direction.ZERO;
    }

    private void Duplicate() {
//        if (round > 490) uc.println(" total workers " + comm.GetUnitCount(UnitType.WORKER));
        if (!IsAtZone()) return;
        Direction adjacentFreeZoneDir = adjacentFreeZone();
        if (adjacentFreeZoneDir == Direction.ZERO) return;
        int nworkers = comm.GetUnitCount(UnitType.WORKER);
        if (nworkers > 20 && round < 500 || nworkers > 35 && round < 1000 || nworkers > 45) return;
        if (uc.getResources() > 200 && round > 10) {
            Direction[] spawnDirs = Utils.GetOrderedDirections(adjacentFreeZoneDir);
            for(Direction spawnDir: spawnDirs) {
                if (uc.canSpawn(spawnDir, UnitType.WORKER)) {
                    comm.SendCyclicMessage(comm.SPAWNING_WORKERS_CHANNEL, myType.ordinal(), myLoc, round + GameConstants.CONSTRUCTION_TURNS);
                    uc.spawn(spawnDir, UnitType.WORKER);
                    return;
                }
            }
        }
    }

    //todo fix placement
    private void BuildBarracks() {
        for (Direction dir: Direction.values()) {
            TreeInfo tree = uc.senseTree(myLoc.add(dir));
            if (tree != null && !tree.oak) continue;
            if (uc.canSpawn(dir, UnitType.BARRACKS)) {
                comm.SendCyclicMessage(comm.SPAWNING_BARRACKS_CHANNEL, myType.ordinal(), myLoc, round + GameConstants.CONSTRUCTION_TURNS);
                uc.spawn(dir, UnitType.BARRACKS);
                return;
            }
        }
        for (Direction dir: Direction.values()) {
            if (uc.canSpawn(dir, UnitType.BARRACKS)) {
                comm.SendCyclicMessage(comm.SPAWNING_BARRACKS_CHANNEL, myType.ordinal(), myLoc, round + GameConstants.CONSTRUCTION_TURNS);
                uc.spawn(dir, UnitType.BARRACKS);
                return;
            }
        }
    }

    private void Build() {
        int nworkers = comm.GetUnitCount(UnitType.WORKER);
        int nbarracks = comm.GetUnitCount(UnitType.BARRACKS);
//        uc.println(nbarracks + " barracks, " + nworkers + " workers");
        if (cuttingOaks && nworkers < 10) Duplicate();
        else if (nbarracks * 10 < nworkers + 1 || (uc.getResources() > 800 && nbarracks < 15)) {
            BuildBarracks();
        } else Duplicate();
    }

    ////////////////////////////////////////////////////

    double getTileScore(TileInfo tile) {
        if (tile == null) return 0;
        if (tile.water) return 0;

        TreeInfo tree = tile.tree;
        UnitInfo unit = tile.unit;

        if (unit != null) {
            if (unit.getTeam() == myTeam) return 0;
            return TypePoints(unit.getType()) * 1000 - unit.getHealth();
        }
        if (tree != null) {
            //Small trees always before oaks.
            //Small trees with more health go first
            //Oaks with less health go first
            if (tree.oak) return 50 - Math.log(tree.health);
            if (tree.health <= GameConstants.SMALL_TREE_CHOPPING_DMG) return 0;
            if (tree.stillGrowing()) return 0;
            return tree.health + 100;
        }
        return 0;
    }

    @Override
    protected void Attack(boolean first) {
        if (!uc.canAttack()) return;
        Location bestLocation = null;
        double bestScore = 0;
        //not really a location
        Location arrayCenter = new Location(TILE_ARRAY_CENTER, TILE_ARRAY_CENTER);
        for (Direction dir: Direction.values()) {
            Location adjLoc = arrayCenter.add(dir);
            TileInfo tile = tiles[adjLoc.x][adjLoc.y];
            double score = getTileScore(tile);
            if (score > bestScore) {
                bestScore = score;
                bestLocation = adjLoc;
            }
        }
        if (bestScore > 0) {
            Location realLocation = bestLocation.add(myLoc.x, myLoc.y).add(-TILE_ARRAY_CENTER, -TILE_ARRAY_CENTER);
            uc.attack(realLocation);
        }
    }

    ////////////////////////////////////////////////////

    @Override
    void ExecuteSpecialMechanics() {
        Plant();
        Build();
    }

    ////////////////////////////////////////////////////

    void MoveRandom() {
        Direction dir = Direction.values()[(int)(Math.random() * 8)];
        if (uc.canMove(dir)) uc.move(dir);
    }

    //if there's an accessible oak on the first or second ring
    Location findAccessibleCloseOak() {
        for (TreeInfo tree: trees) {
            if (myLoc.distanceSquared(tree.location) > 5) return null;
            if (!tree.oak) continue;
            if (myLoc.distanceSquared(tree.location) < 3) return tree.location;
            if (myLoc.distanceSquared(tree.location) > 3 && canGoToSecondRingLocation(tree.location)) return tree.location;
        }
        return null;
    }

    @Override
    protected void Move() {
        if (!uc.canMove()) return;
        Location closeOak = findAccessibleCloseOak();
        if (closeOak != null) {
//            uc.println("Found an oak at " + Utils.PrintLoc(closeOak));
            cuttingOaks = true;
            travel.TravelTo(closeOak, waters, trees, myUnits, enemyUnits);
        } else if (myZone != null && Utils.SameLocation(myLoc, myZone.GetCenter())) {
//            uc.println("I'm at center, moving random");
            cuttingOaks = false;
            MoveRandom();
        } else if (myZone != null) {
            cuttingOaks = false;
//            uc.println(Utils.PrintLoc(myLoc) + " travelling to my zone " + Utils.PrintLoc(myZone) + " with center on " + Utils.PrintLoc(myZone.GetCenter()));
            travel.TravelTo(myZone.GetCenter(), waters, trees, myUnits, enemyUnits, false);
        } else {
//            uc.println("rand move");
            cuttingOaks = false;
            MoveRandom();
        }
    }
}
