package Grassmann.utils;

import aic2018.*;
import java.util.*;

public class MessageControllerOld {

    //GameConstants.TEAM_ARRAY_SIZE;
    final private int BARRACKS_SLOTS	= 4;
    final private int E_BARRACKS_COUNT	= 12;
    final private int E_WORKER_COUNT	= 13;
    final private int E_WARRIOR_COUNT	= 14;
    final private int E_ARCHER_COUNT	= 15;
    final private int E_KNIGHT_COUNT	= 16;
    final private int E_BALLISTA_COUNT	= 16;
    final private int A_BARRACKS_COUNT	= 17;
    final private int A_WORKER_COUNT	= 18;
    final private int A_WARRIOR_COUNT	= 19;
    final private int A_ARCHER_COUNT	= 20;
    final private int A_KNIGHT_COUNT	= 21;
    final private int A_BALLISTA_COUNT	= 21;

    private UnitController uc;

    public MessageControllerOld(UnitController _uc) {
        uc = _uc;
    }

    public void setEnemyQueenLocation(int id, Location loc) {
        int i = 0;
        while (i < BARRACKS_SLOTS * 3 && uc.read(i) != id) i += 3;
        if (i == BARRACKS_SLOTS * 3) i = 0;
        uc.write(i, id);
        uc.write(i + 1, loc.x);
        uc.write(i + 2, loc.y);
    }

    public Location[] getEnemyQueensLocations() {
        List<Location> locs = new ArrayList<Location>();
        for (int i = 0; i < BARRACKS_SLOTS * 3; i += 3) {
            if (uc.read(i) != 0) locs.add(new Location(uc.read(i + 1), uc.read(i + 2)));
        }
        return locs.toArray(new Location[locs.size()]);
    }

    public void incEnemy(UnitType type) {
        if (type == UnitType.BARRACKS) uc.write(E_BARRACKS_COUNT, uc.read(E_BARRACKS_COUNT) + 1);
        else if (type == UnitType.WORKER) uc.write(E_WORKER_COUNT, uc.read(E_WORKER_COUNT) + 1);
        else if (type == UnitType.WARRIOR) uc.write(E_WARRIOR_COUNT, uc.read(E_WARRIOR_COUNT) + 1);
        else if (type == UnitType.ARCHER) uc.write(E_ARCHER_COUNT, uc.read(E_ARCHER_COUNT) + 1);
        else if (type == UnitType.KNIGHT) uc.write(E_KNIGHT_COUNT, uc.read(E_KNIGHT_COUNT) + 1);
        else if (type == UnitType.BALLISTA) uc.write(E_BALLISTA_COUNT, uc.read(E_BALLISTA_COUNT) + 1);
    }

    public void incAlly(UnitType type) {
        if (type == UnitType.BARRACKS) uc.write(A_BARRACKS_COUNT, uc.read(A_BARRACKS_COUNT) + 1);
        else if (type == UnitType.WORKER) uc.write(A_WORKER_COUNT, uc.read(A_WORKER_COUNT) + 1);
        else if (type == UnitType.WARRIOR) uc.write(A_WARRIOR_COUNT, uc.read(A_WARRIOR_COUNT) + 1);
        else if (type == UnitType.ARCHER) uc.write(A_ARCHER_COUNT, uc.read(A_ARCHER_COUNT) + 1);
        else if (type == UnitType.KNIGHT) uc.write(A_KNIGHT_COUNT, uc.read(A_KNIGHT_COUNT) + 1);
        else if (type == UnitType.BALLISTA) uc.write(A_BALLISTA_COUNT, uc.read(A_BALLISTA_COUNT) + 1);
    }

    public int countEnemy(UnitType type) {
        if (type == UnitType.BARRACKS) return uc.read(E_BARRACKS_COUNT);
        else if (type == UnitType.WORKER) return uc.read(E_WORKER_COUNT);
        else if (type == UnitType.WARRIOR) return uc.read(E_WARRIOR_COUNT);
        else if (type == UnitType.ARCHER) return uc.read(E_ARCHER_COUNT);
        else if (type == UnitType.KNIGHT) return uc.read(E_KNIGHT_COUNT);
        else if (type == UnitType.BALLISTA) return uc.read(E_BALLISTA_COUNT);
        return 0;
    }

    public int countAlly(UnitType type) {
        if (type == UnitType.BARRACKS) return uc.read(A_BARRACKS_COUNT);
        else if (type == UnitType.WORKER) return uc.read(A_WORKER_COUNT);
        else if (type == UnitType.WARRIOR) return uc.read(A_WARRIOR_COUNT);
        else if (type == UnitType.ARCHER) return uc.read(A_ARCHER_COUNT);
        else if (type == UnitType.KNIGHT) return uc.read(A_KNIGHT_COUNT);
        else if (type == UnitType.BALLISTA) return uc.read(A_BALLISTA_COUNT);
        return 0;
    }
}
