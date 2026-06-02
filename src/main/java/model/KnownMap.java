package model;

/**
 * Robotun <b>kendi öğrendiği</b> iç haritası (belief / occupancy map).
 * <p>
 * Ortamdan (gerçek {@link Room}) tamamen bağımsızdır: başta her hücre
 * {@link Belief#UNKNOWN}'dur ve YALNIZCA sensör gözlemleriyle güncellenir
 * (hiçbir ışının dokunmadığı hücre asla yazılmaz). Robotun gerçekçi moddaki
 * tüm kararları bu haritaya dayanır; gerçek odaya bakmaz.
 */
public class KnownMap {

    public enum Belief { UNKNOWN, FREE, OBSTACLE }

    private final int rows;
    private final int cols;
    private final Belief[][] belief;
    private final boolean[][] cleaned;
    private final boolean[][] dirtSeen;
    private final boolean[][] carpetSeen;

    public KnownMap(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.belief = new Belief[rows][cols];
        this.cleaned = new boolean[rows][cols];
        this.dirtSeen = new boolean[rows][cols];
        this.carpetSeen = new boolean[rows][cols];
        for (Belief[] rowArr : belief) {
            java.util.Arrays.fill(rowArr, Belief.UNKNOWN);
        }
    }

    public int rows() { return rows; }
    public int cols() { return cols; }

    public boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    public Belief at(int r, int c) {
        return inBounds(r, c) ? belief[r][c] : Belief.OBSTACLE;
    }

    public boolean isFree(int r, int c) { return at(r, c) == Belief.FREE; }
    public boolean isKnown(int r, int c) { return at(r, c) != Belief.UNKNOWN; }

    /** FREE yalnızca daha önce OBSTACLE işaretlenmediyse yazılır (engel kalıcıdır). */
    public void markFree(int r, int c) {
        if (inBounds(r, c) && belief[r][c] != Belief.OBSTACLE) {
            belief[r][c] = Belief.FREE;
        }
    }

    public void markObstacle(int r, int c) {
        if (inBounds(r, c)) {
            belief[r][c] = Belief.OBSTACLE;
        }
    }

    public boolean isCleaned(int r, int c) { return inBounds(r, c) && cleaned[r][c]; }
    public void markCleaned(int r, int c) {
        if (inBounds(r, c)) {
            cleaned[r][c] = true;
            dirtSeen[r][c] = false;
        }
    }

    public boolean isDirtSeen(int r, int c) { return inBounds(r, c) && dirtSeen[r][c]; }
    public void markDirtSeen(int r, int c) {
        if (inBounds(r, c)) {
            dirtSeen[r][c] = true;
        }
    }
    public void clearDirtSeen(int r, int c) {
        if (inBounds(r, c)) {
            dirtSeen[r][c] = false;
        }
    }

    public boolean isCarpetSeen(int r, int c) { return inBounds(r, c) && carpetSeen[r][c]; }
    public void markCarpet(int r, int c) {
        if (inBounds(r, c)) {
            carpetSeen[r][c] = true;
        }
    }
}
