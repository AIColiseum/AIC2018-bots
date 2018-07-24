package Grassmann.utils;

import aic2018.*;

import java.util.*;

public class WorkerBuildController {
    UnitController uc;
    Utils utils;

    public Integer nWorkers, nSmall, nAllies, nEnemies, nEnemyWorkers, aCloseBarracks;
    public Double resourcesOaks;

    public WorkerBuildController(UnitController uc, Utils utils, UnitInfo[] allies, UnitInfo[] enemies,
                                 TreeInfo[] trees, Integer nTrees, List<Location> barracks) {
        this.uc = uc;
        this.utils = utils;

        nWorkers = 1;
        resourcesOaks = 0.0;
        nSmall = 0;
        nAllies = 0;
        nEnemies = 0;
        nEnemyWorkers = 0;
        aCloseBarracks = 0;
        for (UnitInfo unit : allies) {
            UnitType type = unit.getType();
            if (type == UnitType.WORKER) nWorkers++;
            else if (utils.isCombat(type)) nAllies++;
        }
        for (UnitInfo unit : enemies) {
            UnitType type = unit.getType();
            if (type == UnitType.WORKER) nEnemyWorkers++;
            else if (utils.isCombat(type)) nEnemies++;
        }
        for (TreeInfo tree : trees) {
            if (tree.isOak()) resourcesOaks += (int)tree.getHealth();
            else nSmall++;
        }
        resourcesOaks *= (double)nTrees / (double)trees.length;
        for (Location loc : barracks) {
            Location myLoc = uc.getLocation();
            // 24 the squared distance that allows the checkConnectedLine
            if (myLoc.distanceSquared(loc) <= 24 && utils.checkConnectedLine(myLoc, loc)) {
                aCloseBarracks++;
            }
        }
    }

    public boolean shouldBuildDefensiveBarracks() {
        return nEnemies + nEnemyWorkers / 5 > nAllies + aCloseBarracks * 2;
    }

    public Integer getNWorkerToBuild() {
        // 24 damage, 25 turns to recover investment
        return Math.max(0, (int)(resourcesOaks / (24 * 50)) + nSmall / 6 - nWorkers);
    }

    public Integer getNSmallToBuild() {
        // 24 damage, 25 turns to recover investment
        return Math.max(0, nWorkers - (int)(resourcesOaks / (24 * 50)) - nSmall / 6);
    }

}
