package XSquare;

import aic2018.Direction;
import aic2018.UnitController;
import aic2018.UnitInfo;
import aic2018.UnitType;

/**
 * Barracks class, if a unit broadcasts that it is being outnumbered it produces knights/archers/ballistas (no warriors! they suck :/ )
 * It starts producing only knights but then it starts building archers at a faster pace (see combatType())
 */
public class Barracks {

    Bugpath bugpath;
    int contUnit;
    int mine_index;
    Messaging mes;
    UnitController uc;
    ConstructionQueue queue;
    final int MINARCHERS = 30;

    Direction[] directions = Direction.values();

    UnitType getRanged(){
        int archers = mes.getArcherCount(), ballistas = mes.getBallistaCount();
        if (archers <= ballistas + MINARCHERS) return UnitType.ARCHER;
        return UnitType.BALLISTA;
    }

    UnitType combatType(){
        int knights = mes.getKnightCount(), archers = mes.getArcherCount(), ballistas = mes.getBallistaCount();
        if (knights <= 5) return UnitType.KNIGHT;
        if ((knights-5)*6 <= (archers + ballistas)*5) return UnitType.KNIGHT;
        return getRanged();
    }

    void cleanChannels(){
        int nextRoundMod = 10*((uc.getRound()+1)%mes.TROOP_MEMORY);
        for (int i = 0; i < 8; ++i){
            uc.write(nextRoundMod+i + mes.FIRST_ENEMY_TROOP_MESSAGE, 0);
            uc.write(nextRoundMod+i + mes.FIRST_TROOP_MESSAGE, 0);
        }
    }

    void run(UnitController _uc) {

        uc = _uc;
        contUnit = 0;
        mine_index = 0;
        BasicCombatUnit basicCombatUnit = new BasicCombatUnit(uc);
        mes = basicCombatUnit.mes;
        bugpath = new Bugpath(uc, mes);
        queue = new ConstructionQueue(uc, mes);


        while (true) {


            basicCombatUnit.getBestTarget();
            UnitType type = combatType();

            if (queue.canBuildTroop()){

                if (mes.getTroopCount2() == 0 || (basicCombatUnit.bestTarget != null && basicCombatUnit.bestTarget.loc.distanceSquared(uc.getLocation()) <= 200)) {

                    if ((type == UnitType.ARCHER || type == UnitType.BALLISTA) && enemiesNear()) type = UnitType.KNIGHT;

                    int index = uc.getLocation().directionTo(uc.getOpponent().getInitialLocations()[0]).ordinal();
                    for (int i = 0; i < 8; ++i) {
                        if (uc.canSpawn(directions[(index + i) % 8], type)) {
                            uc.spawn(directions[(index + i) % 8], type);
                            queue.add(0);
                            ++contUnit;
                        }
                    }
                }
            }

            queue.check();

            cleanChannels();
            basicCombatUnit.getBestTarget();
            basicCombatUnit.mes.putVisibleLocations();

            mes.buyVPs();
            uc.println("VPS: " + uc.getTeam().getVictoryPoints());
            uc.println("BOUGHT VPS: " + uc.read(mes.VP_CHANNEL));

            // End turn
            uc.yield();
        }
    }


    boolean enemiesNear(){
        UnitInfo[] enemies = uc.senseUnits(25, uc.getOpponent());
        for (int i = 0; i < enemies.length; ++i){
            if (enemies[i].getType() == UnitType.WARRIOR) return true;
            if (enemies[i].getType() == UnitType.KNIGHT) return true;
            if (enemies[i].getType() == UnitType.ARCHER) return true;
            if (enemies[i].getType() == UnitType.BALLISTA) return true;
        }
        return false;
    }

}
