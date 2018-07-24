package XSquare;

import aic2018.*;

/**
 * Ugh, you probably don't want to read this class.
 * It manages almost everything related to writing and reading the comm array. (CellManager is a sub-instance)
 * There are basically 3 types of channels:
 * 1.- Static channels: just a fix channel (for example: a channel in which we write the latest round we've seen an enemy)
 * 2.- Cell channels: A somewhat 'static' channel, but which encodes information about a given cell of the map.
 *              Locations and ints are linked by the encodeLocation and getLocation (which is not used, lol) functions
 * 3.- Queue channels: An interval [A,B] of channels in which comm[B] stores the latest message sent and all channels from A to B-1
 *              are of a given type. Each player locally has the last message read, and just keeps reading until channel comm[B].
 *              When a player writes a new message, it is written in comm[comm[B]] and we update comm[B] to comm[B]+1 (we do it cyclically -
 *              normally with intervals of length 300).
 *              This is used when whatever we want to broadcast is 'dynamic' ,like enemies seen,
 *              oaks (they can be chopped etc), etc.
 */

public class Messaging {

    UnitController uc;
    int attackRangeExpanded = 5;

    final int maxBaseX = 100;
    final int maxBaseY = 100;

    int baseX;
    int baseY;

    boolean alert = false;

    Direction[] dirs;

    CellManager cm;

    public Messaging(UnitController _uc){
        uc = _uc;
        Location[] initLoc = uc.getTeam().getInitialLocations();
        baseX = initLoc[0].x;
        baseY = initLoc[0].y;
        attackRangeExpanded = uc.getType().getAttackRangeSquared() + 9;
        dirs = Direction.values();
        cm = new CellManager(this, uc, uc.getInfo().getID());
    }

    final int FIRST_MESSAGE_TREES = 1500;
    final int MAX_CYCLE_TREES = 1800;

    final int FIRST_MESSAGE_LOC = 6000;
    final int MAX_CYCLE_LOC = 6700;

    final int WORKER_READY = 6800;

    final int LATEST_WORKER_BUILT = 8000;

    final int MAX_CYCLE = 300;

    final int VP_CHANNEL = 5500;

    final int SMALL_COUNT = 3000;
    final int TREE_MEMORY = 20;

    final int ALERT_COUNT = 4500;

    final int FULL_COUNT = 4200;
    final int SETTLER_COUNT = 4000;
    final int WORKER_COUNT = 3500;
    final int WORKER_MEMORY = 30;

    final int FIRST_TREE_LOC_MESSAGE = 21000;

    final int FIRST_TROOP_MESSAGE = 500;
    final int FIRST_ENEMY_TROOP_MESSAGE = 1000;

    final int FIRST_QUEEN_BUSY_CHANNEL = 1500;

    final int NEXT_STEP_QUEEN = 2000;

    int TROOP_MEMORY = 20;

    final int BARRACKS = 0;
    final int WORKER = 1;
    final int WARRIOR = 2;
    final int KNIGHT = 3;
    final int ARCHER = 4;
    final int BALLISTA = 6;

    final int ENEMY = 0;
    final int TREE = 1;
    final int LOC = 2;

    int encode(int target_type, int x, int y, int extra){
        return ((((((target_type&0xF) << 8) | (x-baseX + maxBaseX)) << 8) | (y - baseY + maxBaseY)) << 12) | (extra&0xFFF);
    }

    Target getTarget(int code){
        Target ans = new Target();
        ans.type = (code >> 28)&0xF;

        ans.loc = new Location();
        ans.loc.x = (code >> 20)&0xFF;
        ans.loc.x += baseX - maxBaseX;
        ans.loc.y = (code >> 12)&0xFF;
        ans.loc.y += baseY - maxBaseY;

        ans.value = code&0xFFF;

        ans.mes = this;

        return ans;
    }

    void sendMessage(Integer mes){
        if (uc.getEnergyUsed() >= 14000) return;
        Integer latestMessage = uc.read(MAX_CYCLE);
        //uc.println("Sending message " + mes.toString() + " at potision " + latestMessage.toString());
        uc.write(latestMessage, mes);
        ++latestMessage;
        if (latestMessage >= MAX_CYCLE) latestMessage = 0;
        uc.write(MAX_CYCLE, latestMessage);
    }

    int encodeLocation(Location loc){
        return FIRST_TREE_LOC_MESSAGE + (((loc.x-baseX + maxBaseX) << 8) | (loc.y - baseY + maxBaseY));
    }

    Location getLocation(int code){
        Location loc = new Location();
        loc.y = (code&0xFF) + baseY - maxBaseY;
        loc.x = ((code >> 8)&0xFF) + baseX - maxBaseX;
        return loc;
    }


    void putVisibleTrees(){
        //return;
        if (uc.getType() != UnitType.WORKER) return;
        TreeInfo[] trees = uc.senseTrees();
        for (int i = 0; i < trees.length && uc.getEnergyUsed() < 13000; ++i){
            if (trees[i].oak) putTree(trees[i]);
        }
    }

    void buildWorker(){
        uc.write(LATEST_WORKER_BUILT, uc.getRound());
    }

    void putAdjacentSmall(){
        for (int i = 0; i < 8; ++i){
            Location loc = uc.getLocation().add(dirs[i]);
            if (uc.isOutOfMap(loc)) continue;
            UnitInfo unit = uc.senseUnit(loc);
            if (unit != null && unit.getType() == UnitType.WORKER && unit.getConstructionTurns() > 0) putStatus(true);
            TreeInfo tree = uc.senseTree(loc);
            if (tree != null && !tree.oak) putSmall(tree);
        }
        uc.write(SMALL_COUNT + (uc.getRound() + 1)%TREE_MEMORY, 0);
    }

    void putTree(TreeInfo f){
        int mes = encode(TREE, f.location.x, f.location.y, f.health);
        //if (uc.getEnergyUsed() >= 12500) return;
        Integer latestMessage = uc.read(MAX_CYCLE_TREES);
        uc.write(latestMessage + FIRST_MESSAGE_TREES, mes);
        ++latestMessage;
        if (latestMessage + FIRST_MESSAGE_TREES >= MAX_CYCLE_TREES) latestMessage = 0;
        uc.write(MAX_CYCLE_TREES, latestMessage);
    }

    void putSmall(TreeInfo f){
        /*for (int i = 0; i < 9; ++i){
            Location loc = f.location.add(dirs[i]);
            int channel = encodeLocation(loc);
            uc.write(channel, uc.getRound());
        }*/
        int channel = SMALL_COUNT + uc.getRound()%TREE_MEMORY;
        uc.write(channel, uc.read(channel)+1);
        uc.write(SMALL_COUNT + (uc.getRound() + 1)%TREE_MEMORY, 0);
    }

    int getSmalls(){
        return uc.read(SMALL_COUNT + (uc.getRound() + TREE_MEMORY - 1)%TREE_MEMORY);
    }

    boolean isOccupied(Location loc){
        return uc.getRound() - uc.read(encodeLocation(loc)) < 5;
    }

    void putStatus(boolean b){
        if (b){
            int channel = WORKER_COUNT + uc.getRound()%WORKER_MEMORY;
            uc.write(channel, uc.read(channel)+1);
            channel = WORKER_COUNT + uc.getRound()+(WORKER_MEMORY-1)%WORKER_MEMORY;
            uc.write(channel, uc.read(channel)+1);
        }
        uc.write(WORKER_COUNT + (uc.getRound()+3)%WORKER_MEMORY, 0);
    }

    void putFull(boolean b){
        if (b){
            int channel = FULL_COUNT + uc.getRound()%WORKER_MEMORY;
            uc.write(channel, uc.read(channel)+1);
            channel = FULL_COUNT + uc.getRound()+(WORKER_MEMORY-1)%WORKER_MEMORY;
            uc.write(channel, uc.read(channel)+1);
        }
        uc.write(FULL_COUNT + (uc.getRound()+3)%WORKER_MEMORY, 0);
    }

    int getTotalSettled(){
        int ans = uc.read(FULL_COUNT + (uc.getRound()+WORKER_MEMORY-1)%WORKER_MEMORY);
        return ans;
    }

    int getFreeWorkers(){
        int ans = uc.read(SETTLER_COUNT + (uc.getRound()+WORKER_MEMORY-1)%WORKER_MEMORY);
        return ans;
    }

    boolean isOakHunter(){
        int channel = SETTLER_COUNT + (uc.getRound()+WORKER_MEMORY-1)%WORKER_MEMORY;
        int ans = uc.read(channel);
        if (ans == 0){
            uc.write(channel, 1);
            //uc.println("Not an oakHunter anymore!");
            return false;
        }
        return true;
        //return uc.read(WORKER_COUNT + (uc.getRound()%WORKER_MEMORY)) + uc.read(SETTLER_COUNT + (uc.getRound()%WORKER_MEMORY)) > 0;
    }

    int getBarracksCount(){
        int r = 10*((uc.getRound() + TROOP_MEMORY-1)%TROOP_MEMORY);
        return uc.read(FIRST_TROOP_MESSAGE + r + BARRACKS);
    }

    int getBallistaCount(){
        int r = 10*((uc.getRound() + TROOP_MEMORY-1)%TROOP_MEMORY);
        return uc.read(FIRST_TROOP_MESSAGE + r + BALLISTA);
    }
    int getKnightCount(){
        int r = 10*((uc.getRound() + TROOP_MEMORY-1)%TROOP_MEMORY);
        return uc.read(FIRST_TROOP_MESSAGE + r + KNIGHT);
    }
    int getArcherCount(){
        int r = 10*((uc.getRound() + TROOP_MEMORY-1)%TROOP_MEMORY);
        return uc.read(FIRST_TROOP_MESSAGE + r + ARCHER);
    }

    int getWorkerCount(){
        int r = 10*((uc.getRound() + TROOP_MEMORY-1)%TROOP_MEMORY);
        return uc.read(FIRST_TROOP_MESSAGE + r + WORKER);
    }

    boolean shouldBuildWorker(){

        if (uc.getRound() < 2) return false;
        if (uc.getTeam().getVictoryPoints() >= 1600 && getWorkerCount() > 10){
            uc.write(WORKER_READY, uc.getRound());
            return false;
        }
        if (getFreeWorkers() < Math.max(1, 1 + ((getSmalls() - getTroopCount2()))/30)){
            return true;
        }
        //if (getFreeWorkers() == 0) return true;
        uc.write(WORKER_READY, uc.getRound());
        return false;
    }

    void putSettled(boolean b){
        if (b){
            int channel = SETTLER_COUNT + uc.getRound()%WORKER_MEMORY;
            uc.write(channel, uc.read(channel)+1);
            channel = SETTLER_COUNT + uc.getRound()+(WORKER_MEMORY-1)%WORKER_MEMORY;
            uc.write(channel, uc.read(channel)+1);
        }
        uc.write(SETTLER_COUNT + (uc.getRound()+3)%WORKER_MEMORY, 0);
    }

    int getSettledWorkers(){
        return uc.read(SETTLER_COUNT + (uc.getRound()+WORKER_MEMORY-1)%WORKER_MEMORY);
    }

    int getOakHunters(){
        return uc.read(WORKER_COUNT  + (uc.getRound()+WORKER_MEMORY-1)%WORKER_MEMORY);
    }

    int getTroopCount(){
        int ans = 0;
        int r = 10*((uc.getRound() + TROOP_MEMORY-1)%TROOP_MEMORY);
        r += FIRST_TROOP_MESSAGE;
        ans += uc.read(r + WARRIOR);
        ans += uc.read(r + ARCHER);
        ans += uc.read(r + KNIGHT);
        return ans;
    }

    int getTroopCount2(){
        int ans = 0;
        int r = 10*((uc.getRound() + TROOP_MEMORY-1)%TROOP_MEMORY);
        r += FIRST_TROOP_MESSAGE;
        ans += uc.read(r + WARRIOR);
        ans += uc.read(r + ARCHER);
        ans += uc.read(r + KNIGHT);
        ans += uc.read(r + BALLISTA);
        return ans;
    }



    boolean shouldBuildTroop(){
        if (getBarracksCount() == 0) return false;
        if (getTroopCount2() < 1){
            //uc.println("Number of troops: " + getTroopCount2());
            return true;
        }
        int production = 2*getSmalls() + 4*getOakHunters();
        if (getTroopCount2()*6 >  production) return false;
        if ((uc.getRound() - uc.read(ALERT_COUNT)) <= 1){
            //uc.println("Alert detected!");
            return true;
        }
        return false;
    }

    boolean shouldBuildBarracks(){

        boolean safe = (uc.getRound() <= 4 || (uc.getRound() - uc.read(ALERT_COUNT)) > 1);

        if (getBarracksCount() == 0 && (!safe || getSmalls() >= 12)) return true;

        int works = getWorkerCount()/5;
        if (safe) return false;

        return (getBarracksCount() < Math.max(getSmalls()/15, works));
    }

    void sendAlert(){
        uc.write(ALERT_COUNT, uc.getRound());
    }

    void buyVPs(){
        //if (uc.getRound() > 1) return;
        int minz = 3000;
        if (uc.getRound() > 400) minz = 600;
        int cost = uc.getVPCost();
        int vpsToBuy = (uc.getResources() - minz)/cost;
        if (vpsToBuy > 0){
            uc.buyVP(vpsToBuy);
            uc.write(VP_CHANNEL, uc.read(VP_CHANNEL) + vpsToBuy);
        }

        int leftVP = uc.getResources()/cost;
        if (uc.getTeam().getVictoryPoints() + leftVP >= GameConstants.VICTORY_POINTS_MILESTONE) uc.buyVP(leftVP);
    }

    void putLocation(Location loc, int val){
        int mes = encode(LOC, loc.x, loc.y, val);
        Integer latestMessage = uc.read(MAX_CYCLE_LOC);
        uc.write(latestMessage + FIRST_MESSAGE_LOC, mes);
        ++latestMessage;
        if (latestMessage + FIRST_MESSAGE_LOC >= MAX_CYCLE_LOC) latestMessage = 0;
        uc.write(MAX_CYCLE_LOC, latestMessage);
    }

    void putVisibleLocations(){
        cm.updateCells();
    }

}
