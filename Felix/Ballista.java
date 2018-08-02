package Felix;

import aic2018.*;

class Ballista {
  void run(UnitController uc) {
    Tactics tactics = new Tactics();

    for (; true; uc.yield()) {
      // pick up victory points from the floor
      tactics.gatherVictoryPoints(uc);

      // buy victory points
      int victoryPointsToBuy = tactics.amountOfVictoryPointsToBuy(uc);
      uc.buyVP(victoryPointsToBuy);

      // move in random direction
      Direction[] ds = Util.randomShuffle(Util.removeElement(Direction.values(), Direction.ZERO));
      for (Direction d : ds) {
        if (uc.canMove(d)) {
          uc.move(d);
          break;
        }
      }
    }
  }
}
