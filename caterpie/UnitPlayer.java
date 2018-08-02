package caterpie;

import aic2018.*;

public class UnitPlayer {

    public void run(UnitController uc) {
        if (uc.getType() == UnitType.ARCHER){
            new Archer().run(uc);
        } else if (uc.getType() == UnitType.BALLISTA){
            new Ballista().run(uc);
        } else if (uc.getType() == UnitType.BARRACKS){
            new Barracks().run(uc);
        } else if (uc.getType() == UnitType.KNIGHT){
            new Knight().run(uc);
        } else if (uc.getType() == UnitType.WARRIOR){
            new Warrior().run(uc);
        } else if (uc.getType() == UnitType.WORKER){
            new Worker().run(uc);
        }
    }
}
