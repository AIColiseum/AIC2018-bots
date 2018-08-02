package caterpie;

import aic2018.Location;

public class Ballista extends Troop {
    //todo this

    @Override
    protected void InitTurn() {
        comm.Increment(comm.BALLISTAS_COUNT_CHANNEL);
        super.InitTurn();
    }

    @Override
    double TileValue(Location location) {
        return 0;
    }
}
