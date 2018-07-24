package Grassmann;

import aic2018.*;

public class UnitPlayer {

	public void run(UnitController uc) throws InterruptedException {
		UnitType type = uc.getType();
		Unit unit = null;
		if (type == UnitType.WORKER) unit = new Worker();
		else if (type == UnitType.BARRACKS) unit = new Barracks();
		else if (type == UnitType.WARRIOR) unit = new WarriorKnight();
		else if (type == UnitType.ARCHER) unit = new Archer();
		else if (type == UnitType.KNIGHT) unit = new WarriorKnight();
		else if (type == UnitType.BALLISTA) unit = new Ballista();
		unit.run(uc);
	}
}
