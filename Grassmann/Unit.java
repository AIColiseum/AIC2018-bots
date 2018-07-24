package Grassmann;

import aic2018.*;
import Grassmann.utils.*;

public class Unit {
    UnitController uc;
    AttackController ac;
    MessageController rc;
    MovementController mc;
    PathController pc;
    SpawnController sc;
    Objectives objectives;
    Utils utils;

    Integer round;
    Location myLoc;
    Team myTeam;
    Team theirTeam;
    UnitType myType;
    Location[] enemyInitialLocs, allyInitialLocs;

    void runPreparation() {
        ac = new AttackController(uc);
        rc = new MessageController(uc);
        mc = new MovementController(uc);
        sc = new SpawnController(uc);
        utils = new Utils(uc);

        myTeam = uc.getTeam();
        theirTeam = myTeam.getOpponent();
        myType = uc.getType();
        enemyInitialLocs = theirTeam.getInitialLocations();
        allyInitialLocs = myTeam.getInitialLocations();
    }

    void runTurn() {
        round = uc.getRound();
        myLoc = uc.getLocation();
        Integer vpsToBuy = GameConstants.VICTORY_POINTS_MILESTONE - uc.getTeam().getVictoryPoints();
        if (uc.canBuyVP(vpsToBuy)) uc.buyVP(vpsToBuy);
        objectives = new Objectives(uc);
    }

    void run(UnitController uc) {
        this.uc = uc;
        runPreparation();
        for (; true; uc.yield()) {
            runTurn();
            if (uc.getResources() > 2000) {
                Integer vpsToBuy = (uc.getResources() - 2000) / uc.getVPCost();
                if (uc.canBuyVP(vpsToBuy)) uc.buyVP(vpsToBuy);
            }
        }
    }
}
