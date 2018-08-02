package caterpie;

import aic2018.*;

public class EnemyInfo {
    //Info obtained through the send enemy troop message
    final Location location;
    final int health;
    final UnitType type;

    public EnemyInfo(CyclicMessage message) {
        this.location = new Location(message.x, message.y);
        this.type = UnitType.values()[message.senderType]; //Not the sender type but the enemy type
        this.health = message.value;
    }


}
