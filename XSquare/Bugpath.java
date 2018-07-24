package XSquare;

import aic2018.*;

public class Bugpath {


    /**
     * My implementation of bugpath (ugh it is a bit ugly).
     * Micro is pretty solid but pathfinding does not improve too much vs the public bugpath.
     */

    final int NO_RESET = 0;
    final int SOFT_RESET = 1;
    final int HARD_RESET = 2;

    final double maxCos = 0.5;

    Location obstacle = null;
    Location target = null;
    boolean left = true;
    UnitController uc;
    boolean dodging = false;

    Messaging mes;

    final double factor = 1.5;

    Location minLocation;
    int minDist = 0;

    int[] cont;
    int[] mindist;
    int[] dmg;

    int bestIndex;

    Location closestRanger = null;

    double myDPS;
    int DPS;
    UnitType type;

    Direction[] directions = {
            Direction.NORTH, Direction.NORTHWEST, Direction.WEST, Direction.SOUTHWEST,
            Direction.SOUTH, Direction.SOUTHEAST, Direction.EAST, Direction.NORTHEAST, Direction.ZERO
    };

    public Bugpath(UnitController _uc, Messaging mes){
        uc = _uc;
        DPS = dps(uc.getType());
        type = uc.getType();
        if (Math.random() > 0.5) left = false;
        this.mes = mes;
    }

    void reset(){
        obstacle = null;
        if (target != null){
            minDist = uc.getLocation().distanceSquared(target);
            minLocation = uc.getLocation();
        }
        dodging = false;
    }

    void soft_reset(){
        if (target != null){
            if (minLocation != null)  minDist = minLocation.distanceSquared(target);
            else minDist = uc.getLocation().distanceSquared(target);
        }
    }

    double cosSquared(Location loc1, Location loc2, Location loc3){
        int x1 = loc2.x - loc1.x;
        int y1 = loc2.y - loc1.y;
        int x2 = loc3.x - loc1.x;
        int y2 = loc3.y - loc1.y;

        int prod = (x1*x2 + y1*y2);

        if (prod < 0) return -1;
        if (prod == 0) return 0;

        return ((double)prod*prod)/((x1*x1 + y1*y1)*(x2*x2 + y2*y2));
    }

    int resetType(Location newTarget){
        if (target == null) return HARD_RESET;
        if (target.isEqual(newTarget)) return NO_RESET;
        if (target.distanceSquared(newTarget) <= 8) return SOFT_RESET;
        if (cosSquared(uc.getLocation(), target, newTarget) < maxCos*maxCos) return SOFT_RESET;
        return HARD_RESET;
    }

    boolean moveTo(Location _target){
        if (_target == null) return false;
        int a = resetType(_target);
        if (a == SOFT_RESET){
            target = _target;
            soft_reset();
        } else if (a == HARD_RESET){
            target = _target;
            reset();
        }

        if (target != null && uc.getLocation().distanceSquared(target) < minDist){
            reset();
        }
        return bugPath();
    }

    int getIndex(Direction dir){
        return dir.ordinal();
    }

    boolean isSafe(Direction dir){
        int a = getIndex(dir);
        return !isBetter(a, bestIndex);
    }

    boolean bugPath(){
        Location myLoc = uc.getLocation();
        if (target == null) return false;
        if (uc.getType() != UnitType.WORKER && (uc.getLevel() == 0 || uc.getType() != UnitType.WARRIOR) && myLoc.distanceSquared(target) <= uc.getType().attackRangeSquared && !uc.isObstructed(myLoc, target)){
            if (uc.senseUnit(target) != null && uc.senseUnit(target).getTeam() != uc.getTeam()) return isSafe(Direction.ZERO);
        }

        if (obstacle == null){
            if (greedyMove()) return true;
        }

        Direction dir;
        if (obstacle == null || myLoc.distanceSquared(obstacle) == 0) dir = myLoc.directionTo(target);
        else dir = myLoc.directionTo(obstacle);
        if (!uc.canMove(dir) || obstructed(dir)) {
            dodging = true;
            int c = 0;
            if (obstacle != null && myLoc.distanceSquared(obstacle) > 2){
                int d = myLoc.distanceSquared(obstacle);
                Direction bestDir = Direction.ZERO;
                for (int i = 0; i < 8; ++i){
                    Location newLoc = myLoc.add(dir);
                    int d2 = newLoc.distanceSquared(obstacle);
                    if (uc.canMove(dir) && d2 < d){
                        d = d2;
                        bestDir = dir;
                    }
                    if (left) dir = dir.rotateLeft();
                    else dir = dir.rotateRight();
                }
                if (bestDir != Direction.ZERO) {
                    dir = bestDir;
                    c = 20;
                }
            }
            boolean unitFound = false;
            while (!uc.canMove(dir) && c++ < 20) {
                if (uc.isOutOfMap(myLoc.add(dir))) left = !left;
                Location newLoc = myLoc.add(dir);
                if (uc.senseUnit(newLoc) != null) unitFound = true;
                if (!unitFound) obstacle = newLoc;
                if (left) dir = dir.rotateLeft();
                else dir = dir.rotateRight();
            }
        } else{
            //reset();
            //dodging = false;
        }
        if (dir != Direction.ZERO && uc.canMove(dir) && isSafe(dir)){
            uc.move(dir);
            return true;
        }
        return false;
    }

    boolean greedyMove(){
        //if (uc.getRound() > 1) return false;
        Location myLoc = uc.getLocation();
        Direction dir = myLoc.directionTo(target);
        if (uc.canMove(dir) && isSafe(dir)){
            uc.move(dir);
            return true;
        }

        int dist = uc.getLocation().distanceSquared(target);
        Direction dirR = dir.rotateRight(), dirL = dir.rotateLeft();
        Location newLocR = myLoc.add(dirR);
        Location newLocL = myLoc.add(dirL);
        int distR = newLocR.distanceSquared(target), distL = newLocL.distanceSquared(target);
        if (distR < distL){
            if (distR < dist && uc.canMove(dirR) && isSafe(dirR)){
                uc.move(dirR);
                return true;
            }
            if (distL < dist && uc.canMove(dirL) && isSafe(dirL)){
                uc.move(dirL);
                return true;
            }
        }
        if (distL < dist && uc.canMove(dirL) && isSafe(dirL)){
            uc.move(dirL);
            return true;
        }
        if (distR < dist && uc.canMove(dirR) && isSafe(dirR)){
            uc.move(dirR);
            return true;
        }
        return false;
    }

    int dps (UnitType type){
        if (type == UnitType.WARRIOR) return 6;
        if (type == UnitType.KNIGHT) return 3;
        if (type == UnitType.ARCHER) return 5;
        if (type == UnitType.WORKER) return 2;
        if (type == UnitType.BALLISTA) return 8;
        return 0;
    }

    boolean fightMove(){
        Location loc = uc.getLocation();
        //Integer bc = uc.getBytecode();
        //uc.println(bc.toString());
        closestRanger = null;
        UnitInfo[] units = uc.senseUnits(getRange(), uc.getTeam().getOpponent());
        boolean closecombat = uc.getType() == UnitType.KNIGHT || uc.getType() == UnitType.WARRIOR;
        boolean bee = uc.getType() == UnitType.KNIGHT;
        cont = new int[9];
        mindist = new int[9];
        dmg = new int[9];
        for (int i = 0; i < 9; ++i) mindist[i] = 1000;
        for (int i = 0; i < units.length && uc.getEnergyUsed() < 12000; ++i) {
            if (units[i].getType().getMinAttackRangeSquared() > 0 && (closestRanger == null || loc.distanceSquared(closestRanger) > loc.distanceSquared(units[i].getLocation()))) closestRanger = units[i].getLocation();
            UnitType type = units[i].getType();
            if (type == UnitType.WORKER) continue;
            int dps = dps(type);
            if (dps == 0) continue;
            if (closecombat && (type == UnitType.ARCHER || type == UnitType.BALLISTA)) continue;
            if (closecombat && type != UnitType.ARCHER  && type != UnitType.BALLISTA && units[i].getLocation().distanceSquared(uc.getLocation()) > 13) continue;
            boolean ignoreDist = false;
            if (closecombat && units[i].getLocation().distanceSquared(uc.getLocation()) > 20) ignoreDist = true;
            int ars = getAttackRange(type);
            int arsmin = type.getMinAttackRangeSquared();
            boolean ignoreDmg = (bee && units[i].getAttackCooldown() >= 2);
            for (int j = 0; j < 9; ++j){
                Location newLoc = loc.add(directions[j]);
                Location enemyLoc = units[i].getLocation();
                dmg[j] = Math.max(dmg[j], damage(loc, enemyLoc, units[i]));
                if (uc.isObstructed(newLoc, enemyLoc)) continue;
                int d = newLoc.distanceSquared(enemyLoc);
                if (!ignoreDist && mindist[j] > d) mindist[j] = d;
                if (d <= ars && d < arsmin && !ignoreDmg){
                    cont[j]+= dps;
                }
            }
        }

        bestIndex = 8;
        for (int i = 7; i >= 0; --i){
            if (uc.canMove(directions[i]) && isBetter(bestIndex, i)){
                bestIndex = i;
            }
        }
        //if (uc.getBytecode() > 7500) uc.println("Need more bytecode!!");
        return true;
    }

    int damage(Location myLoc, Location enemyLoc, UnitInfo enemy){
        if(!uc.canAttack()) return 0;
        int d = myLoc.distanceSquared(enemyLoc);
        if (d > type.attackRangeSquared || d < type.minAttackRangeSquared) return 0;
        if (uc.isObstructed(myLoc, enemyLoc)) return 0;
        int factor = 2;
        if (enemy.getType() == UnitType.KNIGHT && enemy.getLevel() >= 1 && d >= GameConstants.SHIELD_RANGE) factor = 1;
        if (d <= GameConstants.WARRIOR_MELEE_RANGE && type == UnitType.WARRIOR && uc.getLevel() >= 1){
            factor *= 2;
        }
        return (type.attack*factor)/(int)type.attackDelay;
    }

    boolean isBetter(int j, int i){
        int prevDPS = cont[j], dps = cont[i];
        int prevDist = mindist[j], dist = mindist[i];
        double myPrevDPS = dmg[j], myDPS = dmg[i];
        if (prevDPS <= DPS && dps > DPS) return false;
        if (prevDPS > DPS && dps <= DPS) return true;

        int ars = uc.getType().getAttackRangeSquared();
        if (!uc.canAttack()) ars = 100;
        if (dist <= ars){
            if (prevDist > ars) return true;
            if (dps-myDPS < prevDPS-myPrevDPS) return true;
            if (dps-myDPS > prevDPS-myPrevDPS) return false;
            return (dist > prevDist);
        }
        if (prevDist <= ars) return false;
        if (dps-myDPS < prevDPS-myPrevDPS) return true;
        if (dps-myDPS > prevDPS-myPrevDPS) return false;
        return (dist < prevDist);
    }

    void safeMove(){
        if (bestIndex != 8){
            uc.move(directions[bestIndex]);
            reset();
        }
    }

    int getRange(){
        UnitType type = uc.getType();
        if (type == UnitType.KNIGHT) return 13;
        return type.getSightRangeSquared(uc.getLevel());
    }

    int getAttackRange(UnitType type){
        return type.getAttackRangeSquared();
    }

    boolean obstructed(Direction dir){
        Location loc = uc.getLocation().add(dir);
        int num = loc.x*2000+loc.y+1;
        for (int i = 0; i < 8; ++i){
            if(uc.read(mes.NEXT_STEP_QUEEN+i) == num) return false;
        }
        return true;
    }

}
