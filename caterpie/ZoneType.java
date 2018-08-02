package caterpie;

public class ZoneType {
    int type;

    static int free() { return 1; }

    static int assigned() { return 2; }

    static int inaccessible() { return 3; }

    ZoneType(int type) {
        this.type = type;
    }

    int get() {
        return type;
    }

    boolean isFree() {
        return type == free();
    }

    boolean isAssigned() {
        return type == assigned();
    }

    boolean isInaccessible() {
        return type == inaccessible();
    }

    boolean isAccessible() { return !isInaccessible(); }
}
