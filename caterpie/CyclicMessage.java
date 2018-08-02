package caterpie;

import aic2018.*;

public class CyclicMessage {
    final int senderType;
    final int x; //real coordinates, not offsetted
    final int y;
    final int value;

    /**
     * Bits position in bitmap:
     *
     * - Bits 0-11: Encoded value
     * - Bits 12-20: y position (with respect to yBase)
     * - Bits 21-28: x position (with respect to xBase)
     * - Bits 29-31: type of unit sending the message
     */
    private final int typeMask = 0xF0000000; //at most 15
    private final int xOffMask = 0x0FF00000; //at most 255
    private final int yOffMask = 0x000FF000; //at most 255
    private final int typeShift = 28;
    private final int xOffShift = 20;
    private final int yOffShift = 12;
    private final int valueMask = 0x00000FFF; // at most 4095

    public CyclicMessage(int _senderType, int _x, int _y, int _value) {
        this.senderType = _senderType;
        this.x = _x;
        this.y = _y;
        this.value = _value;
    }

    public CyclicMessage(int bitmap, int xBase, int yBase) {
        this.senderType = (bitmap & typeMask) >>> typeShift;
        this.x = ((bitmap & xOffMask) >>> xOffShift) - 127 + xBase;
        this.y = ((bitmap & yOffMask) >>> yOffShift) - 127 + yBase;
        this.value = (bitmap & valueMask);

    }

    public int Encode(int xBase, int yBase) {
        return
                ((senderType & 0xF) << typeShift)
                        | (((x + 127 - xBase) & 0xFF) << xOffShift) /*-50 <= iOffset <= 50*/
                        | (((y + 127 - yBase) & 0xFF) << yOffShift)
                        | (value & 0xFFF);
    }

    @Override
    public String toString() {
        return "CyclicMessage[SenderType: " + senderType + ", Location: " + Utils.PrintLoc(new Location(x, y)) + ", Value: " + value + "]";
    }
}
