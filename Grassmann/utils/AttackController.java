package Grassmann.utils;

import aic2018.*;

public class AttackController {

	private UnitController uc;

	public AttackController(UnitController _uc) {
		uc = _uc;
	}

	public UnitInfo getAttackUnit(UnitInfo[] units) {
		if (!uc.canAttack()) return null;
		int minHealth = 99999;
		UnitInfo minUnit = null;
		for (UnitInfo unit : units) {
			if (!unit.getTeam().isEqual(uc.getTeam()) && uc.canAttack(unit)) {
				if (unit.getHealth() < minHealth) {
					minUnit = unit;
					minHealth = unit.getHealth();
				}
			}
		}
		return minUnit;
	}

	public UnitInfo getAttackUnit() {
		return getAttackUnit(uc.senseUnits(uc.getTeam().getOpponent()));
	}

	public UnitInfo attack(UnitInfo[] units) {
		UnitInfo unit = getAttackUnit(units);
		if (unit != null) uc.attack(unit);
		return unit;
	}

	public UnitInfo attack() {
		return attack(uc.senseUnits(uc.getTeam().getOpponent()));
	}

}
