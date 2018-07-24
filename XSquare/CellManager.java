package XSquare;

import aic2018.*;

/**
 * Encodes all information of a given cell in the communication array, basically we store if
 * a worker owns the cell, if there is water or it is out of map, if it has been discovered yet,
 * and which is the latest round in which a worker has claimed this cell for itself (it it is not close
 * to the current round then the worker is probably dead and the cell is free again).
 */

//Bits for each cell follow the following pattern
//id | owned | water/outofmap | discovered | round
//#bits of the int: 17 | 1 | 1 | 1 | 12

public class CellManager {

    Messaging mes;
    UnitController uc;
    int ID;
    Direction[] dirs;

    CellManager (Messaging mes, UnitController uc, int ID){
        this.mes = mes;
        this.uc = uc;
        this.ID = ID;
        dirs = Direction.values();
    }


    void updateStandard(Location loc){
        int channel = mes.encodeLocation(loc);
        int code = uc.read(channel);
        int owd = (code >> 12)&7;
        if (owd > 0) return;
        int water = 0;
        if (uc.isOutOfMap(loc)) water = 1;
        else if (uc.senseWaterAtLocation(loc)) water = 1;
        code = ((water << 1) | 1) << 12;
        uc.write(channel,code);
    }

    void updateWorker(Location loc){
        int channel = mes.encodeLocation(loc);
        int code = uc.read(channel);
        int wd = (code >> 12)&3;
        code = (((((ID << 1) | 1) << 2) | wd) << 12) | uc.getRound();
        uc.write(channel,code);
    }

    void updateCells(){
        Location[] locs = uc.getVisibleLocations();
        boolean worker = uc.getType() == UnitType.WORKER;
        for (Location loc : locs){
            if (uc.getEnergyLeft() < 3000) break;
            if (worker && isMyCell(loc)){
                updateWorker(loc);
            }
            else updateStandard(loc);
        }
        for (Location loc : locs){
            if (uc.getEnergyLeft() < 1200) break;
            int val = getValue(loc);
            if (val > 1) mes.putLocation(loc, val);
        }
    }

    boolean isMyCell(Location loc){
        int channel = mes.encodeLocation(loc);
        int code = uc.read(channel);
        int oc = (code >> 14)&1;
        if (oc > 0){
            return ((code >> 15)&0xFFFF) == ID;
        }
        return false;
    }

    boolean isOnHisCell(UnitInfo unit){
        Location loc = unit.getLocation();
        int channel = mes.encodeLocation(loc);
        int code = uc.read(channel);
        int oc = (code >> 14)&1;
        if (oc > 0){
            return ((code >> 15)&0xFFFF) == unit.getID();
        }
        return false;
    }

    int getValue(Location loc){
        //int initialEnergy = uc.getEnergyUsed();
        int goodCells = 0;
        int undiscoveredCells = 0;
        int round = uc.getRound();
        if (uc.canSenseLocation(loc) && !uc.isOutOfMap(loc)){
            UnitInfo unit = uc.senseUnit(loc);
            if (unit != null && unit.getType() == UnitType.BARRACKS && unit.getTeam() == uc.getTeam()) return 0;
        }
        for (int i = 8; i >= 0; --i){
            Location newloc = loc.add(dirs[i]);
            int channel = mes.encodeLocation(newloc);
            int code = uc.read(channel);
            int o = (code >> 14)&1;
            if (o > 0){
                int rd = code&0xFFF;
                if (round - rd <= 5) {
                    if (i == 8) return 0;
                    continue;
                }
            }
            int d = (code >> 12)&1;
            if (d == 0) ++undiscoveredCells;
            else{
                int w = (code >> 13)&1;
                if (w == 0) ++goodCells;
                else if (i == 8) return 0;
            }
        }
        if (goodCells > 6) goodCells = 6;
        if (undiscoveredCells > (6 - goodCells)) undiscoveredCells = 6 - goodCells;
        //uc.println("Energy spent: " + (uc.getEnergyUsed() - initialEnergy));
        return 2*goodCells + undiscoveredCells;
    }

    boolean claimCell(Location loc){
        int channel = mes.encodeLocation(loc);
        int code = uc.read(channel);
        int d = (code >> 12)&1;
        if (d == 0){
            updateStandard(loc);
            code = uc.read(channel);
        }
        int o = (code >> 14)&1, w = (code >> 13)&1;
        if (w > 0) return false;
        if (o > 0) {
            if (uc.getRound() - (code & 0xFFF) <= 5) return false;
        }
        code = (((((ID << 1) | 1) << 2) | 1) << 12) | uc.getRound();
        uc.write(channel,code);
        return true;
    }


    void occupyCell(Location loc){
        int claimedCells = 0;
        for (int i = 8; i >= 0 && claimedCells < 6; --i){
            if (claimCell(loc.add(dirs[i]))) ++claimedCells;
        }
    }


    boolean isOccupied (Location loc){
        int channel = mes.encodeLocation(loc);
        int code = uc.read(channel);
        if (((code >> 14)&1) > 0){
            if (uc.getRound() - (code & 0xFFF) <= 5) return true;
        }
        return false;
    }

}
