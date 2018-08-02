package caterpie;

import aic2018.*;

public class Utils {
    static String PrintLoc(Location location) {
        if (location == null) return "Null";
        return "[" + location.x + "," + location.y + "]";
    }

    static boolean SameLocation(Location l1, Location l2) {
        return l1.x == l2.x && l1.y == l2.y;
    }

    static boolean IsTroop(UnitType type) {
        return
                type == UnitType.ARCHER
                || type == UnitType.BALLISTA
                || type == UnitType.KNIGHT
                || type == UnitType.WARRIOR;
    }

    //This is just to store locations as ints in a Treeset
    static int EncodeLocation (Location location) {
        return
                (location.x & 0xFFFF) << 16
                        | (location.y & 0xFFFF);
    }

    static Direction[] GetOrderedDirections (Direction mainDir) {
        return new Direction[]{
                mainDir,
                mainDir.rotateLeft(),
                mainDir.rotateRight(),
                mainDir.rotateLeft().rotateLeft(),
                mainDir.rotateRight().rotateRight(),
                mainDir.opposite().rotateRight(),
                mainDir.opposite().rotateLeft(),
                mainDir.opposite()
        };
    }

    static Location[] getSecondRingAdjacentLocations(Location diff) {
        Location[] adjacentLocations = new Location[0];
        if (diff.x == 2) {
            if (diff.y == 2) {
                adjacentLocations = new Location[] {new Location(1,1)};
            } else if (diff.y == 1) {
                adjacentLocations = new Location[] {new Location(1,0), new Location(1,1)};
            } else if (diff.y == 0) {
                adjacentLocations = new Location[] {new Location(1,-1), new Location(1,0), new Location(1,1)};
            } else if (diff.y == -1) {
                adjacentLocations = new Location[] {new Location(1,-1), new Location(1,0)};
            } else if (diff.y == -2) {
                adjacentLocations = new Location[] {new Location(1,-1)};
            }
        } else if (diff.x == 1) {
            if (diff.y == 2) {
                adjacentLocations = new Location[] {new Location(0,1), new Location(1,1)};
            } else if (diff.y == -2) {
                adjacentLocations = new Location[] {new Location(0,-1), new Location(-1,-1),};
            }

        } else if (diff.x == 0) {
            if (diff.y == 2) {
                adjacentLocations = new Location[] {new Location(-1,1), new Location(0,1), new Location(1,1)};
            } else if (diff.y == -2) {
                adjacentLocations = new Location[] {new Location(-1,-1), new Location(0,-1), new Location(1,-1),};
            }

        } else if (diff.x == -1) {
            if (diff.y == 2) {
                adjacentLocations = new Location[] {new Location(-1,1), new Location(0,1)};
            } else if (diff.y == -2) {
                adjacentLocations = new Location[] {new Location(-1,-1), new Location(0,-1)};
            }

        } else if (diff.x == -2) {
            if (diff.y == 2) {
                adjacentLocations = new Location[] {new Location(-1,1)};
            } else if (diff.y == 1) {
                adjacentLocations = new Location[] {new Location(-1,0), new Location(-1,1)};
            } else if (diff.y == 0) {
                adjacentLocations = new Location[] {new Location(-1,-1), new Location(-1,0), new Location(-1,1)};
            } else if (diff.y == -1) {
                adjacentLocations = new Location[] {new Location(-1,-1), new Location(-1,0)};
            } else if (diff.y == -2) {
                adjacentLocations = new Location[] {new Location(-1,-1)};
            }
        }
        return adjacentLocations;
    }
}
