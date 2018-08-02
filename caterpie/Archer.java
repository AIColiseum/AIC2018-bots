package caterpie;

import aic2018.*;

public class Archer extends Troop {
    //todo ferlos cagats

    @Override
    protected void InitTurn() {
        comm.Increment(comm.ARCHERS_COUNT_CHANNEL);
        super.InitTurn();
    }

    @Override
    double TileValue(Location location) {
        int distToEnemy = minDistToEnemy(location);
        double dps = dpsReceived(location);
        boolean canAttack = canAttackEnemy(location);
        double value = 0;
        value -= Math.sqrt(distToEnemy);
        value += canAttack ? 10 : 0;
        value -= dps * 3;
        if (dps == 0) value += 5;
        return value;
    }
}
