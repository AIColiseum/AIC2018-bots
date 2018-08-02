package caterpie;

import aic2018.*;

public class TileInfo {
    UnitInfo unit;
    boolean water;
    TreeInfo tree;

    public TileInfo() { }

    TileInfo Add(UnitInfo _unit) {
        unit = _unit;
        return this;
    }

    TileInfo Add(boolean _water) {
        water = _water;
        return this;
    }

    TileInfo Add(TreeInfo _tree) {
        tree = _tree;
        return this;
    }
}
