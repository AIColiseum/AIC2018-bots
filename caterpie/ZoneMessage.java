package caterpie;

import aic2018.*;

public class ZoneMessage {
    final int workerId; // 1 < id < 10000 so it takes 14 bits
    final int round;
    final ZoneType type;
    final Location offsettedCenter; //OFFSETTED COORDS
    final ZoneLocation zone;


    /**
     * Bits position in bitmap:
     * <p>
     * - Bits 0-15: Worker ID
     * - Bits 16-29: Round
     * - Bits 30-31: Zone type
     */
    private final int idMask = 0x0000FFFF;
    private final int roundMask = 0x3FFF0000;
    private final int typeMask = 0xC0000000;
    private final int roundShift = 16;
    private final int typeShift = 30;

    //_x and _y should be offsetted
    public ZoneMessage(ZoneLocation _zone, int _workerId, int _round, int _type) {
        this.offsettedCenter = _zone.GetOffsettedCenter();
        this.zone = _zone;
        this.type = new ZoneType(_type);
        this.workerId = _workerId;
        this.round = _round;
    }

    public ZoneMessage(ZoneLocation _zone, int bitmap) {
        this.offsettedCenter = _zone.GetOffsettedCenter();
        this.zone = _zone;
        this.type = new ZoneType((bitmap & typeMask) >>> typeShift);
        this.round = (bitmap & roundMask) >>> roundShift;
        this.workerId = bitmap & idMask;
    }

    public int Encode() {
        return ((type.get() & 0x3)) << typeShift
                | ((round & 0xFFFF) << roundShift)
                | (workerId & 0xFFFF);
    }
}
