package caterpie;


import aic2018.*;

public abstract class Attacker extends Unit {
    Travel travel;
    protected Location target;
    boolean inCombat;

    @Override
    protected void InitGame(UnitController _uc) {
        super.InitGame(_uc);
        travel = new Travel();
        travel.InitGame(uc);
        inCombat = false;
    }

    int TypePoints(UnitType type) {
        if (type == UnitType.BALLISTA) return 6;
        else if (type == UnitType.ARCHER) return 5;
        else if (type == UnitType.KNIGHT) return 4;
        else if (type == UnitType.WARRIOR) return 3;
        else if (type == UnitType.WORKER) return 2;
        else if (type == UnitType.BARRACKS) return 1;
        else return 0;
    }

    //second ring: distance = 4 or 5
    boolean canGoToSecondRingLocation(Location location) {
        Location diff = location.add(-myLoc.x, -myLoc.y);
        Location[] adjacentLocations = Utils.getSecondRingAdjacentLocations(diff);
        for (Location loc: adjacentLocations) {
            Location offsetLoc = loc.add(TILE_ARRAY_CENTER, TILE_ARRAY_CENTER);
            TileInfo tile = tiles[offsetLoc.x][offsetLoc.y];
            if (tile == null || !tile.water) return true;
        }
        return false;
    }

    abstract void UpdateMoveTarget();

    void GatherVPs() {
//        uc.println("hola");
        for (VictoryPointsInfo info: victoryPoints) {
            if (myLoc.distanceSquared(info.location) > 2) return;
            Direction dir = myLoc.directionTo(info.location);
            if (uc.canGatherVPs(dir)) {
//                uc.println("Gathers vps on dir "+ dir);
                uc.gatherVPs(dir);
            }
        }

    }

    abstract protected void Attack(boolean first);

    abstract void ExecuteSpecialMechanics();

    protected void ExecuteTurn() {
        Attack(true);
        GatherVPs();
        ExecuteSpecialMechanics();
        UpdateMoveTarget();
        Move();
        Attack(false);
        GatherVPs();
        ExecuteSpecialMechanics();
    }

    abstract void Move();

}
