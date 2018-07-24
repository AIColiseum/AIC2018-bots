package XSquare;

import aic2018.Location;
import aic2018.UnitInfo;
import aic2018.UnitType;

/**
 * A utility class that helps encoding/decoding messages from the comm array.
 */
public class Target {
    Location loc;
    int type;
    int value;
    Messaging mes;
    Integer rel = null;

    public Target(){
    }

    public Target(Location _loc, int _type, int _value, Messaging _mes){
        loc = _loc;
        type = _type;
        value = _value;
        mes = _mes;
    }

    public int encode(){
        return mes.encode(type, loc.x, loc.y, value);
    }

    public Target(Messaging _mes, UnitInfo u){
        mes = _mes;
        type = mes.ENEMY;
        loc = u.getLocation();
        if (u.getType() == UnitType.WORKER) value = mes.WORKER;
        else if (u.getType() == UnitType.WARRIOR) value = mes.WARRIOR;
        else if (u.getType() == UnitType.BARRACKS) value = mes.BARRACKS;
        else if (u.getType() == UnitType.KNIGHT) value = mes.KNIGHT;
        else if (u.getType() == UnitType.ARCHER) value = mes.ARCHER;
        else value = mes.BALLISTA;
        int val = 0;
        if (u.getMovementCooldown() > 1) val = 1;
        value = (value << 8) | val;
    }

    int getRelation(UnitType type){
        int enemyType = (value >> 8)&0x7;
        if (rel != null) return rel;
        if (type == UnitType.WARRIOR){
            if (enemyType == mes.KNIGHT) rel = 1;
            else if (enemyType == mes.ARCHER) rel = -1;
        } else if (type == UnitType.KNIGHT){
            if (enemyType == mes.ARCHER || enemyType == mes.BALLISTA) rel = 1;
            else if (enemyType == mes.WARRIOR) rel = -1;
        } else if (type == UnitType.ARCHER){
            if (enemyType == mes.WARRIOR) rel = 1;
            else if (enemyType == mes.KNIGHT) rel = -1;
        }
        if (rel == null) rel = 0;
        return rel;
    }

    boolean isBetterThan(Target B, Location myLoc, UnitType type){
        if (B == null) return true;
        int d1 = myLoc.distanceSquared(loc);
        int d2 = myLoc.distanceSquared(B.loc);

        if (d1 <= mes.attackRangeExpanded && d2 <= mes.attackRangeExpanded){
            int HP1 = value&0xFF;
            int HP2 = B.value&0xFF;
            return HP1 < HP2;
        }

        int r1 = getRelation(type);
        int r2 = B.getRelation(type);

        if (r1 > 0){
            d1 /= 2;
        }
        if (r1 < 0){
            d1 += 5;
            d1 *= 2;
        }

        if (r2 > 0){
            d2/=2;
        }
        if(r2 < 0){
            d2+=5;
            d2*=2;
        }

        return d1 < d2;

    }

    boolean isBetterShootingTarget(Target B){
        if (B == null) return true;

        int type2 = (B.value >> 8)&0x7;
        int type = (value >> 8)&0x7;

        if (type2 == mes.BALLISTA && type != mes.BALLISTA) return true;
        if (type == mes.BALLISTA && type2 != mes.BALLISTA) return false;

        int HP1 = value&0xFF;
        int HP2 = B.value&0xFF;
        return HP1 < HP2;
    }


}
