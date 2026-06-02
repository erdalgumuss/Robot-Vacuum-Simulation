package model;

/**
 * Robotun ve grid algoritmalarinin kullandigi dort ana yon.
 * <p>
 * Her yon, grid uzerindeki birim adimini (dRow, dCol) ve surekli (piksel
 * tabanli) hareket icin radyan cinsinden acisini (heading) tasir.
 * Boylece hem BFS/A* gibi grid algoritmalari hem de robotun akiskan
 * hareketi ayni enum uzerinden ifade edilebilir.
 */
public enum Direction {
    NORTH(-1, 0, "Kuzey", "↑"),
    EAST(0, 1, "Doğu", "→"),
    SOUTH(1, 0, "Güney", "↓"),
    WEST(0, -1, "Batı", "←");

    private final int dRow;   // satir yondeki birim adim
    private final int dCol;   // sutun yondeki birim adim
    private final String label;
    private final String arrow;

    Direction(int dRow, int dCol, String label, String arrow) {
        this.dRow = dRow;
        this.dCol = dCol;
        this.label = label;
        this.arrow = arrow;
    }

    public int dRow() { return dRow; }
    public int dCol() { return dCol; }
    public String label() { return label; }
    public String arrow() { return arrow; }

    /** Ekrandaki gosterim: "Doğu (→)" */
    public String display() {
        return label + " (" + arrow + ")";
    }

    /**
     * Bu yonun matematiksel acisi (radyan). Ekran koordinatinda y asagi
     * dogru arttigi icin: EAST=0, SOUTH=+pi/2, WEST=pi, NORTH=-pi/2.
     */
    public double angleRadians() {
        return Math.atan2(dRow, dCol);
    }

    /** Saat yonunde 90° donus (EAST -> SOUTH -> WEST -> NORTH -> EAST). */
    public Direction turnRight() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    /** Saat yonunun tersine 90° donus. */
    public Direction turnLeft() {
        return switch (this) {
            case NORTH -> WEST;
            case WEST -> SOUTH;
            case SOUTH -> EAST;
            case EAST -> NORTH;
        };
    }

    /** 180° ters yon. */
    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }
}
