package caterpie;

import aic2018.*;

import java.util.Set;
import java.util.TreeSet;

public abstract class Unit {
    //todo buy vps
    //todo exceptions
    //todo methods in lowercase
    boolean first;
    UnitController uc;
    Communication comm;

    Team myTeam;
    Team enemyTeam;

    UnitInfo[] myUnits;
    UnitInfo[] enemyUnits;

    Location[] waters;
    VictoryPointsInfo[] victoryPoints;
    TreeInfo[] trees;
    TileInfo[][] tiles;
    int TILE_ARRAY_CENTER = 6;

    Location myLoc;
    UnitType myType;

    int xBase;
    int yBase;

    Set<Integer> readEnemyLocations;

    int round = 0;
    int myId;

    int initialMessageSpawningWorkers = 0;
    int initialMessageSpawningBarracks = 0;
    int initialMessageSpawningWarriors = 0;
    int initialMessageSpawningArchers = 0;
    int initialMessageSpawningKnights = 0;
    int initialMessageSpawningBallistas = 0;

    protected void InitGame(UnitController _uc) {
        uc = _uc;
        readEnemyLocations = new TreeSet<>();
        myId = uc.getInfo().getID();
        comm = new Communication();
        xBase = uc.read(comm.BASE_X_CHANNEL);
        yBase = uc.read(comm.BASE_Y_CHANNEL);
        comm.InitGame(uc, xBase, yBase);
        myTeam = uc.getTeam();
        enemyTeam = uc.getOpponent();
        myType = uc.getType();
    }

    void CheckMapEdges() {
        if (uc.read(comm.MAP_EDGES_FOUND) >= 4) return; //I have already found the edges
        int squaresAway = (int) Math.floor(Math.sqrt(myType.sightRangeSquared));
        if (uc.read(comm.MIN_X_CHANNEL) == Integer.MIN_VALUE) {
            Location loc = myLoc.add(-squaresAway, 0);
            if (uc.isOutOfMap(loc)) {
                while (uc.isOutOfMap(loc)) loc = loc.add(Direction.EAST);
                //loc is the first location inside the map
                uc.write(comm.MIN_X_CHANNEL, loc.x);
                comm.Increment(comm.MAP_EDGES_FOUND);
            }
        }
        if (uc.read(comm.MAX_X_CHANNEL) == Integer.MAX_VALUE) {
            Location loc = myLoc.add(squaresAway, 0);
            if (uc.isOutOfMap(loc)) {
                while (uc.isOutOfMap(loc)) loc = loc.add(Direction.WEST);
                //loc is the first location inside the map
                uc.write(comm.MAX_X_CHANNEL, loc.x);
                comm.Increment(comm.MAP_EDGES_FOUND);
            }
        }
        if (uc.read(comm.MIN_Y_CHANNEL) == Integer.MIN_VALUE) {
            Location loc = myLoc.add(0, -squaresAway);
            if (uc.isOutOfMap(loc)) {
                while (uc.isOutOfMap(loc)) loc = loc.add(Direction.NORTH);
                //loc is the first location inside the map
                uc.write(comm.MIN_Y_CHANNEL, loc.y);
                comm.Increment(comm.MAP_EDGES_FOUND);
            }
        }
        if (uc.read(comm.MAX_Y_CHANNEL) == Integer.MAX_VALUE) {
            Location loc = myLoc.add(0, squaresAway);
            if (uc.isOutOfMap(loc)) {
                while (uc.isOutOfMap(loc)) loc = loc.add(Direction.SOUTH);
                //loc is the first location inside the map
                uc.write(comm.MAX_Y_CHANNEL, loc.y);
                comm.Increment(comm.MAP_EDGES_FOUND);
            }
        }
    }

    private int UpdateUnitCount(int lastRoundChannel, int countChannel, int spawningChannel, int baseSpawningCyclicChannel, int initialMessage) {
        uc.write(lastRoundChannel, uc.read(countChannel));
        uc.write(countChannel, 0);
        int lastMessage = uc.read(baseSpawningCyclicChannel + comm.CYCLIC_CHANNEL_LENGTH);
        int i = initialMessage;
        int MAX_BYTECODE = 800;
        int initBytecode = uc.getEnergyUsed();
        while (i != lastMessage) {
            if (i >= comm.CYCLIC_CHANNEL_LENGTH) i -= comm.CYCLIC_CHANNEL_LENGTH; //this should go at the end
            if (i == lastMessage) break;
            if (uc.getEnergyUsed() - initBytecode > MAX_BYTECODE) break;
            CyclicMessage message = comm.ReadCyclicMessage(baseSpawningCyclicChannel + i);
            int spawningRound = message.value;
            if (spawningRound < round) initialMessage++;
            i++;
        }
        int spawningUnits = lastMessage - initialMessage;
        if (spawningUnits < 0) spawningUnits += comm.CYCLIC_CHANNEL_LENGTH;
        uc.write(spawningChannel, spawningUnits);
        return initialMessage;
    }
    private void UpdateUnitCount() {
        initialMessageSpawningWorkers = UpdateUnitCount(comm.WORKERS_LAST_ROUND_CHANNEL, comm.WORKERS_COUNT_CHANNEL, comm.WORKERS_SPAWNING_COUNT_CHANNEL, comm.SPAWNING_WORKERS_CHANNEL, initialMessageSpawningWorkers);
        initialMessageSpawningBarracks = UpdateUnitCount(comm.BARRACKS_LAST_ROUND_CHANNEL, comm.BARRACKS_COUNT_CHANNEL, comm.BARRACKS_SPAWNING_COUNT_CHANNEL, comm.SPAWNING_BARRACKS_CHANNEL, initialMessageSpawningBarracks);
        initialMessageSpawningWarriors = UpdateUnitCount(comm.WARRIORS_LAST_ROUND_CHANNEL, comm.WARRIORS_COUNT_CHANNEL, comm.WARRIORS_SPAWNING_COUNT_CHANNEL, comm.SPAWNING_WARRIORS_CHANNEL, initialMessageSpawningWarriors);
        initialMessageSpawningArchers = UpdateUnitCount(comm.ARCHERS_LAST_ROUND_CHANNEL, comm.ARCHERS_COUNT_CHANNEL, comm.ARCHERS_SPAWNING_COUNT_CHANNEL, comm.SPAWNING_ARCHERS_CHANNEL, initialMessageSpawningArchers);
        initialMessageSpawningKnights = UpdateUnitCount(comm.KNIGHTS_LAST_ROUND_CHANNEL, comm.KNIGHTS_COUNT_CHANNEL, comm.KNIGHTS_SPAWNING_COUNT_CHANNEL, comm.SPAWNING_KNIGHTS_CHANNEL, initialMessageSpawningKnights);
        initialMessageSpawningBallistas = UpdateUnitCount(comm.BALLISTAS_LAST_ROUND_CHANNEL, comm.BALLISTAS_COUNT_CHANNEL, comm.BALLISTAS_SPAWNING_COUNT_CHANNEL, comm.SPAWNING_BALLISTAS_CHANNEL, initialMessageSpawningBallistas);
    }

    private void updateTiles() {
        victoryPoints = uc.senseVPs();
        trees = uc.senseTrees();
        waters = uc.senseWater();
        myLoc = uc.getLocation();

        //max vision range is 48, that's 6 tiles in every direction
        int array_center = TILE_ARRAY_CENTER;
        tiles = new TileInfo[2 * array_center + 1][2 * array_center + 1];
        for (TreeInfo tree: trees)
            tiles[array_center + tree.location.x - myLoc.x][array_center + tree.location.y - myLoc.y] = new TileInfo().Add(tree);
        for (Location water: waters)
            tiles[array_center + water.x - myLoc.x][array_center + water.y - myLoc.y] = new TileInfo().Add(true);
        for (UnitInfo unit: myUnits)
            tiles[array_center + unit.getLocation().x - myLoc.x][array_center + unit.getLocation().y - myLoc.y] = new TileInfo().Add(unit);
        for (UnitInfo unit: enemyUnits)
            tiles[array_center + unit.getLocation().x - myLoc.x][array_center + unit.getLocation().y - myLoc.y] = new TileInfo().Add(unit);
    }

    protected void InitTurn() {
        round = uc.getRound();
        myUnits = uc.senseUnits(myTeam);
        enemyUnits = uc.senseUnits(enemyTeam);
        updateTiles();
        int round2 = uc.read(comm.ROUND_NUM_CHANNEL);
        if (round2 != round) {
            //first unit this round
            first = true;
            uc.write(comm.ROUND_NUM_CHANNEL, round);
            if (round % 10 == 0) uc.println("Round " + round);
            UpdateUnitCount();
        }
        CheckMapEdges();
    }



    protected abstract void ExecuteTurn();

    protected void SendMessages() {
        for (UnitInfo enemy: enemyUnits) {
            if (!Utils.IsTroop(enemy.getType())) continue;
            if (!readEnemyLocations.contains(Utils.EncodeLocation(enemy.getLocation()))) {
                comm.SendCyclicMessage(comm.ENEMY_TROOP_CHANNEL, enemy.getType().ordinal(), enemy.getLocation(), enemy.getHealth());
            }
        }
    }

    public void run(UnitController _uc) {
        InitGame(_uc);
        while (true){
//            if (round > 800) return;
            InitTurn();
            ExecuteTurn();
            SendMessages();
            uc.yield();
        }
    }

}
