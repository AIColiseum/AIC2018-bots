package XSquare;

import aic2018.UnitController;
import aic2018.UnitType;

public class UnitPlayer {
	
	public void run(UnitController uc) throws InterruptedException {

		UnitType type = uc.getType();

		try {
			if (type == UnitType.BARRACKS) {
				Barracks barracks = new Barracks();
				barracks.run(uc);
			} else if (type == UnitType.WARRIOR) {
				Warrior warrior = new Warrior();
				warrior.run(uc);
			} else if (type == UnitType.KNIGHT) {
				Knight knight = new Knight();
				knight.run(uc);
			} else if (type == UnitType.ARCHER) {
				Archer archer = new Archer();
				archer.run(uc);
			} else if (type == UnitType.WORKER) {
				Worker worker = new Worker();
				worker.run(uc);
			} else if (type == UnitType.BALLISTA) {
				Ballista ballista = new Ballista();
				ballista.run(uc);
			}
		} catch (Exception e){

		}

	}

}

