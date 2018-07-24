package Grassmann.utils;

import aic2018.*;
import java.util.*;

public class Objectives {
    UnitController uc;
    List<List<Location>> objs;

    public Objectives(UnitController uc) {
        this.uc = uc;
        objs = new ArrayList<List<Location>>();
        for (int i = 0; i < 3; ++i) objs.add(new ArrayList<Location>());
    }

    public void handleBasicMilitarObjectives(MessageController rc) {
        Location loc = rc.getEnemyWorker();
        if (loc != null) {
            if (uc.canSenseLocation(loc) && uc.senseUnits(loc, 0, uc.getOpponent()).length == 0) {
                rc.clearEnemyWorker();
            }
            else add(loc, 1);
        }
        loc = rc.getEnemyMilitar();
        if (loc != null) {
            if (uc.canSenseLocation(loc) && uc.senseUnits(loc, 0, uc.getOpponent()).length == 0) {
                rc.clearEnemyMilitar();
            }
            else add(loc, 1);
        }
    }

    public Location get() {
        for (int i = 0; i < objs.size(); ++i) {
            if (objs.get(i).size() > 0) return objs.get(i).get(0);
        }
        return null;
    }

    public Location pop() {
        Location loc = null;
        for (int i = 0; i < objs.size(); ++i) {
            if (objs.get(i).size() > 0) {
                loc = objs.get(i).get(0);
                objs.get(i).remove(0);
                break;
            }
        }
        return loc;
    }

    public void add(Location loc, int priority) {
        objs.get(priority).add(loc);
    }

    public int count(int priority) { return objs.get(priority).size(); }

    public int count() {
        int ans = 0;
        for (int i = 0; i < objs.size(); i++) ans += objs.get(i).size();
        return ans;
    }

}
