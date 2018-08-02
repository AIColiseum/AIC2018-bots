package caterpie;

import aic2018.*;

public class Warrior extends Troop {
    @Override
    protected void InitTurn() {
        comm.Increment(comm.WARRIORS_COUNT_CHANNEL);
        super.InitTurn();
    }


    double TileValue(Location location) {
        int distToEnemy = minDistToEnemy(location);
        double dps = dpsReceived(location);
        boolean canAttack = canAttackEnemy(location);
        double value = 0;
        boolean moreAllies = moreAllies(myLoc);//this is correct
        value += Math.sqrt(distToEnemy) * (moreAllies ? -1 : 1);
        value += canAttack ? 10 : 0;
        value -= dps * (moreAllies ? 1 : 3);
        return value;
    }
}
