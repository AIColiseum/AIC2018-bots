package XSquare;

import aic2018.*;

import java.util.HashSet;

/**
 * Nasty class... basically reads all messages about enemy locations and broadcasts the enemies seen which have not been broadcasted yet
 * Also it keeps some info about the units it sees (enemy workers, enemy units, units being built, etc)
 * This class also has the main functions of all combat units (attack, gather VP, choose Target, etc)
 */
public class BasicCombatUnit {

    Target bestTarget;
    Location bestTargetLocation;
    int lastMessageRead;
    Messaging mes;
    UnitController uc;
    Target shootBallista;
    boolean closeTarget;
    Location bestVPLocation;
    boolean enemyWorkerNearby;
    boolean enemyNearby;
    int workers, barrackCount;

    BasicCombatUnit(UnitController _uc){
        uc = _uc;
        mes = new Messaging(_uc);
        lastMessageRead = uc.read(mes.MAX_CYCLE);
    }

    void getBestTarget(){
        gatherVPs();
        bestTarget = null;
        Location myLoc = uc.getLocation();
        closeTarget = false;
        shootBallista = null;
        bestVPLocation = null;
        bestTargetLocation = null;
        double bestValue = 0;
        enemyWorkerNearby = false;
        workers = 0; barrackCount = 0;
        enemyNearby = false;

        //goes for VP if round > 150

        if (uc.getRound() > 150 && uc.getType().isCombatUnit()) {

            VictoryPointsInfo[] vps = uc.senseVPs();
            for (VictoryPointsInfo vp : vps) {
                int d = uc.getLocation().distanceSquared(vp.location) + 1;
                float moveDelay = uc.getType().getMovementDelay();
                int walkingTurns = (int) ((float) vp.roundsLeft / moveDelay);
                if (walkingTurns * walkingTurns < 2 * d * d) continue;
                double value = (double) vp.victoryPoints;
                value *= value / d;
                if (value > bestValue) {
                    bestValue = value;
                    bestVPLocation = vp.location;
                }
            }
        }

        //reads enemy locations

        HashSet<Integer> readMessages = new HashSet<>();
        int time = 3500;
        boolean worker = uc.getType() == UnitType.WORKER;
        if (uc.getType() == UnitType.BALLISTA) time = 8000;
        else if (worker) time = 2000;
        while (lastMessageRead != uc.read(mes.MAX_CYCLE) && uc.getEnergyUsed() < time){
            int code = uc.read(lastMessageRead);
            readMessages.add(code);
            Target newTarget = mes.getTarget(code);
            if (newTarget.isBetterThan(bestTarget, myLoc, uc.getType())) bestTarget = newTarget;
            lastMessageRead++;
            if (lastMessageRead >= mes.MAX_CYCLE) lastMessageRead = 0;
            if (uc.getType() == UnitType.BALLISTA){
                if (uc.canAttack(newTarget.loc)){
                    if ((newTarget.value&1) == 1) shootBallista = newTarget;
                }
                if (uc.getLocation().distanceSquared(newTarget.loc) <= 45) closeTarget = true;
            }
        }

        //checks visible units and broadcasts enemies

        UnitInfo[] enemies = uc.senseUnits();

        int contAlly = 0, contEnemy = 0;

        time = 6500;
        for (int i = 0; i < enemies.length && uc.getEnergyUsed() < time; ++i){
            Location loc = enemies[i].getLocation();
            if (uc.isObstructed(myLoc, loc)) continue;
            if (uc.getTeam().isEqual(enemies[i].getTeam())){
                if (enemies[i].getType().isCombatUnit()) ++contAlly;
                else if (worker && enemies[i].getType() == UnitType.WORKER){
                    if (!mes.cm.isOnHisCell(enemies[i])){
                        ++workers;
                    }
                }
                if (enemies[i].getType() == UnitType.BARRACKS){
                    if (enemies[i].getConstructionTurns() > 0) {
                        reportUnit(UnitType.BARRACKS, false);
                    }
                    ++barrackCount;
                }
                continue;
            }
            if (enemies[i].getType().isCombatUnit()){
                ++contEnemy;
                enemyNearby = true;
            }
            else if (enemies[i].getType() == UnitType.WORKER){
                enemyWorkerNearby = true;
                //--cont;
            }
            //else --cont;
            Target newTarget = new Target(mes, enemies[i]);
            if (newTarget.isBetterThan(bestTarget, myLoc, uc.getType())) bestTarget = newTarget;
            int code = newTarget.encode();
            if (!readMessages.contains(code)) {
                mes.sendMessage(code);
                reportUnit(enemies[i].getType(), true);
            }
        }
        if (uc.getType().isCombatUnit()){
            ++contAlly;
        }
        if (enemyWorkerNearby && contEnemy == 0) contEnemy = 1;
        if (contEnemy > 0 && contAlly <= contEnemy + 2){
            mes.sendAlert();  //OUTNUMBERED!!! :(
        }
        reportUnit(uc.getType(), false);
        lastMessageRead = uc.read(mes.MAX_CYCLE);
        if (bestTarget != null) bestTargetLocation = bestTarget.loc;
        if (bestVPLocation != null) bestTargetLocation = bestVPLocation;
    }

    //counts total number of units seen between all units (both enemies [estimate] and mine)
    void reportUnit(UnitType type, boolean enemy){
        int channel = 10*(uc.getRound()%mes.TROOP_MEMORY);
        if (type == UnitType.WARRIOR) {
            channel += mes.WARRIOR;
        } else if (type == UnitType.KNIGHT){
            channel += mes.KNIGHT;
        } else if (type == UnitType.ARCHER){
            channel += mes.ARCHER;
        } else if (type == UnitType.BARRACKS){
            channel += mes.BARRACKS;
        } else if (type == UnitType.WORKER){
            channel += mes.WORKER;
        } else if (type == UnitType.BALLISTA){
            channel += mes.BALLISTA;
        }
        if (enemy) channel += mes.FIRST_ENEMY_TROOP_MESSAGE;
        else channel += mes.FIRST_TROOP_MESSAGE;
        int mes = uc.read(channel);
        uc.write(channel, mes+1);
    }

    /**
     * Tries to attack an enemy small tree or an enemy
     **/
    void tryAttack(){
        if (!uc.canAttack()) return;
        int bestIndex = -1;
        float leastHP = 1000;
        TreeInfo[] trees = uc.senseTrees();
        int atk = uc.getType().getAttack(uc.getLevel());
        for (TreeInfo tree : trees){
            if (uc.getEnergyUsed() > 10000) break;
            if (!uc.canAttack(tree)) continue;
            if (tree.getHealth() <= atk){
                if (!mes.cm.isOccupied(tree.location)) {
                    uc.attack(tree);
                    return;
                }
            }
        }
        UnitInfo[] units = uc.senseUnits(uc.getType().getAttackRangeSquared(), uc.getTeam().getOpponent());
        for (int i = 0; i < units.length; ++i) {
            if(!uc.canAttack(units[i])) continue;
            int hp = units[i].getHealth();
            if (!uc.getType().isCombatUnit()) hp += 600;
            if (hp < leastHP){
                leastHP = hp;
                bestIndex = i;
            }
        }
        if (bestIndex >= 0) uc.attack(units[bestIndex]);
    }

    void tryAttackTree(){
        if (uc.getType().isCombatUnit()) return;
        if (!uc.canAttack()) return;
        int bestIndex = -1;
        double leastDurability = 10000;
        TreeInfo[] rocks = uc.senseTrees(uc.getType().getAttackRangeSquared());
        for (int i = 0; i < rocks.length; ++i) {
            if (!rocks[i].isOak()){
                if (mes.isOccupied(rocks[i].location)) continue;
            }
            if(!uc.canAttack(rocks[i])) continue;
            if (rocks[i].getHealth() < leastDurability && rocks[i].getHealth() > 0){
                leastDurability = rocks[i].getHealth();
                bestIndex = i;
            }
        }
        if (bestIndex >= 0) uc.attack(rocks[bestIndex]);
    }

    Location getEnemyLocation(boolean enemy){
        if (enemy){
            return uc.getOpponent().getInitialLocations()[0];
        }
        return null;
    }

    void gatherVPs(){
        VictoryPointsInfo[] VP = uc.senseVPs(2);
        for (int i = 0; i < VP.length; ++i){
            uc.gatherVPs(uc.getLocation().directionTo(VP[i].location));
        }
    }

}
