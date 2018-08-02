package caterpie;

import aic2018.*;

public abstract class Troop extends Attacker {
    private int initialMessageEnemyTroop = 0;

    @Override
    protected void InitTurn() {
        super.InitTurn();
    }

    ///////////////////// PICK ATTACK TARGET


    //who is best to attack
    private UnitInfo GetBestUnit(UnitInfo A, UnitInfo B) {
        if (A == null) return B;
        if (B == null) return A;

        //return one with most priority
        int typePointsA = TypePoints(A.getType());
        int typePointsB = TypePoints(B.getType());
        if (typePointsA > typePointsB) return A;
        else if (typePointsA < typePointsB) return B;

        //return first one to die
        int nhitsA = A.getHealth() / myType.attack + ((A.getHealth() % myType.attack == 0) ? 0 : 1);
        int nhitsB = B.getHealth() / myType.attack + ((B.getHealth() % myType.attack == 0) ? 0 : 1);

        if (nhitsA < nhitsB) return A;
        else if (nhitsA > nhitsB) return B;

        // return closest one
        return myLoc.distanceSquared(A.getLocation()) < myLoc.distanceSquared(B.getLocation()) ? A : B;
    }

    void AttackTroop() {
        if (!uc.canAttack()) return;
//        uc.println("Troop enters attack, " + enemyUnits.length + " enemies");
        UnitInfo bestUnit = null;
        for (UnitInfo unitInfo : enemyUnits) {
            if (uc.canAttack(unitInfo)) {
                bestUnit = GetBestUnit(bestUnit, unitInfo);
//                uc.println("Updates best target to " + Utils.PrintLoc(bestUnit.getLocation()));
            }
        }
//        uc.println("Best unit to attack: " + (bestUnit == null ? null : Utils.PrintLoc(bestUnit.getLocation())));
        if(bestUnit != null) uc.attack(bestUnit);
    }

    void AttackTree(boolean first) {
        if (!uc.canAttack()) return;
        if (enemyUnits.length > 0 && first) return;
        if (myType.attackDelay > 1) return;
        for (TreeInfo tree: trees) {
            if (tree.oak && uc.canAttack(tree)){
                uc.attack(tree);
                return;
            }
        }

    }

    //todo attack trees
    @Override
    protected void Attack(boolean first) {
        AttackTroop();
        AttackTree(first);
    }

    ///////////////////// PICK ATTACK TARGET


    ///////////////////// PICK MOVE TARGET

    Location GetEnemyTroopInSight() {
        for (UnitInfo enemy: enemyUnits) {
            if (Utils.IsTroop(enemy.getType())) return enemy.getLocation();
        }
        return null;
    }

    EnemyInfo[] GetReportedEnemyTroops() {
//        if (initialMessageEnemyTroop != 325436564) return null;
        int baseChannel = comm.ENEMY_TROOP_CHANNEL;
        int lastMessage = uc.read(baseChannel + comm.CYCLIC_CHANNEL_LENGTH);
        int i = initialMessageEnemyTroop;
        int troopCount = lastMessage - initialMessageEnemyTroop;
        if (troopCount < 0) troopCount += comm.CYCLIC_CHANNEL_LENGTH;
        EnemyInfo[] enemies = new EnemyInfo[troopCount];
        int arrayIndex = 0;
        while (i != lastMessage) {
            if (i >= comm.CYCLIC_CHANNEL_LENGTH) i -= comm.CYCLIC_CHANNEL_LENGTH; //this should go at the end
            if (i == lastMessage) break;
            CyclicMessage message = comm.ReadCyclicMessage(baseChannel + i);
            enemies[arrayIndex++] = new EnemyInfo(message);
            i++;
        }
        initialMessageEnemyTroop = lastMessage;
        return enemies;
    }

    Location GetEnemyTroopMessage() {
//        if (initialMessageEnemyTroop != 325436564) return null;
        int baseChannel = comm.ENEMY_TROOP_CHANNEL;
        int lastMessage = uc.read(baseChannel + comm.CYCLIC_CHANNEL_LENGTH);
        int i = initialMessageEnemyTroop;
        Location closestEnemy = null;
        int closestEnemyDist = Integer.MAX_VALUE;
        int MAX_BYTECODE = 5000;
        int initBytecode = uc.getEnergyUsed();
        while (i != lastMessage) {
            if (i >= comm.CYCLIC_CHANNEL_LENGTH) i -= comm.CYCLIC_CHANNEL_LENGTH; //this should go at the end
            if (i == lastMessage) break;
            if (uc.getEnergyUsed() - initBytecode > MAX_BYTECODE) break;
            CyclicMessage message = comm.ReadCyclicMessage(baseChannel + i);
            Location enemyLocation = new Location(message.x, message.y);
//            uc.println("Location from message " + Utils.PrintLoc(enemyLocation));
            readEnemyLocations.add(Utils.EncodeLocation(enemyLocation));
            int distanceToEnemy = myLoc.distanceSquared(enemyLocation);
            if (distanceToEnemy < closestEnemyDist) {
                closestEnemyDist = distanceToEnemy;
                closestEnemy = enemyLocation;
            }
            i++;
        }
        initialMessageEnemyTroop = lastMessage;
        if (closestEnemy != null) {
//            uc.println("Goes to help a nearby troop at " + Utils.PrintLoc(closestEnemy));
            return closestEnemy;
        }
        return null;
    }

    Location GetEnemyUnitInSight() {
        return enemyUnits.length == 0 ? null : enemyUnits[0].getLocation();
    }

    protected void UpdateMoveTarget() {
//        uc.println("Updates move target");
        Location enemyTroopInSight = GetEnemyTroopInSight();
        if (enemyTroopInSight != null) {
            target = enemyTroopInSight;
            return;
        }
        Location enemyTroopInMessage = GetEnemyTroopMessage();
        int distToMessageTroop = enemyTroopInMessage == null ? Integer.MAX_VALUE : myLoc.distanceSquared(enemyTroopInMessage);

        Location enemyUnitInSight = GetEnemyUnitInSight();
        if (enemyUnitInSight != null && distToMessageTroop > 200){
            target = enemyUnitInSight;
            return;
        }
        if (enemyTroopInMessage != null) {
            target = enemyTroopInMessage;
            return;
        }
        target = enemyTeam.getInitialLocations()[0];
    }

    ///////////////////// PICK MOVE TARGET


    ///////////////////// PICK COMBAT MOVE

    boolean moreAllies(Location location) {
        //allies and enemies at dist <= 8
        int nallies = 0;
        int nenemies = 0;
        for (UnitInfo ally: myUnits) {
            if (!Utils.IsTroop(ally.getType())) continue;
            if (ally.isInConstruction()) continue;
            if (location.distanceSquared(ally.getLocation()) < 9) nallies++;
        }
        for (UnitInfo enemy: enemyUnits) {
            if (!Utils.IsTroop(enemy.getType())) continue;
            if(enemy.isInConstruction()) continue;
            if (location.distanceSquared(enemy.getLocation()) < 9) nenemies++;
        }
        return nallies + 1 > nenemies; //+1 because we don't count ourselves
    }

    int minDistToEnemy(Location location) {
        if (enemyUnits.length == 0) return 100;
        return location.distanceSquared(enemyUnits[0].getLocation());
    }

    double dpsReceived(Location location) {
        double dps = 0;
        for (UnitInfo enemy: enemyUnits) {
            if (!Utils.IsTroop(enemy.getType())) continue;
            if (enemy.isInConstruction()) continue;
            if (location.distanceSquared(enemy.getLocation()) > enemy.getType().attackRangeSquared) continue;
            dps += (double) enemy.getType().attack / (double) enemy.getType().attackDelay;
        }
        return dps;
    }

    boolean canAttackEnemy(Location location) {
        if (!uc.canAttack()) return false;
        for (UnitInfo enemy: enemyUnits) {
            if (location.distanceSquared(enemy.getLocation()) <= myType.attackRangeSquared) return true;
        }
        return false;
    }

    abstract double TileValue(Location location);


    private void MoveCombat() {
        if (!uc.canMove()) return;
        double maxValue = Double.NEGATIVE_INFINITY;
        Direction maxValueDir = null;
        for (Direction dir: Direction.values()) {
            if (!uc.canMove(dir)) continue;
            double value = TileValue(myLoc.add(dir));
//            uc.println(Utils.PrintLoc(myLoc.add(dir)) + " direction " + dir + " has value " + value);
            if (value > maxValue) {
                maxValue = value;
                maxValueDir = dir;
            }
        }
        if (maxValueDir != null && uc.canMove(maxValueDir)) uc.move(maxValueDir);
    }

    protected void Move() {
        if (enemyUnits.length > 0) {
            MoveCombat();
        }else {
            travel.TravelTo(target, waters, trees, myUnits, enemyUnits);
        }
    }

    ///////////////////// PICK COMBAT MOVE


    ///////////////////// USE ACTIVE

    private void UseActive() {
        for (UnitInfo enemy: enemyUnits) {
            if (Utils.IsTroop(enemy.getType()) && uc.canUseActiveAbility(enemy.getLocation()))
                uc.useActiveAbility(enemy.getLocation());
        }
    }

    @Override
    void ExecuteSpecialMechanics() {
//        if (uc.canUseActiveAbility()) UseActive();
    }

    ///////////////////// USE ACTIVE



}
