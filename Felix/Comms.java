package Felix;

import aic2018.*;

class Comms {
  UnitController uc;

  final int MAX_ENEMY_LOCATIONS = 20;
  final int MIN_DIST2_BETWEEN_ENEMY_LOCATIONS = 7 * 7;

  final int ENEMY_LOCATIONS_BEGIN_ADDR = 0; // size: 3 * (1 + MAX_ENEMY_LOCATIONS)

  Comms(UnitController uc) {
    this.uc = uc;
  }

  void clearEnemyLocationsOfAge2(int currentRound) {
    int numLocsAddr = ENEMY_LOCATIONS_BEGIN_ADDR + ((currentRound + 1) % 3) * (1 + MAX_ENEMY_LOCATIONS);
    uc.write(numLocsAddr, 0);
  }

  Location[] getEnemyLocationsOfAge1(int currentRound) {
    clearEnemyLocationsOfAge2(currentRound);
    int numLocsAddr = ENEMY_LOCATIONS_BEGIN_ADDR + ((currentRound + 2) % 3) * (1 + MAX_ENEMY_LOCATIONS);
    int numLocs = uc.read(numLocsAddr);
    Location[] locs = new Location[numLocs];
    int addr = numLocsAddr + 1;
    for (int i = 0; i < numLocs; ++i) {
      int encodedLoc = uc.read(addr);
      locs[i] = new Location(encodedLoc&0xffff, encodedLoc>>>16);
      ++addr;
    }
    return locs;
  }

  void markEnemyLocation(int currentRound, int y, int x) {
    int numLocsAddr = ENEMY_LOCATIONS_BEGIN_ADDR + (currentRound % 3) * (1 + MAX_ENEMY_LOCATIONS);
    int numLocs = uc.read(numLocsAddr);
    if (numLocs >= MAX_ENEMY_LOCATIONS) return;
    clearEnemyLocationsOfAge2(currentRound);

    int addr = numLocsAddr + 1;
    for (int i = 0; i < numLocs; ++i) {
      int encodedLoc = uc.read(addr);
      int dy = (encodedLoc>>>16) - y;
      int dx = (encodedLoc&0xffff) - x;
      if (dy * dy + dx * dx < MIN_DIST2_BETWEEN_ENEMY_LOCATIONS) return;
      ++addr;
    }

    int encodedNewLoc = ((y<<16)|x);
    int locAddr = numLocsAddr + 1 + numLocs;
    uc.write(locAddr, encodedNewLoc);
    uc.write(numLocsAddr, numLocs + 1);
  }
}
