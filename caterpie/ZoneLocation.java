package caterpie;

import aic2018.*;

public class ZoneLocation extends Location {
    int xBase;
    int yBase;
    //x and y are the zone coordinates, not the location
    public ZoneLocation(int x, int y, int xBase, int yBase) {
        super(x, y);
        this.xBase = xBase;
        this.yBase = yBase;
    }

    public ZoneLocation add(Direction dir) {
        return new ZoneLocation(super.add(dir).x, super.add(dir).y, xBase, yBase);
    }

    static ZoneLocation fromOffsetCoords(int xOffset, int yOffset, int xBase, int yBase) {
        return new ZoneLocation(
                Math.floorDiv(xOffset, 2),
                Math.floorDiv(yOffset + 1, 3),
                xBase, yBase);
    }

    static ZoneLocation fromLocation(Location location, int xBase, int yBase) {
        return fromOffsetCoords(location.x - xBase, location.y - yBase, xBase, yBase);
    }

    Location GetCenter() {
        return new Location(2 * x + xBase, 3 * y + yBase);
    }

    Location GetOffsettedCenter() {
        return new Location(2 * x, 3 * y);
    }


    Location[] GetZoneTiles() {
        Location center = GetCenter();
        return new Location[]{
                center,
                center.add(Direction.NORTH),
                center.add(Direction.NORTHEAST),
                center.add(Direction.EAST),
                center.add(Direction.SOUTHEAST),
                center.add(Direction.SOUTH)
        };
    }

    boolean IsInside(Location location) {
        return false;
    }
}
