package XSquare;

import aic2018.GameConstants;
import aic2018.UnitController;
import aic2018.UnitType;

/**
 * Keeps track of what we should build next using the info of the communication array
 * If anyone wants to build something else it should wait until it has enough wood to build both
 * Magic numbers (srsly why are they negative? -> don't ask pls ;_; ):
 * Combat unit = 0
 * Small tree = -1
 * Worker (farmer) = -2
 * Barracks = -3
 */


public class ConstructionQueue {

    final int CHANNEL = 5000;
    UnitController uc;
    Messaging mes;

    public ConstructionQueue(UnitController uc, Messaging mes){
        this.uc = uc;
        this.mes = mes;
    }

    void initializeQueue(){
        if (uc.read(CHANNEL+1) == 0){
            uc.write(CHANNEL+1, 1);
            uc.write(CHANNEL, -1);
        }
    }



    int getNextElement(){
        check();
        return uc.read(CHANNEL);
    }

    void check(){
        if (mes.shouldBuildBarracks()) uc.write(CHANNEL,-3);
        else if (mes.shouldBuildWorker()) uc.write(CHANNEL, -2);
        else if (mes.shouldBuildTroop()) uc.write(CHANNEL, 0);
        else uc.write(CHANNEL, -1);
    }

    int getElement(){
        return uc.read(CHANNEL);
    }

    int getCost(){
        int unit = uc.read(CHANNEL);
        if (unit == -2) return UnitType.WORKER.cost;
        else if (unit == -1) return GameConstants.SMALL_TREE_COST;
        else if (unit == -3) return GameConstants.BARRACKS_COST;
        else if (unit == 0) return GameConstants.BALLISTA_COST;
        return 0;
    }

    boolean canBuildBarracks(){
        check();
        if (uc.read(CHANNEL) == -3) return true;
        else if (!mes.shouldBuildBarracks()) return false;
        return uc.getResources() >= getCost() + GameConstants.BARRACKS_COST;
    }

    boolean canBuildWorker(){
        check();
        if (uc.read(CHANNEL) == -2) return true;
        else if (!mes.shouldBuildWorker()) return false;
        return uc.getResources() >= getCost() + GameConstants.WORKER_COST;
    }

    boolean canBuildSmall(){
        check();
        if (uc.read(CHANNEL) == -1) return true;
        return uc.getResources() >= getCost() + GameConstants.SMALL_TREE_COST;
    }

    boolean canBuildTroop(){
        check();
        if (uc.read(CHANNEL) >= 0) return true;
        else if (!mes.shouldBuildTroop()) return false;
        return uc.getResources() >= getCost() + GameConstants.BALLISTA_COST;
    }

    void add (int x){
        if (uc.read(CHANNEL) == x) getNextElement();
    }
}
