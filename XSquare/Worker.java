package XSquare;

import aic2018.*;

/**
 * Worker class: we go in depth in the run func.
 */
public class Worker {

    final int MIN_TARGET_VALUE = 4;
    Bugpath bugPath;
    UnitController uc;
    BasicCombatUnit basicCombatUnit;

    Direction[] dirs = Direction.values();

    Target target = null;

    boolean barracks = false;

    boolean settled = false;

    boolean full = false;

    Location settleLoc = null;

    int lastMessageRead;

    int lastMessageReadTree;

    ConstructionQueue queue;

    boolean oakHunter;

    void run(UnitController _uc){
        uc = _uc;
        basicCombatUnit = new BasicCombatUnit(uc);
        bugPath = new Bugpath(uc, basicCombatUnit.mes);
        lastMessageRead = uc.read(basicCombatUnit.mes.MAX_CYCLE_LOC);
        lastMessageReadTree = uc.read(basicCombatUnit.mes.MAX_CYCLE_TREES);
        queue = new ConstructionQueue(uc, basicCombatUnit.mes);
        queue.initializeQueue();
        oakHunter = true;
        //ID + 10000 stores if this unit should be an 'oak Hunter' or a 'farmer'
        if (uc.read(uc.getInfo().getID()+10000) == 1) oakHunter = false;


        while (true) {
            boolean choppedOak = tryChop();

            //update info about close units and broadcasts
            basicCombatUnit.getBestTarget();

            /*
            If there is enough 'free wood' (oaks) I clone myself regardless of what the queue says
            Else, if we need farmers I swap my role.
            */

            if (tryClone() == -1 && oakHunter) oakHunter = isOakHunter();


            /*
            If I'm a farmer (or there are none) I spawn barracks/workers if the queue says so
            */

            if (!oakHunter || basicCombatUnit.mes.getTotalSettled() == 0){
                if (basicCombatUnit.barrackCount == 0 && queue.canBuildBarracks()){
                    if (tryBuildBarracks()) queue.add(-3);
                }
                if (queue.canBuildWorker()) if (tryBuildWorker()) queue.add(-2);
            }

            /*
            If ran with prints, I draw a nice picture about where I settled and 'my area'
            */

            if (settleLoc != null){
                for (Direction dir : dirs){
                    Location loc = settleLoc.add(dir);
                    if (basicCombatUnit.mes.cm.isMyCell(loc)) uc.drawPoint(loc, "red");
                }
                uc.drawPoint(settleLoc, "blue");
            }


            //case oak Hunter
            if (oakHunter){
                //go to closest Oak, else go bother the enemy and kill their trees *evil*
                if (!goChop()){
                    bugPath.fightMove();
                    if (uc.canMove()) bugPath.moveTo(uc.getOpponent().getInitialLocations()[0]);
                }
            //case farmer
            } else {
                //try finding a good spot for farming
                if (!settled) trySettle();
                if (settled){
                    //if already settled and there are no nasty oaks in my area try building small trees
                    if (!choppedOak) {
                        if (queue.canBuildSmall()){
                            if(tryBuildSmall()) queue.add(-1);
                        }
                        //keep moving inside my zone so I can chop all trees without hurting myself
                        moveInsideMyArea();
                    }
                    //If I have or not room for more trees
                    if (!full && checkfull()) full = true;
                }
            }

            //Report whatever I'm spawning and how many small trees I have
            reportUnits();


            //Report which is my status (full, settled, hunter, etc)
            basicCombatUnit.mes.putStatus(oakHunter);
            basicCombatUnit.mes.putSettled(!oakHunter && !choppedOak && (!settled || !full));
            basicCombatUnit.mes.putFull(settled);

            //try attacking enemy units (idk if I ever get here)
            basicCombatUnit.tryAttack();

            //report visible oaks and places for farming
            if (oakHunter) basicCombatUnit.mes.putVisibleTrees();
            basicCombatUnit.mes.putVisibleLocations();

            //buy VP and update latest messages read.
            lastMessageReadTree = uc.read(basicCombatUnit.mes.MAX_CYCLE_TREES);
            lastMessageRead = uc.read(basicCombatUnit.mes.MAX_CYCLE_LOC);
            basicCombatUnit.mes.buyVPs();
            uc.yield();
        }
    }

    //I WOULD NOT SUGGEST READING THE HORRIBLY WRITTEN CODE FROM HERE ON. PROCEED AT YOUR OWN RISK

    void trySettle(){
        getTarget();
        if (uc.canMove()) {
            bugPath.fightMove();
            if (target != null && target.loc != null && target.loc.distanceSquared(uc.getLocation()) > 0)
                bugPath.moveTo(target.loc);
            if (target != null && target.value >= MIN_TARGET_VALUE && uc.getLocation().distanceSquared(target.loc) == 0) {
                settled = true;
                settleLoc = uc.getLocation();
                basicCombatUnit.mes.cm.occupyCell(settleLoc);
            }
        }
    }

    void moveInsideMyArea(){
        if (!uc.canMove()) return;
        int randomIndex = (int) (Math.random() * 8);
        for (int i = 0; i < 8; ++i) {
            Direction dir = dirs[(randomIndex + i) % 8];
            Location newLoc = uc.getLocation().add(dir);
            if (uc.canMove(dir)) {
                if (basicCombatUnit.mes.cm.isMyCell(newLoc)) {
                    uc.move(dir);
                    return;
                }
            }
        }
    }

    void reportUnits(){
        Location myLoc = settleLoc;
        if (myLoc == null) myLoc = uc.getLocation();
        for (int i = 0; i < 9; ++i) {
            Location loc = myLoc.add(dirs[i]);
            UnitInfo unit = uc.senseUnit(loc);
            if (unit != null && unit.getType() == UnitType.WORKER && unit.getConstructionTurns() > 0) {
                int channel = unit.getID()+10000;
                if (uc.read(channel) == 1) basicCombatUnit.mes.putSettled(true);
                else basicCombatUnit.mes.putStatus(true);
            }
            if (!basicCombatUnit.mes.cm.isMyCell(loc)) continue;
            TreeInfo tree = uc.senseTree(loc);
            if (tree != null && tree.isSmall()) basicCombatUnit.mes.putSmall(tree);
        }
    }

    boolean tryBuildSmall(){
        for (int i = 0; i < 8; ++i) {
            Direction dir = dirs[i];
            Location newLoc = uc.getLocation().add(dir);
            if (basicCombatUnit.mes.cm.isMyCell(newLoc)) {
                UnitInfo unit = uc.senseUnit(newLoc);
                if (unit != null && unit.getType() == UnitType.BARRACKS) continue;
                if (uc.canUseActiveAbility(newLoc)) {
                    uc.useActiveAbility(newLoc);
                    return true;
                }
            }
        }
        return false;
    }

    boolean tryBuildBarracks(){
        for (int i = 0; i < 8; ++i) {
            if (planted(i)) continue;
            Location loc = uc.getLocation().add(dirs[i]);
            if (basicCombatUnit.mes.cm.isOccupied(loc)) continue;
            if (uc.canSpawn(dirs[i], UnitType.BARRACKS)) {
                uc.spawn(dirs[i], UnitType.BARRACKS);
                queue.getNextElement();
                return true;
            }
        }
        return false;
    }

    boolean tryBuildWorker() {
        if (!shouldBeMeWhoBuilds()) return false;
        for (int i = 0; i < 8; ++i) {
            if (uc.canSpawn(dirs[i], UnitType.WORKER)) {
                uc.spawn(dirs[i], UnitType.WORKER);
                UnitInfo unit = uc.senseUnit(uc.getLocation().add(dirs[i]));
                if (unit != null) {
                    uc.write(unit.getID() + 10000, 1);
                }
                //uc.println("BUILT A WORKER!! :D");
                return true;
            }
        }
        return false;
    }

    boolean[] putGoodDirections(){
        boolean[] ans = new boolean[9];
        Location myLoc = uc.getLocation();
        TreeInfo[] closeTrees = uc.senseTrees(8);
        for (TreeInfo tree: closeTrees){
            if (tree.isSmall()) continue;
            int dx = tree.location.x - myLoc.x;
            int dy = tree.location.y - myLoc.y;
            if (dx == 2){
                if (dy == 2){
                    ans[7] = true;
                } else if (dy == 1){
                    ans[7] = true;
                    ans[6] = true;
                } else if (dy == 0){
                    ans[7] = true;
                    ans[6] = true;
                    ans[5] = true;
                } else if (dy == -1){
                    ans[6] = true;
                    ans[5] = true;
                } else{
                    ans[5] = true;
                }
            } else if (dx == 1){
                if (dy == 2){
                    ans[0] = true;
                    ans[7] = true;
                } else if (dy == 1){
                    ans[0] = true;
                    ans[6] = true;
                    ans[8] = true;
                } else if (dy == 0){
                    ans[0] = true;
                    ans[7] = true;
                    ans[5] = true;
                    ans[4] = true;
                    ans[8] = true;
                } else if (dy == -1){
                    ans[6] = true;
                    ans[4] = true;
                    ans[8] = true;
                } else{
                    ans[5] = true;
                    ans[4] = true;
                }
            } else if (dx == 0){
                if (dy == 2){
                    ans[1] = true;
                    ans[0] = true;
                    ans[7] = true;
                } else if (dy == 1){
                    ans[2] = true;
                    ans[1] = true;
                    ans[7] = true;
                    ans[6] = true;
                    ans[8] = true;
                } else if (dy == -1){
                    ans[2] = true;
                    ans[3] = true;
                    ans[5] = true;
                    ans[6] = true;
                    ans[8] = true;
                } else{
                    ans[3] = true;
                    ans[4] = true;
                    ans[5] = true;
                }
            } else if (dx == -1){
                if (dy == 2){
                    ans[0] = true;
                    ans[1] = true;
                } else if (dy == 1){
                    ans[0] = true;
                    ans[2] = true;
                    ans[8] = true;
                } else if (dy == 0){
                    ans[0] = true;
                    ans[1] = true;
                    ans[3] = true;
                    ans[4] = true;
                    ans[8] = true;
                } else if (dy == -1){
                    ans[2] = true;
                    ans[4] = true;
                    ans[8] = true;
                } else{
                    ans[3] = true;
                    ans[4] = true;
                }
            } else{
                if (dy == 2){
                    ans[1] = true;
                } else if (dy == 1){
                    ans[1] = true;
                    ans[2] = true;
                } else if (dy == 0){
                    ans[1] = true;
                    ans[2] = true;
                    ans[3] = true;
                } else if (dy == -1){
                    ans[2] = true;
                    ans[3] = true;
                } else{
                    ans[3] = true;
                }
            }
        }
        return ans;
    }

    boolean goChop(){
        //uc.println(uc.getEnergyUsed());
        TreeInfo[] trees = uc.senseTrees();
        Location target = null, myLoc = uc.getLocation();
        int maxHealth = 0;
        int minDist = 8;

        boolean[] goodDirs = putGoodDirections();
        for (int i = 0; i < 9; ++i){
            if (goodDirs[i]) uc.drawPoint(uc.getLocation().add(dirs[i]), "red");
        }

        for (TreeInfo tree : trees){
            if (uc.getEnergyUsed() > 12000) break;
            if (!tree.isOak()) continue;
            if (!canReach(tree.location)) continue;
            int d = Math.max(8, myLoc.distanceSquared(tree.location));
            if (target == null){
                target = tree.location;
                minDist = d;
                maxHealth = tree.health;
                continue;
            }
            if (d > minDist) break;
            if (maxHealth < tree.health){
                target = tree.location;
                maxHealth = tree.health;
                //minDist = d;
            }
        }

        if (target == null) target = getClosestTree();

        if (target != null && myLoc.distanceSquared(target) < 150){

            Direction dir = myLoc.directionTo(target);
            int index = dir.ordinal();
            for (int i = 0; i < 8; ++i){
                int newIndex = (index + i)%8;
                if (uc.canMove(dirs[newIndex]) && goodDirs[newIndex]){
                    uc.move(dirs[newIndex]);
                    break;
                }
                dir = dir.rotateRight();
            }
            if (goodDirs[8]) uc.move(Direction.ZERO);

            /*
            if (myLoc.distanceSquared(target) <= 2){
                Direction dir = myLoc.directionTo(target);
                Direction dirR = dir.rotateRight(), dirL = dir.rotateLeft();
                int tryRightFirst = (int)(Math.random()*2);
                if (tryRightFirst == 1) {
                    if (uc.canMove(dirR)) uc.move(dirR);
                    if (uc.canMove(dirL)) uc.move(dirL);
                }
                else{
                    if (uc.canMove(dirL)) uc.move(dirL);
                    if (uc.canMove(dirR)) uc.move(dirR);
                }
                return true;
            }*/
            if (uc.canMove()){
                bugPath.fightMove();
                bugPath.moveTo(target);
            }
            return true;
        }
        return false;
    }

    int tryClone(){
        if (uc.getResources() < GameConstants.WORKER_COST) return 0;
        Direction ans = null;
        int totalWood = 0;

        boolean firstTimer = uc.getRound() < 10;

        if (basicCombatUnit.enemyNearby) return 0;
        //if (basicCombatUnit.workers > 5) return 0;
        if (basicCombatUnit.mes.getOakHunters() > 50) return -1;

        int time = 4500;
        if (uc.getRound() < 100) time = 9000;
        if (oakHunter) time = 12000;

        int q1 = 300 + 2*uc.getRound();
        int q2 = 500 + 2*uc.getRound();

        boolean[] spawns = new boolean[8];

        for (int i = 0; i < 8; ++i) spawns[i] = uc.canSpawn(dirs[i], UnitType.WORKER);

        Location myLoc = uc.getLocation();

        TreeInfo[] trees = uc.senseTrees();
        for (TreeInfo tree : trees){
            if (tree.isSmall()) continue;
            if (uc.getEnergyUsed() > time) break;
            Location treeLoc = tree.getLocation();
            if (!canReach(treeLoc)) continue;
            totalWood += (tree.health - 1)/(GameConstants.OAK_CHOPPING_DMG-1);
            if (ans == null){
                int index = myLoc.directionTo(tree.getLocation()).ordinal();
                if (spawns[index]){
                    Location newLoc = myLoc.add(dirs[index]);
                    if (firstTimer || newLoc.distanceSquared(tree.getLocation()) <= 2) ans = dirs[index];
                }
            }
            if (ans == null){
                int index = (myLoc.directionTo(tree.getLocation()).ordinal()+1)%8;
                if (spawns[index]){
                    Location newLoc = myLoc.add(dirs[index]);
                    if (firstTimer || newLoc.distanceSquared(tree.getLocation()) <= 2) ans = dirs[index];
                }
            }
            if (ans == null){
                int index = (myLoc.directionTo(tree.getLocation()).ordinal()+7)%8;
                if (spawns[index]){
                    Location newLoc = myLoc.add(dirs[index]);
                    if (firstTimer || newLoc.distanceSquared(tree.getLocation()) <= 2) ans = dirs[index];
                }
            }
        }

        totalWood*= GameConstants.OAK_CHOPPING_WOOD;

        int a = 0;
        if (oakHunter) ++a;
        totalWood/= (basicCombatUnit.workers + 1 + a);

        if (totalWood <= q1 || (!firstTimer && totalWood <= q2)) return -1;

        if (ans == null) return 0;

        //uc.println("Trying to spawn!!!");
        uc.spawn(ans, UnitType.WORKER);
        return 1;
    }

    boolean canReach(Location loc){
        Location myLoc = uc.getLocation();
        for (int i = 0; i < 4; ++i){
            if (myLoc.isEqual(loc)) return true;
            Direction dir = myLoc.directionTo(loc);
            Location newLoc = myLoc.add(dir);
            if (notWater(newLoc)){
                myLoc = newLoc;
                continue;
            }
            Direction dirR = dir.rotateRight();
            newLoc = myLoc.add(dirR);
            if (notWater(newLoc)){
                myLoc = newLoc;
                continue;
            }
            Direction dirL = dir.rotateLeft();
            newLoc = myLoc.add(dirL);
            if (notWater(newLoc)){
                myLoc = newLoc;
                continue;
            }
            return false;
        }
        return false;
    }

    boolean notWater(Location newLoc){
        if (!uc.canSenseLocation(newLoc) || uc.isOutOfMap(newLoc)) return false;
        return !uc.senseWaterAtLocation(newLoc);
    }

    boolean isOakHunter(){
        return (basicCombatUnit.mes.isOakHunter());
    }

    boolean checkfull(){
        int ans = 0;
        for (int i = 0; i < 9; ++i){
            Location loc = settleLoc.add(dirs[i]);
            if (!basicCombatUnit.mes.cm.isMyCell(loc)) continue;
            if (!uc.canSenseLocation(loc)) ++ans;
            if (uc.isOutOfMap(loc)) continue;
            if (uc.senseWaterAtLocation(loc)) continue;
            UnitInfo unit = uc.senseUnit(loc);
            if (unit != null && unit.getType() == UnitType.BARRACKS) continue;
            if (uc.senseTree(loc) == null) ++ans;
            int minFree = 2;
            if (uc.getRound() < 200) minFree = 1;
            if (ans >= minFree) return false;
        }
        return true;
    }

    boolean planted (int i){
        Location newLoc = uc.getLocation().add(dirs[i]);
        TreeInfo tree = uc.senseTree(newLoc);
        if (tree != null && !tree.oak) return true;
        return false;
    }


    boolean tryChop(){
        if (!uc.canAttack()) return false;
        int bestIndex = -1;
        double maxDurability = 0;
        int bestIndexOak = -1;
        double minDurability = 10000000;
        TreeInfo[] rocks = uc.senseTrees(uc.getType().getAttackRangeSquared());
        int totalWood = 0;
        for (int i = 0; i < rocks.length; ++i) {
            UnitInfo unit = uc.senseUnit(rocks[i].location);
            if (unit != null && unit.getTeam().equals(uc.getTeam())) continue;
            if(!uc.canAttack(rocks[i])) continue;
            if (rocks[i].stillGrowing()) continue;
            boolean oak = rocks[i].isOak();
            if (oak){
                totalWood += (rocks[i].getHealth()-1)/(GameConstants.OAK_CHOPPING_DMG-1);
                if (rocks[i].getHealth() < minDurability && rocks[i].getHealth() > 0){
                    minDurability = rocks[i].getHealth();
                    bestIndexOak = i;
                }
                continue;
            }
            if (rocks[i].getHealth() > maxDurability && rocks[i].getHealth() > 0){
                maxDurability = rocks[i].getHealth();
                bestIndex = i;
            }
        }
        if (bestIndexOak >= 0){
            uc.attack(rocks[bestIndexOak]);
            return totalWood >= 5;
        }
        else if (bestIndex >= 0 && rocks[bestIndex].health > 1 + GameConstants.SMALL_TREE_CHOPPING_DMG) uc.attack(rocks[bestIndex]);
        return false;
    }

    int freeCells(Location initialLoc){
        int ans = 0;
        int nosense = 0;
        for (int i = 0; i < 9; ++i){
            Location loc = initialLoc.add(dirs[i]);
            if (!uc.canSenseLocation(loc)){
                nosense++;
                continue;
            }
            if (uc.isOutOfMap(loc)) continue;
            if (!uc.senseWaterAtLocation(loc)) ++ans;
        }
        if (ans > 6) ans = 6;
        if (nosense > 6 - ans) nosense = 6-ans;
        return 2*ans + nosense;
    }

    void goToPosition(){
        Location locTarget = null;
        int numOpen = 0;
        Location[] visibleCells = uc.getVisibleLocations();
        for (Location loc : visibleCells){
            if (uc.getEnergyLeft() < 4000) break;
            if (uc.hasObstacle(loc)) continue;
            int free = freeCells(loc);
            if (free > numOpen){
                locTarget = loc;
                numOpen = free;
            }
            if (numOpen >= 6) break;
        }
        this.target = new Target(locTarget, basicCombatUnit.mes.LOC, numOpen, basicCombatUnit.mes);
    }

    boolean compare(int newVal, int newDist, int oldVal, int oldDist){
        if (newVal > oldVal) return true;
        if (newVal < oldVal) return false;
        return (newDist < oldDist);
    }

    boolean compare2(int newVal, int newDist, int oldVal, int oldDist){
        if (basicCombatUnit.mes.getSmalls() < 50) {
            if (newVal > oldVal) return true;
            if (newVal < oldVal) return false;
            return (newDist < oldDist);
        }
        return 20*newVal + oldDist > 20*oldVal + newDist;
    }

    void getTarget() {
        //uc.println("searching target!!");
        Messaging mes = basicCombatUnit.mes;
        if (target != null){
            target.value = basicCombatUnit.mes.cm.getValue(target.loc);
        }

        if (uc.getRound() <= 5) {
            goToPosition();
            return;
        } else {
            Location[] visibleLocations = uc.getVisibleLocations();
            for (Location loc : visibleLocations) {
                if (uc.getEnergyUsed() > 9000) break;
                Target target2 = new Target(loc, mes.LOC, mes.cm.getValue(loc), mes);
                if (target == null || target.value < MIN_TARGET_VALUE) {
                    target = target2;
                    continue;
                }
                int d = uc.getLocation().distanceSquared(loc);
                if (compare2(target2.value, d, target.value, uc.getLocation().distanceSquared(target.loc)))
                    target = target2;
                if (target != null && target.value >= 12) return;
            }
        }

        while (lastMessageRead != uc.read(mes.MAX_CYCLE_LOC) && uc.getEnergyUsed() < 11500) {
            Target target2 = mes.getTarget(uc.read(mes.FIRST_MESSAGE_LOC + lastMessageRead));
            int d = target2.loc.distanceSquared(uc.getLocation());
            if (d <= 16 && uc.getRound() > 5) {
                ++lastMessageRead;
                if (lastMessageRead + mes.FIRST_MESSAGE_LOC >= mes.MAX_CYCLE_LOC) lastMessageRead = 0;
                continue;
            }

            if (target == null || target.value < MIN_TARGET_VALUE) {
                target = target2;
                ++lastMessageRead;
                if (lastMessageRead + mes.FIRST_MESSAGE_LOC >= mes.MAX_CYCLE_LOC) lastMessageRead = 0;
                continue;
            }

            if (compare2(target2.value, d, target.value, uc.getLocation().distanceSquared(target.loc)))
                target = target2;

            ++lastMessageRead;
            if (lastMessageRead + mes.FIRST_MESSAGE_LOC >= mes.MAX_CYCLE_LOC) lastMessageRead = 0;
        }
        lastMessageRead = uc.read(mes.MAX_CYCLE_LOC);
        if (target != null && target.value < MIN_TARGET_VALUE) target = null;
        //if (target != null){
            //uc.drawPoint(target.loc, "white");
        //}
    }

    Location getClosestTree() {
        Messaging mes = basicCombatUnit.mes;
        Location ans = null;
        while (lastMessageReadTree != uc.read(mes.MAX_CYCLE_TREES) && uc.getEnergyUsed() < 12000) {
            Target target2 = mes.getTarget(uc.read(mes.FIRST_MESSAGE_TREES + lastMessageReadTree));
            int d = target2.loc.distanceSquared(uc.getLocation());
            if (ans == null) {
                ans = target2.loc;
                ++lastMessageReadTree;
                if (lastMessageReadTree + mes.FIRST_MESSAGE_TREES >= mes.MAX_CYCLE_TREES) lastMessageReadTree = 0;
                continue;
            }

            if (ans.distanceSquared(uc.getLocation()) > d) ans = target2.loc;

            ++lastMessageReadTree;
            if (lastMessageReadTree + mes.FIRST_MESSAGE_TREES >= mes.MAX_CYCLE_TREES) lastMessageReadTree = 0;
        }
        return ans;
    }

    boolean shouldBeMeWhoBuilds(){

        Messaging mes = basicCombatUnit.mes;

        if (uc.getRound() - uc.read(basicCombatUnit.mes.WORKER_READY) > 2) return true;

        Target target = null;

        Location[] visibleLocations = uc.getVisibleLocations();
        for (Location loc : visibleLocations) {
            if (uc.getEnergyUsed() > 6500) break;
            if(!canReach(loc)) continue;
            Target target2 = new Target(loc, mes.LOC, mes.cm.getValue(loc), mes);
            if (target == null || target.value < MIN_TARGET_VALUE) {
                target = target2;
                continue;
            }
            int d = uc.getLocation().distanceSquared(loc);
            if (compare(target2.value, d, target.value, uc.getLocation().distanceSquared(target.loc)))
                target = target2;
            if (target != null && target.value >= 12) return true;
        }

        while (lastMessageRead != uc.read(mes.MAX_CYCLE_LOC) && uc.getEnergyUsed() < 9000) {
            Target target2 = mes.getTarget(uc.read(mes.FIRST_MESSAGE_LOC + lastMessageRead));
            int d = target2.loc.distanceSquared(uc.getLocation());
            if (d <= 16 && uc.getRound() > 5) {
                ++lastMessageRead;
                if (lastMessageRead + mes.FIRST_MESSAGE_LOC >= mes.MAX_CYCLE_LOC) lastMessageRead = 0;
                continue;
            }

            if (target == null || target.value < MIN_TARGET_VALUE) {
                target = target2;
                ++lastMessageRead;
                if (lastMessageRead + mes.FIRST_MESSAGE_LOC >= mes.MAX_CYCLE_LOC) lastMessageRead = 0;
                continue;
            }

            if (compare(target2.value, d, target.value, uc.getLocation().distanceSquared(target.loc)))
                return false;

            ++lastMessageRead;
            if (lastMessageRead + mes.FIRST_MESSAGE_LOC >= mes.MAX_CYCLE_LOC) lastMessageRead = 0;
        }
        if (target != null && target.value >= MIN_TARGET_VALUE) return true;
        return false;
    }


}
