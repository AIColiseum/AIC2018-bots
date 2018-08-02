package Felix;

import aic2018.*;

public class UnitPlayer {
  public void run(UnitController uc) throws Exception {
    if (uc.getType() == UnitType.ARCHER) {
      Archer archer = new Archer();
      archer.run(uc);
    } else if (uc.getType() == UnitType.BALLISTA) {
      Ballista ballista = new Ballista();
      ballista.run(uc);
    } else if (uc.getType() == UnitType.BARRACKS) {
      Barracks barracks = new Barracks();
      barracks.run(uc);
    } else if (uc.getType() == UnitType.KNIGHT) {
      Knight knight = new Knight();
      knight.run(uc);
    } else if (uc.getType() == UnitType.WARRIOR) {
      Warrior warrior = new Warrior();
      warrior.run(uc);
    } else if (uc.getType() == UnitType.WORKER) {
      Worker worker = new Worker();
      worker.run(uc);
    } else {
      throw new Exception("Invalid unit type");
    }
  }
}
