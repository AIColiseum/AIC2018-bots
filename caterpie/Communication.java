package caterpie;

import aic2018.*;

public class Communication {
    private UnitController uc;

    //Base location. Initial location of the first queen
    int xBase = Integer.MAX_VALUE;
    int yBase = Integer.MAX_VALUE;

    //Unitary channels
    int MAP_COORDS = 0;
    final int BASE_X_CHANNEL = MAP_COORDS++;
    final int BASE_Y_CHANNEL = MAP_COORDS++;
    final int MIN_X_CHANNEL = MAP_COORDS++; //last coords inside the map
    final int MAX_X_CHANNEL = MAP_COORDS++;
    final int MIN_Y_CHANNEL = MAP_COORDS++;
    final int MAX_Y_CHANNEL = MAP_COORDS++;
    final int MAP_EDGES_FOUND = MAP_COORDS++; //number of map edges that have been found.

    final int ROUND_NUM_CHANNEL = 10; //to know who is the first unit to execute

    /**
     * - [UNIT] last round channel: how many units I had last round of that type
     * - [UNIT] count channel: counting how many units I have this round
     * - [UNIT] spawning channel: how many units are spawning
     */
    int UNIT_COUNT = 20;
    final int WORKERS_LAST_ROUND_CHANNEL = UNIT_COUNT++;
    final int WORKERS_COUNT_CHANNEL = UNIT_COUNT++;
    final int WORKERS_SPAWNING_COUNT_CHANNEL = UNIT_COUNT++;
    final int BARRACKS_LAST_ROUND_CHANNEL = UNIT_COUNT++;
    final int BARRACKS_COUNT_CHANNEL = UNIT_COUNT++;
    final int BARRACKS_SPAWNING_COUNT_CHANNEL = UNIT_COUNT++;
    final int WARRIORS_LAST_ROUND_CHANNEL = UNIT_COUNT++;
    final int WARRIORS_COUNT_CHANNEL = UNIT_COUNT++;
    final int WARRIORS_SPAWNING_COUNT_CHANNEL = UNIT_COUNT++;
    final int ARCHERS_LAST_ROUND_CHANNEL = UNIT_COUNT++;
    final int ARCHERS_COUNT_CHANNEL = UNIT_COUNT++;
    final int ARCHERS_SPAWNING_COUNT_CHANNEL = UNIT_COUNT++;
    final int KNIGHTS_LAST_ROUND_CHANNEL = UNIT_COUNT++;
    final int KNIGHTS_COUNT_CHANNEL = UNIT_COUNT++;
    final int KNIGHTS_SPAWNING_COUNT_CHANNEL = UNIT_COUNT++;
    final int BALLISTAS_LAST_ROUND_CHANNEL = UNIT_COUNT++;
    final int BALLISTAS_COUNT_CHANNEL = UNIT_COUNT++;
    final int BALLISTAS_SPAWNING_COUNT_CHANNEL = UNIT_COUNT++;

    //Cyclic channels
    int CYCLIC_CHANNELS = 1000; //Cyclic channels start at 1100 because of how += works
    final int CYCLIC_CHANNEL_LENGTH = 99;
    final int ASSIGNED_ZONE_CHANNEL = CYCLIC_CHANNELS += CYCLIC_CHANNEL_LENGTH + 1;
    final int ENEMY_TROOP_CHANNEL = CYCLIC_CHANNELS += CYCLIC_CHANNEL_LENGTH + 1;
    final int EMERGENCY_CHANNEL = CYCLIC_CHANNELS += CYCLIC_CHANNEL_LENGTH + 1;
    final int NEED_TROOP_CHANNEL = CYCLIC_CHANNELS += CYCLIC_CHANNEL_LENGTH + 1;
    final int SPAWNING_WORKERS_CHANNEL = CYCLIC_CHANNELS += CYCLIC_CHANNEL_LENGTH + 1;
    final int SPAWNING_BARRACKS_CHANNEL = CYCLIC_CHANNELS += CYCLIC_CHANNEL_LENGTH + 1;
    final int SPAWNING_WARRIORS_CHANNEL = CYCLIC_CHANNELS += CYCLIC_CHANNEL_LENGTH + 1;
    final int SPAWNING_ARCHERS_CHANNEL = CYCLIC_CHANNELS += CYCLIC_CHANNEL_LENGTH + 1;
    final int SPAWNING_KNIGHTS_CHANNEL = CYCLIC_CHANNELS += CYCLIC_CHANNEL_LENGTH + 1;
    final int SPAWNING_BALLISTAS_CHANNEL = CYCLIC_CHANNELS += CYCLIC_CHANNEL_LENGTH + 1;


    //This takes 1650 channels
    final int ASSIGNED_ZONES = 50000;

    void InitGame(UnitController _uc, int x, int y) {
        uc = _uc;
        xBase = x;
        yBase = y;
    }

    private int GetZoneChannel(ZoneLocation zone) {
        return ASSIGNED_ZONES + (zone.x + 25) * 33 + (zone.y + 16);

    }

    private int GetZoneChannelFromZone(ZoneLocation zone) {
        return ASSIGNED_ZONES + (zone.x + 25) * 33 + (zone.y + 16);
    }

    void SendZoneAssignedMessage(ZoneLocation zone, int round) {
        int channel = GetZoneChannel(zone);
        ZoneMessage message = new ZoneMessage(zone, uc.getInfo().getID(), round, ZoneType.assigned());
        uc.write(channel, message.Encode());
    }

    void sendZoneInaccessibleMessage(ZoneLocation zone) {
        int channel = GetZoneChannel(zone);
        ZoneMessage message = new ZoneMessage(zone, uc.getInfo().getID(), 2001, ZoneType.inaccessible());
        uc.write(channel, message.Encode());
    }

    ZoneMessage ReadZoneMessage(ZoneLocation zone) {
        int channel = GetZoneChannelFromZone(zone);
        return new ZoneMessage(zone, uc.read(channel));
    }

    boolean isZoneFree(ZoneLocation zone) {
        ZoneMessage zoneMessage = ReadZoneMessage(zone);
//        uc.println("Read zone " + Utils.PrintLoc(zone) + " message. Worker id = " + zoneMessage.workerId + ", last round = " + zoneMessage.round + ", type = " + zoneMessage.type.get());
        return !IsOutOfMap(zone.GetCenter()) && zoneMessage.type.isAccessible() && (zoneMessage.workerId == 0 || uc.getRound() > zoneMessage.round + 5);
    }

    //Send a message in one of the cyclic channels
    private void SendCyclicMessage(int baseChannel, int senderType, int x, int y, int value) {
        if (xBase == Integer.MAX_VALUE) uc.println("ERROR: Tried to send message without first setting the base");
        int lastMessage = uc.read(baseChannel + CYCLIC_CHANNEL_LENGTH);
        CyclicMessage message = new CyclicMessage(senderType, x, y, value);
        int bitmap = message.Encode(xBase, yBase);
        uc.write(baseChannel + lastMessage, bitmap);
        uc.write(baseChannel + CYCLIC_CHANNEL_LENGTH, (lastMessage + 1) % CYCLIC_CHANNEL_LENGTH);
    }

    void SendCyclicMessage(int baseChannel, int senderType, Location location, int value) {
        SendCyclicMessage(baseChannel, senderType, location.x, location.y, value);
    }

    CyclicMessage ReadCyclicMessage(int channel) {
        return new CyclicMessage(uc.read(channel), xBase, yBase);
    }

    boolean IsOutOfMap(Location location) {
        return location.x < uc.read(MIN_X_CHANNEL)
                || location.x > uc.read(MAX_X_CHANNEL)
                || location.y < uc.read(MIN_Y_CHANNEL)
                || location.y > uc.read(MAX_Y_CHANNEL);
    }

    void Increment(int channel) {
        uc.write(channel, uc.read(channel) + 1);
    }

    int GetUnitCount(UnitType type) {
        int count = 0;
        if (type == UnitType.WORKER) count = uc.read(WORKERS_LAST_ROUND_CHANNEL) + uc.read(WORKERS_SPAWNING_COUNT_CHANNEL);
        if (type == UnitType.BARRACKS) count = uc.read(BARRACKS_LAST_ROUND_CHANNEL) + uc.read(BARRACKS_SPAWNING_COUNT_CHANNEL);
        if (type == UnitType.WARRIOR) count = uc.read(WARRIORS_LAST_ROUND_CHANNEL) + uc.read(WARRIORS_SPAWNING_COUNT_CHANNEL);
        if (type == UnitType.ARCHER) count = uc.read(ARCHERS_LAST_ROUND_CHANNEL) + uc.read(ARCHERS_SPAWNING_COUNT_CHANNEL);
        if (type == UnitType.KNIGHT) count = uc.read(KNIGHTS_LAST_ROUND_CHANNEL) + uc.read(KNIGHTS_SPAWNING_COUNT_CHANNEL);
        if (type == UnitType.BALLISTA) count = uc.read(BALLISTAS_LAST_ROUND_CHANNEL) + uc.read(BALLISTAS_SPAWNING_COUNT_CHANNEL);
//        uc.println(count + " units of type " + type);
        return count;
    }
}
