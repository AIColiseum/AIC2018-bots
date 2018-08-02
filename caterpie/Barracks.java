package caterpie;

import aic2018.*;

public class Barracks extends Unit {
    private UnitType unitToBuild;

    protected void InitTurn() {
        comm.Increment(comm.BARRACKS_COUNT_CHANNEL);
        super.InitTurn();
    }


    private UnitType PickTroop(int ntroops) {
        if (ntroops % 4 == 0 && ntroops > 10) {
            return UnitType.ARCHER;
        } else {
            return UnitType.WARRIOR;
        }
    }

    //todo do a proper queue
    private void PickNextUnit() {
        int nworkers = comm.GetUnitCount(UnitType.WORKER);
        int nwarriors = comm.GetUnitCount(UnitType.WARRIOR);
        int narchers = comm.GetUnitCount(UnitType.ARCHER);
        int nknights = comm.GetUnitCount(UnitType.KNIGHT);
        int nballistas = comm.GetUnitCount(UnitType.BALLISTA);
        int ntroops = nwarriors + narchers + nknights + nballistas;
//        uc.println(ntroops + " troops, " + nworkers + " workers");
        if (nworkers == 0 || uc.getResources() > 750) {
            unitToBuild = PickTroop(ntroops);
        }else if (nworkers < 2 * ntroops + 2) {
            //We need a worker, so we don't pick any troop
//            uc.println("Building worker");
            unitToBuild = null;
        } else unitToBuild = PickTroop(ntroops);
    }

    private void BuildTroop() {
        if (unitToBuild == null) return;
        for (Direction dir: Direction.values()) {
            if (uc.canSpawn(dir, unitToBuild)) {
                int channel = -1;
                if (unitToBuild == UnitType.WARRIOR) channel = comm.SPAWNING_WARRIORS_CHANNEL;
                else if (unitToBuild == UnitType.ARCHER) channel = comm.SPAWNING_ARCHERS_CHANNEL;
                else if (unitToBuild == UnitType.KNIGHT) channel = comm.SPAWNING_KNIGHTS_CHANNEL;
                else if (unitToBuild == UnitType.BALLISTA) channel = comm.SPAWNING_BALLISTAS_CHANNEL;
                comm.SendCyclicMessage(channel, myType.ordinal(), myLoc, round + GameConstants.CONSTRUCTION_TURNS);
                uc.spawn(dir, unitToBuild);
                return;
            }
        }
    }

    @Override
    protected void ExecuteTurn() {
        PickNextUnit();
        BuildTroop();
    }

}
