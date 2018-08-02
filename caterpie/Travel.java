package caterpie;

import aic2018.*;

import java.util.HashSet;
import java.util.Set;

public class Travel {
    private final boolean DEBUG = false;

    private final int FREE = 0;
    private final int CLOCKWISE = 1;
    private final int COUNTER_CLOCKWISE = 2;

    private final int OK = 0;
    private final int REPEAT = 1;

    private boolean[] obstacleDirections; //with respect to myLoc

    private UnitController uc;
    private int currentState;
    private Location myLoc;
    private Location target;

    private int minDistObstacleToTarget;
    private Location lastSurroundedObstacle;

    //To encode locations for the hashset. This is where this unit started, its not the same as the xBase, yBase in communication
    private int xBase;
    private int yBase;

    private Set<Integer> visitedLocations; //Positions where I have been since I started surrounding


    public void InitGame(UnitController _uc) {
        uc = _uc;
        currentState = FREE;
        minDistObstacleToTarget = Integer.MAX_VALUE;
        visitedLocations = new HashSet<>();
        xBase = uc.getLocation().x;
        yBase = uc.getLocation().y;
    }

    private void InitTurn(Location _target, Location[] water, TreeInfo[] trees, UnitInfo[] myUnits, UnitInfo[] enemyUnits) {
        myLoc = uc.getLocation();
        if (target != null && target.distanceSquared(_target) > 2) {
            //New target is a new unit, we reset
            if (DEBUG) uc.println("Our target changed, we reset");
            Reset();
        }
        target = _target;
        obstacleDirections = new boolean[8];
        for (Location location : water) {
            if (myLoc.distanceSquared(location) < 3)
                obstacleDirections[myLoc.directionTo(location).ordinal()] = true;
        }
        for (TreeInfo tree : trees) {
            if (tree.oak && myLoc.distanceSquared(tree.location) < 3)
                obstacleDirections[myLoc.directionTo(tree.location).ordinal()] = true;
        }
        for (UnitInfo unit : myUnits) {
            if (unit.getType() == UnitType.BARRACKS && myLoc.distanceSquared(unit.getLocation()) < 3)
                obstacleDirections[myLoc.directionTo(unit.getLocation()).ordinal()] = true;
        }
        for (UnitInfo unit : enemyUnits) {
            if (unit.getType() == UnitType.BARRACKS && myLoc.distanceSquared(unit.getLocation()) < 3)
                obstacleDirections[myLoc.directionTo(unit.getLocation()).ordinal()] = true;
        }
    }

    public void Reset() {
        currentState = FREE;
        minDistObstacleToTarget = Integer.MAX_VALUE;
        lastSurroundedObstacle = null;
        visitedLocations = new HashSet<>();
    }

    /**
     * Bits position in bitmap:
     * <p>
     * - Bit 0: Surrounding direction
     * - Bits 1-4: Direction to last surrounded obstacle
     * - Bits 5-13: y position (with respect to yBase)
     * - Bits 14-22: x position (with respect to xBase)
     */
    private int EncodeLocation() {
        return (((myLoc.x + 127 - xBase) & 0xFF) << 14)
                | (((myLoc.y + 127 - yBase) & 0xFF) << 5)
                | ((myLoc.directionTo(lastSurroundedObstacle).ordinal() & 0xF) << 1)
                | (currentState & 0x1);
    }

    private boolean HaveVisitedLocation() {
        return visitedLocations.contains(EncodeLocation());
    }

    private void AddVisitedLocation() {
        visitedLocations.add(EncodeLocation());
    }

    //todo this
    private int DecideSurroundingState() {
        int minDistClockwise = Integer.MAX_VALUE;
        int minDistCounterClockwise = Integer.MAX_VALUE;
        Direction targetDir = myLoc.directionTo(target);
        for (Direction dir: GetSideDirections(targetDir, /*clockwise*/ true)) {
            if (!uc.canMove(dir)) continue;
            minDistClockwise = myLoc.add(dir).distanceSquared(target);
            break;
        }
        for (Direction dir: GetSideDirections(targetDir, /*clockwise*/ false)) {
            if (!uc.canMove(dir)) continue;
            minDistCounterClockwise = myLoc.add(dir).distanceSquared(target);
            break;
        }
        return minDistClockwise < minDistCounterClockwise ? CLOCKWISE : COUNTER_CLOCKWISE;
    }

    private void SetSurrounding() {
        currentState = DecideSurroundingState();
    }

    private void SetFree() {
        Reset();
    }

    //When we touch the map edge
    private void InvertSurroundingDirection() {
        if (currentState == CLOCKWISE)
            currentState = COUNTER_CLOCKWISE;
        else if (currentState == COUNTER_CLOCKWISE)
            currentState = CLOCKWISE;
    }

    private Direction[] GetSideDirections(Direction dir, boolean clockwise) {
        return clockwise
                ? new Direction[]{
                dir.rotateRight(),
                dir.rotateRight().rotateRight(),
                dir.opposite().rotateLeft()}
                : new Direction[]{
                dir.rotateLeft(),
                dir.rotateLeft().rotateLeft(),
                dir.opposite().rotateRight()};
    }

    private Direction[] GetOrderedDirections(int state, Direction bestDir) {
        if (state == FREE) {
            return new Direction[]{
                    bestDir,
                    bestDir.rotateLeft(),
                    bestDir.rotateRight(),
                    bestDir.rotateLeft().rotateLeft(),
                    bestDir.rotateRight().rotateRight(),
                    bestDir.opposite().rotateRight(),
                    bestDir.opposite().rotateLeft(),
                    bestDir.opposite()
            };
        } else if (state == CLOCKWISE) {
            Direction dir = bestDir;
            Direction[] ret = new Direction[8];
            for (int i = 0; i < 8; i++) {
                ret[i] = dir;
                dir = dir.rotateRight();
            }
            return ret;
        } else {
            Direction dir = bestDir;
            Direction[] ret = new Direction[8];
            for (int i = 0; i < 8; i++) {
                ret[i] = dir;
                dir = dir.rotateLeft();
            }
            return ret;
        }
    }


    private int FreeTravel() {
        if (DEBUG) uc.println("Free travel");
        Direction bestDir = myLoc.directionTo(target);
        if (uc.canMove(bestDir)) {
            //If we can go to the target, we do that
            if (DEBUG) uc.println("Moves in the target direction, " + bestDir);
            uc.move(bestDir);
            return OK;
        } else {
            //We check if we can move in another direction
            Direction[] dirs = GetOrderedDirections(FREE, bestDir);
            for (Direction dir : dirs)
                if (uc.canMove(dir)) {
                    if (DEBUG) uc.println("Moves " + dir + ", best direction was " + bestDir);
                    uc.move(dir);
                } else {
                    if (obstacleDirections[dir.ordinal()]) {
                        //If we hit an obstacle, we set our state to surrounding and repeat
                        if (DEBUG) uc.println("We hit an obstacle " + dir + ", start surrounding");
                        SetSurrounding();
                        lastSurroundedObstacle = myLoc.add(dir);
                        return REPEAT;
                    }
                }
            return OK;
        }
    }

    private int Surround() {
        Direction targetDir = myLoc.directionTo(target);
        if (DEBUG) uc.println("Start surrounding " + (currentState == CLOCKWISE ? "clockwise" : "counterclockwise") + ", target dir is " + targetDir);
        if (HaveVisitedLocation()) {
            //Something went wrong and we completed a loop around the obstacle. We try surrounding again.
            if (DEBUG) uc.println("Oops, we had already visited this location. Restarting.");
            Reset();
            return REPEAT;
        }
        Direction dirToObstacle = myLoc.directionTo(lastSurroundedObstacle);
        if (dirToObstacle == Direction.ZERO) {
            //This can happen if I destroy a tree and go to its position
            if (DEBUG) uc.println("WTF I'm at the location of my obstacle. Restarting.");
            SetFree();
            return REPEAT;
        }
        Direction[] dirsToObstacle = GetOrderedDirections(currentState, dirToObstacle);
        for (int i = 0; i < dirsToObstacle.length; i++) {
            // update closest obstacle to target (to know when we finish surrounding)
            if (!obstacleDirections[i]) continue;
            Location loc = myLoc.add(dirsToObstacle[i]);
            if (loc.distanceSquared(target) < minDistObstacleToTarget) {
                minDistObstacleToTarget = loc.distanceSquared(target);
                if (DEBUG) uc.println("Updates min dist obstacle to target to " + minDistObstacleToTarget + " " + Utils.PrintLoc(loc));
            }
        }
        Direction[] targetDirs = GetOrderedDirections(currentState, targetDir);
        for (int i = 0; i < dirsToObstacle.length; i++) {
            Direction dir = targetDirs[i];
            if (uc.canMove(dir) && myLoc.add(dir).distanceSquared(target) < minDistObstacleToTarget) {
                //if we have finished surrounding, we set state to free
                if (DEBUG) uc.println("I finished surrounding, yay!");
                SetFree();
                uc.move(dir);
                return OK;
            }
        }
        //We have not finished surrounding the obstacle
        if (DEBUG) uc.println("I have not finished surrounding. Dist to target " + myLoc.distanceSquared(target) + ", obstacle to target " + minDistObstacleToTarget);
        for (int i = 0; i < dirsToObstacle.length; i++) {
            Direction dir = dirsToObstacle[i];
            Location newLoc = myLoc.add(dir);
            if (uc.canMove(dir)) {
                if (DEBUG) uc.println("Move " + dir + " to surround.");
                uc.move(dir);
                return OK;
            }
            if (obstacleDirections[dir.ordinal()]) {
                //Update any obstacle that we find as the last one we surrounded
                lastSurroundedObstacle = newLoc;
                if (DEBUG) uc.println("Update surrounded obstacle to " + Utils.PrintLoc(lastSurroundedObstacle));
            }
            if (i <= 3 && uc.isOutOfMap(newLoc)) {
                if (DEBUG) uc.println("Hit the edge of the map. Restarting.");
                //If we find the edge of the map, we invert the surrounding direction and repeat
                InvertSurroundingDirection();
                return REPEAT;
            }
        }
        //Can't move anywhere
        if (DEBUG) uc.println("I can't move anywhere");
        return OK;
    }

    public void TravelTo(Location _target, Location[] waters, TreeInfo[] trees, UnitInfo[] myUnits, UnitInfo[] enemyUnits, boolean shouldStopAtAdjacentTile) {
        if (_target == null) return;
        if (!uc.canMove()) return;
        InitTurn(_target, waters, trees, myUnits, enemyUnits);
        if (shouldStopAtAdjacentTile && myLoc.distanceSquared(_target) < 3) return;
        if (DEBUG) uc.println("===================== " + Utils.PrintLoc(myLoc) + " Travels to target " + Utils.PrintLoc(target) + " at distance " + myLoc.distanceSquared(target));
        int status = REPEAT;
        int tries = 20;
        while (status != OK && tries-- > 0) {
            if (currentState == FREE) {
                status = FreeTravel();
            } else {
                status = Surround();
                if (status == OK && currentState != FREE) {
                    //Add current location to hashset
                    AddVisitedLocation();
                }
            }
        }
    }

    public void TravelTo(Location _target, Location[] waters, TreeInfo[] trees, UnitInfo[] myUnits, UnitInfo[] enemyUnits) {
        TravelTo(_target, waters, trees, myUnits, enemyUnits, true);
    }
}
